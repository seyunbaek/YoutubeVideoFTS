package test;

import backend.VTTProcessor;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestTranscribe {
    public static void main(String[] args) {
        // Specify the directory path
        String directoryPath = "Data/Test/The Rusty Bits/transcriptions";

        // Create a File object for the directory
        File directory = new File(directoryPath);

        // Check if the directory exists and is indeed a directory
        if (directory.exists() && directory.isDirectory()) {
            // Get all files and directories in the specified directory
            File[] filesList = directory.listFiles();

            // Check if the directory is empty
            if (filesList != null && filesList.length > 0) {
                System.out.println("Files in the directory:");
                for (File file : filesList) {
                    if (file.isFile()) {
                        String vttFilePath = directoryPath+"/"+ file.getName();
                        VTTProcessor processor = new VTTProcessor();
                        System.out.println(vttFilePath);
                        try {
                            List<String[]> transcriptions = processor.processVTT(vttFilePath);
                            for (String[] line: transcriptions) {
                                System.out.printf("[%s - %s] %s%n", line[0], line[1], line[2]);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else {
                System.out.println("The directory is empty.");
            }
        } else {
            System.out.println("The specified path is not a directory or does not exist.");
        }


    }
}