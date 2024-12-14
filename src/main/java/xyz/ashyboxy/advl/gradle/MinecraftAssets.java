package xyz.ashyboxy.advl.gradle;

import xyz.ashyboxy.advl.gradle.mc.AssetIndex;

import java.io.File;
import java.nio.file.Path;

import static xyz.ashyboxy.advl.gradle.MinecraftFiles.assetsDir;

public class MinecraftAssets {
    public static AssetIndex assetIndex;
    public static File indexFile;
    public static String indexName;

    public static void download() {
        try {
            Path objectsDir = assetsDir.toPath().resolve("objects");
            indexName = MinecraftMetadata.version.id() + "-" + MinecraftMetadata.version.assetIndex().id();
            indexFile = assetsDir.toPath().resolve("indexes/" + indexName + ".json").toFile();

            String assetIndexData = Downloader.tryDownloadText(indexFile, MinecraftMetadata.version.assetIndex().url());

            assetIndex = AdvlGradlePlugin.gson.fromJson(assetIndexData, AssetIndex.class);

            if (assetIndex.map_to_resources()) throw new AssertionError();

            for (AssetIndex.Entry entry : assetIndex.objects().values()) {
                Downloader.tryDownload(objectsDir.resolve(entry.getPath()).toFile(), entry.getDownloadUrl());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
