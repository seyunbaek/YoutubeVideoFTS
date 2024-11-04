package operators;

import java.sql.*;
import java.io.File;
import backend.Channel;
import backend.Video;

public class FillVideosTable {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/Test/testDB.db";
        String fileDirectory = "Data/Test";
        String query = "SELECT channelName, channelID, URL, numVideos FROM Channels";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Loop through each channel in the database
            while (rs.next()) {
                String channelName = rs.getString("channelName");
                String channelID = rs.getString("channelID");
                String URL = rs.getString("URL");

                // Create a Channel object and print its details
                Channel channel = new Channel(channelName, channelID, URL, url, fileDirectory);
                System.out.println(channel);
                // Access the 'transcriptions' folder for the channel
                File transcriptionsDir = new File(fileDirectory + "/" + channelName + "/transcriptions");

                // Check if the transcriptions directory exists
                if (transcriptionsDir.exists() && transcriptionsDir.isDirectory()) {
                    // List all .vtt files in the transcriptions directory
                    File[] vttFiles = transcriptionsDir.listFiles((dir, name) -> name.endsWith(".vtt"));

                    if (vttFiles != null) {
                        for (File vttFile : vttFiles) {
                            // Get the video ID by excluding the .vtt extension from the file name
                            String videoID = vttFile.getName().replace(".vtt", "");

                            // Create an instance of Video for each VTT file
                            Video video = new Video(channelName, videoID, url, fileDirectory);
                            System.out.println("Processing video: " + video);

                            // Use the shared connection to update or add the video in the database
                            video.updateDB(conn);
                        }
                    } else {
                        System.out.println("No .vtt files found in the transcriptions folder for channel: " + channelName);
                    }
                } else {
                    System.out.println("Transcriptions directory does not exist for channel: " + channelName);
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}