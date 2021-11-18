package br.dev.pedrolamarao.loom.wget;

import java.net.URI;

public interface Downloader
{
    void test(URI source) throws Exception;
}
