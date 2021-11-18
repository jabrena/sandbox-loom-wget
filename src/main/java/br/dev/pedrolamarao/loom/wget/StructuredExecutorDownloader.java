package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.StructuredExecutor;
import java.util.concurrent.ThreadFactory;

final class StructuredExecutorDownloader extends BaseDownloader
{
    final ThreadFactory factory;

    public StructuredExecutorDownloader (ThreadFactory factory)
    {
        this.factory = factory;
    }

    @Override
    void doGetRecursive (URI source, Path target, String resource) throws Exception
    {
        final var path = doGet(source, target, resource);

        final var document = Jsoup.parse(path.toFile(), "UTF-8");

        try (var executor = StructuredExecutor.open("foo", factory))
        {
            final var handler = new StructuredExecutor.ShutdownOnFailure();

            executor.fork(() ->
            {
                for (var element : document.select("[src]"))
                {
                    executor.fork(() ->
                    {
                        doGet( source, target, element.attr("src") );
                        return null;
                    },
                    handler);
                }
                return null;
            },
            handler);

            executor.fork(() ->
            {
                for (var element : document.select("link[href]"))
                {
                    executor.fork(() ->
                    {
                        doGet( source, target, element.attr("href") );
                        return null;
                    },
                    handler);
                }
                return null;
            },
            handler);

            executor.fork(() ->
            {
                for (var element : document.select("a[href]"))
                {
                    executor.fork(() ->
                    {
                        doGetRecursive( source, target, element.attr("href") );
                        return null;
                    },
                    handler);
                }
                return null;
            },
            handler);

            executor.join();

            handler.throwIfFailed();
        }
    }
}