package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.*;

final class ExecutorDownloader extends BaseDownloader
{
    final LinkedBlockingQueue<Exception> exceptions = new LinkedBlockingQueue<>();

    final ExecutorService executor;

    final LinkedBlockingQueue<Future<?>> futures = new LinkedBlockingQueue<>();

    public ExecutorDownloader (ExecutorService executor)
    {
        this.executor = executor;
    }

    @Override
    void doGetRecursive (URI source, String resource) throws Exception
    {
        final var bytes = doGet(source, resource);

        final var document = Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", source.toString());

        final var phaser = new Phaser(1);

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                try
                {
                    for (var element : document.select("[src]"))
                    {
                        if (! exceptions.isEmpty()) { executor.shutdown(); break; }
                        phaser.register();
                        futures.add(
                            executor.submit(() ->
                            {
                                try {
                                    if (! exceptions.isEmpty()) { executor.shutdown(); return; }
                                    doGet( source, element.attr("src") );
                                }
                                catch (Exception e) { exceptions.add(e); }
                                finally { phaser.arriveAndDeregister(); }
                            })
                        );
                    }
                }
                finally { phaser.arriveAndDeregister(); }
            })
        );

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                try
                {
                    for (var element : document.select("link[href]"))
                    {
                        if (! exceptions.isEmpty()) { executor.shutdown(); break; }
                        phaser.register();
                        futures.add(
                            executor.submit(() ->
                            {
                                try {
                                    if (! exceptions.isEmpty()) { executor.shutdown(); return; }
                                    doGet( source, element.attr("href") );
                                }
                                catch (Exception e) { exceptions.add(e); }
                                finally { phaser.arriveAndDeregister(); }
                            })
                        );
                    }
                }
                finally { phaser.arriveAndDeregister(); }
            })
        );

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                try
                {
                    for (var element : document.select("a[href]"))
                    {
                        if (! exceptions.isEmpty()) { executor.shutdown(); break; }
                        phaser.register();
                        futures.add(
                            executor.submit(() ->
                            {
                                try {
                                    if (! exceptions.isEmpty()) { executor.shutdown(); return; }
                                    doGetRecursive( source, element.attr("href") );
                                }
                                catch (Exception e) { exceptions.add(e); }
                                finally { phaser.arriveAndDeregister(); }
                            })
                        );
                    }
                }
                finally { phaser.arriveAndDeregister(); }
            })
        );

        phaser.arriveAndAwaitAdvance();

        for (var future : futures) {
            future.get();
        }

        executor.shutdownNow();

        if (! exceptions.isEmpty()) {
            throw exceptions.peek();
        }
    }
}