
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double FINE_PER_DAY = 1.0;

    public boolean isBookAvailable(int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT available FROM books WHERE id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("available");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Book getBookFromRecord(int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT id, title, author, category, isbn, available FROM books WHERE id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("category"),
                    rs.getString("isbn"),
                    rs.getBoolean("available")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Book> executeBookSearch(String query, String param) {
        List<Book> books = new ArrayList<>();
        try {
            Connection con = DBconnection.getConnection();
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, "%" + param + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                books.add(new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("category"),
                    rs.getString("isbn"),
                    rs.getBoolean("available")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    public List<Book> searchByTitle(String title) {
        return executeBookSearch("SELECT id, title, author, category, isbn, available FROM books WHERE title LIKE ?", title);
    }

    public List<Book> searchByAuthor(String author) {
        return executeBookSearch("SELECT id, title, author, category, isbn, available FROM books WHERE author LIKE ?", author);
    }

    public List<Book> searchByCategory(String category) {
        return executeBookSearch("SELECT id, title, author, category, isbn, available FROM books WHERE category LIKE ?", category);
    }

    public List<Book> searchByIsbn(String isbn) {
        return executeBookSearch("SELECT id, title, author, category, isbn, available FROM books WHERE isbn LIKE ?", isbn);
    }

    public boolean issueBook(int studentId, int bookId) {
        try {
            if (!isBookAvailable(bookId)) {
                ReservationService rs = new ReservationService();
                rs.addToReservation(studentId, bookId);
                System.out.println("Book not currently available; added to reservation queue.");
                return false;
            }

            Connection con = DBconnection.getConnection();
            LocalDateTime issueDateTime = LocalDateTime.now();
            LocalDateTime dueDateTime = issueDateTime.plusDays(LOAN_PERIOD_DAYS);

            String query = "INSERT INTO issued_books(student_id, book_id, issue_date, due_date, status) VALUES (?, ?, ?, ?, 'issued')";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            ps.setTimestamp(3, Timestamp.valueOf(issueDateTime));
            ps.setTimestamp(4, Timestamp.valueOf(dueDateTime));
            ps.executeUpdate();

            String update = "UPDATE books SET available=false WHERE id=?";
            PreparedStatement ps2 = con.prepareStatement(update);
            ps2.setInt(1, bookId);
            ps2.executeUpdate();

            System.out.println("Book issued! Due date: " + dueDateTime.toLocalDate());

            // Update in-memory model availability state if provided
            try {
                Book book = getBookFromRecord(bookId);
                if (book != null) {
                    book.setAvailable(false);
                }
            } catch (Exception ex) {
                // non-critical, DB state is authoritative
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public double calculateFine(int studentId, int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT issue_date, due_date, return_date, status FROM issued_books WHERE student_id=? AND book_id=? ORDER BY issue_date DESC LIMIT 1";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return 0.0;
            }

            LocalDateTime dueDate = null;
            Timestamp dueTimestamp = rs.getTimestamp("due_date");
            if (dueTimestamp != null) {
                dueDate = dueTimestamp.toLocalDateTime();
            }

            if (dueDate == null) {
                LocalDateTime issueDate = rs.getTimestamp("issue_date").toLocalDateTime();
                dueDate = issueDate.plusDays(LOAN_PERIOD_DAYS);
            }

            Timestamp returnTimestamp = rs.getTimestamp("return_date");
            LocalDateTime effectiveDate = returnTimestamp != null ? returnTimestamp.toLocalDateTime() : LocalDateTime.now();

            String status = rs.getString("status");
            if ("returned".equalsIgnoreCase(status) && returnTimestamp == null) {
                return 0.0;
            }

            long overdueDays = Duration.between(dueDate, effectiveDate).toDays();
            if (overdueDays <= 0) {
                return 0.0;
            }
            return overdueDays * FINE_PER_DAY;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public IssueRecord getLatestIssueRecord(int studentId, int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT id, student_id, book_id, issue_date, due_date, return_date, status FROM issued_books WHERE student_id=? AND book_id=? ORDER BY issue_date DESC LIMIT 1";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return null;
            }

            int id = rs.getInt("id");
            LocalDateTime issueDateTime = rs.getTimestamp("issue_date").toLocalDateTime();
            LocalDateTime dueDateTime = rs.getTimestamp("due_date") != null ? rs.getTimestamp("due_date").toLocalDateTime() : null;
            LocalDateTime returnDateTime = rs.getTimestamp("return_date") != null ? rs.getTimestamp("return_date").toLocalDateTime() : null;
            String status = rs.getString("status");

            double fine = calculateFine(studentId, bookId);
            IssueRecord record = new IssueRecord(
                    id,
                    studentId,
                    bookId,
                    new Student(studentId, null, null),
                    new Book(bookId, null, null, null, null, isBookAvailable(bookId)),
                    issueDateTime.toLocalDate(),
                    dueDateTime != null ? dueDateTime.toLocalDate() : issueDateTime.toLocalDate().plusDays(LOAN_PERIOD_DAYS),
                    returnDateTime != null ? returnDateTime.toLocalDate() : null,
                    fine,
                    status
            );
            return record;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean issueBook(Student student, Book book) {
        if (student == null || book == null) {
            throw new IllegalArgumentException("Student and Book must not be null");
        }

        boolean issued = issueBook(student.getId(), book.getId());
        if (issued) {
            book.setAvailable(false);
        }
        return issued;
    }

    public boolean returnBook(int studentId, int bookId) {
        try {
            Connection con = DBconnection.getConnection();

            String updateIssued = "UPDATE issued_books SET status='returned', return_date=NOW() WHERE student_id=? AND book_id=? AND status='issued'";
            PreparedStatement ps = con.prepareStatement(updateIssued);
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                System.out.println("No matching issued record found for return.");
                return false;
            }

            String updateBook = "UPDATE books SET available=true WHERE id=?";
            PreparedStatement ps2 = con.prepareStatement(updateBook);
            ps2.setInt(1, bookId);
            ps2.executeUpdate();

            IssueRecord issued = getLatestIssueRecord(studentId, bookId);
            if (issued != null) {
                System.out.println("Issue Record due date: " + issued.getDueDate());
            }

            double fine = calculateFine(studentId, bookId);
            if (fine > 0) {
                System.out.printf("Book returned successfully. Overdue fine: $%.2f\n", fine);
            } else {
                System.out.println("Book returned successfully. No overdue fine.");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean returnBook(Student student, Book book) {
        if (student == null || book == null) {
            throw new IllegalArgumentException("Student and Book must not be null");
        }

        boolean returned = returnBook(student.getId(), book.getId());
        if (returned) {
            book.setAvailable(true);
        }
        return returned;
    }

    public boolean returnBook(int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT student_id FROM issued_books WHERE book_id=? AND status='issued' ORDER BY issue_date DESC LIMIT 1";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("No currently issued record found for book " + bookId);
                return false;
            }

            int studentId = rs.getInt("student_id");
            return returnBook(studentId, bookId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkAvailability(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book must not be null");
        }
        return isBookAvailable(book.getId());
    }
}