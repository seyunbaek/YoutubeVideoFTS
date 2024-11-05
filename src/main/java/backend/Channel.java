package backend;

import org.json.JSONObject;
import java.sql.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class Channel {
    public String channelName;
    public String channelID;
    private final String URL;
    public int numVideos;
    public final String archive;
    private final String fileDirectory;
    private final Connection conn;

    public Channel(String channelName, String channelID, String URL, String fileDirectory, Connection conn) {
        this.channelName = channelName;
        this.channelID = channelID;
        this.URL = URL;
        this.fileDirectory = fileDirectory;
        this.conn = conn;
        this.archive = fileDirectory + "/" + channelName + "/archive.txt";
    }

    public void fetchData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = getProcessBuilder(this.archive, this.URL);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        System.out.println("Download finished with exit code: " + exitCode);

        updateArchive();
    }

    private static ProcessBuilder getProcessBuilder(String archive, String URL) {
        String[] command = {
                "yt-dlp",
                "--write-auto-sub",
                "--sub-format", "vtt",
                "--sub-lang", "en",
                "--write-info-json",
                "--skip-download",
                "--download-archive", archive,
                "--ignore-errors",
                "-o", "Data/Files/%(uploader)s/%(title)s.%(ext)s",
                URL
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    public void updateArchive() throws IOException {
        String directoryPath = fileDirectory + "/" + this.channelName;
        File dir = new File(directoryPath);
        File archiveFile = new File(this.archive);
        if (!archiveFile.exists()) {
            archiveFile.getParentFile().mkdirs();
            archiveFile.createNewFile();
            System.out.println("Created new archive file at: " + this.archive);
        }

        File metadataDir = new File(directoryPath, "metadata");
        File transcriptionsDir = new File(directoryPath, "transcriptions");
        metadataDir.mkdirs();
        transcriptionsDir.mkdirs();

        BufferedWriter writer = new BufferedWriter(new FileWriter(archiveFile, true));
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".en.vtt")) {
                String baseName = file.getName().replace(".en.vtt", "");
                File jsonFile = new File(directoryPath, baseName + ".info.json");

                if (jsonFile.exists()) {
                    String id = extractVideoId(jsonFile);
                    if (id != null) {
                        File newJsonFile = new File(metadataDir, id + ".json");
                        Files.move(jsonFile.toPath(), newJsonFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        File newVttFile = new File(transcriptionsDir, id + ".vtt");
                        Files.move(file.toPath(), newVttFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        writer.write("youtube " + id);
                        writer.newLine();
                        System.out.println("Added to archive: youtube " + id);
                    }
                }
            }
        }


        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                if (file.delete()) {
                    System.out.println("Deleted remaining JSON file: " + file.getName());
                } else {
                    System.out.println("Failed to delete JSON file: " + file.getName());
                }
            }
        }
    }

    private String extractVideoId(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            return jsonObject.optString("id", null);
        } catch (IOException e) {
            System.out.println("Could not read JSON file: " + jsonFile.getName());
        } catch (Exception e) {
            System.out.println("Error parsing JSON file: " + jsonFile.getName() + " - " + e.getMessage());
        }
        return null;
    }

    public int countVideos() {
        String countQuery = "SELECT COUNT(*) FROM Videos WHERE channel = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(countQuery)) {
            pstmt.setString(1, this.channelName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                this.numVideos = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error counting videos: " + e.getMessage());
        }
        return this.numVideos;
    }

    public void updateDB() {
        String upsertChannelQuery = """
                INSERT INTO Channels (channelName, channelID, URL, numVideos)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(channelID) DO UPDATE SET
                    channelName = excluded.channelName,
                    URL = excluded.URL,
                    numVideos = excluded.numVideos;
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(upsertChannelQuery)) {
            pstmt.setString(1, channelName);
            pstmt.setString(2, channelID);
            pstmt.setString(3, URL);
            pstmt.setInt(4, numVideos);
            pstmt.executeUpdate();
            System.out.println("Channel inserted/updated in the database: " + this);
        } catch (SQLException e) {
            System.out.println("Error inserting/updating channel in database: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Channel{" +
                "channelName='" + channelName + '\'' +
                ", channelID='" + channelID + '\'' +
                ", URL='" + URL + '\'' +
                ", numVideos=" + numVideos +
                '}';
    }
}