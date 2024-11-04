package backend;

public class Transcription {
    public String videoID;
    public int startTime;
    public int endTime;
    public String text;
    public boolean indexed = false;

    public Transcription(String videoID, int startTime, int endTime, String text) {
        this.videoID = videoID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

}
