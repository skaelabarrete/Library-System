import java.time.LocalDate;

public class IssueRecord {
    private int id;
    private int studentId;
    private int bookId;
    private Student student;
    private Book book;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fine;
    private String status; 

    public IssueRecord(int id, int studentId, int bookId, Student student, Book book,
                       LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fine, String status) {
        this.id = id;
        this.studentId = studentId;
        this.bookId = bookId;
        this.student = student;
        this.book = book;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
        this.status = status;
    }

    public IssueRecord(int id, int studentId, int bookId, LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fine, String status) {
        this(id, studentId, bookId, null, null, issueDate, dueDate, returnDate, fine, status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFine() {
        return fine;
    }

    public void setFine(double fine) {
        this.fine = fine;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "IssueRecord{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", bookId=" + bookId +
                ", student=" + (student != null ? student.getName() : "null") +
                ", book=" + (book != null ? book.getTitle() : "null") +
                ", issueDate=" + issueDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", fine=" + fine +
                ", status='" + status + '\'' +
                '}';
    }
}