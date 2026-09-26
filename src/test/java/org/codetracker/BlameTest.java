package org.codetracker;

import gr.uom.java.xmi.LocationInfo;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.change.Change;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlameTest {
    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";

    /***************************************************************************
     * *************************************
     * Blame
     * False Rename Detection Instead Of Extract Class & Move Method
     * *************************************
     */
    @Test
    public void returnBlameWhenExtractClassNew() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/newclass/Calculator.java")
                .startCommitId("8ef06dced725cca8cc22705d4eba95ef39ae1e36")
                .methodName("print")
                .methodDeclarationLineNumber(17)
                .build();
        History.HistoryInfo<Method> methodHistory = methodTracker.blame();
        printHappyCaseOutputMethod(methodHistory);
        assertNotNull(methodHistory);
//        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }

    @Test
    public void returnsBlameRenameMethodGivenCommitInfoOfRepository() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/service/CryptoService.java")
                .startCommitId("d0d9edcfb611ee35978938f7e35250f1ae982057")
                .methodName("ceateUser")
                .methodDeclarationLineNumber(46) //54 43
                .build();
        History.HistoryInfo<Method> methodHistory = methodTracker.blame();
        printHappyCaseOutputMethod(methodHistory);
        assertNotNull(methodHistory);
//        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().size() > 2);
    }

    @Test
    public void ifToTernaryOperatorTest() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        BlockTracker blockTracker = CodeTracker.blockTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/blocktracking/ternary/Main.java")
                .startCommitId("3b7a3dfdbc8c9a6233a7d631bc9ff1eb35bc0653")
                .methodName("isPassed")
                .methodDeclarationLineNumber(5)
                .codeElementType(LocationInfo.CodeElementType.RETURN_STATEMENT)
                .blockStartLineNumber(6)
                .blockEndLineNumber(6)
                .blameLineNumber(6)
                .build();

        History.HistoryInfo<Block> blockHistory = blockTracker.blame();
        printHappyCaseOutputBlock(blockHistory);
        assertNotNull(blockHistory);
//        assertTrue(!blockHistory.getHistoryInfoList().isEmpty() && blockHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }

    private void printHappyCaseOutputBlock(History.HistoryInfo<Block> blockHistory) {
        System.out.println("======================================================");
        System.out.println("Commit ID: " + blockHistory.getCommitId());
        System.out.println("Date: " +
                LocalDateTime.ofEpochSecond(blockHistory.getCommitTime(), 0, ZoneOffset.UTC));
        System.out.println("CommitterName: " + blockHistory.getCommitterName());
        System.out.println("Before: " + blockHistory.getElementBefore().getName());
        System.out.println("After: " + blockHistory.getElementAfter().getName());

        for (Change change : blockHistory.getChangeList()) {
            System.out.println(change.getType().getTitle() + ": " + change);
        }
        System.out.println("======================================================");
    }

    private void printHappyCaseOutputMethod(History.HistoryInfo<Method> methodHistory) {
        System.out.println("======================================================");
        System.out.println("Commit ID: " + methodHistory.getCommitId());
        System.out.println("Date: " +
                LocalDateTime.ofEpochSecond(methodHistory.getCommitTime(), 0, ZoneOffset.UTC));
        System.out.println("CommitterName: " + methodHistory.getCommitterName());
        System.out.println("Before: " + methodHistory.getElementBefore().getName());
        System.out.println("After: " + methodHistory.getElementAfter().getName());

        for (Change change : methodHistory.getChangeList()) {
            System.out.println(change.getType().getTitle() + ": " + change);
        }
        System.out.println("======================================================");
    }

}
