package org.refactoringrefiner.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.graph.EndpointPair;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.RefactoringMiner;
import org.refactoringrefiner.api.*;
import org.refactoringrefiner.change.AbstractChange;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.element.Variable;
import org.refactoringrefiner.test.oracle.MethodOracle;
import org.refactoringrefiner.util.Util;

import java.io.File;
import java.util.*;

public class VariableDatasetCreator {
    private final static String RESULT_PATH = "E:\\Data\\History\\Variable\\dataset\\";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter WRITER = MAPPER.writer(new DefaultPrettyPrinter());

    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void main(String[] args) throws Exception {
        new VariableDatasetCreator().createVariableDataset();
    }

    private void createVariableDataset() throws Exception {
//        for (MethodOracle methodOracle : MethodOracle.all())
        MethodOracle methodOracle = MethodOracle.training();
        {
            for (Map.Entry<String, MethodHistoryInfo> oracleEntry : methodOracle.getOracle().entrySet()) {
                String fileName = oracleEntry.getKey();
                MethodHistoryInfo methodHistoryInfo = oracleEntry.getValue();
                String repositoryWebURL = methodHistoryInfo.getRepositoryWebURL();
                String repositoryName = Util.getRepositoryNameFromRepositoryWebUrl(repositoryWebURL);
                String projectDirectory = Configs.FOLDER_TO_CLONE + repositoryName;
                GitService gitService = new GitServiceImpl();
                try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                    try (Git git = new Git(repository)) {
                        RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
                        for (ChangeHistory changeHistory : methodHistoryInfo.getExpectedChanges()) {
                            String commitId = changeHistory.getCommitId();
                            Version currentVersion = refactoringMiner.getVersion(commitId);
                            String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
                            Version parentVersion = refactoringMiner.getVersion(parentCommitId);

                            UMLModel changedModelRight = refactoringMiner.getUMLModel(commitId, Collections.singletonList(changeHistory.getElementFileAfter()));
                            Method changedMethodRight = RefactoringMiner.getMethodByName(changedModelRight, currentVersion, changeHistory.getElementNameAfter());
                            if (Change.Type.INTRODUCED.equals(Change.Type.get(changeHistory.getChangeType()))) {
                                for (VariableDeclaration variableDeclaration : changedMethodRight.getUmlOperation().getAllVariableDeclarations()) {
                                    Variable variableBefore = Variable.of(variableDeclaration, changedMethodRight.getUmlOperation(), parentVersion);
                                    Variable variableAfter = Variable.of(variableDeclaration, changedMethodRight.getUmlOperation(), currentVersion);
                                    refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(variableBefore, variableAfter);
                                }
                                continue;
                            }
                            UMLModel changedModelLeft = refactoringMiner.getUMLModel(changeHistory.getParentCommitId(), Collections.singletonList(changeHistory.getElementFileBefore()));
                            Method changedMethodLeft = RefactoringMiner.getMethodByName(changedModelLeft, parentVersion, changeHistory.getElementNameBefore());

                            UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(changedMethodLeft.getUmlOperation(), changedMethodRight.getUmlOperation(), null);
                            Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();

                            refactoringMiner.analyseVariableRefactorings(refactorings, currentVersion, parentVersion, variable -> true);

                            for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : umlOperationBodyMapper.getMatchedVariablesPair()) {
                                Variable variableAfter = Variable.of(matchedVariablePair.getRight().getLeft(), matchedVariablePair.getRight().getRight(), currentVersion);
                                Variable variableBefore = Variable.of(matchedVariablePair.getLeft().getLeft(), matchedVariablePair.getLeft().getRight(), parentVersion);
                                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                            }


                            for (Pair<VariableDeclaration, UMLOperation> addedVariable : umlOperationBodyMapper.getAddedVariables()) {
                                Variable variableAfter = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), currentVersion);
                                Variable variableBefore = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), parentVersion);
                                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(variableBefore, variableAfter);
                            }

