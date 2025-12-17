import java.sql.Connection;
import java.sql.DriverManager;

class DBConnection {
    public static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/sms_db";
            String user = "root";
            String pass = "6969";   

            return DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
