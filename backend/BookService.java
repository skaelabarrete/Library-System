package backend;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookService {

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

    public void issueBook(int studentId, int bookId) {
        try {
            Connection con = DBconnection.getConnection();

            if (isBookAvailable(bookId)) {
                String query = "INSERT INTO issued_books(student_id, book_id, issue_date, status) VALUES (?, ?, NOW(), 'issued')";
                PreparedStatement ps = con.prepareStatement(query);
                ps.setInt(1, studentId);
                ps.setInt(2, bookId);
                ps.executeUpdate();

                String update = "UPDATE books SET available=false WHERE id=?";
                PreparedStatement ps2 = con.prepareStatement(update);
                ps2.setInt(1, bookId);
                ps2.executeUpdate();

                System.out.println("Book issued!");
            } else {
                ReservationService rs = new ReservationService();
                rs.addToReservation(studentId, bookId);
                System.out.println("Added to reservation queue.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}