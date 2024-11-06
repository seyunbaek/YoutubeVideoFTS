package supplement;

import backend.FTSIndex;
import backend.Video;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class AppendVideos {
    private static String url = "jdbc:sqlite:Data/database.db";
    private static String fileDirectory = "Data/Files";

    public static void main(String[] args) {
//        String archivePath = "Data/Files/Marques Brownlee/archive.txt";
        String indexPath = "Data/FTSIndex.json";
        String selectVideo = "SELECT * FROM Videos WHERE channel='Marques Brownlee'";

        try (Connection conn = DriverManager.getConnection(url)) {
            FTSIndex index = new FTSIndex(indexPath, conn);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectVideo);
            while (rs.next()) {
                String vidoeID = rs.getString("videoID");
                Video video = new Video("Marques Brownlee", vidoeID, conn, fileDirectory);
                video.transcribe();
                video.index(index);
                System.out.println(video);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
