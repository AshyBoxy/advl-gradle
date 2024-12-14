package xyz.ashyboxy.advl.gradle.mc;

import java.net.URL;
import java.util.List;
import java.util.Map;

public record Version(
        Object arguments,
        AssetIndex assetIndex,
        String assets,
        int complianceLevel,
        Map<String, Download> downloads,
        String id,
        Object javaVersion,
        List<Library> libraries,
        Object logging,
        String mainClass,
        int minimumLauncherVersion,
        String releaseTime,
        String time,
        String type
) {
    public record Download(
            String path,
            String sha1,
            int size,
            URL url
    ) {
    }

    public record Downloads(Download artifact, Object classifiers) {
    }

    public record Library(
            Downloads downloads,
            String name,
            Object natives,
            Object extract,
            List<Rule> rules
    ) {
        public boolean allowedForOS(String os) {
            if (this.rules == null) return true;

            boolean allowed = false;
            for (Rule rule : this.rules) {
                if (rule.os.name.equals(os))
                    allowed = rule.action.equals("allow");
            }

            return allowed;
        }
    }

    public record Rule(String action, OS os) {
    }

    public record OS(String name, String version, String arch) {
    }

    public record AssetIndex(
            String id,
            String sha1,
            int size,
            int totalSize,
            URL url
    ) {
    }
}
