package xyz.ashyboxy.advl.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import xyz.ashyboxy.advl.gradle.mc.Version;

import java.net.URI;
import java.nio.file.Files;

import static xyz.ashyboxy.advl.gradle.MinecraftFiles.*;

public class MinecraftMetadata {
    public static Version version;

    public static void download() {
        try {
            version = AdvlGradlePlugin.gson.fromJson(Downloader.tryDownloadText(jsonPath,
                    new URI(Consts.TMP_VERSION_JSON_URL).toURL()), Version.class);
            Version.Download download = version.downloads().get("client");
            Version.Download mappingsDownload = version.downloads().get("client_mappings");
            if (download == null || mappingsDownload == null) throw new AssertionError();
            Downloader.tryDownload(jarPath, download.url());
            Downloader.tryDownload(mappingsPath, mappingsDownload.url());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void apply(Project project) {
        if (AdvlGradleExtension.get(project).getDependOnMinecraft()) {
            project.getLogger().lifecycle("Adding a dependency on Minecraft");
            project.getDependencies().add("implementation", project.files(remappedJarPath));
        }

        if (!AdvlGradleExtension.get(project).getDependOnMinecraftDependencies()) return;
        project.getLogger().lifecycle("Adding dependencies on Minecraft's dependencies");
        try {
            Version version = AdvlGradlePlugin.gson.fromJson(Files.readString(jsonPath.toPath()), Version.class);

            // not actually tested on anything other than linux
            String _currentOS = System.getProperty("os.name").toLowerCase();
            if (_currentOS.contains("windows")) _currentOS = "windows";
            else if (_currentOS.contains("linux")) _currentOS = "linux";
            else if (_currentOS.contains("mac")) _currentOS = "osx";
            else throw new AssertionError();

            final String currentOS = _currentOS;

            version.libraries().forEach((library -> {
                // ignoring natives for now, since we're not using < 1.19.3
                if (!library.allowedForOS(currentOS)) return;

                var dependency = project.getDependencies().add(Consts.MC_RUNTIME, library.name());
                // thanks loom
                if (dependency instanceof ModuleDependency md) md.setTransitive(false);

                // and then natives...
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
