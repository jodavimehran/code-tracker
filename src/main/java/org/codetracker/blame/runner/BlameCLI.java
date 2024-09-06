package org.codetracker.blame.runner;

import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.FileTrackerBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
import org.codetracker.blame.util.Utils;
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
        List<LineBlameResult> lineBlameResults = new FileTrackerBlame().blameFile(workingDirRepo, commitId, filePath);
        List<String[]> out = new BlameFormatter(Utils.getFileContentByCommit(workingDirRepo, commitId, filePath)).make(lineBlameResults);
        System.out.println(TabularPrint.make(out));
    }


    static Repository getWorkingDirRepo() throws IOException {
        File currentDir = new File(System.getProperty("user.dir"));
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .findGitDir(currentDir)
                .build();
    }
    static String getWorkingDirCommit(Repository repository) throws IOException {
        ObjectId head = repository.resolve("HEAD");
        return head.getName();
    }
}
