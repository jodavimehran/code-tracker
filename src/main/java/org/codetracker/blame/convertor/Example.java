package org.codetracker.blame.convertor;

import org.apache.commons.io.FileUtils;
import org.codetracker.blame.model.IBlameTool;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
import org.codetracker.blame.util.Utils;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.List;

import static org.codetracker.blame.util.Utils.getRepository;

/* Created by pourya on 2024-10-06*/
public class Example {
    private static final GitService gitService = new GitServiceImpl();
    private static final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    private static String url = "https://github.com/junit-team/junit4/commit/7a3e99635d7ffcc4d730f27835eeaeb082003199";
    private static String filePath = "src/main/java/org/junit/runners/BlockJUnit4ClassRunner.java";




    public static void main(String[] args) throws Exception {

        String commitID = URLHelper.getCommit(url);
        Repository repo = getRepository(url, gitService, REPOS_PATH);
        writeToolOutput(repo, commitID, filePath, BlamersEnum.FileTrackerBlame);
        writeToolOutput(repo, commitID, filePath, BlamersEnum.LHDiffNaive);
        writeToolOutput(repo, commitID, filePath, BlamersEnum.LHDiffRMDriven);

//        List<LineBlameResult> lineBlameResults = new LHDiffBlame().blameFile(repo, commitID, filePath);
//        TabularPrint.make(new BlameFormatter(Utils.getFileContentByCommit(repo, commitID, filePath)).make(lineBlameResults));

//        if (true) return;
//        writeToolOutput(repo, commitID, filePath, BlamersEnum.FileTrackerBlame);
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-M"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-M100"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-M200"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-M400"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-M1000"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-C"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-C100"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-C200"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-C400"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(true, new String[]{"-C1000"}));
//
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-M"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-M100"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-M200"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-M400"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-M1000"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-C"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-C100"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-C200"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-C400"}));
//        writeToolOutput(repo, commitID, filePath, getCliBlameTool(false, new String[]{"-C1000"}));


    }

    private static void writeToolOutput(Repository repository, String commitId, String filePath, IBlameTool blamer) throws Exception {
        List<LineBlameResult> lineBlameResults = blamer.blameFile(repository, commitId, filePath);
        List<String[]> out = new BlameFormatter(Utils.getFileContentByCommit(repository, commitId, filePath)).make(lineBlameResults);
        FileUtils.write(
            new File("exp/" + blamer.getToolName() + ".txt"),
                TabularPrint.make(out));
    }
}
