package xyz.ashyboxy.advl.gradle.mc;

import xyz.ashyboxy.advl.gradle.Consts;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

public record AssetIndex(boolean map_to_resources, Map<String, Entry> objects) {
    public record Entry(String hash, int size) {
        public URL getDownloadUrl() {
            try {
                return URI.create(Consts.ASSETS_URL + getPath()).toURL();
            } catch (MalformedURLException e) {
                // shouldn't happen
                throw new RuntimeException(e);
            }
        }

        public Path getPath() {
            return Path.of(hash.substring(0, 2), hash);
        }
    }
}
