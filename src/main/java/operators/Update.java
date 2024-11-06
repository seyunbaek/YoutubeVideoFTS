package operators;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import backend.Channel;
import backend.FTSIndex;
import backend.Video;

public class Update {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db";
        String fileDirectory = "Data/Files";
        String indexPath = "Data/FTSIndex.json";
        String channelsQuery = "SELECT * FROM Channels";

        try (Connection conn = DriverManager.getConnection(url)){
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(channelsQuery);

            FTSIndex index = new FTSIndex(indexPath, conn);
            List<Channel> channels = new ArrayList<>();
            // Loop through each channel in the database
            while (rs.next()) {
                String channelName = rs.getString("channelName");
                String channelID = rs.getString("channelID");
                String URL = rs.getString("URL");
                int numVideos = rs.getInt("numVideos");

                // Create a Channel object and print its details
                Channel channel = new Channel(channelName, channelID, URL, fileDirectory, conn, numVideos);
                System.out.println("Updating Channel: " + channelName);
                channels.add(channel);

                List<String> newVideos = channel.fetchData();
                System.out.println(newVideos.size() + " new videos for " + channel.channelName);
                if (!newVideos.isEmpty()) {
                    for (String newvideo:newVideos) {
                        Video video = new Video(channel.channelName, newvideo, conn, fileDirectory);
                        video.transcribe();
                        video.index(index);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
