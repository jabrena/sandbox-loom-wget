package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

final class SequentialDownloader extends BaseDownloader
{
    final CopyOnWriteArrayList<Throwable> exceptions = new CopyOnWriteArrayList<>();

    @Override
    public void get (URI source, Path target) throws Exception
    {
        if (! Files.isDirectory(target))
            throw new RuntimeException("target is not a directory");
        doGetRecursive(source, target, "");
    }

    final ArrayList<Path> links = new ArrayList<>();

    void doGetRecursive (URI source, Path target, String resource) throws Exception
    {
        final var path = doGet(source, target, resource);

        final var document = Jsoup.parse(path.toFile(), "UTF-8");

        for (var element : document.select("[src]"))
        {
            try { doGet( source, target, element.attr("src") ); }
            catch (Exception e) { exceptions.add(e); }
        }

        for (var element : document.select("link[href]"))
        {
            try { doGet( source, target, element.attr("href") ); }
            catch (Exception e) { exceptions.add(e); }
        }

        for (var element : document.select("a[href]"))
        {
            try { doGetRecursive( source, target, element.attr("href") ); }
            catch (Exception e) { exceptions.add(e); }
        }
    }
}
