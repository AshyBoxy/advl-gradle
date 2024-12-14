package xyz.ashyboxy.advl.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;

import javax.inject.Inject;

public abstract class AdvlGradleExtension {
    private boolean dependOnMinecraft;
    private boolean dependOnMinecraftDependencies;

    @Inject
    public AdvlGradleExtension() {
        dependOnMinecraft = true;
        dependOnMinecraftDependencies = true;
        getSourcesJar().set(MinecraftFiles.sourceJarPath);
    }

    public static AdvlGradleExtension get(Project project) {
        return project.getExtensions().getByType(AdvlGradleExtension.class);
    }

    @Inject
    public abstract Project getProject();

    public boolean getDependOnMinecraft() {
        return this.dependOnMinecraft;
    }

    public void setDependOnMinecraft(boolean dependOnMinecraft) {
        this.dependOnMinecraft = dependOnMinecraft;
    }

    public boolean getDependOnMinecraftDependencies() {
        return this.dependOnMinecraftDependencies;
    }

    public void setDependOnMinecraftDependencies(boolean dependOnMinecraftDependencies) {
        this.dependOnMinecraftDependencies = dependOnMinecraftDependencies;
    }

    @OutputFile
    public abstract RegularFileProperty getSourcesJar();
}
