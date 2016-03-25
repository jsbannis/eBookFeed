package com.github.jsbannis.worker;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.github.jsbannis.data.Book;

/**
 * Created by jared on 3/16/2016.
 */
public class Parser
{
    public static final String BASE = "http://www.amazon.com/Best-Sellers-Kindle-Store/zgbs/digital-text/ref=zg_bs_fvp_p_f_digital-text?_encoding=UTF8&tf=1#";
    private static final int PAGES = 1;
    private static final int LONG_LIMIT = 128;

    public static void main(String[] args)
    {
        List<Book> books = new Parser().parse();
        books.forEach(System.out::println);
    }

    public List<Book> parse()
    {
        return IntStream.rangeClosed(1, PAGES)
            .mapToObj(Integer::valueOf)
            .flatMap(this::parsePage)
            .collect(Collectors.toList());
    }

    private Stream<Book> parsePage(int page)
    {
        try
        {
            Document doc = Jsoup.connect(getURL(page)).get();
            Elements bookElements = doc.getElementsByClass("zg_itemImmersion");
            return bookElements.stream().map(this::processBook);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    private Book processBook(Element bookElement)
    {
        String link = getAttributeBySelect(bookElement, "href", "div.zg_title", "a");
        DetailedInfo detailedInfo = getDetailedInfo(link);
        return new Book(
            detailedInfo._asin,
            getTextBySelect(bookElement, "span.zg_rankNumber"),
            getTextBySelect(bookElement, "div.zg_title", "a"),
            getTextBySelect(bookElement, "div.zg_byline"),
            link,
            getTextBySelect(bookElement, "div.zg_reviews", "span.a-icon-alt"),
            getTextBySelect(bookElement, "div.zg_price", "strong.price"),
            processImageString(getAttributeBySelect(bookElement, "src", "div.zg_image", "img")),
            detailedInfo._detailedInfo);
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

    private DetailedInfo getDetailedInfo(String bookUrl)
    {
        try
        {
            Document document = Jsoup.connect(bookUrl)
                .userAgent("Mozilla/5.0 Chrome/26.0.1410.64 Safari/537.31")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get();
            String detailedInfo = getTextBySelect(document, "div#bookDescription_feature_div", "noscript")
                .trim()
                .substring(0, LONG_LIMIT)
                + "...";

            String asin = "";
            Optional<Element> details = selectElement(document, "div#detail-bullets");
            if (details.isPresent())
            {
                asin = details.get().select("li").stream()
                    .map(element -> element.text().trim())
                    .filter(text -> text.startsWith("ASIN:"))
                    .map(text -> text.substring(5).trim())
                    .findFirst()
                    .orElse("");
            }
            return new DetailedInfo(asin, detailedInfo);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return new DetailedInfo("", "");
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
