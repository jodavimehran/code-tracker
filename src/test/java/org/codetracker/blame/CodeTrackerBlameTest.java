package org.codetracker.blame;

import org.apache.commons.io.IOUtils;
import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.GitBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

import static org.codetracker.blame.util.Utils.getBlameOutput;
import static org.codetracker.blame.util.Utils.getProject;

/* Created by pourya on 2024-06-26*/
@Disabled
public class CodeTrackerBlameTest {
    private static final GitService gitService = new GitServiceImpl();
    private final String REPOS_PATH = System.getProperty("user.dir") + "/tmp";
    @ParameterizedTest
    @MethodSource("testBlamerInputProvider")
    public void testBlameWithFormatting(String url, String filePath, IBlame blamer, String expectedPath) throws Exception {
        String actual = getBlameOutput(url, filePath, blamer, REPOS_PATH, gitService);
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

        String actual = getBlameOutput(url, filePath, new CodeTrackerBlame(), REPOS_PATH, gitService);
        assertEqualWithFile(expectedPath, actual);
    }

    private void assertEqualWithFile(String expectedResultPath, String actual) throws IOException {
        String expected = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream(expectedResultPath)),
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

    @Test
    public void blameTestSingleLine2() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        int lineNumber = 24;
        String expected = "LineBlameResult{commitId='ae3a4f8a50b1d25b2d3db50495a50feaa0b2b872', filePath='src/main/java/dat/MakeIntels.java', shortCommitId='ae3a4f8a5', beforeFilePath='src/main/java/dat/Make.java', committer='Pouryafard75', commitDate='1719280029', lineNumber=18}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
//        Assertions.assertEquals(expected, lineBlameResult.toString()); //TODO: Update this one
    }

    @Test
    public void blameTestSingleLine_3() throws Exception {
        String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        int lineNumber = 78;
        String expected = "???";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
//        Assertions.assertEquals(expected, lineBlameResult.toString()); //TODO: Update this one
    }


}