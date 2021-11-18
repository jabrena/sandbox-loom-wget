package br.dev.pedrolamarao.loom.wget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

import static java.lang.System.err;

class DownloaderTest
{
    static final URI source = URI.create("https://blank.org/");

    @Test
    void sequential (@TempDir Path tmp) throws Exception
    {
        err.printf("sequential: tmp = %s%n", tmp);
        final var downloader = new SequentialDownloader();
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("sequential: elapsed = %s%n", elapsed);
    }

    @Test
    void thread (@TempDir Path tmp) throws Exception
    {
        err.printf("thread: tmp = %s%n", tmp);
        final var downloader = new ThreadDownloader(Thread.ofPlatform().factory());
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("thread: elapsed = %s%n", elapsed);
    }

    @Test
    void threadVirtual (@TempDir Path tmp) throws Exception
    {
        err.printf("threadVirtual: tmp = %s%n", tmp);
        final var downloader = new ThreadDownloader(Thread.ofVirtual().factory());
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("threadVirtual: elapsed = %s%n", elapsed);
    }

    @Test
    void executor (@TempDir Path tmp) throws Exception
    {
        err.printf("executor: tmp = %s%n", tmp);
        final var downloader = new ExecutorDownloader(Executors.newCachedThreadPool(Thread.ofPlatform().factory()));
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("executor: elapsed = %s%n", elapsed);
    }

    @Test
    void executorVirtual (@TempDir Path tmp) throws Exception
    {
        err.printf("executor: tmp = %s%n", tmp);
        final var downloader = new ExecutorDownloader(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("executor: elapsed = %s%n", elapsed);
    }

    @Test
    void structuredExecutor (@TempDir Path tmp) throws Exception
    {
        err.printf("structuredExecutor: tmp = %s%n", tmp);
        final var downloader = new StructuredExecutorDownloader(Thread.ofPlatform().factory());
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("structuredExecutor: elapsed = %s%n", elapsed);
    }

    @Test
    void structuredExecutorVirtual (@TempDir Path tmp) throws Exception
    {
        err.printf("structuredExecutorVirtual: tmp = %s%n", tmp);
        final var downloader = new StructuredExecutorDownloader(Thread.ofVirtual().factory());
        final var start = Instant.now();
        downloader.get(source, tmp);
        final var finish = Instant.now();
        final var elapsed = Duration.between(start, finish);
        err.printf("structuredExecutorVirtual: elapsed = %s%n", elapsed);
    }
}
