package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
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
    void doGetRecursive (URI source, Path target, String resource) throws Exception
    {
        final var path = doGet(source, target, resource);

        final var document = Jsoup.parse(path.toFile(), "UTF-8");

        final var phaser = new Phaser(1);

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                for (var element : document.select("[src]"))
                {
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try {
                                if (! exceptions.isEmpty()) return;
                                doGet( source, target, element.attr("src") );
                            }
                            catch (Exception e) { exceptions.add(e); }
                            finally { phaser.arriveAndDeregister(); }
                        })
                    );
                }
                phaser.arriveAndDeregister();
            })
        );

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                for (var element : document.select("link[href]"))
                {
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try {
                                if (! exceptions.isEmpty()) return;
                                doGet( source, target, element.attr("href") );
                            }
                            catch (Exception e) { exceptions.add(e); }
                            finally { phaser.arriveAndDeregister(); }
                        })
                    );
                }
                phaser.arriveAndDeregister();
            })
        );

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                for (var element : document.select("a[href]"))
                {
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try {
                                if (! exceptions.isEmpty()) return;
                                doGetRecursive( source, target, element.attr("href") );
                            }
                            catch (Exception e) { exceptions.add(e); }
                            finally { phaser.arriveAndDeregister(); }
                        })
                    );
                }
                phaser.arriveAndDeregister();
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