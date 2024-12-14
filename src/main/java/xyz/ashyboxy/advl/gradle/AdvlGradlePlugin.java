package xyz.ashyboxy.advl.gradle;

import com.google.gson.Gson;
import org.gradle.api.*;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class AdvlGradlePlugin implements Plugin<Project> {
    public static Gson gson = new Gson();

    protected static Logger logger;

    public AdvlGradleExtension extension;

    @Override
    public void apply(Project target) {
        logger = target.getLogger();
        logger.lifecycle("Hello from AdvlGradlePlugin");
        target.apply(Map.of("plugin", "java-library"));

        MinecraftFiles.init(target);

        extension = target.getExtensions().create("advl", AdvlGradleExtension.class);

        // thanks again loom
        target.getRepositories().maven(repo -> {
            repo.setName("Mojang");
            repo.setUrl(Consts.MOJANG_LIBRARIES);
            repo.metadataSources(sources -> {
				sources.mavenPom();
				sources.artifact();
				sources.ignoreGradleMetadataRedirection();
			});
            repo.artifactUrls(ArtifactRepositoryContainer.MAVEN_CENTRAL_URL);
        });
        target.getRepositories().mavenCentral();

        TaskContainer tasks = target.getTasks();
        tasks.register("runMc", RunMcTask.class, t -> {
            t.setWorkingDir(target.file("mcRun"));
        });
        tasks.register("remapMc", RemapMcTask.class, t -> {});
        // caching who?
        var decompileMcTask = tasks.register("decompileMc", DecompileTask.class, t -> {});
        var genSourcePatchesTask = tasks.register("genSourcePatches", GenSourcePatchesTask.class, t -> {
            t.dependsOn(decompileMcTask);
        });
        tasks.register("genPatches", DefaultTask.class, t -> {
            t.dependsOn(genSourcePatchesTask);
            t.setGroup(Consts.TASK_GROUP);
        });
        tasks.register("extractMc", ExtractMcTask.class, t -> {});

        try {
            NamedDomainObjectProvider<Configuration> mcRuntimeConfiguration = target.getConfigurations().register(Consts.MC_RUNTIME);
            mcRuntimeConfiguration.configure(c -> {
                c.setCanBeConsumed(false);
                c.setCanBeResolved(true);
            });
            target.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, c -> {
                c.extendsFrom(mcRuntimeConfiguration.get());
            });

            MinecraftMetadata.download();
            MinecraftAssets.download();
            Remapper.maybeRemap(MinecraftFiles.jarPath, MinecraftFiles.remappedJarPath, MinecraftFiles.mappingsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        target.afterEvaluate(this::afterEvaluate);
    }

    public void afterEvaluate(Project target) {
        MinecraftMetadata.apply(target);
    }
}
