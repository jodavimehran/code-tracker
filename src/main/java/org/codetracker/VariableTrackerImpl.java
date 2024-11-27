package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.codetracker.api.*;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;

import java.util.*;
import java.util.function.Predicate;

public class VariableTrackerImpl extends BaseTracker implements VariableTracker {
    private final VariableTrackerChangeHistory changeHistory;

    public VariableTrackerImpl(Repository repository, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber, String variableName, int variableDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.changeHistory = new VariableTrackerChangeHistory(methodName, methodDeclarationLineNumber, variableName, variableDeclarationLineNumber);
    }

    @Override
    public History<Variable> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {

            Version startVersion = gitRepository.getVersion(startCommitId);

            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));

            Method startMethod = getMethod(umlModel, startVersion, changeHistory::isStartMethod);
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
                if (commits == null || !currentVariable.getFilePath().equals(lastFileName)) {
                    lastFileName = currentVariable.getFilePath();
                    commits = getCommits(repository, currentVariable.getVersion().getId(), currentVariable.getFilePath(), git);
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

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);

                    Method currentMethod = Method.of(currentVariable.getOperation(), currentVersion);

                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                    if (rightMethod == null) {
                        continue;
                    }
                    String rightMethodClassName = rightMethod.getUmlOperation().getClassName();
                    String rightMethodSourceFolder = rightMethod.getUmlOperation().getLocationInfo().getSourceFolder();

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
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethod.getFilePath()));

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


                            UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodSourceFolder, rightMethodClassName);
                            if (umlClassDiff != null) {
                                found = changeHistory.checkClassDiffForVariableChange(currentVersion, parentVersion, equalMethod, equalVariable, umlClassDiff);

                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }

                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getLocationInfo().getSourceFolder(), rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
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
}
