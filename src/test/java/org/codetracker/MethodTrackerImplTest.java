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

public class MethodTrackerImplTest {

    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";

    @Test
    public void returnsHistoryOfRenameMethodGivenCommitInfoOfRepository() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/service/CryptoService.java")
                .startCommitId("d0d9edcfb611ee35978938f7e35250f1ae982057")
                .methodName("ceateUser")
                .methodDeclarationLineNumber(43)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().size() > 2);
    }
    @Test
    public void returnsHistoryOfMovedMethodContainerChanged() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/CryptographyApplication.java")
                .startCommitId("8e0ada4854818a764d337d40c25f6060aa8ea1b2")
                .methodName("main")
                .methodDeclarationLineNumber(42)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));
    }
    @Test
    public void returnNoChangeForBodyChangeDueToCollision() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/CryptographyApplication.java")
                .startCommitId("5c08104df4a48e253714226c0d9c0d0f4e40ee30")
                .methodName("run")
                .methodDeclarationLineNumber(47)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("body change"));
    }
    @Test
    public void returnContainerChangeForMovedFile() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/dto/request/DecryptReqDto.java")
                .startCommitId("213ed6bbd3c0b94423e7338b191e26f876c0c0d5")
                .methodName("getBase64OfCipherTest")
                .methodDeclarationLineNumber(17)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));

    }
@Test
public void returnTheLastRefactoringTypeBeforeRenamingFile() throws Exception {
    GitService gitService = new GitServiceImpl();
    Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
            GITHUB_REPO_TO_CLONE);
    MethodTracker methodTracker = CodeTracker.methodTracker()
            .repository(repository)
            .filePath("CryptoServer/src/main/java/com/cryptography/dto/EncryptReqDto.java")
            .startCommitId("213ed6bbd3c0b94423e7338b191e26f876c0c0d5")
            .methodName("getPlainText")
            .methodDeclarationLineNumber(6)
            .build();
    History<Method> methodHistory = methodTracker.track();
    printHappyCaseOutput(methodHistory);
    assertNotNull(methodHistory);
    assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));

}
    @Test
    public void returnContainerChangeAfterRenamingFile() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/dto/EncryptRequestDto.java")
                .startCommitId("f8b244f015b79c95dfb7fb86340c749447f61b34")
                .methodName("getPlainText")
                .methodDeclarationLineNumber(6)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));

    }
    @Test
    public void testMoveFileAndBodyChangeSimultaneously() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/dto/Response/EncryptResDto.java")
                .startCommitId("d64f222f3512c8ce5453d53c77093ae4b2a995ca")
                .methodName("getCipherText")
                .methodDeclarationLineNumber(6)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));
    }
    @Test
    public void testConstructorWithMinimalChangeCommit() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/dto/UserDto.java")
                .startCommitId("d64f222f3512c8ce5453d53c77093ae4b2a995ca")
                .methodName("UserDto")
                .methodDeclarationLineNumber(9)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));

    }
    @Test
    public void testMethodClassImplementsInterface() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/config/CustomUserDetailManagerImpl.java")
                .startCommitId("d64f222f3512c8ce5453d53c77093ae4b2a995ca")
                .methodName("createUser")
                .methodDeclarationLineNumber(22)
                .build();
        History<Method> methodHistory = methodTracker.track();
        printHappyCaseOutput(methodHistory);
        assertNotNull(methodHistory);
        assertTrue(!methodHistory.getHistoryInfoList().isEmpty() && methodHistory.getHistoryInfoList().get(1).getChangeType().getTitle().equals("container change"));
    }
    @Test
    public void uncommentedMethod() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/comment_to_method/Main.java")
                .startCommitId("f1860ea8ac8f61b539b83236e6b7ca020e069cdb")
                .methodName("goodbye")
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
            System.out.println("CommitterName: " + historyInfo.getCommitterName());

            System.out.println("Before: " + historyInfo.getElementBefore().getName());
            System.out.println("After: " + historyInfo.getElementAfter().getName());

            for (Change change : historyInfo.getChangeList()) {
                System.out.println(change.getType().getTitle() + ": " + change);
            }
        }
        System.out.println("======================================================");
    }

}
