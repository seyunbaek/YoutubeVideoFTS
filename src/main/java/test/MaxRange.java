package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MaxRange {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db"; // Update with your database path
        String query = "SELECT videoID, startTime, endTime, text, (endTime - startTime) AS range FROM Transcriptions ORDER BY range DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                String videoID = rs.getString("videoID");
                int startTime = rs.getInt("startTime");
                int endTime = rs.getInt("endTime");
                String text = rs.getString("text");
                int range = rs.getInt("range");

                System.out.println("Row with the largest time range:");
                System.out.println("VideoID: " + videoID);
                System.out.println("Start Time: " + startTime);
                System.out.println("End Time: " + endTime);
                System.out.println("Text: " + text);
                System.out.println("Time Range: " + range + " seconds");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}