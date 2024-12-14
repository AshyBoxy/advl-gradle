package xyz.ashyboxy.advl.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.util.JrtFinder;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public abstract class DecompileTask extends DefaultTask {
    private boolean force = false;

    @Option(option = "force", description = "")
    private void setForce(boolean force) {
        this.force = force;
    }

    @Inject
    public DecompileTask() {
        super();
        setGroup(Consts.TASK_GROUP);
    }

    @TaskAction
    public void run() throws IOException {
        File sourcesJar = AdvlGradleExtension.get(getProject()).getSourcesJar().get().getAsFile();

        if (sourcesJar.exists()) {
            if (!force) {
                getLogger().lifecycle("Not decompiling (already decompiled)");
                return;
            } else getLogger().lifecycle("Already decompiled, but continuing anyway");
        }

        getLogger().lifecycle("Decompiling {} to {}", MinecraftFiles.remappedJarPath, sourcesJar);

        Files.deleteIfExists(sourcesJar.toPath());

        Map<String, Object> prefs = Map.of(
                IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1",
                IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
                IFernflowerPreferences.REMOVE_SYNTHETIC, "1",
                IFernflowerPreferences.LOG_LEVEL, "info",
                IFernflowerPreferences.THREADS, String.valueOf(Runtime.getRuntime().availableProcessors()),
                IFernflowerPreferences.INDENT_STRING, "    ",
                IFernflowerPreferences.IGNORE_INVALID_BYTECODE, "1",
                IFernflowerPreferences.INCLUDE_JAVA_RUNTIME, JrtFinder.CURRENT // magic
        );

        Fernflower vf = new Fernflower(new SingleFileSaver(sourcesJar), prefs, new VineflowerLogger());

        vf.addSource(MinecraftFiles.remappedJarPath);

        // also magic
        for (File dep : this.getProject().getConfigurations().getByName(Consts.MC_RUNTIME).getFiles()) {
            if (!dep.isFile() || !dep.getName().endsWith(".jar")) continue;
            getLogger().lifecycle("Adding library {}", dep.getName());
            vf.addLibrary(dep);
        }

        vf.decompileContext();
        vf.clearContext();
    }

//    private static class VineflowerSaver implements IResultSaver {
//        @Override
//        public void createArchive(String path, String archiveName, Manifest manifest) {}
//
//        @Override
//        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {}
//
//        @Override
//        public void closeArchive(String path, String archiveName) {}
//
//
//        @Override
//        public void saveFolder(String path) {}
//
//        @Override
//        public void copyFile(String source, String path, String entryName) {}
//
//        @Override
//        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {}
//
//        @Override
//        public void saveDirEntry(String path, String archiveName, String entryName) {}
//
//        @Override
//        public void copyEntry(String source, String path, String archiveName, String entry) {}
//    }

    private class VineflowerLogger extends IFernflowerLogger {
        @Override
        public void writeMessage(String message, Severity severity) {
            if (severity.ordinal() < Severity.ERROR.ordinal()) return;

            System.err.println(message);
        }

        @Override
        public void writeMessage(String message, Severity severity, Throwable t) {
            if (severity.ordinal() < Severity.ERROR.ordinal()) return;

            writeMessage(message, severity);
            t.printStackTrace(System.err);
        }

        @Override
        public void startClass(String className) {
            getLogger().lifecycle("Decompiling {}", className);
        }
    }
}
