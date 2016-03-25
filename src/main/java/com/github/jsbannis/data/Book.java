package com.github.jsbannis.data;

import java.time.Instant;

/**
 * Created by jared on 3/16/2016.
 */
public class Book
{
    public final String _asin;
    public final String _rank;
    public final String _title;
    public final String _byline;
    public final String _link;
    public final String _reviews;
    public final String _price;
    public final String _image;
    public final String _detailedInfo;
    public final Instant _created;

    public Book(
        String asin, String rank, String title, String byline, String link,
        String reviews, String price, String image, String detailedInfo,
        Instant created)
    {
        _rank = rank;
        _title = title;
        _byline = byline;
        _link = link;
        _reviews = reviews;
        _price = price;
        _image = image;
        _detailedInfo = detailedInfo;
        _asin = asin;
        _created = created;
    }

    @Override
    public String toString()
    {
        return "Book{" +
            "_asin='" + _asin + '\'' +
            "_created'" + _created.toString() + '\'' +
            " _rank='" + _rank + '\'' +
            ", _title='" + _title + '\'' +
            ", _byline='" + _byline + '\'' +
            ", _link='" + _link + '\'' +
            ", _reviews='" + _reviews + '\'' +
            ", _price='" + _price + '\'' +
            ", _image='" + _image + '\'' +
            ", _detailedInfo='" + _detailedInfo + '\'' +
            '}';
    }
}
