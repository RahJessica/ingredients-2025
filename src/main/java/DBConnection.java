import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public Connection getConnection() {
        try {
            String JDBC_URL = System.getenv("JDBC_URL"); //
            String user = System.getenv("USER");
            String password = System.getenv("PASSWORD");
            return DriverManager.getConnection(JDBC_URL, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
