package br.dev.pedrolamarao.loom.wget;

import java.net.URI;

public class DownloaderException extends RuntimeException
{
    final URI base;

    final String resource;

    public DownloaderException (URI base, String resource, Throwable cause)
    {
        super(cause);
        this.base = base;
        this.resource = resource;
    }

    @Override
    public String getMessage ()
    {
        return "download failed: base = " + base + ", resource = " + resource;
    }
}