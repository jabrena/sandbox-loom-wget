package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.net.URI;
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
    void doGetRecursive (URI source, String resource) throws Exception
    {
        final var bytes = doGet(source, resource);

        final var document = Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", source.toString());

        try (var executor = StructuredExecutor.open("foo", factory))
        {
            final var handler = new StructuredExecutor.ShutdownOnFailure();

            executor.fork(() ->
            {
                for (var element : document.select("[src]"))
                {
                    executor.fork(() ->
                    {
                        doGet( source, element.attr("src") );
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
                        doGet( source, element.attr("href") );
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
                        doGetRecursive( source, element.attr("href") );
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