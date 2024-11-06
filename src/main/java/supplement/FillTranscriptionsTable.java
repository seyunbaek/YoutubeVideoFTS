package supplement;

import java.sql.*;

import backend.FTSIndex;
import backend.Video;


public class FillTranscriptionsTable {
    public static void main(String[] args) throws SQLException {
        String url = "jdbc:sqlite:Data/database.db";
        String fileDirectory = "Data/Files";
        String selectVideosQuery = "SELECT channel, videoID FROM Videos WHERE transcribed = 0";
        String indexPath = "Data/FTSIndex.json";
        String updateVideoQuery = "UPDATE Videos SET transcribed = ?, numLines = ? WHERE videoID = ?";
        String insertTranscriptionQuery = "INSERT INTO Transcriptions (videoID, startTime, endTime, text, indexed) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                FTSIndex index = new FTSIndex(indexPath, conn);
                // Step 1: Retrieve all untranscribed videos from the Videos table
                try (PreparedStatement selectStmt = conn.prepareStatement(selectVideosQuery);
                     ResultSet rs = selectStmt.executeQuery()) {

                    while (rs.next()) {
                        String channelName = rs.getString("channel");
                        String videoID = rs.getString("videoID");

                        // Step 2: Create Video instance and transcribe
                        Video video = new Video(channelName, videoID, conn, fileDirectory);
                        video.transcribe();
                        System.out.println(video);
                    }
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                }
            }
        }
    }
}
