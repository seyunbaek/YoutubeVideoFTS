package supplement;

import backend.Video;
import backend.FTSIndex;

import java.sql.*;

public class IndexTranscriptions {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db";
        String fileDirectory = "Data/Files";
        String indexPath = "Data/FTSIndex.json";

        String selectVideoQuery = "SELECT channel, videoID FROM Videos WHERE indexed = 0";
        try (Connection conn = DriverManager.getConnection(url)){
            FTSIndex index = new FTSIndex(indexPath, conn);
            try (PreparedStatement selectStmt = conn.prepareStatement(selectVideoQuery);
                 ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String channel = rs.getString("channel");
                    String videoID = rs.getString("videoID");
                    Video video = new Video(channel, videoID, conn, fileDirectory);
                    System.out.println(video);
                    video.index(index);
                }
                index.updateIndex();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

