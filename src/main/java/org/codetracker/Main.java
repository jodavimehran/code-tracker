package org.codetracker;

import gr.uom.java.xmi.LocationInfo;
import org.codetracker.api.*;
import org.codetracker.change.Change;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Main {
    private final static String FOLDER_TO_CLONE = "tmp/";

    public static void main(String args[]) throws Exception {
        GitService gitService = new GitServiceImpl();
        // METHOD TRACKING EXAMPLE
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")) {

            MethodTracker methodTracker = CodeTracker.methodTracker()
                    .repository(repository)
                    .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                    .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                    .methodName("fireErrors")
                    .methodDeclarationLineNumber(384)
                    .build();

            History<Method> methodHistory = methodTracker.track();

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

        // VARIABLE TRACKING EXAMPLE
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")) {

            VariableTracker variableTracker = CodeTracker.variableTracker()
                    .repository(repository)
                    .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                    .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                    .methodName("fireErrors")
                    .methodDeclarationLineNumber(384)
                    .variableName("stripped")
                    .variableDeclarationLineNumber(385)
                    .build();

            History<Variable> variableHistory = variableTracker.track();

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

        // ATTRIBUTE TRACKING EXAMPLE
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")) {

            AttributeTracker attributeTracker = CodeTracker.attributeTracker()
                    .repository(repository)
                    .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                    .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                    .attributeName("cacheFile")
                    .attributeDeclarationLineNumber(132)
                    .build();

            History<Attribute> attributeHistory = attributeTracker.track();

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

        // BLOCK TRACKING EXAMPLE
        try (Repository repository = gitService.cloneIfNotExists(FOLDER_TO_CLONE + "checkstyle\\checkstyle",
                "https://github.com/checkstyle/checkstyle.git")) {

            BlockTracker blockTracker = CodeTracker.blockTracker()
                    .repository(repository)
                    .filePath("src/main/java/com/puppycrawl/tools/checkstyle/Checker.java")
                    .startCommitId("119fd4fb33bef9f5c66fc950396669af842c21a3")
                    .methodName("setupChild")
                    .methodDeclarationLineNumber(448)
                    .codeElementType(LocationInfo.CodeElementType.TRY_STATEMENT)
                    .blockStartLineNumber(453)
                    .blockEndLineNumber(465)
                    .build();

            History<Block> blockHistory = blockTracker.track();

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
}

