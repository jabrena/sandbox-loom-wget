package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.net.URI;

final class SequentialDownloader extends BaseDownloader
{
    @Override
    void doGetRecursive (URI source, String resource) throws Exception
    {
        final var bytes = doGet(source, resource);

        final var document = Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", source.toString());

        for (var element : document.select("[src]"))
        {
            doGet( source, element.attr("src") );
        }

        for (var element : document.select("link[href]"))
        {
            doGet( source, element.attr("href") );
        }

        for (var element : document.select("a[href]"))
        {
            doGetRecursive( source, element.attr("href") );
        }
    }
}