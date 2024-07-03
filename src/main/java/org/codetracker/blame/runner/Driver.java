package org.codetracker.blame.runner;

import org.apache.commons.io.FileUtils;
import org.codetracker.blame.IBlame;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.GitBlame;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;

import static org.codetracker.blame.util.Utils.getBlameOutput;

/* Created by pourya on 2024-07-03*/
public class Driver {
    private static final GitService gitService = new GitServiceImpl();
    private static final String reposPath = System.getProperty("user.dir") + "/tmp";


    public static void main(String[] args) throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        writeBlameToFile(url, filePath, new CodeTrackerBlame(), "cd-blame.txt");
        writeBlameToFile(url, filePath, new GitBlame(), "git-blame.txt");
    }

    private static void writeBlameToFile(String url, String filePath, IBlame blamer, String outputFile) throws Exception {
        String res = getBlameOutput(url, filePath, blamer, reposPath, gitService);
        FileUtils.writeStringToFile(new File(outputFile), res, "UTF-8");
    }


}
