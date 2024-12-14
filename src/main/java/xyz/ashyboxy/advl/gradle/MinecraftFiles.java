package xyz.ashyboxy.advl.gradle;

import org.gradle.api.Project;

import java.io.File;

import static xyz.ashyboxy.advl.gradle.Consts.*;

public class MinecraftFiles {
    public static File jsonPath;
    public static File jarPath;
    public static File remappedJarPath;
    public static File sourceJarPath;
    public static File mappingsPath;
    public static File assetsDir;

    public static void init(Project project) {
        jsonPath = project.file(BASE_PATH.resolve(VERSION_JSON_PATH));
        jarPath = project.file(BASE_PATH.resolve(CLIENT_JAR_PATH));
        remappedJarPath = project.file(BASE_PATH.resolve(CLIENT_REMAPPED_JAR_PATH));
        sourceJarPath = project.file(BASE_PATH.resolve(CLIENT_SOURCE_JAR_PATH));
        mappingsPath = project.file(BASE_PATH.resolve(CLIENT_MAPPINGS_PATH));
        assetsDir = project.file(BASE_PATH.resolve(ASSETS_DIR));
    }
}
