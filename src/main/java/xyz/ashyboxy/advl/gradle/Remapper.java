package xyz.ashyboxy.advl.gradle;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Remapper {
    // from 21w37a
    public static final Pattern MC_LV_NAME_PATTERN = Pattern.compile("\\$\\$\\d+");
    public static final Map<String, String> JAVAX_TO_JETBRAINS = Map.of(
			"javax/annotation/Nullable", "org/jetbrains/annotations/Nullable",
			"javax/annotation/Nonnull", "org/jetbrains/annotations/NotNull",
			"javax/annotation/concurrent/Immutable", "org/jetbrains/annotations/Unmodifiable"
    );

    public static void maybeRemap(File src, File dst, File mappings) throws IOException {
        if (!src.exists()) throw new RuntimeException(src + " does not exist");
        if (dst.exists()) return;
        if (!mappings.exists()) throw new RuntimeException(mappings + " does not exist");
        remap(src.toPath(), dst.toPath(), mappings.toPath());
    }

    public static void tryRemap(File src, File dst, File mappings) throws IOException {
        MinecraftMetadata.download();
        if (dst.exists() && !dst.delete()) throw new RuntimeException("Could not delete " + dst);
        remap(src.toPath(), dst.toPath(), mappings.toPath());
    }

    public static void remap(Path src, Path dst, Path mappings) throws IOException {
        MemoryMappingTree tree = new MemoryMappingTree();
        MappingFormat mappingFormat = MappingReader.detectFormat(mappings);
        MappingReader.read(mappings, mappingFormat, tree);

        // either proguard has its src and dst namespaces reversed or i'm dumb
        String srcNamespace;
        String dstNamespace;
        if (mappingFormat == MappingFormat.PROGUARD_FILE) {
            srcNamespace = "target";
            dstNamespace = "source";
        } else if (mappingFormat == MappingFormat.TINY_FILE || mappingFormat == MappingFormat.TINY_2_FILE) {
            srcNamespace = "official";
            dstNamespace = "named";
        } else {
            throw new UnsupportedOperationException("Unsupported mapping format: " + mappingFormat);
        }

        AdvlGradlePlugin.logger.lifecycle("Remapping {} from {} to {} using {}", src.getFileName(), srcNamespace, dstNamespace, mappings.getFileName());

        IMappingProvider provider = TinyUtils.createMappingProvider(tree, srcNamespace, dstNamespace);
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(provider)
                .withMappings((out -> JAVAX_TO_JETBRAINS.forEach(out::acceptClass)))
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .inferNameFromSameLvIndex(true)
                .invalidLvNamePattern(MC_LV_NAME_PATTERN)
                .build();

        try (OutputConsumerPath output = new OutputConsumerPath.Builder(dst).build()) {
            output.addNonClassFiles(src);
            remapper.readInputs(src);
            remapper.apply(output);
            remapper.finish();
        } catch (Exception e) {
            throw new RuntimeException("Remapping failed", e);
        }

        AdvlGradlePlugin.logger.lifecycle("Finished remapping");

        URI dstUri = URI.create("jar:" + dst.toUri());
        AdvlGradlePlugin.logger.lifecycle("Deleting META-INF in {}", dstUri);
        try (FileSystem dstFs = FileSystems.newFileSystem(dstUri, Map.of("create", "false"))) {
            Path metaInfPath = dstFs.getPath("META-INF");
            try (Stream<Path> dirStream = Files.walk(metaInfPath)) {
                dirStream
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            if (Files.exists(metaInfPath)) throw new IOException("Could not delete META-INF");
        }
    }
}
