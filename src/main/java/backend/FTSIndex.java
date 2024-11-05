package backend;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.sql.*;

    public class FTSIndex {
        private final PorterStemmer stemmer =  new PorterStemmer();
        private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        private final String indexPath;
        public Map<String, List<Integer>> index;
        private final Connection conn;
        private final Set<String> stopWords = new HashSet<>(Arrays.asList(
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

        public FTSIndex(String indexPath, Connection conn) {
            this.conn = conn;
            this.indexPath = indexPath;
            // Initialise the index from the specified index file path
            try {
                this.index = loadIndex(indexPath);
            } catch (IOException e) {
                System.out.println("No existing index file found. Initialising empty index.");
                this.index = new HashMap<>();
            }
        }

        private Map<String, List<Integer>> loadIndex(String indexPath) throws IOException {
            System.out.println("Loading index from " + indexPath);

            Map<String, List<Integer>> loadedIndex = new HashMap<>();

            try {
                // Try to read the content of the existing file
                String jsonContent = Files.readString(Paths.get(indexPath));
                JSONObject jsonIndex = new JSONObject(jsonContent);

                for (String key : jsonIndex.keySet()) {
                    JSONArray array = jsonIndex.getJSONArray(key);
                    List<Integer> docIds = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        docIds.add(array.getInt(i));
                    }
                    loadedIndex.put(key, docIds);
                }

                System.out.println("Index loaded successfully!");
            } catch (IOException e) {
                // If the file doesn't exist or cannot be read, create a new file with an empty JSON object
                System.out.println("Index file not found. Creating a new index file at " + indexPath);

                // Create an empty JSON object and write it to the specified file path
                JSONObject emptyIndex = new JSONObject();
                Files.writeString(Paths.get(indexPath), emptyIndex.toString(2), StandardOpenOption.CREATE);

                System.out.println("Empty index file created successfully!");
            }

            return loadedIndex;
        }

        public void indexVideo(String videoID) throws SQLException {
            String selectQuery = "SELECT * FROM Transcriptions WHERE videoID = ? AND indexed = 0";
            PreparedStatement pstmt = conn.prepareStatement(selectQuery);
            pstmt.setString(1, videoID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String text = rs.getString("text");
                int startTime = rs.getInt("startTime");
                int endTime = rs.getInt("endTime");
                int transcriptionID = rs.getInt("id");
                Transcription transcription = new Transcription(videoID, transcriptionID, startTime, endTime, text);
                indexTranscription(transcription);
            }

            System.out.println("Indexing completed for video: " + videoID);
        }

        public void indexTranscription(Transcription transcription) throws SQLException {
            String text = transcription.text;
            List<String> tokens = preprocessText(text);
            // Update the index with processed tokens
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                index.computeIfAbsent(token, k -> new ArrayList<>()).add(transcription.transcriptionID);
            }
            transcription.setIndexed(true);
            transcription.updateDB(conn);
        }

        public void updateIndex() throws IOException {
            // Append new entries to the JSON file
            JSONObject jsonIndex = new JSONObject();
            for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
                jsonIndex.put(entry.getKey(), new JSONArray(entry.getValue()));
            }

            Files.writeString(Paths.get(indexPath), jsonIndex.toString(2), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private List<String> preprocessText(String text) {
            text = text.toLowerCase();
            String[] tokens = tokenizer.tokenize(text);
            List<String> processedTokens = new ArrayList<>();

            for (String token : tokens) {
                if (!stopWords.contains(token) && token.matches("\\w+")) {
                    processedTokens.add(stemmer.stem(token));
                }
            }
            return processedTokens;
        }

        public List<Transcription> search(String query) throws SQLException {
            List<String> queryTokens = preprocessText(query);
            String searchQuery = "SELECT * FROM Transcriptions WHERE indexed = 1 AND rowid = ?";

            if (queryTokens.isEmpty()) {
                System.out.println("No valid search terms found.");
                return new ArrayList<>();
            }

            // Initialize result set with transcriptions matching first token
            Set<Integer> resultDocs = new HashSet<>(index.getOrDefault(queryTokens.get(0), new ArrayList<>()));

            // Perform intersection for remaining tokens
            for (int i = 1; i < queryTokens.size(); i++) {
                List<Integer> tokenDocs = index.getOrDefault(queryTokens.get(i), new ArrayList<>());
                resultDocs.retainAll(new HashSet<>(tokenDocs));
            }

            // Format results
            List<Transcription> searchResults = new ArrayList<>();
            PreparedStatement pstmt = conn.prepareStatement(searchQuery);
            for (Integer rowID : resultDocs) {
                pstmt.setInt(1, rowID);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String videoID = rs.getString("videoID");
                    String text = rs.getString("text");
                    int startTime = rs.getInt("startTime");
                    int endTime = rs.getInt("endTime");
                    int transcriptionID = rs.getInt("id");
                    Transcription transcription = new Transcription(videoID, transcriptionID, startTime, endTime, text);
                    searchResults.add(transcription);
                }
            }

            return searchResults;
        }
    }