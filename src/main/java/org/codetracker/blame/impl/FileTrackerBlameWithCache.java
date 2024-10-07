package org.codetracker.blame.impl;

import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-10-06*/
public class FileTrackerBlameWithCache extends FileTrackerBlame
{
    protected static Map<String, List<LineBlameResult>> cache = new LinkedHashMap<>();

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        String cacheKey = getKey(repository, commitId, filePath);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        System.out.println("Cache miss for " + cacheKey);
        List<LineBlameResult> lineBlameResults = super.blameFile(repository, commitId, filePath);
        cache.put(cacheKey, lineBlameResults);
        return lineBlameResults;
    }

    private String getKey(Repository repository, String commitId, String filePath) {
        return repository.getDirectory().getAbsolutePath() + commitId + filePath;
    }
}