                            for (Pair<VariableDeclaration, UMLOperation> removedVariable : umlOperationBodyMapper.getRemovedVariables()) {
                                Variable variableBefore = Variable.of(removedVariable.getLeft(), removedVariable.getRight(), parentVersion);
                                Variable variableAfter = Variable.of(removedVariable.getLeft(), removedVariable.getRight(), currentVersion);

                                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleRemoved(variableBefore, variableAfter);
                            }

                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().connectRelatedNodes();
                        }

                        String startCommitId = methodHistoryInfo.getStartCommitId();
                        UMLModel startModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(methodHistoryInfo.getFilePath()));
                        Method startMethod = RefactoringMiner.getMethodByName(startModel, refactoringMiner.getVersion(startCommitId), methodHistoryInfo.getFunctionKey());
                        Version startVersion = refactoringMiner.getVersion(startCommitId);

                        for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
                            Variable variableStart = Variable.of(variableDeclaration, startMethod.getUmlOperation(), startVersion);
                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addNode(variableStart);
                        }
                        refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().connectRelatedNodes();

                        Map<String, Integer> variableCount = new HashMap<>();
                        for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
                            variableCount.merge(variableDeclaration.getVariableName(), 1, Integer::sum);
                        }

                        for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
                            Variable variableStart = Variable.of(variableDeclaration, startMethod.getUmlOperation(), startVersion);
                            Graph<CodeElement, Edge> variableHistory = refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().findSubGraph(variableStart);

                            VariableHistoryInfo variableHistoryInfo = new VariableHistoryInfo();
                            variableHistoryInfo.setRepositoryName(repositoryName);
                            variableHistoryInfo.setRepositoryWebURL(repositoryWebURL);
                            variableHistoryInfo.setFilePath(startMethod.getFilePath());
                            variableHistoryInfo.setFunctionName(startMethod.getUmlOperation().getName());
                            variableHistoryInfo.setFunctionKey(startMethod.getName());
                            variableHistoryInfo.setFunctionStartLine(methodHistoryInfo.getFunctionStartLine());
                            variableHistoryInfo.setStartCommitId(methodHistoryInfo.getStartCommitId());
                            variableHistoryInfo.setVariableName(variableDeclaration.getVariableName());
                            int startLine = variableDeclaration.getLocationInfo().getStartLine();
                            variableHistoryInfo.setVariableKey(String.format("%s$%s(%d)", methodHistoryInfo.getFunctionKey(), variableDeclaration.getVariableName(), startLine));
                            variableHistoryInfo.setVariableStartLine(startLine);

                            for (EndpointPair<CodeElement> edge : variableHistory.getEdges()) {
                                EdgeImpl edgeValue = (EdgeImpl) variableHistory.getEdgeValue(edge).get();
                                for (Change change : edgeValue.getChangeList()) {
                                    if (Change.Type.NO_CHANGE.equals(change.getType()))
                                        continue;
                                    Change.Type changeType = change.getType();
                                    CodeElement target = edge.target();
                                    String parentCommitId = edge.source().getVersion().getId();
                                    String commitId = target.getVersion().getId();
                                    ChangeHistory changeHistory = new ChangeHistory();
                                    changeHistory.setChangeType(changeType.getTitle());

                                    changeHistory.setCommitId(commitId);
                                    changeHistory.setParentCommitId(parentCommitId);
                                    changeHistory.setCommitTime(target.getVersion().getTime());

                                    changeHistory.setElementFileAfter(target.getFilePath());
                                    changeHistory.setElementNameAfter(target.getName());

                                    CodeElement source = edge.source();
                                    changeHistory.setElementFileBefore(source.getFilePath());
                                    changeHistory.setElementNameBefore(source.getName());

                                    variableHistoryInfo.getExpectedChanges().add(changeHistory);
                                }
                            }
                            variableHistoryInfo.getExpectedChanges().sort(Comparator.comparing(ChangeHistory::getCommitTime).thenComparing(ChangeHistory::getCommitId).thenComparing(ChangeHistory::getChangeType).reversed());
                            File newFile;
                            if (variableCount.get(variableDeclaration.getVariableName()) == 1) {
                                newFile = new File(RESULT_PATH + "\\" + methodOracle.getName() + "\\" + fileName.replace(".json", "") + "-" + variableDeclaration.getVariableName() + ".json");
                            } else {
                                newFile = new File(RESULT_PATH + "\\" + methodOracle.getName() + "\\" + fileName.replace(".json", "") + "-" + variableDeclaration.getVariableName() + "1.json");
                                int i = 2;
                                while (newFile.exists()) {
                                    newFile = new File(RESULT_PATH + "\\" + methodOracle.getName() + "\\" + fileName.replace(".json", "") + "-" + variableDeclaration.getVariableName() + (i++) + ".json");
                                }
                            }
                            WRITER.writeValue(newFile, variableHistoryInfo);
                        }
                    }
                }
            }
        }

