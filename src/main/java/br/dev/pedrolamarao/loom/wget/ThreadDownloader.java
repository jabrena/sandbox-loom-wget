package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;

final class ThreadDownloader extends BaseDownloader
{
    final CopyOnWriteArrayList<Throwable> exceptions = new CopyOnWriteArrayList<>();

    final ThreadFactory factory;

    final LinkedBlockingQueue<Thread> threads = new LinkedBlockingQueue<>();

    public ThreadDownloader(ThreadFactory factory)
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

    void doGetRecursive (URI source, Path target, String resource) throws Exception
    {
        final var path = doGet(source, target, resource);

        final var document = Jsoup.parse(path.toFile(), "UTF-8");

        final var phaser = new Phaser(1);

        {
            phaser.register();
            final var thread = factory.newThread(() ->
            {
                for (var element : document.select("[src]"))
                {
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try { doGet( source, target, element.attr("src") ); }
                        catch (Exception e) { exceptions.add(e); }
                        finally { phaser.arriveAndDeregister(); }
                    });
                    thread_.start();
                    threads.add(thread_);
                }
                phaser.arriveAndDeregister();
            });
            thread.start();
            threads.add(thread);
        }

        {
            phaser.register();
            final var thread = factory.newThread(() ->
            {
                for (var element : document.select("link[href]"))
                {
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try { doGet( source, target, element.attr("href") ); }
                        catch (Exception e) { exceptions.add(e); }
                        finally { phaser.arriveAndDeregister(); }
                    });
                    thread_.start();
                    threads.add(thread_);
                }
                phaser.arriveAndDeregister();
            });
            thread.start();
            threads.add(thread);
        }

        {
            phaser.register();
            final var thread = factory.newThread(() ->
            {
                for (var element : document.select("a[href]"))
                {
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try { doGetRecursive( source, target, element.attr("href") ); }
                        catch (Exception e) { exceptions.add(e); }
                        finally { phaser.arriveAndDeregister(); }
                    });
                    thread_.start();
                    threads.add(thread_);
                }
                phaser.arriveAndDeregister();
            });
            thread.start();
            threads.add(thread);
        }

        phaser.arriveAndAwaitAdvance();

        for (var thread : threads) {
            thread.join();
        }
    }
}
