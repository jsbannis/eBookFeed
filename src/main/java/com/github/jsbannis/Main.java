package com.github.jsbannis;

import com.github.jsbannis.data.BookLoader;
import com.github.jsbannis.rss.RssGenerator;
import com.github.jsbannis.worker.IndexWorker;
import static ratpack.groovy.Groovy.groovyTemplate;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

public class Main
{
    public static void main(String... args) throws Exception
    {
        if(args == null || args.length == 0 || args[0].equalsIgnoreCase("web"))
        {
            startWeb();
        }
        else if (args.length == 1 && args[0].equalsIgnoreCase("index"))
        {
            startIndexing();
        }
    }
    private static void startIndexing() throws Exception
    {
        new IndexWorker().doWork();
    }
    private static void startWeb() throws Exception
    {
        RatpackServer
            .start(b -> b
            .serverConfig(s -> s
                .baseDir(BaseDir.find())
                .env()
            )
            .registry(
                Guice.registry(s -> s
                    .module(TextTemplateModule.class,
                        conf -> conf.setStaticallyCompile(true)))
            )
            .handlers(c -> {
                    c
                        .get("index.html", ctx -> {
                            ctx.redirect(301, "/");
                        })
                        .get(ctx -> ctx.render(groovyTemplate("index.html")))
                        .get("books", ctx -> {
                            ctx.getResponse().contentType("text/xml");
                            ctx.getResponse().send(
                                new RssGenerator().createRss(
                                    new BookLoader().loadBooks()));
                        });
                }
            )
        );
    }
}
