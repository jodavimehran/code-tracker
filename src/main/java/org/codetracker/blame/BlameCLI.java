package org.codetracker.blame;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

/* Created by pourya on 2024-06-26*/
public class BlameCLI {
    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                cmdApp(args[0]);
            } else {
                System.out.println("Usage: BlameCLI <file-path>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void cmdApp(String filePath) throws Exception {
        Repository workingDirRepo = getWorkingDirRepo();
        String commitId = getWorkingDirCommit(workingDirRepo);
        List<String[]> blameResult = new CodeTrackerBlame().blameFile(workingDirRepo, commitId, filePath);
        TabularPrint.printTabularData(blameResult);
    }


    static Repository getWorkingDirRepo() throws IOException {
        File currentDir = new File(System.getProperty("user.dir"));

        // Build the repository from the current directory
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .findGitDir(currentDir)
                .build();
        return repository;
    }
    static String getWorkingDirCommit(Repository repository) throws IOException {
        ObjectId head = repository.resolve("HEAD");
        return head.getName();
    }
}
