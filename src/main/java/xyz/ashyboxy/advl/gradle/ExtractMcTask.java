package xyz.ashyboxy.advl.gradle;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ExtractMcTask extends DefaultTask {
    @Inject
    public ExtractMcTask() {
        super();
        setGroup(Consts.TASK_GROUP);

        getShouldPatch().convention(true);
        getOutDir().convention("src/main");
        getMcJavaWhitelist().set(Consts.MC_EXTRACT_JAVA_WHITELIST);
        getMcAssetsWhitelist().set(Consts.MC_EXTRACT_ASSETS_WHITELIST);
    }

    @Input
    @Optional
    public abstract Property<Object> getPatchSource();

    @Input
    public abstract Property<Boolean> getShouldPatch();

    @Input
    public abstract Property<String> getOutDir();

    @Input
    public abstract ListProperty<String> getMcJavaWhitelist();

    @Input
    public abstract ListProperty<String> getMcAssetsWhitelist();

    @TaskAction
    public void extractMc() throws IOException {
        File jar = AdvlGradleExtension.get(getProject()).getSourcesJar().get().getAsFile();
        getLogger().lifecycle("Extracting Minecraft from {}", jar.getAbsolutePath());

        if (getShouldPatch().get()) {
            if (!getPatchSource().isPresent()) throw new IllegalStateException("Patch source not set");
            Object patchSource = getPatchSource().get();
            if (!(patchSource instanceof String || patchSource instanceof File))
                throw new IllegalArgumentException("Patch source must be a string or a file");
            getLogger().lifecycle("Patching using {}", getPatchSource().get());
        }

        File outDir = getProject().file(getOutDir().get());
        Path javaOutDir = outDir.toPath().resolve("java");
        Path assetsOutDir = outDir.toPath().resolve("resources");

        getLogger().lifecycle("Outputting to {}", outDir.getAbsolutePath());
        getLogger().lifecycle("Source code file whitelist: {}", getMcJavaWhitelist().get());
        getLogger().lifecycle("Assets file whitelist: {}", getMcAssetsWhitelist().get());

        javaOutDir.toFile().mkdirs();
        assetsOutDir.toFile().mkdirs();

        for (String path : getMcJavaWhitelist().get()) {
            Path wPath = javaOutDir.resolve(path);
            if (Files.exists(wPath)) {
                getLogger().lifecycle("Removing old {}", wPath);
                FileUtils.deleteRecursively(wPath);
            }
        }

        for (String path : getMcAssetsWhitelist().get()) {
            Path wPath = assetsOutDir.resolve(path);
            if (Files.exists(wPath)) {
                getLogger().lifecycle("Removing old {}", wPath);
                FileUtils.deleteRecursively(wPath);
            }
        }

        // extract
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
            ZipEntry entry = zis.getNextEntry();

            while (entry != null) {
                if (entry.isDirectory()) continue;

                Path out = (getMcJavaWhitelist().get().stream().anyMatch(entry.getName()::startsWith)
                        ? javaOutDir
                        : assetsOutDir)
                        .resolve(entry.getName());
                out.getParent().toFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                    fos.write(zis.readAllBytes());
                }

                out.toFile().setLastModified(entry.getTime());

                entry = zis.getNextEntry();
            }

            zis.closeEntry();
        }

        getLogger().lifecycle("Finished extracting");

        // patch
        if (!getShouldPatch().getOrElse(true)) return;

        Object patchSource = getPatchSource().get();
        Path patchPath;
        if (patchSource instanceof String sPatchSource) {
            if (sPatchSource.matches("^https?://"))
                throw new UnsupportedOperationException("Downloading patches not supported (yet)");
            else
                patchPath = Path.of(sPatchSource);
        } else if (patchSource instanceof File fPatchSource) {
            if (!fPatchSource.exists()) throw new IOException(patchSource + " does not exist");
            patchPath = fPatchSource.toPath();
        } else throw new IllegalArgumentException("Patch source is an invalid type");

        if (Files.isSymbolicLink(patchPath)) patchPath = patchPath.toRealPath();
        if (!Files.exists(patchPath)) throw new IOException(patchSource + " does not exist");

        if (Files.isRegularFile(patchPath)) {
            try (FileSystem patchFs = FileSystems.newFileSystem(URI.create("jar:" + patchPath.toUri()), Map.of("create",
                    "false"))) {
                patchFile(patchFs.getPath(""), patchFs.getPath(""), javaOutDir);
            }
        } else if (Files.isDirectory(patchPath)) {
            patchFile(patchPath, patchPath, javaOutDir);
        } else {
            throw new UnsupportedOperationException("Not sure how to handle patch source " + patchSource);
        }
    }

    private void patchFile(Path basePath, Path file, Path javaDir) throws IOException {
        if (Files.isDirectory(file)) try (Stream<Path> files = Files.list(file)) {
            for (Path f : files.toList()) patchFile(basePath, f, javaDir);
            return;
        }

        Path relativePath = basePath.relativize(file);

        if (relativePath.startsWith("new")) {
            Path out = javaDir.resolve(relativePath.subpath(1, relativePath.getNameCount()).toString());
            getLogger().lifecycle("Copying {} to {}", file, out);
            out.getParent().toFile().mkdirs();
            Files.copy(file, out, StandardCopyOption.COPY_ATTRIBUTES);
        } else if (relativePath.startsWith("patches")) {
            if (!relativePath.toString().endsWith(".diff")) {
                getLogger().warn("Non patch file {} in patches directory", relativePath);
                return;
            }

            String diffPath = relativePath.subpath(1, relativePath.getNameCount()).toString();
            Path dest = javaDir.resolve(diffPath.substring(0, diffPath.length() - 5));
            if (!Files.exists(dest)) {
                getLogger().warn("Patch target {} does not exist", dest);
                return;
            }

            getLogger().lifecycle("Patching {}", dest);

            List<String> patchLines = Files.readAllLines(file);
            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines);
            List<String> originalLines = Files.readAllLines(dest);
            List<String> patchedLines;
            try {
                patchedLines = DiffUtils.patch(originalLines, patch);
            } catch (PatchFailedException e) {
                getLogger().error("Patching {} failed: {}", dest, e);
                return;
            }

            Files.write(dest, patchedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            getLogger().warn("Unknown file {}", file);
        }
    }
}
