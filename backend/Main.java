package backend;

import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Connection con = DBconnection.getConnection();
        if (con != null) {
            System.out.println("Connected to library_db!");
        } else {
            System.out.println("Failed to connect to library_db!");
        }

        BookService bs = new BookService();
        bs.issueBook(1, 1); // test issue book
    }
}