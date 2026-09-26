package org.codetracker;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.VariableTracker;
import org.codetracker.change.Change;
import org.codetracker.element.Variable;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VariableTrackerImplTest {
    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";
    @Test
    public void testSimpleVariableTracker() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);

        VariableTracker variableTracker = CodeTracker.variableTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/variabletracking/Main.java")
                .startCommitId("6ad3db2a40834eba62b82c36aa07ab7e01c469d7")
                .methodName("calculate")
                .methodDeclarationLineNumber(5)
                .variableName("num")
                .variableDeclarationLineNumber(6)
                .build();
            History<Variable> variableHistory = variableTracker.track();
        printHappyCaseOutput(variableHistory);
        assertNotNull(variableHistory);
        assertTrue(!variableHistory.getHistoryInfoList().isEmpty() && variableHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }
    @Test
    public void returnMultiVariableDeclarationRefactoring() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);

        VariableTracker variableTracker = CodeTracker.variableTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/multiVariableDeclaration/Main.java")
                .startCommitId("5bcc51064d77c323f4422009e66e2c6de93c86f1")
                .methodName("main")
                .methodDeclarationLineNumber(6)
                .variableName("n")
                .variableDeclarationLineNumber(8)
                .build();
        History<Variable> variableHistory = variableTracker.track();
        printHappyCaseOutput(variableHistory);
        assertNotNull(variableHistory);
        assertTrue(!variableHistory.getHistoryInfoList().isEmpty() && variableHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    @Test
    public void returnMultiVariableDeclarationSlideStatementRefactoring() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);

        VariableTracker variableTracker = CodeTracker.variableTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/multiVariableDeclaration/SlideStatement.java")
                .startCommitId("b7a6cdde69b3af5247a1289ce34a3b5e5cff4593")
                .methodName("main")
                .methodDeclarationLineNumber(6)
                .variableName("n")
                .variableDeclarationLineNumber(9)
                .build();
        History<Variable> variableHistory = variableTracker.track();
        printHappyCaseOutput(variableHistory);
        assertNotNull(variableHistory);
        assertTrue(!variableHistory.getHistoryInfoList().isEmpty() && variableHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    public void printHappyCaseOutput(History<Variable> variableHistory){
        for (History.HistoryInfo<Variable> historyInfo : variableHistory.getHistoryInfoList()) {
            System.out.println("======================================================");
            System.out.println("Commit ID: " + historyInfo.getCommitId());
            System.out.println("Date: " +
                    LocalDateTime.ofEpochSecond(historyInfo.getCommitTime(), 0, ZoneOffset.UTC));
            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());

            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");

    }

}
