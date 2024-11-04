package backend;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class Indexer {
    private final String dbFile;
    private final PorterStemmer stemmer;
    private final SimpleTokenizer tokenizer;
    private final Set<String> stopWords;
    private Map<String, List<Integer>> index;
    private List<DocumentRecord> documents;

    // Inner class to store document records
    public static class DocumentRecord {
        String videoId;
        String title;
        String timestamp;
        String text;
        String channelName;

        public DocumentRecord(String videoId, String title, String timestamp, String text, String channelName) {
            this.videoId = videoId;
            this.title = title;
            this.timestamp = timestamp;
            this.text = text;
            this.channelName = channelName;
        }
    }

    public Indexer(String dbFile) {
        this.dbFile = dbFile;
        this.stemmer = new PorterStemmer();
        this.tokenizer = SimpleTokenizer.INSTANCE;
        this.stopWords = initializeStopWords();
        this.index = new HashMap<>();
        this.documents = new ArrayList<>();
        loadData();
    }

    private Set<String> initializeStopWords() {
        Set<String> stops = new HashSet<>(Arrays.asList(
                "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
                "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
                "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
                "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
                "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
                "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
                "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
                "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
                "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
                "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
        ));
        return stops;
    }

    private void loadData() {
        String sql = """
            SELECT 
                Transcriptions.videoID,
                Videos.title,
                Transcriptions.timestamp,
                Transcriptions.text,
                Videos.channelName
            FROM Transcriptions
            JOIN Videos ON Transcriptions.videoID = Videos.youtubeID
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DocumentRecord doc = new DocumentRecord(
                        rs.getString("videoID"),
                        rs.getString("title"),
                        rs.getString("timestamp"),
                        rs.getString("text"),
                        rs.getString("channelName")
                );
                documents.add(doc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading data from database", e);
        }
    }

    private List<String> preprocessText(String text) {
        // Convert to lowercase
        text = text.toLowerCase();

        // Remove punctuation and tokenize
        String[] tokens = tokenizer.tokenize(text);

        // Remove stop words and apply stemming
        List<String> processedTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!stopWords.contains(token) && token.matches("\\w+")) {
                processedTokens.add(stemmer.stem(token));
            }
        }

        return processedTokens;
    }

    public void buildIndex() {
        System.out.println("Building index...");

        // Reset index
        index.clear();

        // Process each document
        for (int idx = 0; idx < documents.size(); idx++) {
            if (idx % 1000 == 0) {
                System.out.println("Processing " + idx);
            }

            DocumentRecord doc = documents.get(idx);
            List<String> tokens = preprocessText(doc.text);

            // Remove duplicates and update index
            Set<String> processedTokens = new HashSet<>(tokens);
            for (String token : processedTokens) {
                index.computeIfAbsent(token, k -> new ArrayList<>()).add(idx);
            }
        }
    }

    public void saveIndex(String filename) throws IOException {
        System.out.println("Saving index to " + filename);

        // Convert the index to JSONObject
        JSONObject jsonIndex = new JSONObject();
        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            jsonIndex.put(entry.getKey(), new JSONArray(entry.getValue()));
        }

        // Write the JSON to file with pretty printing
        Files.writeString(Paths.get(filename), jsonIndex.toString(2));
        System.out.println("Index saved successfully!");
    }

    public void loadIndex(String filename) throws IOException {
        System.out.println("Loading index from " + filename);

        String jsonContent = Files.readString(Paths.get(filename));
        JSONObject jsonIndex = new JSONObject(jsonContent);

        // Clear existing index and load from JSON
        index.clear();
        for (String key : jsonIndex.keySet()) {
            JSONArray array = jsonIndex.getJSONArray(key);
            List<Integer> docIds = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                docIds.add(array.getInt(i));
            }
            index.put(key, docIds);
        }

        System.out.println("Index loaded successfully!");
    }

    public List<String[]> search(String query) {
        List<String> queryTokens = preprocessText(query);

        if (queryTokens.isEmpty()) {
            System.out.println("No valid search terms found.");
            return new ArrayList<>();
        }

        // Initialize result set with documents matching first token
        Set<Integer> resultDocs = new HashSet<>(
                index.getOrDefault(queryTokens.get(0), new ArrayList<>())
        );

        // Perform intersection for remaining tokens
        for (int i = 1; i < queryTokens.size(); i++) {
            List<Integer> tokenDocs = index.getOrDefault(queryTokens.get(i), new ArrayList<>());
            resultDocs.retainAll(new HashSet<>(tokenDocs));
        }

        // Format results
        List<String[]> resultLinks = new ArrayList<>();
        for (Integer result : resultDocs) {
            DocumentRecord doc = documents.get(result);
            String timestamp = doc.timestamp.split(" ")[0]
                    .replace("[", "")
                    .replace("s", "");
            String youtubeUrl = String.format(
                    "https://youtu.be/%s?t=%ds",
                    doc.videoId,
                    (int) Float.parseFloat(timestamp)
            );
            resultLinks.add(new String[]{youtubeUrl, doc.text});
        }

        Collections.sort(resultLinks, (a, b) -> a[0].compareTo(b[0]));
        return resultLinks;
    }

    public static void main(String[] args) {
        try {
            Indexer indexer = new Indexer("Full-text search/my_data.db");

            // Try to load existing index, build if not found
            try {
                indexer.loadIndex("search_index.json");
            } catch (IOException e) {
                System.out.println("No existing index found. Building new index...");
                indexer.buildIndex();
                indexer.saveIndex("search_index.json");
            }

            // Interactive search loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nEnter your search query (or 'quit' to exit): ");
                String query = scanner.nextLine();

                if (query.toLowerCase().equals("quit")) {
                    break;
                }

                List<String[]> results = indexer.search(query);

                if (!results.isEmpty()) {
                    System.out.printf("\nFound %d results:%n", results.size());
                    for (String[] result : results) {
                        System.out.println(result[0] + "; " + result[1]);
                    }
                } else {
                    System.out.println("No results found.");
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}