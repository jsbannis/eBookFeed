package com.github.jsbannis.data;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.heroku.sdk.jdbc.DatabaseUrl;

/**
 * @author Jared Bannister
 */
public class BookLoader
{
    private final static Logger LOG = LoggerFactory.getLogger(BookLoader.class);

    public List<Book> loadBooks() throws URISyntaxException, SQLException
    {
        List<Book> ret = new ArrayList<>();

        LOG.debug("Connecting to database...");
        Connection connection;
        connection = DatabaseUrl.extract().getConnection();
        LOG.debug("Connected {}", connection);

        LOG.debug("Querying for books...");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT * FROM books ORDER BY time");
        while(resultSet.next())
        {
            String asin = resultSet.getString("asin");
            // TODO time
            String title = resultSet.getString("title");
            String byline = resultSet.getString("byline");
            String link = resultSet.getString("link");
            String review = resultSet.getString("review");
            String price = resultSet.getString("price");
            String image = resultSet.getString("image");
            String detail = resultSet.getString("detail");
            Book book = new Book(asin, "", title, byline, link, review, price, image, detail);
            ret.add(book);
            LOG.debug("Adding book {}", book);
        }
        statement.close();
        connection.close();
        LOG.debug("Done.");
        return ret;
    }
}
