package test;

import backend.Transcription;
import backend.Video;
import backend.FTSIndex;

import java.sql.*;
import java.util.List;

public class TestIndex {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/Test/testDB.db";
        String fileDirectory = "Data/Test";
        String indexPath = "Data/Test/FTSIndex.json";

        String selectVideoQuery = "SELECT channel, videoID FROM Videos WHERE indexed = 0";
        try (Connection conn = DriverManager.getConnection(url)){
            FTSIndex index = new FTSIndex(indexPath, conn);
            try (PreparedStatement selectStmt = conn.prepareStatement(selectVideoQuery);
                 ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String channel = rs.getString("channel");
                    String videoID = rs.getString("videoID");
                    Video video = new Video(channel, videoID, conn, fileDirectory);
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
