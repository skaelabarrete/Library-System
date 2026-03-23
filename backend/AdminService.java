import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminService {

    private final BookService bookService = new BookService();

    public boolean addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        try {
            Connection con = DBconnection.getConnection();
            String sql = "INSERT INTO books(title, author, category, isbn, available) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getCategory());
            ps.setString(4, book.getIsbn());
            ps.setBoolean(5, book.isAvailable());
            int count = ps.executeUpdate();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeBook(int bookId) {
        try {
            Connection con = DBconnection.getConnection();
            String sql = "DELETE FROM books WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, bookId);
            int count = ps.executeUpdate();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Book> viewAllBooks() {
        List<Book> books = new ArrayList<>();
        try {
            Connection con = DBconnection.getConnection();
            String sql = "SELECT id, title, author, category, isbn, available FROM books";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("category"),
                    rs.getString("isbn"),
                    rs.getBoolean("available")
                );
                books.add(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    public List<IssueRecord> viewAllIssuedBooks() {
        List<IssueRecord> records = new ArrayList<>();
        try {
            Connection con = DBconnection.getConnection();
            String sql = "SELECT id, student_id, book_id, issue_date, due_date, return_date, status FROM issued_books";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int studentId = rs.getInt("student_id");
                int bookId = rs.getInt("book_id");
                LocalDate issueDate = rs.getTimestamp("issue_date").toLocalDateTime().toLocalDate();
                LocalDate dueDate = rs.getTimestamp("due_date").toLocalDateTime().toLocalDate();
                LocalDate returnDate = null;
                if (rs.getTimestamp("return_date") != null) {
                    returnDate = rs.getTimestamp("return_date").toLocalDateTime().toLocalDate();
                }
                String status = rs.getString("status");
                Book book = bookService.getBookFromRecord(bookId);

                IssueRecord record = new IssueRecord(
                    id,
                    studentId,
                    bookId,
                    new Student(studentId, null, null),
                    book,
                    issueDate,
                    dueDate,
                    returnDate,
                    bookService.calculateFine(studentId, bookId),
                    status
                );
                records.add(record);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    public Student searchStudentById(int studentId) {
        try {
            Connection con = DBconnection.getConnection();
            String sql = "SELECT id, name, email FROM students WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Student(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
