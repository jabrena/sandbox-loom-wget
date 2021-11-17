package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.*;

final class ExecutorDownloader extends BaseDownloader
{
    final CopyOnWriteArrayList<Throwable> exceptions = new CopyOnWriteArrayList<>();

    final ExecutorService executor;

    final LinkedBlockingQueue<Future<?>> futures = new LinkedBlockingQueue<>();

    public ExecutorDownloader (ExecutorService executor)
    {
        this.executor = executor;
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

        final var phaser = new Phaser(1);

        phaser.register();
        futures.add(
            executor.submit(() ->
            {
                for (var element : document.select("[src]"))
                {
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try { doGet( source, target, element.attr("src") ); }
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
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try { doGet( source, target, element.attr("href") ); }
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
                    phaser.register();
                    futures.add(
                        executor.submit(() ->
                        {
                            try { doGetRecursive( source, target, element.attr("href") ); }
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
    }
}
