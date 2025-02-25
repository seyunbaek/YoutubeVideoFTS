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
    public String videoTitle;
    public String uploadDate;
    public String thumbnailURL;
    public Boolean transcribed = false;
    public Boolean indexed = false;
    public int numLines;
    public String metadataFilePath;
    public String transcriptionFilePath;

    public Video(String channelName, String videoID, String fileDirectory) throws IOException, Exception{
        this.channelName = channelName;
        this.videoID = videoID;
        this.metadataFilePath = fileDirectory + "/" + channelName + "/metadata/" + videoID + ".json";
        this.transcriptionFilePath = fileDirectory + "/" + channelName + "/transcriptions/" + videoID + ".vtt";

        File metadataFile = new File(this.metadataFilePath);
        File transcriptionFile = new File(this.transcriptionFilePath);

        if (metadataFile.exists() && transcriptionFile.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(metadataFile));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            this.thumbnailURL = jsonObject.optString("thumbnail", null);
            this.uploadDate = jsonObject.optString("upload_date", null);
            this.videoTitle = jsonObject.optString("title", null);
        } else {
            System.out.println("Metadata or Transcription file does not exist");
        }
    }

    public void transcribe(Connection conn) throws IOException{
        String insertTranscriptionQuery = "INSERT INTO Transcriptions (videoID, startTime, endTime, text, indexed) VALUES (?, ?, ?, ?, ?)";

        List<String[]> transcriptions = VTTProcessor.processVTT(this.transcriptionFilePath);
        this.transcribed = true;
        this.numLines = transcriptions.size();
        try {
            updateDB(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(insertTranscriptionQuery)) {
            for (String[] transcription : transcriptions) {
                String startTime = transcription[0];
                String endTime = transcription[1];
                String text = transcription[2];

                // Convert startTime and endTime to integer seconds (rounded)
                int startSeconds = (int) Math.round(Double.parseDouble(startTime.replace("s", "")));
                int endSeconds = (int) Math.round(Double.parseDouble(endTime.replace("s", "")));

                insertStmt.setString(1, videoID);
                insertStmt.setInt(2, startSeconds);
                insertStmt.setInt(3, endSeconds);
                insertStmt.setString(4, text);
                insertStmt.setBoolean(5, false);
                insertStmt.addBatch(); // Use batch processing for efficiency
            }
            insertStmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void index(FTSIndex index, Connection conn) throws SQLException, IOException {
        index.indexVideo(videoID, conn);
        this.indexed = true;
        try {
            updateDB(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDB(Connection conn) throws SQLException {
        String upsertVideoQuery = """
            INSERT INTO Videos (channel, videoID, videoTitle, thumbnailURL, uploadDate, transcribed, indexed, numLines)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(videoID) DO UPDATE SET
                channel = excluded.channel,
                videoTitle = excluded.videoTitle,
                thumbnailURL = excluded.thumbnailURL,
                uploadDate = excluded.uploadDate,
                transcribed = excluded.transcribed,
                indexed = excluded.indexed,
                numLines = excluded.numLines;
            """;

        PreparedStatement pstmt = conn.prepareStatement(upsertVideoQuery);

        pstmt.setString(1, channelName);
        pstmt.setString(2, videoID);
        pstmt.setString(3, videoTitle);
        pstmt.setString(4, thumbnailURL);
        pstmt.setString(5, uploadDate);
        pstmt.setBoolean(6, transcribed);
        pstmt.setBoolean(7, indexed);
        pstmt.setInt(8, numLines);

        pstmt.executeUpdate();
    }

    @Override
    public String toString() {
        return "Video{" +
                "channelName='" + channelName + '\'' +
                ", videoID='" + videoID + '\'' +
                ", videoTitle='" + videoTitle + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", transcribed=" + transcribed + '\'' +
                ", indexed=" + indexed + '\'' +
                ", numLines=" + numLines +
                '}';
    }
}