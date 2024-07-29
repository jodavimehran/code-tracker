package org.codetracker;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.VariableTracker;
import org.codetracker.api.Version;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class VariableTrackerWithLocalFiles extends BaseTrackerWithLocalFiles implements VariableTracker {
    private final VariableTrackerChangeHistory changeHistory;

    public VariableTrackerWithLocalFiles(String cloneURL, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber, String variableName, int variableDeclarationLineNumber) {
        super(cloneURL, startCommitId, filePath);
        this.changeHistory = new VariableTrackerChangeHistory(methodName, methodDeclarationLineNumber, variableName, variableDeclarationLineNumber);
    }

    @Override
    public History<Variable> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        Version startVersion = new VersionImpl(startCommitId, 0, 0, "");
        CommitModel startModel = getCommitModel(startCommitId);
        Set<String> startFileNames = Collections.singleton(filePath);
    	Map<String, String> startFileContents = new LinkedHashMap<>();
    	for(String rightFileName : startFileNames) {
    		startFileContents.put(rightFileName, startModel.fileContentsCurrentOriginal.get(rightFileName));
    	}
    	UMLModel umlModel = GitHistoryRefactoringMinerImpl.createModel(startFileContents, startModel.repositoryDirectoriesCurrent);
    	umlModel.setPartial(true);
        Method startMethod = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
        String startFilePath = startMethod.getFilePath();
        if (startMethod == null) {
            throw new CodeElementNotFoundException(filePath, changeHistory.getMethodName(), changeHistory.getMethodDeclarationLineNumber());
        }
        Variable startVariable = startMethod.findVariable(changeHistory::isStartVariable);
        if (startVariable == null) {
            throw new CodeElementNotFoundException(filePath, changeHistory.getVariableName(), changeHistory.getVariableDeclarationLineNumber());
        }

        changeHistory.get().addNode(startVariable);

        changeHistory.addFirst(startVariable);
        HashSet<String> analysedCommits = new HashSet<>();
        List<String> commits = null;
        String lastFileName = null;
        while (!changeHistory.isEmpty()) {
            Variable currentVariable = changeHistory.poll();
            if (currentVariable.isAdded()) {
                commits = null;
                continue;
            }
            final String currentMethodFilePath = currentVariable.getFilePath();
            if (commits == null || !currentVariable.getFilePath().equals(lastFileName)) {
                lastFileName = currentVariable.getFilePath();
                String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
        		String className = startFilePath.substring(startFilePath.lastIndexOf("/") + 1);
        		className = className.endsWith(".java") ? className.substring(0, className.length()-5) : className;
                String jsonPath = System.getProperty("user.dir") + "/src/test/resources/variable/" + repoName + "-" + className + "-" + changeHistory.getMethodName() + ".json";
                File jsonFile = new File(jsonPath);
                commits = getCommits(currentVariable.getVersion().getId(), jsonFile);
                historyReport.gitLogCommandCallsPlusPlus();
                analysedCommits.clear();
            }
            if (analysedCommits.containsAll(commits))
                break;
            for (String commitId : commits) {
                if (analysedCommits.contains(commitId))
                    continue;
                //System.out.println("processing " + commitId);
                analysedCommits.add(commitId);

                CommitModel lightCommitModel = getLightCommitModel(commitId, currentMethodFilePath);
                String parentCommitId = lightCommitModel.parentCommitId;
                Version currentVersion = new VersionImpl(commitId, 0, 0, "");
                Version parentVersion = new VersionImpl(parentCommitId, 0, 0, "");
            	
            	UMLModel leftModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsBeforeOriginal, lightCommitModel.repositoryDirectoriesBefore);
            	leftModel.setPartial(true);
            	UMLModel rightModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsCurrentOriginal, lightCommitModel.repositoryDirectoriesCurrent);
            	rightModel.setPartial(true);

                Method currentMethod = Method.of(currentVariable.getOperation(), currentVersion);
                Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                if (rightMethod == null) {
                    continue;
                }
                String rightMethodClassName = rightMethod.getUmlOperation().getClassName();

                Variable rightVariable = rightMethod.findVariable(currentVariable::equalIdentifierIgnoringVersion);
                if (rightVariable == null) {
                    continue;
                }
                Predicate<Method> equalMethod = rightMethod::equalIdentifierIgnoringVersion;
                Predicate<Variable> equalVariable = rightVariable::equalIdentifierIgnoringVersion;
                historyReport.analysedCommitsPlusPlus();
                if ("0".equals(parentCommitId)) {
                    Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                    Variable leftVariable = Variable.of(rightVariable.getVariableDeclaration(), leftMethod);
                    changeHistory.get().handleAdd(leftVariable, rightVariable, "Initial commit!");
                    changeHistory.get().connectRelatedNodes();
                    changeHistory.add(leftVariable);
                    break;
                }
                //NO CHANGE
                Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                if (leftMethod != null) {
                    historyReport.step2PlusPlus();
                    continue;
                }
                //CHANGE BODY OR DOCUMENT
                leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
                if (leftMethod != null) {
                    VariableDeclarationContainer leftOperation = leftMethod.getUmlOperation();
                    VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
                    UMLOperationBodyMapper bodyMapper = null;
                    Set<Refactoring> refactorings = Collections.emptySet();
                    if (leftOperation instanceof UMLOperation && rightOperation instanceof UMLOperation) {
                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
                        bodyMapper = new UMLOperationBodyMapper((UMLOperation) leftOperation, (UMLOperation) rightOperation, lightweightClassDiff);
                        refactorings = bodyMapper.getRefactorings();
                        if (VariableTrackerChangeHistory.involvedInVariableRefactoring(refactorings, rightVariable) && containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
                            bodyMapper = null;
                        }
                    }
                    else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
                        UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
                        bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
                        refactorings = bodyMapper.getRefactorings();
                        if (VariableTrackerChangeHistory.involvedInVariableRefactoring(refactorings, rightVariable) && containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
                            bodyMapper = null;
                        }
                    }
                    if (changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, refactorings)) {
                        historyReport.step3PlusPlus();
                        break;
                    }
                }
                UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                {
                    //Local Refactoring
                    List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                    boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                    if (found) {
                        historyReport.step4PlusPlus();
                        break;
                    }

                    found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                    if (found) {
                        historyReport.step4PlusPlus();
                        break;
                    }
                    UMLOperationBodyMapper bodyMapper = findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion);
                    Set<Refactoring> bodyMapperRefactorings = bodyMapper != null ? bodyMapper.getRefactoringsAfterPostProcessing() : Collections.emptySet();
                    found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, bodyMapperRefactorings);
                    if (found) {
                        historyReport.step4PlusPlus();
                        break;
                    }
                }

                //All refactorings
                {
                    CommitModel commitModel = getCommitModel(commitId);
                    if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                        String leftFilePath = null;
                        for (MoveSourceFolderRefactoring ref : commitModel.moveSourceFolderRefactorings) {
                            if (ref.getIdenticalFilePaths().containsValue(currentVariable.getFilePath())) {
                                for (Map.Entry<String, String> entry : ref.getIdenticalFilePaths().entrySet()) {
                                    if (entry.getValue().equals(currentVariable.getFilePath())) {
                                        leftFilePath = entry.getKey();
                                        break;
                                    }
                                }
                                if (leftFilePath != null) {
                                    break;
                                }
                            }
                        }
                        Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentMethod.getFilePath(), s -> true, true);
                        if (leftFilePath != null) {
                            boolean found = false;
                            for (UMLClass umlClass : umlModelPairPartial.getLeft().getClassList()) {
                                if (umlClass.getSourceFile().equals(leftFilePath)) {
                                    for (UMLOperation operation : umlClass.getOperations()) {
                                        if (operation.equals(rightMethod.getUmlOperation())) {
                                            found = changeHistory.isMatched(operation, rightVariable, parentVersion);
                                            if (found) {
                                                break;
                                            }
                                        }
                                    }
                                    if (found) {
                                        break;
                                    }
                                }
                            }
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        else {
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
                            //List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();

                            boolean found;
                            UMLOperationBodyMapper bodyMapper = findBodyMapper(umlModelDiffPartial, rightMethod, currentVersion, parentVersion);
                            Set<Refactoring> bodyMapperRefactorings = bodyMapper != null ? bodyMapper.getRefactoringsAfterPostProcessing() : Collections.emptySet();
                            found = changeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, bodyMapperRefactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                    {
                        Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
                        Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
                        UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                        Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
                        UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightMethodClassName);
                        if (classDiff != null) {
                            List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
                            boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightVariable, classLevelRefactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.isVariableRefactored(classLevelRefactorings, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightVariable, classLevelRefactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = changeHistory.checkClassDiffForVariableChange(currentVersion, parentVersion, equalMethod, equalVariable, classDiff);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
                        boolean flag = false;
                        for (Refactoring refactoring : refactorings) {
                            if (RefactoringType.MOVE_AND_RENAME_OPERATION.equals(refactoring.getRefactoringType()) || RefactoringType.MOVE_OPERATION.equals(refactoring.getRefactoringType())) {
                                MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                                Method movedOperation = Method.of(moveOperationRefactoring.getMovedOperation(), currentVersion);
                                if (rightMethod.equalIdentifierIgnoringVersion(movedOperation)) {
                                    fileNames.add(moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath());
                                    flag = true;
                                }
                            }
                        }
                        if (flag) {
                            umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
                            umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                            refactorings = umlModelDiffAll.getRefactorings();
                        }

                        boolean found = changeHistory.checkForExtractionOrInline(currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }

                        found = changeHistory.isVariableRefactored(refactorings, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }

                        found = changeHistory.checkRefactoredMethod(currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step5PlusPlus();
                            break;
                        }


                        UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodClassName);
                        if (umlClassDiff != null) {
                            found = changeHistory.checkClassDiffForVariableChange(currentVersion, parentVersion, equalMethod, equalVariable, umlClassDiff);

                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }

                        if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
                        }, currentVersion)) {
                            Variable variableBefore = Variable.of(rightVariable.getVariableDeclaration(), rightVariable.getOperation(), parentVersion);
                            changeHistory.get().handleAdd(variableBefore, rightVariable, "added with method");
                            changeHistory.add(variableBefore);
                            changeHistory.get().connectRelatedNodes();
                            historyReport.step5PlusPlus();
                            break;
                        }
                    }
                }
            }
        }
        return new HistoryImpl<>(changeHistory.get().getCompleteGraph(), historyReport);
    }
}
