package test;

import backend.Channel;
import backend.FTSIndex;
import backend.Video;
import me.tongfei.progressbar.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import operators.Operators;

public class Main {

    public static void main(String[] args) throws Exception {

        String DB_URL = "jdbc:sqlite:Data/database.db";
        String FILE_DIRECTORY = "Data/Files";
        String indexPath = "Data/FTSIndex.json";

        Connection conn = DriverManager.getConnection(DB_URL);
        FTSIndex index = new FTSIndex(indexPath);
        Operators manager = new Operators(conn, index);
        manager.addChannel("The Rusty Bits", "https://www.youtube.com/@therustybits");


//        String channelQuery = "SELECT * FROM Channels";
//        PreparedStatement pstmt = conn.prepareStatement(channelQuery);
////        pstmt.setString(1, "@3blue1brown");
//        ResultSet rs = pstmt.executeQuery();
//
//        while (rs.next()) {
//            String channelName = rs.getString("channelName");
//            String URL = rs.getString("URL");
//            int numVideos = rs.getInt("numVideos");
//
//            Channel channel = new Channel(channelName, URL, FILE_DIRECTORY, conn, numVideos);
//            System.out.println(channel.toString());
//            List<String> newVideos = channel.fetchData();
//            System.out.println(newVideos.size() + " new videos for " + channel.channelName);
//            if (!newVideos.isEmpty()) {
//                for (String newVideo : newVideos) {
//                    Video video = new Video(channel.channelName, newVideo, conn, FILE_DIRECTORY);
//                    video.transcribe();
//                    video.index(index);
//                }
//            }
//
//            channel.countVideos();
//            channel.updateDB();
//        }
    }
}
