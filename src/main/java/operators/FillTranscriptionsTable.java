package operators;

import java.sql.*;
import java.util.List;
import backend.Video;


public class FillTranscriptionsTable {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db";
        String fileDirectory = "Data/Files";
        String selectVideosQuery = "SELECT channel, videoID FROM Videos WHERE transcribed = 0";
        String updateVideoQuery = "UPDATE Videos SET transcribed = ?, numLines = ? WHERE videoID = ?";
        String insertTranscriptionQuery = "INSERT INTO Transcriptions (videoID, startTime, endTime, text, indexed) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                // Step 1: Retrieve all untranscribed videos from the Videos table
                try (PreparedStatement selectStmt = conn.prepareStatement(selectVideosQuery);
                     ResultSet rs = selectStmt.executeQuery()) {

                    while (rs.next()) {
                        String channelName = rs.getString("channel");
                        String videoID = rs.getString("videoID");

                        // Step 2: Create Video instance and transcribe
                        Video video = new Video(channelName, videoID, url, fileDirectory);
                        List<String[]> transcriptions = video.transcribe();

                        // Step 3: Update transcribed status and numSentences in the Videos table
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateVideoQuery)) {
                            updateStmt.setBoolean(1, video.transcribed);
                            updateStmt.setInt(2, video.numLines);
                            updateStmt.setString(3, video.videoID);
                            updateStmt.executeUpdate();
                        }

                        // Step 4: Insert each line of transcription into the Transcriptions table
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertTranscriptionQuery)) {
                            for (String[] transcription : transcriptions) {
                                String startTime = transcription[0];
                                String endTime = transcription[1];
                                String text = transcription[2];

                                // Convert startTime and endTime to integer seconds (rounded)
                                int startSeconds = (int) Math.round(Double.parseDouble(startTime.replace("s", "")));
                                int endSeconds = (int) Math.round(Double.parseDouble(endTime.replace("s", "")));

                                insertStmt.setString(1, videoID);
                                insertStmt.setInt(2, startSeconds);
                                insertStmt.setInt(3, endSeconds);
                                insertStmt.setString(4, text);
                                insertStmt.setBoolean(5, false);
                                insertStmt.addBatch(); // Use batch processing for efficiency
                            }
                            insertStmt.executeBatch();
                            System.out.println("Inserted transcriptions for videoID: " + videoID);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}


