package org.codetracker;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.change.Change;
import org.codetracker.element.Method;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FalseRenameDetectionTests {
    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";
    /***************************************************************************
     * *************************************
     * False Rename Detection Instead Of Extract Class & Move Method
     * *************************************
     */
    @Test
    public void breakRMReturnRenameWhenExtractClassNew() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/newclass/Calculator.java")
                .startCommitId("8ef06dced725cca8cc22705d4eba95ef39ae1e36")
                .methodName("print")
                .methodDeclarationLineNumber(15)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }
    @Test
    public void breakRMReturnRenameWhenExtractClassExtend() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/extend/Calculator.java")
                .startCommitId("a14ddd9082a29b53a0fe4b153f65b777fe122931")
                .methodName("print")
                .methodDeclarationLineNumber(20)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }
    @Test
    public void breakRMReturnRenameWhenExtractClassFieldInjection() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/autowired/Calculator.java")
                .startCommitId("0eae1091dc5ce1eddbd1edd62dcb12936ebb1978")
                .methodName("print")
                .methodDeclarationLineNumber(18)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }


    @Test
    public void breakRMReturnRenameWhenExtractClassConstructionInjection() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/constructor/Calculator.java")
                .startCommitId("ebe0ba262dc9c92473336baeb4d1d71df77f7d92")
                .methodName("print")
                .methodDeclarationLineNumber(19)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }

    @Test
    public void breakRMReturnRenameWhenExtractClassSetterInjection() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/setter/Calculator.java")
                .startCommitId("ea57744b29d0a8edf24348cf85db997f9192c12c")
                .methodName("print")
                .methodDeclarationLineNumber(20)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    @Test
    public void returnRenameWhenExtractClassStaticMethod() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/staticmethod/DiscountUtil.java")
                .startCommitId("33bf82563170a549f8366271055a43ee1a53e13d")
                .methodName("calculateDiscount")
                .methodDeclarationLineNumber(4)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    @Test
    public void returnRenameWhenExtractClassFunctionalInterfaceSupplier() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/functionalinterface/Calculator.java")
                .startCommitId("2df483b1cd01681f1cdc976ddc53d010b8e9cf23")
                .methodName("sum")
                .methodDeclarationLineNumber(7)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }

    private void printHappyCaseOutput(History<Method> methodHistory){
        for (History.HistoryInfo<Method> historyInfo : methodHistory.getHistoryInfoList()) {
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
