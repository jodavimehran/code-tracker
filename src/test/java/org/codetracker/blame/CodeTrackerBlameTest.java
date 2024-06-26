package org.codetracker.blame;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.util.List;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlameTest {
    private static final GitService gitService = new GitServiceImpl();
    public void testDriver() throws Exception {
        String commitId = "9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        String owner = "Alluxio";
        String repoName = "alluxio";
        String gitHubToken = System.getProperty("OAuthToken");
        Repository repository = gitService.cloneIfNotExists(
                "tmp/" + owner + "/" + repoName,
                "https://github.com/" + owner + "/" + repoName + ".git",
                owner,
                gitHubToken
        );
        List<String[]> blameResult = new CodeTrackerBlame().blameFile(repository, commitId, filePath);
        TabularPrint.printTabularData(blameResult);
        //TODO: Add assertions
        //TODO: Add test for Tabular Print formatting
    }
}