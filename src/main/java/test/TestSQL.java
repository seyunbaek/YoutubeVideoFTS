package test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestSQL {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db"; // Replace with the path to your SQLite database

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                String query = "SELECT channelName FROM Channels";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {

                    while (rs.next()) {
                        String channelName = rs.getString("channelName");
                        System.out.println(channelName);
                    }
                } catch (SQLException e) {
                    System.out.println("Error while retrieving data: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}