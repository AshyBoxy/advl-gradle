package xyz.ashyboxy.advl.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class FileUtils {
    public static void deleteRecursively(Path dir) throws IOException {
        if (!dir.toFile().exists()) return;
        if (!Files.isDirectory(dir)) {
            dir.toFile().delete();
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
        }
    }

    public static void deleteRecursively(File dir) throws IOException {
        deleteRecursively(dir.toPath());
    }
}
