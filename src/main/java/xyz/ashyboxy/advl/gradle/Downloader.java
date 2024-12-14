package xyz.ashyboxy.advl.gradle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class Downloader {
    static String tryDownloadText(File dst, URL src) throws IOException {
        tryDownload(dst, src);
        return Files.readString(dst.toPath());
    }

    static void tryDownload(File dst, URL src) throws IOException {
        if (dst.exists()) return;

        dst.getParentFile().mkdirs();

        try (BufferedInputStream in = new BufferedInputStream(src.openStream());
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1)
                out.write(buffer, 0, read);
        }
    }
}
