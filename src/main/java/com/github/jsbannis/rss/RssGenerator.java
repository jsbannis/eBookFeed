package com.github.jsbannis.rss;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.github.jsbannis.data.Book;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;

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
                        entry.setPublishedDate(Date.from(book._created));

                        SyndContent description = new SyndContentImpl();
                        description.setType("text/html");
                        description.setValue(
                                "<img src=\"" + book._image + "\" height=\"450px\"/>"
                                        + "<br>" + book._byline
                                        + "<br> Rating: " + book._reviews
                                        + "<br> Price: " + book._price
                                        + " <p>" + book._detailedInfo);
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
