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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Channel {
    public String channelName;
    public String channelID;
    private final String URL;
    public int numVideos;
    public final String archive;
    private final String fileDirectory;

    public Channel(String channelName, String URL, String fileDirectory, int numVideos) {
        this.channelName = channelName;
        this.channelID = URL.substring(URL.lastIndexOf("@"));
        this.URL = URL;
        this.numVideos = numVideos;
        this.fileDirectory = fileDirectory;
        this.archive = fileDirectory + "/" + channelName + "/archive.txt";
    }

    public List<String> fetchData() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = getProcessBuilder(archive, URL);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        return updateArchive();
    }

    private static ProcessBuilder getProcessBuilder(String archive, String URL) {
        String[] command = {
                "yt-dlp",
                "--write-auto-sub",
                "--sub-format", "vtt",
                "--sub-lang", "en",
                "--quiet",
                "--no-warnings",
                "--write-info-json",
                "--skip-download",
                "--download-archive", archive,
                "--ignore-errors",
                "-o", "Data/Files/%(uploader)s/%(title)s.%(ext)s",
                URL
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        return processBuilder;
    }

    public List<String> updateArchive() throws IOException {
        String directoryPath = fileDirectory + "/" + channelName;
        File dir = new File(directoryPath);
        File archiveFile = new File(archive);
        if (!archiveFile.exists()) {
            archiveFile.getParentFile().mkdirs();
            archiveFile.createNewFile();
            System.out.println("Created new archive file at: " + archive);
        }

        File metadataDir = new File(directoryPath, "metadata");
        File transcriptionsDir = new File(directoryPath, "transcriptions");
        metadataDir.mkdirs();
        transcriptionsDir.mkdirs();

        List<String> newVideos = new ArrayList<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archiveFile, true))) {
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
                            newVideos.add(id);
                        }
                    }
                }
            }
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    if (!file.getName().startsWith(channelName)) {
                        writer.write("youtube " + extractVideoId(file));
                        writer.newLine();
                    }
                    file.delete();
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to archive file: " + e.getMessage());
            throw e;
        }
        return newVideos;
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

    public int countVideos(Connection conn) {
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

    public void updateDB(Connection conn) {
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