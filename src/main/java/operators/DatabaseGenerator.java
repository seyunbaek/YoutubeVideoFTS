package operators;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseGenerator {

    public static void main(String[] args) {
        String fileName = "Data/Test/testDB.db"; // Specify the database file name here
        String url = "jdbc:sqlite:" + fileName;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            if (conn != null) {
                // SQL statements for creating tables
                String createChannelsTable = """
                        CREATE TABLE IF NOT EXISTS Channels (
                            channelName TEXT,
                            channelID TEXT,
                            URL TEXT,
                            numVideos INTEGER
                        );
                        """;

                String createVideosTable = """
                        CREATE TABLE IF NOT EXISTS Videos (
                            channel TEXT,
                            videoID TEXT,
                            thumnailURL TEXT,
                            uploadDate TEXT,
                            transcribed INTEGER,
                            indexed INTEGER,
                            numSentences INTEGER
                        );
                        """;

                String createTranscriptionsTable = """
                        CREATE TABLE IF NOT EXISTS Transcriptions (
                            videoID TEXT,
                            startTime INTEGER,
                            endTime INTEGER,
                            text TEXT,
                            indexed INTEGER
                        );
                        """;

                // Execute table creation statements
                stmt.execute(createChannelsTable);
                stmt.execute(createVideosTable);
                stmt.execute(createTranscriptionsTable);

                System.out.println("Tables created successfully.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}