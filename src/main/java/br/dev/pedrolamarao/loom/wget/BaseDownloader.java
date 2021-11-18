package br.dev.pedrolamarao.loom.wget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;

abstract class BaseDownloader implements Downloader
{
    final HashMap<URI, byte[]> resources = new HashMap<>();

    @Override
    public final void test (URI source) throws Exception
    {
        doGetRecursive(source, "");
    }

    abstract void doGetRecursive (URI source, String resource) throws Exception;

    final byte[] doGet (URI source, String resource) throws DownloaderException
    {
        try
        {
            final var uri = source.resolve( new URI(resource) );
            if (resources.containsKey(uri)) return resources.get(uri);
            final var buffer = new ByteArrayOutputStream();
            try (var stream = uri.toURL().openStream()) { copy(stream, buffer); }
            resources.put(uri, buffer.toByteArray());
            return buffer.toByteArray();
        }
        catch (Exception e)
        {
            throw new DownloaderException(source, resource, e);
        }
    }

    static void copy (InputStream source, OutputStream sink) throws IOException
    {
        final var buffer = new byte[8192];
        while (true) {
            final var read = source.read(buffer);
            if (read == -1) break;
            sink.write(buffer, 0, read);
        }
    }
}
