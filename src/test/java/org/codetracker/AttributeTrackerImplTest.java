package org.codetracker;

import org.codetracker.api.AttributeTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.change.Change;
import org.codetracker.element.Attribute;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttributeTrackerImplTest {
    String FOLDER_TO_CLONE = "D:\\work\\programming\\CodeTrackerTesting";
    String GITHUB_REPO_TO_CLONE = "https://github.com/parinaz-st/CryptographyRestService.git";

    @Test
    public void returnFalseRename() throws Exception {
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE,
                GITHUB_REPO_TO_CLONE);

        AttributeTracker attributeTracker = CodeTracker.attributeTracker()
                .repository(repository)
                .filePath("CryptoServer/src/main/java/com/cryptography/codetrackertest/dependencyinjection/genericcase/ServiceInfo.java")
                .startCommitId("88cbdb012a2e92d029f20b5253272f32ce84c3be")
                .attributeName("serviceName")
                .attributeDeclarationLineNumber(3)
                .build();
        History<Attribute> attributeHistory = attributeTracker.track();
        printHappyCaseOutput(attributeHistory);
        assertNotNull(attributeHistory);
        assertTrue(!attributeHistory.getHistoryInfoList().isEmpty() && attributeHistory.getHistoryInfoList().get(0).getChangeType().getTitle().equals("introduced"));

    }
    private void printHappyCaseOutput(History<Attribute> attributeHistory){
            for (History.HistoryInfo<Attribute> historyInfo : attributeHistory.getHistoryInfoList()) {
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
