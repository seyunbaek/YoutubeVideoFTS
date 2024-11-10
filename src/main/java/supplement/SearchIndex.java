package supplement;

import backend.FTSIndex;
import backend.VideoSearchResult;
import backend.Transcription;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;

public class SearchIndex {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:Data/database.db";
        String indexPath = "Data/FTSIndex.json";

        try (Connection conn = DriverManager.getConnection(url)) {
            FTSIndex index = new FTSIndex(indexPath, conn);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("\nEnter your search query (or 'quit' to exit): ");
                String query = scanner.nextLine();

                if (query.toLowerCase().equals("quit")) {
                    break;
                }

                List<VideoSearchResult> results = index.search(query);

                if (!results.isEmpty()) {
                    System.out.printf("\nFound matches in %d videos:%n", results.size());
                    for (VideoSearchResult result : results) {
                        System.out.printf("\nVideo: %s (%s)%n",
                                result.video.videoTitle,
                                result.video.videoID);
                        System.out.printf("Channel: %s%n", result.video.channelName);
                        System.out.printf("Upload Date: %s%n", result.video.uploadDate);
                        System.out.printf("Matches found: %d%n", result.transcriptions.size());

                        for (Transcription t : result.transcriptions) {
                            System.out.printf("  %d:%02d - %s%n",
                                    t.startTime / 60,
                                    t.startTime % 60,
                                    t.text);
                        }
                    }
                } else {
                    System.out.println("No results found.");
                }
            }
            scanner.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}