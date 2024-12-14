package xyz.ashyboxy.advl.gradle;

import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;
import java.io.File;

public abstract class RunMcTask extends JavaExec {
    @Inject
    public RunMcTask() {
        super();
        setGroup(Consts.TASK_GROUP);

        getMainClass().set("xyz.ashyboxy.advl.loader.mc.Bootstrap");
        args(
                "--assetIndex", MinecraftAssets.indexName,
                "--assetsDir", MinecraftFiles.assetsDir.getAbsolutePath()
        );
    }

    @Override
	public void setWorkingDir(File dir) {
		if (!dir.exists()) dir.mkdirs();
		super.setWorkingDir(dir);
	}
}
