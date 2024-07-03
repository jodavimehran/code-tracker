package org.codetracker.blame;

import org.apache.commons.io.IOUtils;
import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.GitBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.codetracker.blame.util.Utils.getFileContentByCommit;
import static org.junit.Assert.assertEquals;

/* Created by pourya on 2024-06-26*/
public class CodeTrackerBlameTest {
    private static final GitService gitService = new GitServiceImpl();
    private final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    @ParameterizedTest
    @MethodSource("testBlamerInputProvider")
    public void testBlameWithFormatting(String url, String filePath, IBlame blamer, String expectedPath) throws Exception {
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        BlameFormatter formatter = new BlameFormatter(lines);
        List<LineBlameResult> blameResult = apply(commitId, filePath, blamer, repository);
        String actual = TabularPrint.make(formatter.make(blameResult));
        assertEqualWithFile(expectedPath, actual);
    }

    private static Stream<Arguments> testBlamerInputProvider(){
        String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        return Stream.of(
                Arguments.of(url, filePath, new CodeTrackerBlame(), "/blame/formatting/codetracker.txt"),
                Arguments.of(url, filePath, new GitBlame(), "/blame/formatting/git.txt")
        );
    }


    @Test
    public void testBlameWithLocalRepo() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        String expectedPath = "/blame/blameTestWithLocalRepo.txt";
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        List<LineBlameResult> blameResult = new CodeTrackerBlame().blameFile(repository, commitId, filePath);
        List<String> lines = getFileContentByCommit(repository, commitId, filePath);
        BlameFormatter formatter = new BlameFormatter(lines);
        String actual = TabularPrint.make(formatter.make(blameResult));
        assertEqualWithFile(expectedPath, actual);
    }

    private void assertEqualWithFile(String expectedResultPath, String actual) throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(expectedResultPath);
        String expected = IOUtils.toString(
                this.getClass().getResourceAsStream(expectedResultPath),
                StandardCharsets.UTF_8
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void blameTestSingleLine() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        int lineNumber = 18;
        String expected = "LineBlameResult{commitId='ae3a4f8a50b1d25b2d3db50495a50feaa0b2b872', filePath='src/main/java/dat/MakeIntels.java', shortCommitId='ae3a4f8a5', beforeFilePath='src/main/java/dat/Make.java', committer='Pouryafard75', commitDate='1719280029', lineNumber=18}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        Assertions.assertEquals(expected, lineBlameResult.toString());
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