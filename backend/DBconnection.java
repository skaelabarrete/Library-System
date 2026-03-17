package backend;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBconnection {
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            String url = "jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC";
            String user = "root"; 
            String password = ""; 
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}