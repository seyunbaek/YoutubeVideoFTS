package test;

import backend.Transcription;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class OffRangeCounter {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db"; // Update with your database path
        String query = "SELECT *, rowid FROM Transcriptions WHERE (endTime - startTime) > 50";

        int count = 0;

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("VideoIDs with time range greater than 50 seconds:");
            while (rs.next()) {
                String videoID = rs.getString("videoID");
                int startTime = rs.getInt("startTime");
                int endTime = rs.getInt("endTime");
                String text = rs.getString("text");
                int transcriptionID = rs.getInt("rowid");
                Transcription transcription = new Transcription(videoID, transcriptionID, startTime, endTime, text);
                System.out.print("(Range: " + (endTime-startTime) + ")");
                System.out.println(transcription);
                count++;
            }

            System.out.println("Total count: " + count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}