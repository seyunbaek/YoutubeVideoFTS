package backend;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Video {
    public final String channelName;
    public String videoID;
    public String uploadDate;
    public String thumbnailURL;
    public Boolean transcribed = false;
    public Boolean indexed = false;
    public int numLines;
    public String metadataFilePath;
    public String transcriptionFilePath;
    private final String databaseAddress;

    public Video(String channelName, String videoID, String databaseAddress, String fileDirectory) {
        this.channelName = channelName;
        this.videoID = videoID;
        this.databaseAddress = databaseAddress;
        this.metadataFilePath = fileDirectory + "/" + channelName + "/metadata/" + videoID + ".json";
        this.transcriptionFilePath = fileDirectory + "/" + channelName + "/transcriptions/" + videoID + ".vtt";

        File metadataFile = new File(this.metadataFilePath);
        File transcriptionFile = new File(this.transcriptionFilePath);

        if (metadataFile.exists() && transcriptionFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                JSONObject jsonObject = new JSONObject(jsonContent.toString());
                this.thumbnailURL = jsonObject.optString("thumbnail", null);
                this.uploadDate = jsonObject.optString("upload_date", null);
            } catch (IOException e) {
                System.out.println("Could not read JSON file: " + metadataFile.getName());
            } catch (Exception e) {
                System.out.println("Error parsing JSON file: " + metadataFile.getName() + " - " + e.getMessage());
            }
        } else {
            System.out.println("Metadata or Transcription file does not exist");
        }
    }

    public List<String[]> transcribe() {
        try {
            List<String[]> transcriptions = VTTProcessor.processVTT(this.transcriptionFilePath);
            this.transcribed = true;
            this.numLines = transcriptions.size();
            return transcriptions;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void index() {
        System.out.println("Hello from index() method!");
    }

    public void updateDB(Connection conn) {
        String upsertVideoQuery = """
            INSERT INTO Videos (channel, videoID, thumnailURL, uploadDate, transcribed, indexed, numSentences)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(videoID) DO UPDATE SET
                channel = excluded.channel,
                thumnailURL = excluded.thumnailURL,
                uploadDate = excluded.uploadDate,
                transcribed = excluded.transcribed,
                indexed = excluded.indexed,
                numSentences = excluded.numSentences;
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(upsertVideoQuery)) {

            pstmt.setString(1, channelName);
            pstmt.setString(2, videoID);
            pstmt.setString(3, thumbnailURL);
            pstmt.setString(4, uploadDate);
            pstmt.setInt(5, transcribed ? 1 : 0);
            pstmt.setInt(6, indexed ? 1 : 0);
            pstmt.setInt(7, numLines);

            pstmt.executeUpdate();
            System.out.println("Video inserted/updated in the database: " + this);

        } catch (SQLException e) {
            System.out.println("Error inserting/updating video in database: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Video{" +
                "channelName='" + channelName + '\'' +
                ", videoID='" + videoID + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", transcribed=" + transcribed +
                ", indexed=" + indexed +
                ", numSentences=" + numLines +
                '}';
    }
}