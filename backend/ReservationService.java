
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReservationService {

    public void addToReservation(int studentId, int bookId) {
        try {
            Connection con = DBconnection.getConnection();

            String countQuery = "SELECT COUNT(*) FROM reservations WHERE book_id=?";
            PreparedStatement ps = con.prepareStatement(countQuery);
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            int position = 1;
            if (rs.next()) {
                position = rs.getInt(1) + 1;
            }

            String insert = "INSERT INTO reservations(student_id, book_id, queue_position, status) VALUES (?, ?, ?, 'waiting')";
            PreparedStatement ps2 = con.prepareStatement(insert);
            ps2.setInt(1, studentId);
            ps2.setInt(2, bookId);
            ps2.setInt(3, position);
            ps2.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
