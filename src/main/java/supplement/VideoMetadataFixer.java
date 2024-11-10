package supplement;

import java.sql.*;

public class VideoMetadataFixer {
    private static final String DB_URL = "jdbc:sqlite:Data/database.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Get all videos
            String selectVideosQuery = "SELECT videoID FROM Transcriptions WHERE indexed = 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectVideosQuery);

            // Prepare update statement
            String updateVideoQuery = """
                UPDATE Videos 
                SET numLines = ?
                WHERE videoID = ?
            """;
            String updateTranscriptionQuery = "UPDATE Transcriptions SET indexed = ? WHERE videoID = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateVideoQuery);
            PreparedStatement updateTStmt = conn.prepareStatement(updateTranscriptionQuery);

            // For each video
            while (rs.next()) {
                String videoID = rs.getString("videoID");
                updateTStmt.setBoolean(1, true);
                updateTStmt.setString(2, videoID);
                updateTStmt.executeUpdate();
//
//                // Count transcriptions for this video
//                String countQuery = "SELECT COUNT(*) as lineCount FROM Transcriptions WHERE videoID = ?";
//                PreparedStatement countStmt = conn.prepareStatement(countQuery);
//                countStmt.setString(1, videoID);
//                ResultSet countRs = countStmt.executeQuery();
//
//                if (countRs.next()) {
//                    int lineCount = countRs.getInt("lineCount");
//
//                    // Update video record
//                    updateStmt.setInt(1, lineCount);
//                    updateStmt.setString(2, videoID);
//                    updateStmt.executeUpdate();
//
//                    updateTStmt.setBoolean(1, true);
//                    updateTStmt.setString(2, videoID);
//                    updateTStmt.executeUpdate();
//
//                    System.out.printf("Updated video %s: %d lines%n", videoID, lineCount);
//                }
//
//                countRs.close();
//                countStmt.close();
            }

            System.out.println("Video metadata update completed!");

        } catch (SQLException e) {
            System.err.println("Error updating video metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }
}