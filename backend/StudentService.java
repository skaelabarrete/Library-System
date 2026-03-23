import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentService {
    private final BookService bookService = new BookService();

    public List<IssueRecord> viewIssuedBooks(int studentId) {
        List<IssueRecord> issued = new ArrayList<>();
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT id, book_id, issue_date, due_date, return_date, status FROM issued_books WHERE student_id=? AND status='issued'";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int bookId = rs.getInt("book_id");
                LocalDate issueDate = rs.getTimestamp("issue_date").toLocalDateTime().toLocalDate();
                LocalDate dueDate = rs.getTimestamp("due_date").toLocalDateTime().toLocalDate();
                Timestamp returnTs = rs.getTimestamp("return_date");
                LocalDate returnDate = returnTs != null ? returnTs.toLocalDateTime().toLocalDate() : null;
                String status = rs.getString("status");

                IssueRecord record = new IssueRecord(id, studentId, bookId,
                        new Student(studentId, null, null),
                        bookService.getBookFromRecord(bookId),
                        issueDate,
                        dueDate,
                        returnDate,
                        calculateFine(dueDate, returnDate),
                        status);
                issued.add(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issued;
    }

    public List<IssueRecord> viewAllIssuedBooks(int studentId) {
        List<IssueRecord> issued = new ArrayList<>();
        try {
            Connection con = DBconnection.getConnection();
            String query = "SELECT id, book_id, issue_date, due_date, return_date, status FROM issued_books WHERE student_id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int bookId = rs.getInt("book_id");
                LocalDate issueDate = rs.getTimestamp("issue_date").toLocalDateTime().toLocalDate();
                LocalDate dueDate = rs.getTimestamp("due_date").toLocalDateTime().toLocalDate();
                Timestamp returnTs = rs.getTimestamp("return_date");
                LocalDate returnDate = returnTs != null ? returnTs.toLocalDateTime().toLocalDate() : null;
                String status = rs.getString("status");

                IssueRecord record = new IssueRecord(id, studentId, bookId,
                        new Student(studentId, null, null),
                        bookService.getBookFromRecord(bookId),
                        issueDate,
                        dueDate,
                        returnDate,
                        calculateFine(dueDate, returnDate),
                        status);
                issued.add(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issued;
    }

    public List<IssueRecord> viewOverdueBooks(int studentId) {
        List<IssueRecord> overdue = new ArrayList<>();
        for (IssueRecord record : viewIssuedBooks(studentId)) {
            if (isOverdue(record)) {
                overdue.add(record);
            }
        }
        return overdue;
    }

    public boolean isOverdue(IssueRecord record) {
        if (record == null || record.getDueDate() == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return record.getReturnDate() == null && record.getDueDate().isBefore(now);
    }

    private double calculateFine(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null) {
            return 0.0;
        }

        LocalDate actual = returnDate != null ? returnDate : LocalDate.now();
        long overdueDays = java.time.Duration.between(dueDate.atStartOfDay(), actual.atStartOfDay()).toDays();
        if (overdueDays <= 0) {
            return 0.0;
        }

        return overdueDays * 1.0; // 1 unit per day
    }

    public double calculateFine(IssueRecord issueRecord) {
        if (issueRecord == null || issueRecord.getDueDate() == null) {
            return 0.0;
        }

        LocalDate dueDate = issueRecord.getDueDate();
        LocalDate returnDate = issueRecord.getReturnDate();
        LocalDate actual = returnDate != null ? returnDate : LocalDate.now();

        if (!actual.isAfter(dueDate)) {
            return 0.0;
        }

        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(dueDate, actual);
        return overdueDays * 1.0;
    }

    public void printIssuedBooks(int studentId) {
        List<IssueRecord> list = viewIssuedBooks(studentId);
        System.out.println("Issued books for student " + studentId + ":");
        for (IssueRecord record : list) {
            System.out.println(record);
        }
    }

    public void printOverdueStatus(int studentId) {
        List<IssueRecord> overdue = viewOverdueBooks(studentId);
        if (overdue.isEmpty()) {
            System.out.println("No overdue books for student " + studentId);
            return;
        }
        System.out.println("Overdue books for student " + studentId + ":");
        for (IssueRecord record : overdue) {
            System.out.printf("Book %s is overdue, due date %s, fine $%.2f\n", record.getBook() != null ? record.getBook().getTitle() : record.getBookId(), record.getDueDate(), record.getFine());
        }
    }
}