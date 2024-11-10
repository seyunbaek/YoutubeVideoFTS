package backend;

import java.util.List;
import java.util.Objects;

public class VideoSearchResult {
    public final Video video;
    public final List<Transcription> transcriptions;

    public VideoSearchResult(Video video, List<Transcription> transcriptions) {
        this.video = video;
        this.transcriptions = transcriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoSearchResult that = (VideoSearchResult) o;
        return Objects.equals(video.videoID, that.video.videoID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(video.videoID);
    }

    @Override
    public String toString() {
        return "VideoSearchResult{" +
                "video=" + video +
                ", transcriptions=" + transcriptions.size() +
                '}';
    }
}