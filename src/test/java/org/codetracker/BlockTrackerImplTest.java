package org.codetracker;

import gr.uom.java.xmi.LocationInfo;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.change.Change;
import org.codetracker.element.Block;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockTrackerImplTest {
    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";
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
                    .build();

            History<Block> blockHistory = blockTracker.track();
        printHappyCaseOutput(blockHistory);
        assertNotNull(blockHistory);
        assertTrue(!blockHistory.getHistoryInfoList().isEmpty() && blockHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    @Test
    public void testDelegateMethodLocalFile() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        BlockTracker blockTracker = CodeTracker.blockTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/blocktracking/delegate/Main.java")
                .startCommitId("97eb547f6fae7c09aba90f030f0557a77ee86cdf")
                .methodName("getTheYoungestEmployeeSalary")
                .methodDeclarationLineNumber(12)
                .codeElementType(LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT)
                .blockStartLineNumber(15)
                .blockEndLineNumber(17)
                .build();

        History<Block> blockHistory = blockTracker.track();
        printHappyCaseOutput(blockHistory);
        assertNotNull(blockHistory);
        assertTrue(!blockHistory.getHistoryInfoList().isEmpty() && blockHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }
    @Test
    public void testDelegateSameSignatureMethodOtherFile() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);
        BlockTracker blockTracker = CodeTracker.blockTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/blocktracking/delegate/Utilitu.java")
                .startCommitId("f99f42b46810963f9e27973a28169734c5ee2ce3")
                .methodName("getTheYoungestEmployeeSalary")
                .methodDeclarationLineNumber(6)
                .codeElementType(LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT)
                .blockStartLineNumber(8)
                .blockEndLineNumber(10)
                .build();

        History<Block> blockHistory = blockTracker.track();
        printHappyCaseOutput(blockHistory);
        assertNotNull(blockHistory);
        assertTrue(!blockHistory.getHistoryInfoList().isEmpty() && blockHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));
    }
    private void printHappyCaseOutput(History<Block> blockHistory){
        for (History.HistoryInfo<Block> historyInfo : blockHistory.getHistoryInfoList()) {
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
//            System.out.println("---- Statements visible to CodeTracker ----");
//            for (AbstractStatement st :
//                    startMethod.getUmlOperation()
//                            .getBody()
//                            .getCompositeStatement()
//                            .getAllStatements()) {
//
//                System.out.printf("%s  %d-%d%n",
//                        st.getLocationInfo().getCodeElementType(),
//                        st.getLocationInfo().getStartLine(),
//                        st.getLocationInfo().getEndLine());
//            }
//            System.out.println("-------------------------------------------");


