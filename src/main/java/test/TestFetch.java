package test;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.StandardCopyOption;


public class TestFetch {
    public static void fetchData() {
        String URL = "https://www.youtube.com/@therustybits/videos";
        System.out.println("Hello from fetchData!");

        // Define yt-dlp command with options using an array
        String[] command = {
                "yt-dlp",
                "--write-auto-sub",             // Download automatically generated subtitles
                "--sub-format", "vtt",          // Set subtitle format to WebVTT (.vtt)
                "--sub-lang", "en",
                "--write-info-json",            // Write info JSON file
                "--skip-download",
                "--download-archive", "Data/Test/archive.txt",  // Archive file to avoid re-downloads
                "--ignore-errors",              // Ignore errors
                "-o", "Data/Test/%(uploader)s/%(title)s.%(ext)s", // Output template
                URL                              // Channel URL
        };

        // Use ProcessBuilder to execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr

        try {
            Process process = processBuilder.start();

            // Capture output from yt-dlp
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // Print each line of output
            }

            int exitCode = process.waitFor();
            System.out.println("Download finished with exit code: " + exitCode);

        } catch (IOException e) {
            System.out.println("An error occurred while running yt-dlp: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Process was interrupted: " + e.getMessage());
        }
    }
    public static void updateArchive(String directoryPath, String archivePath) {
        File dir = new File(directoryPath);
        File archiveFile = new File(archivePath);
        // Check if archive.txt exists, create it if it doesn't
        try {
            if (!archiveFile.exists()) {
                archiveFile.getParentFile().mkdirs(); // Create parent directories if necessary
                archiveFile.createNewFile(); // Create the file
                System.out.println("Created new archive file at: " + archivePath);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating archive file: " + e.getMessage());
            return; // Exit if the archive file could not be created
        }
        // Prepare the metadata and transcriptions directories
        File metadataDir = new File(directoryPath, "metadata");
        File transcriptionsDir = new File(directoryPath, "transcriptions");
        metadataDir.mkdirs();
        transcriptionsDir.mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archiveFile, true))) {
            // Loop through each file in the directory
            for (File file : dir.listFiles()) {
                // Check if the file is a .vtt file
                if (file.isFile() && file.getName().endsWith(".en.vtt")) {
                    // Construct the corresponding .info.json file path
                    String baseName = file.getName().replace(".en.vtt", "");
                    File jsonFile = new File(directoryPath, baseName + ".info.json");

                    // Check if the .info.json file exists
                    if (jsonFile.exists()) {
                        // Read the JSON file to get the video ID
                        String id = extractVideoId(jsonFile);
                        if (id != null) {
                            // Rename and move the JSON file to metadata directory
                            File newJsonFile = new File(metadataDir, id + ".json");
                            Files.move(jsonFile.toPath(), newJsonFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            // Rename and move the VTT file to transcriptions directory
                            File newVttFile = new File(transcriptionsDir, id + ".vtt");
                            Files.move(file.toPath(), newVttFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            // Write "youtube " + id to archive.txt
                            writer.write("youtube " + id);
                            writer.newLine();
                            System.out.println("Added to archive: youtube " + id);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    // Helper method to extract video ID from the JSON file
    private static String extractVideoId(File jsonFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            // Parse JSON content
            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            return jsonObject.optString("id", null); // Return the "id" if it exists, otherwise null
        } catch (IOException e) {
            System.out.println("Could not read JSON file: " + jsonFile.getName());
        } catch (Exception e) {
            System.out.println("Error parsing JSON file: " + jsonFile.getName() + " - " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
//        fetchData();
        updateArchive("Data/Test/The Rusty Bits", "Data/Test/archive.txt");
    }
}
