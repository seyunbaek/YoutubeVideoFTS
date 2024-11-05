package operators;

import backend.FTSIndex;
import backend.Transcription;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

                List<Transcription> results = index.search(query);

                if (!results.isEmpty()) {
                    System.out.printf("\nFound %d results:%n", results.size());
                    for (Transcription result : results) {
                        System.out.println(result.URL);
                    }
                } else {
                    System.out.println("No results found.");
                }
            }
            scanner.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}