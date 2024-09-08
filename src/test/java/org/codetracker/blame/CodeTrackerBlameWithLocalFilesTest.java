package org.codetracker.blame;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codetracker.FileTrackerWithLocalFilesImpl;
import org.codetracker.blame.util.BlameFormatter;
import org.codetracker.blame.util.TabularPrint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.astDiff.utils.URLHelper;

@Disabled
public class CodeTrackerBlameWithLocalFilesTest {

    @ParameterizedTest
    @CsvSource({
        "https://github.com/checkstyle/checkstyle/commit/119fd4fb33bef9f5c66fc950396669af842c21a3, src/main/java/com/puppycrawl/tools/checkstyle/Checker.java, /src/test/resources/blame/blameTestWithLocalRepo3.txt",
        "https://github.com/javaparser/javaparser/commit/97555053af3025556efe1a168fd7943dac28a2a6, javaparser-core/src/main/java/com/github/javaparser/printer/lexicalpreservation/Difference.java, /src/test/resources/blame/blameTestWithLocalRepo4.txt",
        "https://github.com/javaparser/javaparser/commit/97555053af3025556efe1a168fd7943dac28a2a6, javaparser-symbol-solver-core/src/main/java/com/github/javaparser/symbolsolver/javaparsermodel/contexts/MethodCallExprContext.java, /src/test/resources/blame/blameTestWithLocalRepo5.txt",
        "https://github.com/spring-projects/spring-framework/commit/b325c74216fd9564a36602158fa1269e2e832874, spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java, /src/test/resources/blame/blameTestWithLocalRepo6.txt",
        "https://github.com/junit-team/junit5/commit/77cfe71e7f787c59626198e25350545f41e968bd, junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/descriptor/ClassTestDescriptor.java, /src/test/resources/blame/blameTestWithLocalRepo7.txt",
        "https://github.com/hibernate/hibernate-orm/commit/8bd79b29cfa7b2d539a746dc356d60b66e1e596b, hibernate-core/src/main/java/org/hibernate/cfg/AnnotationBinder.java, /src/test/resources/blame/blameTestWithLocalRepo8.txt",
        //"https://github.com/apache/flink/commit/9e936a5f8198b0059e9b5fba33163c2bbe3efbdd, flink-streaming-java/src/main/java/org/apache/flink/streaming/api/datastream/DataStream.java, /src/test/resources/blame/blameTestWithLocalRepo9.txt",
        "https://github.com/eclipse/jgit/commit/bd1a82502680b5de5bf86f6c4470185fd1602386, org.eclipse.jgit/src/org/eclipse/jgit/internal/storage/pack/PackWriter.java, /src/test/resources/blame/blameTestUntilCommitZero.txt",
        "https://github.com/JetBrains/intellij-community/commit/ecb1bb9d4d484ae63ee77f8ad45bdce154db9356, java/compiler/impl/src/com/intellij/compiler/CompilerManagerImpl.java, /src/test/resources/blame/blameTestUntilCommitZero2.txt",
        "https://github.com/JetBrains/intellij-community/commit/ecb1bb9d4d484ae63ee77f8ad45bdce154db9356, java/compiler/impl/src/com/intellij/compiler/actions/CompileDirtyAction.java, /src/test/resources/blame/blameTestUntilCommitZero3.txt"
    })
    public void testBlameWithLocalRepoUsingFileTracker(String url, String filePath, String testResultFileName) throws Exception {
        String expectedFilePath = System.getProperty("user.dir") + testResultFileName;
        String cloneURL = URLHelper.getRepo(url);
        String commitId = URLHelper.getCommit(url);
        FileTrackerWithLocalFilesImpl fileTracker = new FileTrackerWithLocalFilesImpl(cloneURL, commitId, filePath);
        fileTracker.blame();
        BlameFormatter blameFormatter = new BlameFormatter(fileTracker.getLines());
        List<String[]> results = blameFormatter.make(fileTracker.getBlameInfo());
        String actual = TabularPrint.make(results);
        assertEqualWithFile(expectedFilePath, actual);
    }

    private void assertEqualWithFile(String expectedResultPath, String actual) throws IOException {
        String expected = IOUtils.toString(
                new FileInputStream(expectedResultPath),
                StandardCharsets.UTF_8
        );
        Assertions.assertEquals(expected, actual);
    }
}
