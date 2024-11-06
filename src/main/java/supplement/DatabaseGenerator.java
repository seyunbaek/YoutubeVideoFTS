package supplement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseGenerator {

    public static void main(String[] args) {
        String fileName = "Data/Backup/database.db"; // Specify the database file name here
        String url = "jdbc:sqlite:" + fileName;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            if (conn != null) {
                // SQL statements for creating tables
                String createChannelsTable = """
                        CREATE TABLE "Channels" (
                        	"channelName"	TEXT,
                        	"channelID"	TEXT,
                        	"URL"	TEXT,
                        	"numVideos"	INTEGER,
                        	PRIMARY KEY("channelID")
                        );
                        """;

                String createVideosTable = """
                        CREATE TABLE "Videos" (
                        	"channel"	TEXT,
                        	"videoID"	TEXT,
                        	"thumnailURL"	TEXT,
                        	"uploadDate"	TEXT,
                        	"transcribed"	INTEGER,
                        	"indexed"	INTEGER,
                        	"numLines"	INTEGER,
                        	PRIMARY KEY("videoID")
                        );
                        """;

                String createTranscriptionsTable = """
                        CREATE TABLE "Transcriptions" (
                        	"id"	INTEGER,
                        	"videoID"	TEXT,
                        	"startTime"	INTEGER,
                        	"endTime"	INTEGER,
                        	"text"	TEXT,
                        	"indexed"	INTEGER,
                        	PRIMARY KEY("id")
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