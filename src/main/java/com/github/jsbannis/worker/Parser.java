package com.github.jsbannis.worker;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.jsbannis.data.Book;

/**
 * Created by jared on 3/16/2016.
 */
public class Parser
{
    private final static Logger LOG = LoggerFactory.getLogger(Parser.class);

    public static final String BASE = "http://www.amazon.com/Best-Sellers-Kindle-Store/zgbs/digital-text/?_encoding=UTF8&tf=1&pg=";
    private static final int PAGES = 5;

    public static final int TIMEOUT = 5000;
    private static final int ATTEMPTS = 5;

    private long _timeOffset = 0;
    private final static long OFFSET_INCREMENT = 1000;
    private Instant _publishTime = Instant.now();

    public List<Book> parse()
    {
        _publishTime = Instant.now();
        _timeOffset = 0;

        LOG.info("Begin crawling...");
        return IntStream.rangeClosed(1, PAGES)
            .mapToObj(Integer::valueOf)
            .flatMap(this::parsePage)
            .collect(Collectors.toList());
    }

    private Stream<Book> parsePage(int page)
    {
        String url = getURL(page);
        LOG.info("Parsing page {} at URL \"{}\"", page, url);
        Optional<Document> doc = getDocument(url, 0);

        if(!doc.isPresent())
            return Stream.empty();

        Elements bookElements = doc.get().getElementsByClass("zg_itemImmersion");
        return bookElements.stream()
            .map(this::processBook)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Optional<Book> processBook(Element bookElement)
    {
        String link = getAttributeBySelect(bookElement, "href", "div.zg_title", "a");
        Optional<DetailedInfo> detailedInfo = getDetailedInfo(link);
        if(!detailedInfo.isPresent())
            return Optional.empty();
        Book book = new Book(
            detailedInfo.get()._asin,
            getTextBySelect(bookElement, "span.zg_rankNumber"),
            getTextBySelect(bookElement, "div.zg_title", "a"),
            getTextBySelect(bookElement, "div.zg_byline"),
            link,
            getTextBySelect(bookElement, "div.zg_reviews", "span.a-icon-alt"),
            getTextBySelect(bookElement, "div.zg_price", "strong.price"),
            processImageString(getAttributeBySelect(bookElement, "src", "div.zg_image", "img")),
            detailedInfo.get()._detailedInfo,
            getPublishTime());
        LOG.info("Found book {}", book);
        return Optional.of(book);
    }

    /**
     * We crawl in the forward direction, but want the books to have a created
     * time such that bigger rank = older (the 'worse' books show up later
     * in the feed).
     * <p>
     * We have a fixed publish time and decrement each by a second to push the
     * worse books into the past.
     */
    private Instant getPublishTime()
    {
        Instant time = _publishTime.minusMillis(_timeOffset);
        _timeOffset += OFFSET_INCREMENT;
        return time;
    }

    private String processImageString(String imageString)
    {
        int z = imageString.lastIndexOf('/');
        int i = imageString.indexOf('.', z);
        if (i > 0)
        {
            imageString = imageString.substring(0, i) + ".jpg";
        }
        return imageString;
    }

    private String getTextBySelect(Element bookElement, String... selects)
    {
        Optional<Element> element = selectElement(bookElement, selects);
        return element.isPresent() ? element.get().text().trim() : "";
    }

    private Optional<Element> selectElement(Element bookElement, String... selects)
    {
        Element element = bookElement;
        for (String select : selects)
        {
            element = element.select(select).first();
            if (element == null)
                return Optional.empty();
        }
        return Optional.of(element);
    }

    private String getAttributeBySelect(Element bookElement, String attribute, String... selects)
    {
        Optional<Element> element = selectElement(bookElement, selects);
        return element.isPresent() ? element.get().attr(attribute).trim() : "";
    }

    private String getURL(int page)
    {
        return BASE + Integer.toString(page);
    }

    private Optional<DetailedInfo> getDetailedInfo(String bookUrl)
    {
        Optional<Document> document = getDocument(bookUrl, 1);
        if(!document.isPresent())
            return Optional.empty();

        String description = getTextBySelect(
            document.get(),
            "div#bookDescription_feature_div",
            "noscript")
            .trim();

        Optional<Element> details = selectElement(
            document.get(), "div#detail-bullets");
        if (!details.isPresent())
            return Optional.empty();

        String asin = details.get().select("li").stream()
            .map(element -> element.text().trim())
            .filter(text -> text.startsWith("ASIN:"))
            .map(text -> text.substring(5).trim())
            .findFirst()
            .orElse("");
        if(asin.trim().isEmpty())
            return Optional.empty();

        return Optional.of(new DetailedInfo(asin, description));
    }

    private Optional<Document> getDocument(String url, int attempt)
    {
        try
        {
            return Optional.of(Jsoup.connect(url)
                .userAgent("Mozilla/5.0 Chrome/26.0.1410.64 Safari/537.31")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .get());
        }
        catch (IOException e)
        {
            if (attempt < ATTEMPTS)
            {
                LOG.info("Get page request for \"{}\" failed. Attempt {} of {}. Retrying...", url, attempt, ATTEMPTS);
                return getDocument(url, attempt + 1);
            }
            LOG.warn("Get page request failed on final attempt. Giving up.");
            return Optional.empty();
        }
    }

    private class DetailedInfo
    {
        final String _asin;
        final String _detailedInfo;
        private DetailedInfo(String asin, String detailedInfo)
        {
            assert asin != null;
            assert detailedInfo != null;

            _asin = asin;
            _detailedInfo = detailedInfo;
        }
    }
}
