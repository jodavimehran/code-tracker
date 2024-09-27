package org.codetracker.blame.util;

import com.github.gumtreediff.utils.Pair;
import org.eclipse.jgit.lib.Repository;

import java.util.HashMap;
import java.util.Map;

/* Created by pourya on 2024-09-25*/
public class GithubUtilsWithCache {
    private final String repo;
    private final String project;
    private Map<String, Boolean> cache = new HashMap<>();

    public GithubUtilsWithCache(String repo, String project) {
        this.repo = repo;
        this.project = project;

    }

    public GithubUtilsWithCache(Repository repository) {
        Pair<String, String> ownerAndProject = GithubUtils.getOwnerAndProject(repository);
        this.repo = ownerAndProject.first;
        this.project = ownerAndProject.second;
    }

    public boolean areSameConsideringMerge(String commitId1, String commitId2) {
        String key = keyGenerator(commitId1, commitId2);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        boolean b = GithubUtils.areSameConsideringMerge(repo, project, commitId1, commitId2);
        updateCache(commitId1, commitId2, b);
        return b;
    }

    private void updateCache(String commitId1, String commitId2, boolean b) {
        String key = keyGenerator(commitId1, commitId2);
        cache.put(key, b);
    }

    public String keyGenerator(String commitId1, String commitId2) {
        //Sort alphabetically
        if (commitId1.compareTo(commitId2) > 0) {
            String temp = commitId1;
            commitId1 = commitId2;
            commitId2 = temp;
        }
        return commitId1 + "..." + commitId2;
    }

}
