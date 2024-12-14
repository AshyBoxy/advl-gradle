package xyz.ashyboxy.advl.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Consts {
    // various urls
    public static final String MOJANG_LIBRARIES = "https://libraries.minecraft.net/";
    public static final String ASSETS_URL = "https://resources.download.minecraft.net/";
    public static final String TMP_VERSION_JSON_URL = "https://piston-meta.mojang.com/v1/packages/4298ca546ceb8a7ba52b7e154bc0df4d952b8dbf/1.21.1.json";

    // file paths
    public static final Path BASE_PATH = Path.of(".gradle/advl");
    public static final Path VERSION_JSON_PATH = Path.of("version.json");
    public static final Path CLIENT_JAR_PATH = Path.of("client.jar");
    public static final Path CLIENT_REMAPPED_JAR_PATH = Path.of("client-remapped.jar");
    public static final Path CLIENT_SOURCE_JAR_PATH = Path.of("client-remapped-sources.jar");
    public static final Path CLIENT_MAPPINGS_PATH = Path.of("client-mappings.txt");
    public static final Path ASSETS_DIR = Path.of("assets");

    // other stuff
    public static final String TASK_GROUP = "advl";

    // configurations
    public static final String MC_RUNTIME = "mcRuntime";

    // patches
    // whitelist for generating patches
    public static final Path[] MC_PACKAGES = new Path[]{
            Path.of("com/mojang"),
            Path.of("net/minecraft")
    };
    public static final int DIFF_CONTEXT_SIZE = 3;

    public static final List<String> MC_EXTRACT_JAVA_WHITELIST = Arrays.stream(MC_PACKAGES).map(Path::toString).toList();
    public static final List<String> MC_EXTRACT_ASSETS_WHITELIST = List.of(
            "assets/minecraft", "data/minecraft",
            "flightrecorder-config.jfc", "pack.png", "version.json"
    );
}
