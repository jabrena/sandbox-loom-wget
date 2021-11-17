package br.dev.pedrolamarao.loom.wget;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

abstract class BaseDownloader implements Downloader
{
    final HashMap<URI, Path> resources = new HashMap<>();

    Path doGet (URI source, Path target, String resource) throws Exception
    {
        final var uri0 = new URI(resource);
        final var uri1 = uri0.isAbsolute() ? uri0 : source.resolve(uri0);
        if (resources.containsKey(uri1)) return resources.get(uri1);
        final var path = Paths.get(
            target.toString(), uri1.getHost(),
            ( "".equals(resource) || "/".equals(resource) ? "index.html" : resource )
        );
        Files.createDirectories(path.getParent());
        try (var stream = uri1.toURL().openStream()) { Files.copy(stream, path); }
        resources.put(uri1, path);
        return path;
    }
}
