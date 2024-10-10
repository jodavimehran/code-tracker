package org.codetracker.blame.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-10-06*/
public class FileTrackerBlameWithSerialization extends FileTrackerBlameWithCache {
    static String SER_PATH = "cache.ser";
    static ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        try {
            deserializeCache();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        int size = cache.size();
        List<LineBlameResult> lineBlameResults = super.blameFile(repository, commitId, filePath);
        if (cache.size() > size) {
            saveCacheToFile();
        }
        return lineBlameResults;
    }

    public static void saveCacheToFile() throws IOException {
        System.out.println("Saving cache to file" + cache.size());
        objectMapper.writeValue(new File(SER_PATH), cache);
    }

    public static void deserializeCache() throws IOException {
        if (!new File(SER_PATH).exists()) {
            return;
        }
        cache = objectMapper.readValue(new File(SER_PATH), new TypeReference<Map<String, List<LineBlameResult>>>() {});
    }
    public static void clearCache() {
        cache.clear();
    }
}
