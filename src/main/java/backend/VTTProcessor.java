package backend;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class VTTProcessor {
    private static SentenceDetectorME sentenceDetector;

    // Load the sentence detector model once during class initialization
    static {
        try (InputStream modelIn = new FileInputStream("NLPModels/en-sent.bin")) {
            SentenceModel model = new SentenceModel(modelIn);
            sentenceDetector = new SentenceDetectorME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double convertVTTTimeToSeconds(String vttTime) {
        // Convert VTT timestamp to total seconds with two decimal places
        String[] parts = vttTime.replace(',', '.').split(":");
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    private static String formatSecondsToTimestamp(double seconds) {
        // Convert seconds back to timestamp format with two decimal places
        return String.format("%.2fs", seconds);
    }

    private static String cleanText(String text) {
        // Remove unnecessary tags, timestamps, and text within square brackets
        text = text.replaceAll("<.*?>", ""); // Remove <c>...</c> tags
        text = text.replaceAll("<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>", ""); // Remove timestamps like <00:00:50.320>
        text = text.replaceAll("\\[.*?]", ""); // Remove text inside square brackets
        return text.trim();
    }

    private static List<String[]> mergeRepeatedText(List<String[]> subtitles) {
        // Remove repeated words/phrases that appear in adjacent lines
        List<String[]> filteredSubtitles = new ArrayList<>();
        String previousText = "";
        for (String[] subtitle : subtitles) {
            String text = subtitle[2];
            if (!text.equals(previousText)) {
                filteredSubtitles.add(subtitle);
                previousText = text;
            }
        }
        return filteredSubtitles;
    }

    private static List<String[]> sentenceBased(List<String[]> subtitles) {
        List<String[]> chunks = new ArrayList<>();
        StringBuilder combinedTextBuilder = new StringBuilder();

        // Combine all subtitle texts into a single string
        for (String[] subtitle : subtitles) {
            combinedTextBuilder.append(subtitle[2]).append(" ");
        }
        String combinedText = combinedTextBuilder.toString().trim();

        // Attempt to split combined text into sentences
        String[] sentences = sentenceDetector.sentDetect(combinedText);

        int subtitleIndex = 0;
        double currentStart = 0;
        double currentEnd = 0;

        // Sentence-based chunking with start-end time checks
        for (String sentence : sentences) {
            sentence = sentence.trim();
            String[] words = sentence.split(" ");
            boolean foundStart = false;

            while (subtitleIndex < subtitles.size()) {
                String[] subtitle = subtitles.get(subtitleIndex);
                String subtitleText = subtitle[2];
                double startTime = Double.parseDouble(subtitle[0].replace("s", ""));
                double endTime = Double.parseDouble(subtitle[1].replace("s", ""));

                if (!foundStart && subtitleText.contains(words[0])) {
                    currentStart = (currentEnd != -1 && startTime < currentEnd) ? currentEnd : startTime;
                    foundStart = true;
                }

                if (subtitleText.contains(words[words.length - 1])) {
                    currentEnd = endTime;
                    subtitleIndex++;
                    break;
                }

                subtitleIndex++;
            }

            if (currentStart != -1 && currentEnd != -1) {
                chunks.add(new String[]{
                        formatSecondsToTimestamp(currentStart),
                        formatSecondsToTimestamp(currentEnd),
                        sentence
                });
            }
        }
        return chunks;
    }

    private static List<String[]> timeBased(List<String[]> subtitles) {
        List<String[]> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        double currentStart = 0;
        double currentEnd = 0;

        for (String[] subtitle : subtitles) {
            double startTime = Double.parseDouble(subtitle[0].replace("s", ""));
            double endTime = Double.parseDouble(subtitle[1].replace("s", ""));
            String text = subtitle[2];

            if (currentChunk.isEmpty()) {
                currentStart = startTime;
                currentChunk.add(text);
                currentEnd = endTime;
            } else {
                double chunkDuration = endTime - currentStart;
                if (chunkDuration > 10 + 1 || (chunkDuration >= 10 - 1 && chunkDuration <= 10 + 1)) {
                    chunks.add(new String[]{
                            formatSecondsToTimestamp(currentStart),
                            formatSecondsToTimestamp(currentEnd),
                            String.join(" ", currentChunk)
                    });
                    currentChunk.clear();
                    currentChunk.add(text);
                    currentStart = startTime;
                    currentEnd = endTime;
                } else {
                    currentChunk.add(text);
                    currentEnd = endTime;
                }
            }
        }

        // Add any remaining chunk
        if (!currentChunk.isEmpty()) {
            chunks.add(new String[]{
                    formatSecondsToTimestamp(currentStart),
                    formatSecondsToTimestamp(currentEnd),
                    String.join(" ", currentChunk)
            });
        }

        return chunks;
    }

    private static List<String[]> combineIntoChunks(List<String[]> subtitles) {
        List<String[]> initialChunks;

        // First, attempt sentence-based chunking if there are more than one sentence
        StringBuilder combinedTextBuilder = new StringBuilder();
        for (String[] subtitle : subtitles) {
            combinedTextBuilder.append(subtitle[2]).append(" ");
        }
        String combinedText = combinedTextBuilder.toString().trim();
        String[] sentences = sentenceDetector.sentDetect(combinedText);

        if (sentences.length > 1) {
            initialChunks = sentenceBased(subtitles);
            // Check if any chunk has a duration greater than 30 seconds
            boolean needsReChunking = initialChunks.stream().anyMatch(chunk -> {
                double startTime = Double.parseDouble(chunk[0].replace("s", ""));
                double endTime = Double.parseDouble(chunk[1].replace("s", ""));
                return (endTime - startTime) > 30;
            });

            // If any chunk exceeds 30 seconds, re-chunk using the 10-second based method
            if (needsReChunking) {
                return timeBased(subtitles);
            } else {
                return initialChunks;
            }
        } else {
            return timeBased(subtitles);
        }
    }

    public static List<String[]> processVTT(String vttFilePath) throws IOException {
        List<String[]> subtitles = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(vttFilePath));
        String startTime = null;
        String endTime = null;

        Pattern timePattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) --> (\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

        for (String line : lines) {
            Matcher matcher = timePattern.matcher(line);
            if (matcher.find()) {
                startTime = formatSecondsToTimestamp(convertVTTTimeToSeconds(matcher.group(1)));
                endTime = formatSecondsToTimestamp(convertVTTTimeToSeconds(matcher.group(2)));
            } else if (!line.trim().isEmpty() && startTime != null && !line.startsWith("WEBVTT")) {
                String cleanedLine = cleanText(line);
                if (!cleanedLine.isEmpty()) {
                    subtitles.add(new String[] {startTime, endTime, cleanedLine});
                }
            }
        }

        List<String[]> filteredSubtitles = mergeRepeatedText(subtitles);
        return combineIntoChunks(filteredSubtitles);
    }
}
