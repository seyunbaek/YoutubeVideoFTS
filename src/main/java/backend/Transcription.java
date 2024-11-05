package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Transcription {
    public String videoID;
    public int transcriptionID;
    public int startTime;
    public int endTime;
    public String text;
    public boolean indexed = false;
    public String URL;

    public Transcription(String videoID, int transcriptionID, int startTime, int endTime, String text) {
        this.videoID = videoID;
        this.transcriptionID = transcriptionID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
        this.URL = String.format("https://youtu.be/%s?t=%ds", videoID, startTime);
    }

    public void setIndexed(boolean indexed){
        this.indexed = indexed;
    }

    public void updateDB(Connection conn) throws SQLException {
        String upsertSQL = """
        INSERT INTO Transcriptions (id, videoID, startTime, endTime, text, indexed)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            videoID = excluded.videoID,
            startTime = excluded.startTime,
            endTime = excluded.endTime,
            text = excluded.text,
            indexed = excluded.indexed;
    """;
        try (PreparedStatement pstmt = conn.prepareStatement(upsertSQL)) {
            pstmt.setInt(1, transcriptionID); // Ensure id is set
            pstmt.setString(2, videoID);
            pstmt.setInt(3, startTime);
            pstmt.setInt(4, endTime);
            pstmt.setString(5, text);
            pstmt.setInt(6, indexed ? 1 : 0);
            pstmt.executeUpdate();
        }
    }

    @Override
    public String toString() {
        return "Transcription{" +
                "videoID='" + videoID + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", text=" + text + '\'' +
                ", indexed=" + indexed + '\'' +
                ", transcriptionID=" + transcriptionID +
                '}';
    }
}
