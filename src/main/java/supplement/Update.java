package supplement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import backend.Channel;
import backend.FTSIndex;
import backend.Video;

public class Update {
    private final String url;
    private final String fileDirectory;
    private final String indexPath;
    private final Connection conn;
    private final FTSIndex index;

    public Update(String url, String fileDirectory, String indexPath, Connection conn) throws SQLException {
        this.url = url;
        this.fileDirectory = fileDirectory;
        this.indexPath = indexPath;
        this.conn = conn;
        this.index = new FTSIndex(indexPath);
    }

    public void updateAllChannels() {
        String channelsQuery = "SELECT * FROM Channels";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(channelsQuery);
            List<Channel> channels = new ArrayList<>();

            while (rs.next()) {
                String channelName = rs.getString("channelName");
                String channelID = rs.getString("channelID");
                String URL = rs.getString("URL");
                int numVideos = rs.getInt("numVideos");

                Channel channel = new Channel(channelName, URL, fileDirectory, numVideos);
                System.out.println("Updating Channel: " + channelName);
                channels.add(channel);
                updateChannel(channel);
            }
        } catch (Exception e) {
            System.err.println("Error updating all channels: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateChannel(String channelID) {
        String channelQuery = "SELECT * FROM Channels WHERE channelID = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(channelQuery);
            pstmt.setString(1, channelID);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String channelName = rs.getString("channelName");
                String URL = rs.getString("URL");
                int numVideos = rs.getInt("numVideos");

                Channel channel = new Channel(channelName, URL, fileDirectory, numVideos);
                System.out.println("Updating Channel: " + channelName);
                updateChannel(channel);
            }
        } catch (Exception e) {
            System.err.println("Error updating channel " + channelID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateChannel(Channel channel) throws Exception {
        List<String> newVideos = channel.fetchData();
        System.out.println(newVideos.size() + " new videos for " + channel.channelName);

        if (!newVideos.isEmpty()) {
            for (String newVideo : newVideos) {
                Video video = new Video(channel.channelName, newVideo, fileDirectory);
                video.transcribe(conn);
                video.index(index, conn);
            }
        }

        // Update the channel's video count
        channel.countVideos(conn);
        channel.updateDB(conn);
    }

    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db";
        String fileDirectory = "Data/Files";
        String indexPath = "Data/FTSIndex.json";

        try (Connection conn = DriverManager.getConnection(url)) {
            Update updater = new Update(url, fileDirectory, indexPath, conn);

            if (args.length > 0) {
                // If channelID is provided, update only that channel
                updater.updateChannel(args[0]);
            } else {
                // Otherwise update all channels
                updater.updateAllChannels();
            }

        } catch (Exception e) {
            System.err.println("Error in Update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}