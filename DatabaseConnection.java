import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/EventManagement";
    private static final String USER = "root";
    private static final String PASSWORD = "12345678";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Database Connection Successful!");
            return conn;
        } catch (SQLException e) {
            System.out.println("❌ Database Connection Failed!");
            e.printStackTrace(); // Print exact error
            return null;
        }
    }
}
