package br.dev.pedrolamarao.loom.wget;

import java.net.URI;
import java.nio.file.Path;

public interface Downloader
{
    void get(URI source, Path target) throws Exception;
}
