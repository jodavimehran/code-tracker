package org.codetracker.blame;

import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.GitBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitServiceImpl;

import java.util.List;
import java.util.stream.Stream;

import static org.codetracker.blame.util.Utils.getFileContentByCommit;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlameTest {
    private static final GitService gitService = new GitServiceImpl();
    private final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    @ParameterizedTest
    @MethodSource("testBlamerInputProvider")
    public void testBlamer(String url, String filePath, IBlame blamer) throws Exception {
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        BlameFormatter formatter = new BlameFormatter(lines);
        List<LineBlameResult> blameResult = apply(commitId, filePath, blamer, repository);
        TabularPrint.printTabularData(formatter.make(blameResult));
        //Add assertions accordingly
    }

    private static Stream<Arguments> testBlamerInputProvider(){
        String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        return Stream.of(
                Arguments.of(url, filePath, new CodeTrackerBlame()),
                Arguments.of(url, filePath, new GitBlame())
        );
    }


    @Test
    public void blameTestWithLocalRepo() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH, URLHelper.getRepo(url));
        List<LineBlameResult> blameResult = new CodeTrackerBlame().blameFile(repository, commitId, filePath);
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        BlameFormatter formatter = new BlameFormatter(lines);
        TabularPrint.printTabularData(formatter.make(blameResult));
    }
    @Test
    public void blameTestSingleLine() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        int lineNumber = 18;

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(
                REPOS_PATH,
                URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        System.out.println(lineBlameResult);
    }

    private List<LineBlameResult> apply(String commitId, String filePath, IBlame blamer, Repository repository) throws Exception {
        return blamer.blameFile(repository, commitId, filePath);
    }


    private static String getOwner(String gitURL){
        return gitURL.split("/")[3];
    }
    private static String getProject(String gitURL){
        return gitURL.split("/")[4];
    }

}