//import jdk.incubator.vector.VectorOperators;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    private static String url;
    private static String user;
    private static String pass;
    TestConnection() {
        this.url = "jdbc:mysql://localhost:3306/EventManagement";
        this.user = "root";
        this.pass = "12345678";
    }
    public void testConn() {
        try {
            Connection conn = DriverManager.getConnection(url,user,pass);

            if (conn != null) {
                System.out.println("✅ Database Connection Successful!");
                conn.close(); // Close after testing
            } else {
                System.out.println("❌ Database Connection Failed!");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Print full error message
        }
    }
}
