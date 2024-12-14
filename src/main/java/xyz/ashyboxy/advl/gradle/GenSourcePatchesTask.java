package xyz.ashyboxy.advl.gradle;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class GenSourcePatchesTask extends DefaultTask {
    @Inject
    public GenSourcePatchesTask() {
        super();
        setGroup(Consts.TASK_GROUP);

        getOutputDir().convention(getProject().file("patches"));
        getSourceDir().convention(getProject().file("src/main/java"));
        getPatchZip().convention(getProject().getLayout().getProjectDirectory().file("patches.zip"));
        getDiffContext().convention(Consts.DIFF_CONTEXT_SIZE);
    }

    @Input
    public abstract Property<File> getOutputDir();

    @Input
    public abstract Property<File> getSourceDir();

    @Input
    public abstract Property<Integer> getDiffContext();

    @OutputFile
    public abstract RegularFileProperty getPatchZip();

    @TaskAction
    public void run() throws IOException {
        try (FileSystem mcFs = FileSystems.newFileSystem(
                URI.create("jar:" + AdvlGradleExtension.get(getProject()).getSourcesJar().get().getAsFile().toURI()),
                Map.of("create", "false"))) {
            genSourcePatches(mcFs);
        }

    }

    public void genSourcePatches(FileSystem mcFs) throws IOException {
        getLogger().lifecycle("Generating source patches");

        File sourceDir = getSourceDir().get();
        File outputDir = getOutputDir().get();

        getLogger().lifecycle("Using old files from {}", mcFs);
        getLogger().lifecycle("Using new files from {}", sourceDir);
        getLogger().lifecycle("Outputting in {}", outputDir);

        if (outputDir.exists()) {
            File oldOutputDir = outputDir.getParentFile().getAbsoluteFile().toPath().resolve(outputDir.getName() + ".old").toFile();
            getLogger().lifecycle("Renaming old {} to {}", outputDir, oldOutputDir);
            if (oldOutputDir.exists()) {
                getLogger().lifecycle("Deleting old {}", oldOutputDir);
                FileUtils.deleteRecursively(oldOutputDir.toPath());
            }
            if (!outputDir.renameTo(oldOutputDir)) {
                getLogger().error("Renaming failed");
                return;
            }
        }

        File patchesOutputDir = outputDir.toPath().resolve("patches").toFile();
        File newFilesOutputDir = outputDir.toPath().resolve("new").toFile();
        patchesOutputDir.mkdirs();
        newFilesOutputDir.mkdirs();

        for (Path packagePath : Consts.MC_PACKAGES) {
            Path path = mcFs.getPath(packagePath.toString());
            getLogger().lifecycle("Searching for changed files in package {}", packagePath);

            if (!Files.exists(path)) {
                getLogger().warn("Minecraft package {} does not exist?", packagePath);
                continue;
            }

            genDiffs(path, sourceDir, patchesOutputDir);
        }

        for (Path packagePath : Consts.MC_PACKAGES) {
            Path path = sourceDir.toPath().resolve(packagePath.toString());
            getLogger().lifecycle("Searching for new files in package {}", packagePath);

            if (!Files.exists(path)) {
                getLogger().warn("Minecraft package {} doesn't exist in sources?", packagePath);
                continue;
            }

            newFiles(path, sourceDir.toPath(), mcFs.getPath("/"), newFilesOutputDir.toPath());
        }

        mcFs.close();

        getLogger().lifecycle("Creating {}", getPatchZip().get().getAsFile());
        createZip(getPatchZip().get().getAsFile().toPath(), outputDir.toPath());
    }

    private void genDiffs(Path path, File sourceDir, File outputDir) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) try (Stream<Path> files = Files.list(path)) {
            for (Path dir : files.toList()) genDiffs(dir, sourceDir, outputDir);
            return;
        }

        Path sourcePath = sourceDir.toPath().resolve(path.toString());

        List<String> oldLines = Files.readAllLines(path);
        List<String> newLines;

        if (!Files.exists(sourcePath)) {
            newLines = Collections.emptyList();
        } else {
            newLines = Files.readAllLines(sourcePath);
        }

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        if (patch.getDeltas().isEmpty()) return;

        List<String> diff = UnifiedDiffUtils.generateUnifiedDiff(
                "a/" + path, "b/" + path,
                oldLines, patch, getDiffContext().get()
        );

        if (newLines.isEmpty()) getLogger().lifecycle("{} was deleted", path);
        else getLogger().lifecycle("{} was changed", path);

        Path patchPath = outputDir.toPath().resolve(path + ".diff");
        patchPath.toAbsolutePath().getParent().toFile().mkdirs();
        Files.write(patchPath, diff, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private void newFiles(Path path, Path sourceDir, Path originalDir, Path outputDir) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) try (Stream<Path> files = Files.list(path)) {
            for (Path dir : files.toList()) newFiles(dir, sourceDir, originalDir, outputDir);
            return;
        }

        Path relativePath = sourceDir.relativize(path);
        Path oldPath = originalDir.resolve(relativePath.toString());
        if (Files.exists(oldPath)) return;

        getLogger().lifecycle("{} is new", relativePath);

        Path outPath = outputDir.resolve(relativePath);
        outPath.toAbsolutePath().getParent().toFile().mkdirs();
        Files.copy(path, outPath, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void createZip(Path path, Path dir) throws IOException {
        if (Files.exists(path)) {
            getLogger().lifecycle("Deleting old {}", path);
            Files.delete(path);
        }

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
                ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)
        ) {
            makeZipEntries(zipOutputStream, dir.toFile(), dir);
        }
    }

    private void makeZipEntries(ZipOutputStream zos, File file, Path baseDir) throws IOException {
        if (file.isDirectory()) {
            for (File f : Objects.requireNonNull(file.listFiles())) makeZipEntries(zos, f, baseDir);
            return;
        }
        ZipEntry entry = new ZipEntry(baseDir.relativize(file.toPath()).toString());
        zos.putNextEntry(entry);
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }
}
