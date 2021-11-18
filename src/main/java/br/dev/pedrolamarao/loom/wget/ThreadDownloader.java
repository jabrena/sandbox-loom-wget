package br.dev.pedrolamarao.loom.wget;

import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;

final class ThreadDownloader extends BaseDownloader
{
    final LinkedBlockingQueue<Exception> exceptions = new LinkedBlockingQueue<>();

    final ThreadFactory factory;

    final LinkedBlockingQueue<Thread> threads = new LinkedBlockingQueue<>();

    public ThreadDownloader (ThreadFactory factory)
    {
        this.factory = factory;
    }

    @Override
    void doGetRecursive (URI source, String resource) throws Exception
    {
        final var bytes = doGet(source, resource);

        final var document = Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", source.toString());

        final var phaser = new Phaser(1);

        {
            phaser.register();
            final var thread = factory.newThread(() ->
            {
                for (var element : document.select("[src]"))
                {
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try {
                            if (! exceptions.isEmpty()) return;
                            doGet( source, element.attr("src") );
                        }
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
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try {
                            if (! exceptions.isEmpty()) return;
                            doGet( source, element.attr("href") );
                        }
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
                    if (! exceptions.isEmpty()) break;
                    phaser.register();
                    final var thread_ = factory.newThread(() ->
                    {
                        try {
                            if (! exceptions.isEmpty()) return;
                            doGetRecursive( source, element.attr("href") );
                        }
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

        if (! exceptions.isEmpty()) {
            throw exceptions.peek();
        }
    }
}