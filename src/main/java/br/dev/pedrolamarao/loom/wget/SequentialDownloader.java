package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

final class SequentialDownloader extends BaseDownloader
{
    @Override
    void doGetRecursive (URI source, Path target, String resource) throws Exception
    {
        final var path = doGet(source, target, resource);

        final var document = Jsoup.parse(path.toFile(), "UTF-8");

        for (var element : document.select("[src]"))
        {
            doGet( source, target, element.attr("src") );
        }

        for (var element : document.select("link[href]"))
        {
            doGet( source, target, element.attr("href") );
        }

        for (var element : document.select("a[href]"))
        {
            doGetRecursive( source, target, element.attr("href") );
        }
    }
}