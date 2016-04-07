package com.github.jsbannis.worker;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.jsbannis.data.Book;
import com.heroku.sdk.jdbc.DatabaseUrl;

/**
 * Created by jared on 3/25/2016.
 */
public class IndexWorker
{
    private final static Logger LOG = LoggerFactory.getLogger(IndexWorker.class);

    public void doWork() throws URISyntaxException, SQLException
    {
        LOG.info("Crawling pages...");
        List<Book> books = new Parser().parse();
        LOG.info("Crawling complete.");

        LOG.info("Connecting to database...");
        Connection connection;
        connection = DatabaseUrl.extract().getConnection();
        LOG.info("Connected {}", connection);
        Statement lock = connection.createStatement();

        try
        {
            // Lock tables
            LOG.info("Starting database transaction...");
            lock.execute("BEGIN WORK");
            lock.execute("CREATE TABLE IF NOT EXISTS books "
                + "("
                + "asin text NOT NULL, "
                + "\"time\" timestamp without time zone NOT NULL, "
                + "title text, "
                + "byline text, "
                + "link text, "
                + "review text, "
                + "price text, "
                + "image text, "
                + "detail text, "
                + "CONSTRAINT \"primary\" PRIMARY KEY (asin)"
                + ")");
            lock.execute("CREATE TABLE IF NOT EXISTS history "
                + "("
                + "asin text NOT NULL, "
                + "\"time\" timestamp without time zone NOT NULL, "
                + "CONSTRAINT \"primary_history\" PRIMARY KEY (asin)"
                + ")");
            lock.execute("LOCK TABLE books");
            lock.execute("LOCK TABLE history");

            // Find books in database that are no longer on the list
            // TODO we can probably move some of the database stuff to Book class
            Set<String> asins = books.stream()
                .map(book -> book._asin)
                .collect(Collectors.toSet());
            LOG.info("Found {} books from crawling.", asins.size());

            Set<String> removeFromDb = new HashSet<>();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT asin FROM books");
            while (resultSet.next())
            {
                String asin = resultSet.getString("asin");
                if (asin != null)
                {
                    if (asins.contains(asin))
                    {
                        LOG.info("Index already contains book with ASIN={}", asin);
                        asins.remove(asin);
                    }
                    else
                    {
                        LOG.info("Obsolete entry in index for book with ASIN={} will be deleted", asin);
                        removeFromDb.add(asin);
                    }
                }
            }
            statement.close();

            // Remove books that are no longer in database
            LOG.info("Removing {} old books from index...", removeFromDb.size());
            if(!removeFromDb.isEmpty())
            {
                // TODO we can probably move some of the database stuff to Book class
                PreparedStatement deleteBookStatement = connection.prepareStatement(
                    "DELETE FROM books WHERE asin = ?");
                PreparedStatement moveToHistoryStatement = connection.prepareStatement(
                    "INSERT INTO history VALUES (?, now())");
                for (String asin : removeFromDb)
                {
                    LOG.info("Removing ASIN={}", asin);
                    deleteBookStatement.setString(1, asin);
                    deleteBookStatement.addBatch();
                    LOG.info("Adding history entry for ASIN={}", asin);
                    moveToHistoryStatement.setString(1, asin);
                    moveToHistoryStatement.addBatch();
                }
                deleteBookStatement.executeBatch();
                deleteBookStatement.close();
                moveToHistoryStatement.executeBatch();
                moveToHistoryStatement.close();
            }
            LOG.info("Removing old books complete.");

            LOG.info("Searching history for duplicates...");
            PreparedStatement duplicateStatement = connection.prepareStatement(
                "SELECT asin FROM history WHERE asin = ?");
            asins = asins.stream()
                .filter(asin -> isInHistory(duplicateStatement, asin))
                .collect(Collectors.toSet());

            LOG.info("Adding {} new books into index...", asins.size());
            if(!asins.isEmpty())
            {
                // Add books that are new
                // TODO we can probably move some of the database stuff to Book class
                PreparedStatement addBookStatement = connection.prepareStatement(
                    "INSERT INTO books VALUES ("
                        + "?," // ASIN
                        + "?," // Index date / time
                        + "?," // Title
                        + "?," // Byline
                        + "?," // Link
                        + "?," // Review
                        + "?," // Price
                        + "?," // Image
                        + "?)" // Detailed info
                );
                for (Book book : books)
                {
                    if (!asins.contains(book._asin))
                        continue;
                    addBookStatement.setString(1, book._asin);
                    addBookStatement.setTimestamp(2, Timestamp.from(book._created));
                    addBookStatement.setString(3, book._title);
                    addBookStatement.setString(4, book._byline);
                    addBookStatement.setString(5, book._link);
                    addBookStatement.setString(6, book._reviews);
                    addBookStatement.setString(7, book._price);
                    addBookStatement.setString(8, book._image);
                    addBookStatement.setString(9, book._detailedInfo);
                    addBookStatement.addBatch();
                    LOG.info("Adding ASIN={}", book._asin);
                }
                addBookStatement.executeBatch();
                addBookStatement.close();
            }
            LOG.info("Adding new books complete.");
        }
        finally
        {
            LOG.info("Ending database transaction...");
            lock.execute("COMMIT WORK;");
            lock.close();
            connection.close();
            LOG.info("Done.");
        }
    }
    private boolean isInHistory(PreparedStatement duplicateStatement, String asin)
    {
        try
        {
            duplicateStatement.setString(1, asin);
            ResultSet rs = duplicateStatement.executeQuery();
            boolean notInHistory = !rs.next();
            if(!notInHistory)
            {
                LOG.info("ASIN={} was found in the history and will be ignored", asin);
            }
            return notInHistory;
        }
        catch (SQLException e)
        {
            LOG.warn("Exception when searching for duplicate ASIN=" + asin, e);
        }
        return true;
    }
}
