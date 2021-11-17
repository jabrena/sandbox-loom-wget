package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.*;

final class StructuredExecutorDownloader extends BaseDownloader
{
    final CopyOnWriteArrayList<Throwable> exceptions = new CopyOnWriteArrayList<>();

    final ThreadFactory factory;

    public StructuredExecutorDownloader (ThreadFactory factory)
    {
        this.factory = factory;
    }

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

        try (var executor = StructuredExecutor.open("foo", factory))
        {
            executor.fork(() ->
            {
                for (var element : document.select("[src]"))
                {
                    executor.fork(() ->
                    {
                        try { doGet( source, target, element.attr("src") ); }
                        catch (Exception e) { exceptions.add(e); }
                        return null;
                    });
                }
                return null;
            });

            executor.fork(() ->
            {
                for (var element : document.select("link[href]"))
                {
                    executor.fork(() ->
                    {
                        try { doGet( source, target, element.attr("href") ); }
                        catch (Exception e) { exceptions.add(e); }
                        return null;
                    });
                }
                return null;
            });

            executor.fork(() ->
            {
                for (var element : document.select("a[href]"))
                {
                    executor.fork(() ->
                    {
                        try { doGetRecursive( source, target, element.attr("href") ); }
                        catch (Exception e) { exceptions.add(e); }
                        return null;
                    });
                }
                return null;
            });

            executor.join();
        }
    }
}