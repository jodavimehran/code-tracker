package org.codetracker.blame;

import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.codetracker.blame.CodeTrackerBlame.lineBlameFormat;
import static org.codetracker.blame.Utils.getFileContentByCommit;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlameTest {
    private static final GitService gitService = new GitServiceImpl();
//    @Test
    public void blameTest() throws Exception {
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
        System.out.println();
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println();
        TabularPrint.printTabularData(blameResult);
        //TODO: Add assertions
        //TODO: Add test for Tabular Print formatting
    }

    @Test
    public void blameTestWithLocalRepo() throws Exception {
        String commitId = "5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        String owner = "pouryafard75";
        String repoName = "DiffBenchmark";
        String gitHubToken = System.getProperty("OAuthToken");
        Repository repository = gitService.cloneIfNotExists(
                "/Users/pourya/IdeaProjects/DiffBenchmark",
                "https://github.com/" + owner + "/" + repoName + ".git",
                owner,
                gitHubToken
        );
        List<String[]> blameResult = new CodeTrackerBlame().blameFile(repository, commitId, filePath);
        System.out.println();
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println();
        TabularPrint.printTabularData(blameResult);
        //TODO: Add assertions
        //TODO: Add test for Tabular Print formatting
    }
    @Test
    public void blameTestSingleLine() throws Exception {
        String commitId = "5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        String owner = "pouryafard75";
        String repoName = "DiffBenchmark";
        int lineNumber = 18;
        String name = null;
        String gitHubToken = System.getProperty("OAuthToken");
        Repository repository = gitService.cloneIfNotExists(
                "/Users/pourya/IdeaProjects/DiffBenchmark",
                "https://github.com/" + owner + "/" + repoName + ".git",
                owner,
                gitHubToken
        );
        List<String[]> blameResult = new ArrayList<>();
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        blameResult.add(lineBlameFormat(repository, commitId, filePath, name, lineNumber, lines));
        System.out.println();
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println();
        TabularPrint.printTabularData(blameResult);
        //TODO: Add assertions
        //TODO: Add test for Tabular Print formatting
    }

}