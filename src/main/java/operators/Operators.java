package operators;

import java.io.File;
import java.sql.*;
import java.util.List;

import backend.Channel;
import backend.FTSIndex;
import backend.Video;
import org.json.JSONArray;
import org.json.JSONObject;
import supplement.Update;

public class Operators {
    private static final String DB_URL = "jdbc:sqlite:Data/database.db";
    private static final String FILE_DIRECTORY = "Data/Files";
    private static final String INDEX_PATH = "Data/FTSIndex.json";
    private final FTSIndex INDEX;
    private final Connection conn;

    public Operators(Connection conn, FTSIndex index) {
        this.conn = conn;
        this.INDEX = index;
    }

    // Get all channels
    public String getAllChannels() {
        JSONArray channels = new JSONArray();
        String query = "SELECT * FROM Channels";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                JSONObject channel = new JSONObject();
                channel.put("channelName", rs.getString("channelName"));
                channel.put("channelID", rs.getString("channelID"));
                channel.put("URL", rs.getString("URL"));
                channel.put("numVideos", rs.getInt("numVideos"));
                channels.put(channel);
            }

        } catch (SQLException e) {
            System.out.println("Error getting channels: " + e.getMessage());
            return new JSONArray().toString();
        }

        return channels.toString();
    }

    // Add new channel
    public JSONObject addChannel(String channelName, String url) {
        JSONObject response = new JSONObject();

        try {
            // Create new channel instance
            Channel channel = new Channel(channelName, url, FILE_DIRECTORY, conn, 0);

            // Fetch data will set up directories and create archive
            List<String> newVideos = channel.fetchData();

            // Update the channel in database
            channel.updateDB();

            // Process any new videos found
            if (!newVideos.isEmpty()) {
                for (String videoId : newVideos) {
                    Video video = new Video(channel.channelName, videoId, conn, FILE_DIRECTORY);
                    video.transcribe();
                    video.index(INDEX);
                }
            }

            // Update video count and DB
            channel.countVideos();
            channel.updateDB();

            response.put("success", true);
            response.put("message", "Channel added successfully");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding channel: " + e.getMessage());
        }

        return response;
    }

    // Update channel
    public JSONObject updateChannel(String channelID, String newName, String newUrl) {
        JSONObject response = new JSONObject();

        try {
            String updateQuery = "UPDATE Channels SET channelName = ?, URL = ? WHERE channelID = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setString(1, newName);
            pstmt.setString(2, newUrl);
            pstmt.setString(3, channelID);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                response.put("success", true);
                response.put("message", "Channel updated successfully");
            } else {
                response.put("success", false);
                response.put("message", "Channel not found");
            }

        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Error updating channel: " + e.getMessage());
        }

        return response;
    }

    // Delete channel
    public JSONObject deleteChannel(String channelID) {
        JSONObject response = new JSONObject();

        try {
            conn.setAutoCommit(false);

            // Get channel name first
            String getChannelQuery = "SELECT channelName FROM Channels WHERE channelID = ?";
            PreparedStatement getStmt = conn.prepareStatement(getChannelQuery);
            getStmt.setString(1, channelID);
            ResultSet rs = getStmt.executeQuery();

            if (!rs.next()) {
                response.put("success", false);
                response.put("message", "Channel not found");
                return response;
            }

            String channelName = rs.getString("channelName");

            // Delete related transcriptions
            String deleteTranscriptionsQuery = "DELETE FROM Transcriptions WHERE videoID IN (SELECT videoID FROM Videos WHERE channel = ?)";
            PreparedStatement deleteTransStmt = conn.prepareStatement(deleteTranscriptionsQuery);
            deleteTransStmt.setString(1, channelName);
            deleteTransStmt.executeUpdate();

            // Delete videos
            String deleteVideosQuery = "DELETE FROM Videos WHERE channel = ?";
            PreparedStatement deleteVidStmt = conn.prepareStatement(deleteVideosQuery);
            deleteVidStmt.setString(1, channelName);
            deleteVidStmt.executeUpdate();

            // Delete channel
            String deleteChannelQuery = "DELETE FROM Channels WHERE channelID = ?";
            PreparedStatement deleteChanStmt = conn.prepareStatement(deleteChannelQuery);
            deleteChanStmt.setString(1, channelID);
            deleteChanStmt.executeUpdate();

            // Delete the channel's folder
            File channelFolder = new File(FILE_DIRECTORY, channelName);
            if (channelFolder.exists()) {
                deleteDirectory(channelFolder);
            }

            conn.commit();
            response.put("success", true);
            response.put("message", "Channel and related data deleted successfully");

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("Error rolling back transaction: " + rollbackEx.getMessage());
            }
            response.put("success", false);
            response.put("message", "Error deleting channel: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Error resetting auto-commit: " + e.getMessage());
            }
        }

        return response;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }

    // Refresh specific channel
    public JSONObject refreshChannel(String channelID) {
        JSONObject response = new JSONObject();

        try {
            Update updater = new Update(DB_URL, FILE_DIRECTORY, INDEX_PATH, conn);
            updater.updateChannel(channelID);
            response.put("success", true);
            response.put("message", "Channel refreshed successfully");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error refreshing channel: " + e.getMessage());
        }

        return response;
    }

    // Refresh all channels
    public JSONObject refreshAllChannels() {
        JSONObject response = new JSONObject();

        try {
            Update updater = new Update(DB_URL, FILE_DIRECTORY, INDEX_PATH, conn);
            updater.updateAllChannels();
            response.put("success", true);
            response.put("message", "All channels refreshed successfully");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error refreshing channels: " + e.getMessage());
        }

        return response;
    }
}