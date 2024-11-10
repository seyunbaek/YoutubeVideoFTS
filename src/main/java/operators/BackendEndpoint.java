package operators;

import backend.FTSIndex;
import backend.Transcription;
import backend.VideoSearchResult;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class BackendEndpoint {
    private static final String DB_URL = "jdbc:sqlite:Data/database.db";
    private static FTSIndex searchIndex;  // Add static FTSIndex instance

    public static void main(String[] args) throws Exception {
        // Initialize the connection and FTSIndex at startup
        Connection conn = DriverManager.getConnection(DB_URL);
        String indexPath = "Data/FTSIndex.json";
        searchIndex = new FTSIndex(indexPath, conn);
        System.out.println("Search index loaded successfully");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/channels", new ChannelsHandler());
        server.createContext("/api/channels/refresh/", new RefreshChannelHandler());
        server.createContext("/api/channels/refresh-all", new RefreshAllHandler());
        server.createContext("/api/search", new SearchHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        // Send response
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static void handleCORS(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(200, -1);
        }
    }

    static class ChannelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleCORS(exchange);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                Operators manager = new Operators(conn, searchIndex);
                String path = exchange.getRequestURI().getPath();
                String response;

                switch (exchange.getRequestMethod()) {
                    case "GET":
                        System.out.println("GET /api/channels - Fetching all channels");
                        response = manager.getAllChannels();
                        sendResponse(exchange, 200, response);
                        break;

                    case "POST":
                        System.out.println("POST /api/channels - Adding new channel");
                        String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                                .lines().collect(Collectors.joining("\n"));
                        JSONObject data = new JSONObject(requestBody);
                        // Changed variable name from 'response' to 'addResponse'
                        String addResponse = manager.addChannel(
                                data.getString("channelName"),
                                data.getString("URL")
                        ).toString();
                        sendResponse(exchange, 200, addResponse);
                        break;

                    case "PUT":
                        String channelId = path.substring(path.lastIndexOf('/') + 1);
                        System.out.println("PUT /api/channels/" + channelId + " - Updating channel");
                        requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                                .lines().collect(Collectors.joining("\n"));
                        data = new JSONObject(requestBody);
                        response = manager.updateChannel(
                                channelId,
                                data.getString("channelName"),
                                data.getString("URL")
                        ).toString();
                        sendResponse(exchange, 200, response);
                        break;

                    case "DELETE":
                        channelId = path.substring(path.lastIndexOf('/') + 1);
                        System.out.println("DELETE /api/channels/" + channelId + " - Deleting channel");
                        response = manager.deleteChannel(channelId).toString();
                        sendResponse(exchange, 200, response);
                        break;

                    default:
                        response = new JSONObject()
                                .put("success", false)
                                .put("error", "Method not allowed")
                                .toString();
                        sendResponse(exchange, 405, response);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
                e.printStackTrace();
                String response = new JSONObject()
                        .put("success", false)
                        .put("error", "Internal server error: " + e.getMessage())
                        .toString();
                sendResponse(exchange, 500, response);
            }
        }
    }

    static class RefreshChannelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleCORS(exchange);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                if (!exchange.getRequestMethod().equals("POST")) {
                    String response = new JSONObject()
                            .put("success", false)
                            .put("error", "Method not allowed")
                            .toString();
                    sendResponse(exchange, 405, response);
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String channelId = path.substring(path.lastIndexOf('/') + 1);
                System.out.println("POST /api/channels/refresh/" + channelId + " - Refreshing channel");

                Operators manager = new Operators(conn, searchIndex);
                String response = manager.refreshChannel(channelId).toString();
                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error handling refresh request: " + e.getMessage());
                e.printStackTrace();
                String response = new JSONObject()
                        .put("success", false)
                        .put("error", "Internal server error: " + e.getMessage())
                        .toString();
                sendResponse(exchange, 500, response);
            }
        }
    }

    static class RefreshAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleCORS(exchange);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                if (!exchange.getRequestMethod().equals("POST")) {
                    String response = new JSONObject()
                            .put("success", false)
                            .put("error", "Method not allowed")
                            .toString();
                    sendResponse(exchange, 405, response);
                    return;
                }

                System.out.println("POST /api/channels/refresh-all - Refreshing all channels");
                Operators manager = new Operators(conn, searchIndex);
                String response = manager.refreshAllChannels().toString();
                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error handling refresh all request: " + e.getMessage());
                e.printStackTrace();
                String response = new JSONObject()
                        .put("success", false)
                        .put("error", "Internal server error: " + e.getMessage())
                        .toString();
                sendResponse(exchange, 500, response);
            }
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleCORS(exchange);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                if (!exchange.getRequestMethod().equals("GET")) {
                    String response = new JSONObject()
                            .put("success", false)
                            .put("error", "Method not allowed")
                            .toString();
                    sendResponse(exchange, 405, response);
                    return;
                }

                // Get query parameter
                String query = exchange.getRequestURI().getQuery();
                String searchQuery = query.substring(query.indexOf("q=") + 2);
                searchQuery = java.net.URLDecoder.decode(searchQuery, StandardCharsets.UTF_8);

                System.out.println("Searching for: " + searchQuery);

                // Perform search
                List<VideoSearchResult> results = searchIndex.search(searchQuery);

                // Convert results to JSON
                JSONArray resultsArray = new JSONArray();
                for (VideoSearchResult result : results) {
                    JSONObject videoResult = new JSONObject();
                    videoResult.put("videoID", result.video.videoID);
                    videoResult.put("title", result.video.videoTitle);
                    videoResult.put("channelName", result.video.channelName);
                    videoResult.put("uploadDate", result.video.uploadDate);
                    videoResult.put("thumbnailURL", result.video.thumbnailURL);

                    JSONArray matches = new JSONArray();
                    for (Transcription t : result.transcriptions) {
                        JSONObject match = new JSONObject();
                        match.put("transcriptionID", t.transcriptionID);
                        match.put("startTime", t.startTime);
                        match.put("endTime", t.endTime);
                        match.put("text", t.text);
                        matches.put(match);
                    }
                    videoResult.put("matches", matches);
                    resultsArray.put(videoResult);
                }

                String response = new JSONObject()
                        .put("success", true)
                        .put("results", resultsArray)
                        .toString();

                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                System.err.println("Error handling search request: " + e.getMessage());
                e.printStackTrace();
                String response = new JSONObject()
                        .put("success", false)
                        .put("error", "Internal server error: " + e.getMessage())
                        .toString();
                sendResponse(exchange, 500, response);
            }
        }
    }
}