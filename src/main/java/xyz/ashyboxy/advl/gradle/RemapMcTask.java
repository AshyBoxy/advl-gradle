package xyz.ashyboxy.advl.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public class RemapMcTask extends DefaultTask {
    public RemapMcTask() {
        super();
        setGroup(Consts.TASK_GROUP);
    }

    @TaskAction
    public void run() throws IOException {
        Remapper.tryRemap(MinecraftFiles.jarPath, MinecraftFiles.remappedJarPath, MinecraftFiles.mappingsPath);
    }
}
