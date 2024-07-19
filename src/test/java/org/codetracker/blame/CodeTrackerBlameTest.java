package org.codetracker.blame;

import org.apache.commons.io.IOUtils;
import org.codetracker.FileTrackerImpl;
import org.codetracker.api.CodeElement;
import org.codetracker.api.History;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.GitBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.codetracker.blame.util.Utils.getBlameOutput;
import static org.codetracker.blame.util.Utils.getOwner;
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
                Arguments.of(url, filePath, new CodeTrackerBlame(), System.getProperty("user.dir") + "/src/test/resources/blame/formatting/codetracker.txt"),
                Arguments.of(url, filePath, new GitBlame(), System.getProperty("user.dir") + "/src/test/resources/blame/formatting/git.txt")
        );
    }

    @Test
    public void testBlameWithLocalRepoUsingFileTracker() throws Exception {
    	String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        String expectedFilePath = System.getProperty("user.dir") + "/src/test/resources/blame/formatting/codetracker.txt";
        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        FileTrackerImpl fileTracker = new FileTrackerImpl(repository, commitId, filePath);
        fileTracker.blame();
		BlameFormatter blameFormatter = new BlameFormatter(fileTracker.getLines());
		List<String[]> results = blameFormatter.make(fileTracker.getBlameInfo());
		String actual = TabularPrint.make(results);
        assertEqualWithFile(expectedFilePath, actual);
    }

    @Test
    public void testBlameWithLocalRepo() throws Exception {
        String url = "https://github.com/pouryafard75/DiffBenchmark/commit/5b33dc6f8cfcf8c0e31966c035b0406eca97ec76";
        String filePath = "src/main/java/dat/MakeIntels.java";
        String expectedFilePath = System.getProperty("user.dir") + "/src/test/resources/blame/blameTestWithLocalRepo.txt";
        String actual = getBlameOutput(url, filePath, new CodeTrackerBlame(), REPOS_PATH, gitService);
        assertEqualWithFile(expectedFilePath, actual);
    }

    @Test
    public void testBlameLineRangeWithLocalRepo() throws Exception {
        String url = "https://github.com/checkstyle/checkstyle/commit/119fd4fb33bef9f5c66fc950396669af842c21a3";
        String filePath = "src/main/java/com/puppycrawl/tools/checkstyle/Checker.java";
        int fromLine = 443;
        int toLine = 487;
        String expectedFilePath = System.getProperty("user.dir") + "/src/test/resources/blame/blameLineRangeTestWithLocalRepo.txt";
        String actual = getBlameOutput(url, filePath, new CodeTrackerBlame(), REPOS_PATH, gitService, fromLine, toLine);
        assertEqualWithFile(expectedFilePath, actual);
    }

    private void assertEqualWithFile(String expectedResultPath, String actual) throws IOException {
        String expected = IOUtils.toString(
                new FileInputStream(expectedResultPath),
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
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
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
        String expected = "LineBlameResult{commitId='e5e209d526b49544b2fb899bde8856290cf209a0', filePath='src/main/java/dat/Make.java', shortCommitId='e5e209d52', beforeFilePath='src/main/java/dat/Make.java', committer='Pouryafard75', commitDate='1709775227', lineNumber=20}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        Assertions.assertEquals(expected, lineBlameResult.toString());
    }

    @Test
    public void blameTestSingleLine3() throws Exception {
        String url = "https://github.com/Alluxio/alluxio/commit/9aeefcd8120bb3b89cdb437d8c32d2ed84b8a825";
        String filePath = "servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java";
        int lineNumber = 78;
        String expected = "LineBlameResult{commitId='68514f3fe653a87899b0e0e7c9d2e67c85afefe0', filePath='servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java', shortCommitId='68514f3fe', beforeFilePath='servers/src/main/java/tachyon/worker/block/allocator/MaxFreeAllocator.java', committer='Carson Wang', commitDate='1435366348', lineNumber=49}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        Assertions.assertEquals(expected, lineBlameResult.toString());
    }

    @Test
    public void blameTestPackageDeclarationJavadoc() throws Exception {
        String url = "https://github.com/structr/structr/commit/6c59050b8b03adf6d8043f3fb7add0496f447edf";
        String filePath = "structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java";
        int lineNumber = 1;
        String expected = "LineBlameResult{commitId='2572fd72d96812328b1439434f8b42fccec694f8', filePath='structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java', shortCommitId='2572fd72d', beforeFilePath='structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java', committer='Axel Morgner', commitDate='1428756399', lineNumber=1}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        Assertions.assertEquals(expected, lineBlameResult.toString());
    }

    @Test
    public void blameTestPackageDeclaration() throws Exception {
        String url = "https://github.com/structr/structr/commit/6c59050b8b03adf6d8043f3fb7add0496f447edf";
        String filePath = "structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java";
        int lineNumber = 19;
        String expected = "LineBlameResult{commitId='4aefa2bab5f8cda021a05de29ccf046bbdc90b52', filePath='structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java', shortCommitId='4aefa2bab', beforeFilePath='structr-rest/src/main/java/org/structr/rest/resource/SchemaTypeResource.java', committer='amorgner', commitDate='1356007184', lineNumber=48}";

        String commitId = URLHelper.getCommit(url);
        Repository repository = gitService.cloneIfNotExists(REPOS_PATH + "/" + getOwner(url) + "/" + getProject(url), URLHelper.getRepo(url));
        History.HistoryInfo<? extends CodeElement> lineBlame =
                new CodeTrackerBlame().getLineBlame(repository, commitId, filePath, lineNumber);
        LineBlameResult lineBlameResult = LineBlameResult.of(lineBlame);
        Assertions.assertEquals(expected, lineBlameResult.toString());
    }
}