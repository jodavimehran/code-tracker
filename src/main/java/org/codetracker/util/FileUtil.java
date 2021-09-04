package org.codetracker.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtil {
    private FileUtil() {
    }

    public static void createDirectory(String[] neededDirectories) throws IOException {
        for (String directoryName : neededDirectories) {
            Path directoryPath = Paths.get(directoryName);
            if (!directoryPath.toFile().exists())
                Files.createDirectories(directoryPath);
        }
    }

    public static Path appendToFile(String pathString, String header, String content) throws IOException {
        return writeToFile(pathString, header, content, StandardOpenOption.APPEND);
    }

    public static Path writeToFile(String pathString, String header, String content, StandardOpenOption standardOpenOption) throws IOException {
        Path path = Paths.get(pathString);
        if (!path.toFile().exists()) {
            Files.createFile(path);
            if (header != null)
                Files.write(path, header.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (content != null) {
            Files.write(path, content.getBytes(), standardOpenOption);
        }
        return path;
    }
}
