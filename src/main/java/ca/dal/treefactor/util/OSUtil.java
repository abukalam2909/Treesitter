package ca.dal.treefactor.util;

public class OSUtil {

    public static String getLibExtension() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return ".dll";
        } else if (osName.contains("mac")) {
            return ".dylib";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("linux")) {
            return ".so";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }

    public static String getOSFolder() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("linux")) {
            return "linux";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }
}
