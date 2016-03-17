package com.github.jsbannis;

import com.github.jsbannis.data.Book;
import com.github.jsbannis.rss.RssGenerator;
import com.github.jsbannis.worker.Parser;
import com.heroku.sdk.jdbc.DatabaseUrl;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ratpack.groovy.Groovy.groovyTemplate;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(b -> b
        .serverConfig(s -> s
          .baseDir(BaseDir.find())
          .env()
        )
        .registry(
          Guice.registry(s -> s
              .module(TextTemplateModule.class, conf ->
                  conf.setStaticallyCompile(true)
              )
          )
        )
        .handlers(c -> {
          c
            .get("index.html", ctx -> {
              ctx.redirect(301, "/");
            })
            .get(ctx -> ctx.render(groovyTemplate("index.html")))
            .get("books", ctx -> {
              ctx.getResponse().contentType("text/xml");
              ctx.getResponse().send(new RssGenerator().createRss(new Parser().parse()));
            })
            .get("db", ctx -> {
              Connection connection = null;
              Map<String, Object> attributes = new HashMap<>();
              try {
                boolean local = !"cedar-14".equals(System.getenv("STACK"));
                connection = DatabaseUrl.extract(local).getConnection();

                Statement stmt = connection.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
                stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
                ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

                ArrayList<String> output = new ArrayList<String>();
                while (rs.next()) {
                  output.add( "Read from DB: " + rs.getTimestamp("tick"));
                }

                attributes.put("results", output);
                ctx.render(groovyTemplate(attributes, "db.html"));
              } catch (Exception e) {
                attributes.put("message", "There was an error: " + e);
                ctx.render(groovyTemplate(attributes, "error.html"));
              } finally {
                if (connection != null) try{connection.close();} catch(SQLException e){}
              }
            })
            .files(f -> f.dir("public"));
        }
      )
    );
  }

  private static String booksToString(List<Book> books) {
    StringBuilder sb = new StringBuilder();
    books.forEach(book -> sb.append(book.toString()));
    return sb.toString();
  }
}
