package com.github.jsbannis.rss;

import com.github.jsbannis.data.Book;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;

import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jared on 3/16/2016.
 */
public class RssGenerator {
    public String createRss(List<Book> books) {
        try {
            String feedType = "rss_2.0";

            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType(feedType);

            feed.setTitle("Amazon Top Free Kindle Books");
            feed.setLink("https://pure-shelf-61800.herokuapp.com/books");
            feed.setDescription("Amazon Top Free Kindle Books");

            List<SyndEntry> entries = books.stream()
                    .map(book -> {
                        SyndEntry entry = new SyndEntryImpl();
                        entry.setTitle(book._title);
                        entry.setLink(book._link);
                        entry.setPublishedDate(Date.from(Instant.now()));

                        SyndContent description = new SyndContentImpl();
                        description.setType("text/html");
                        description.setValue(
                                "<img src=\"" + book._image + "\"/>"
                                        + "<p>" + book._byline
                                        + "<p>Rating: " + book._reviews
                                        + "<p>Price: " + book._price);
                        entry.setDescription(description);
                        return entry;
                    })
                    .collect(Collectors.toList());
            feed.setEntries(entries);

            String result;
            try(Writer writer = new StringWriter()) {
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(feed, writer);
                result = writer.toString();
            }
            return result;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