//        String processedFilePath = "E:\\Data\\History\\Variable\\dataset\\processed.csv";
//        String finishedFilePath = "E:\\Data\\History\\Variable\\dataset\\report.csv";
//        Set<String> processedFiles = getAllProcessedSamples(processedFilePath);
//        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
//        ClassLoader classLoader = Starter.class.getClassLoader();
//        File historyFolder = new File(classLoader.getResource("history/method/train").getFile());
////        File resultFolder = new File(classLoader.getResource("history/variable/training").getFile());
//
//        for (File file : historyFolder.listFiles()) {
//            if (processedFiles.contains(file.getName()))
//                continue;
//
//            VariableHistoryInfo historyInfo = mapper.readValue(file, VariableHistoryInfo.class);
//            String repositoryWebURL = historyInfo.getRepositoryWebURL();
//            String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
//            String projectDirectory = FOLDER_TO_CLONE + repositoryName;
//
//
//            GitService gitService = new GitServiceImpl();
//            try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
//                try (Git git = new Git(repository)) {
//                    RefactoringMiner refactoringMiner = new RefactoringMiner(repository, repositoryWebURL);
//
//                    List<HistoryResult> methodChangedCommits = getMethodChangedCommits(sessionObj, repositoryWebURL, historyInfo.getFunctionKey());
//                    Set<String> processedCommits = new HashSet<>();
//                    for (HistoryResult historyResult : methodChangedCommits) {
//                        String commitId = historyResult.getElementVersionIdAfter();
//                        Version currentVersion = refactoringMiner.getVersion(commitId);
//                        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
//                        Version parentVersion = refactoringMiner.getVersion(parentCommitId);
//
//                        UMLModel changedModelRight = refactoringMiner.getUMLModel(commitId, Collections.singletonList(historyResult.getElementFileAfter()));
//                        Method changedMethodRight = RefactoringMiner.getMethodByName(changedModelRight, refactoringMiner.getVersion(commitId), historyResult.getElementNameAfter());
//
//                        if ("added".equals(historyResult.getChangeType())) {
//                            for (VariableDeclaration variableDeclaration : changedMethodRight.getUmlOperation().getAllVariableDeclarations()) {
//                                Variable variableBefore = Variable.of(variableDeclaration, changedMethodRight.getUmlOperation(), parentVersion);
//                                Variable variableAfter = Variable.of(variableDeclaration, changedMethodRight.getUmlOperation(), currentVersion);
//                                refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(variableBefore, variableAfter);
//                            }
//                            continue;
//                        }
//                        UMLModel changedModelLeft = refactoringMiner.getUMLModel(historyResult.getElementVersionIdBefore(), Collections.singletonList(historyResult.getElementFileBefore()));
//                        Method changedMethodLeft = RefactoringMiner.getMethodByName(changedModelLeft, refactoringMiner.getVersion(historyResult.getElementVersionIdBefore()), historyResult.getElementNameBefore());
//
//                        UMLOperationBodyMapper umlOperationBodyMapper = new UMLOperationBodyMapper(changedMethodLeft.getUmlOperation(), changedMethodRight.getUmlOperation(), null);
//                        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();
//
//                        refactoringMiner.analyseVariableRefactorings(refactorings, currentVersion, parentVersion, variable -> true);
//
//                        for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : umlOperationBodyMapper.getMatchedVariablesPair()) {
//                            Variable variableAfter = Variable.of(matchedVariablePair.getRight().getLeft(), matchedVariablePair.getRight().getRight(), currentVersion);
//                            Variable variableBefore = Variable.of(matchedVariablePair.getLeft().getLeft(), matchedVariablePair.getLeft().getRight(), parentVersion);
//                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
//                        }
//
//
//                        for (Pair<VariableDeclaration, UMLOperation> addedVariable : umlOperationBodyMapper.getAddedVariables()) {
//                            Variable variableAfter = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), currentVersion);
//                            Variable variableBefore = Variable.of(addedVariable.getLeft(), addedVariable.getRight(), parentVersion);
//                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleAdd(variableBefore, variableAfter);
//                        }
//
//                        for (Pair<VariableDeclaration, UMLOperation> removedVariable : umlOperationBodyMapper.getRemovedVariables()) {
//                            Variable variableBefore = Variable.of(removedVariable.getLeft(), removedVariable.getRight(), parentVersion);
//                            Variable variableAfter = Variable.of(removedVariable.getLeft(), removedVariable.getRight(), currentVersion);
//
//                            refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().handleRemoved(variableBefore, variableAfter);
//                        }
//
//                        refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().connectRelatedNodes();
//                    }
//
//                    String startCommitId = historyInfo.getStartCommitName();
//                    UMLModel startModel = refactoringMiner.getUMLModel(startCommitId, Collections.singletonList(historyInfo.getFilePath()));
//                    Method startMethod = RefactoringMiner.getMethodByName(startModel, refactoringMiner.getVersion(startCommitId), historyInfo.getFunctionKey());
//                    Version startVersion = refactoringMiner.getVersion(startCommitId);
//
//                    for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
//                        Variable variableStart = Variable.of(variableDeclaration, startMethod.getUmlOperation(), startVersion);
//                        refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().addNode(variableStart);
//                    }
//                    refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().connectRelatedNodes();
//
//
//                    for (VariableDeclaration variableDeclaration : startMethod.getUmlOperation().getAllVariableDeclarations()) {
//                        Variable variableStart = Variable.of(variableDeclaration, startMethod.getUmlOperation(), startVersion);
//                        Graph<CodeElement, Edge> variableHistory = refactoringMiner.getRefactoringHandler().getVariableChangeHistoryGraph().findSubGraph(variableStart);
//
//                        VariableHistoryInfo variableHistoryInfo = new VariableHistoryInfo();
//                        variableHistoryInfo.setRepositoryName(repositoryName);
//                        variableHistoryInfo.setRepositoryWebURL(repositoryWebURL);
//                        variableHistoryInfo.setFilePath(startMethod.getFilePath());
//                        variableHistoryInfo.setFunctionName(startMethod.getUmlOperation().getName());
//                        variableHistoryInfo.setFunctionKey(startMethod.getName());
//                        variableHistoryInfo.setFunctionStartLine(historyInfo.getFunctionStartLine());
//
//                        variableHistoryInfo.setVariableName(variableDeclaration.getVariableName());
//                        int startLine = variableDeclaration.getLocationInfo().getStartLine();
//                        variableHistoryInfo.setVariableKey(String.format("%s$%s(%d)", historyInfo.getFunctionKey(), variableDeclaration.getVariableName(), startLine));
//                        variableHistoryInfo.setVariableStartLine(startLine);
//
//                        for (EndpointPair<CodeElement> edge : variableHistory.getEdges()) {
//                            EdgeImpl edgeValue = (EdgeImpl) variableHistory.getEdgeValue(edge).get();
//                            for (Change change : edgeValue.getChangeList()) {
//                                if (Change.Type.NO_CHANGE.equals(change.getType()))
//                                    continue;
//                                Change.Type changeType = change.getType();
//                                CodeElement target = edge.target();
//                                String commitId = target.getVersion().getId();
//                                ChangeHistory changeHistory = new ChangeHistory();
//                                changeHistory.setChangeType(changeType.getTitle());
//
//                                changeHistory.setCommitId(commitId);
//                                changeHistory.setCommitTime(target.getVersion().getTime());
//
//                                changeHistory.setElementFileAfter(target.getFilePath());
//                                changeHistory.setElementNameAfter(target.getName());
//
//                                CodeElement source = edge.source();
//                                changeHistory.setElementFileBefore(source.getFilePath());
//                                changeHistory.setElementNameBefore(source.getName());
//
//                                variableHistoryInfo.getExpectedChanges().add(changeHistory);
//                            }
//                        }
//
//                        File newFile = new File(resultFolder.getPath() + "\\" + file.getName().replace(".json", "") + "-" + variableDeclaration.getVariableName() + ".json");
//                        int i = 1;
//                        while (newFile.exists()) {
//                            newFile = new File(resultFolder.getPath() + "\\" + file.getName().replace(".json", "") + "-" + variableDeclaration.getVariableName() + i + ".json");
//                            i++;
//                        }
//                        writer.writeValue(newFile, variableHistoryInfo);
//                    }
//
//                }
//            }
//        }
    }
}
