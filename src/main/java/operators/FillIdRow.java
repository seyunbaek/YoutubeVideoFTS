package operators;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FillIdRow {

    public static void main(String[] args) {
        // Replace with your actual database URL, username, and password if needed
        String url = "jdbc:sqlite:Data/Test/testDB.db";  // Update path as necessary

        // Establish connection, update id with rowid, and handle any exceptions
        try (Connection conn = DriverManager.getConnection(url)) {
            // SQL statement to update the id column with rowid values
            String updateSQL = "UPDATE Transcriptions SET id = rowid";

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                int affectedRows = pstmt.executeUpdate();
                System.out.println("Updated " + affectedRows + " rows with rowid values.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Error updating id column with rowid values.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection failed.");
        }
    }
}
