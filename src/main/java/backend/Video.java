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
    public Connection conn;

    public Video(String channelName, String videoID, Connection conn, String fileDirectory) throws IOException, Exception{
        this.channelName = channelName;
        this.videoID = videoID;
        this.conn = conn;
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
        } else {
            System.out.println("Metadata or Transcription file does not exist");
        }
    }

    public List<String[]> transcribe() throws IOException{
        List<String[]> transcriptions = VTTProcessor.processVTT(this.transcriptionFilePath);
        this.transcribed = true;
        this.numLines = transcriptions.size();
        return transcriptions;
    }

    public void index(FTSIndex index) throws SQLException {
        index.indexVideo(videoID);
        this.indexed = true;
        try {
            updateDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDB() throws SQLException {
        String upsertVideoQuery = """
            INSERT INTO Videos (channel, videoID, thumnailURL, uploadDate, transcribed, indexed, numLines)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(videoID) DO UPDATE SET
                channel = excluded.channel,
                thumnailURL = excluded.thumnailURL,
                uploadDate = excluded.uploadDate,
                transcribed = excluded.transcribed,
                indexed = excluded.indexed,
                numLines = excluded.numLines;
            """;

        PreparedStatement pstmt = conn.prepareStatement(upsertVideoQuery);

        pstmt.setString(1, channelName);
        pstmt.setString(2, videoID);
        pstmt.setString(3, thumbnailURL);
        pstmt.setString(4, uploadDate);
        pstmt.setInt(5, transcribed ? 1 : 0);
        pstmt.setInt(6, indexed ? 1 : 0);
        pstmt.setInt(7, numLines);

        pstmt.executeUpdate();
    }

    @Override
    public String toString() {
        return "Video{" +
                "channelName='" + channelName + '\'' +
                ", videoID='" + videoID + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", transcribed=" + transcribed + '\'' +
                ", indexed=" + indexed + '\'' +
                ", numLines=" + numLines +
                '}';
    }
}