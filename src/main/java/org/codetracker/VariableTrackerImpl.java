package org.codetracker;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.codetracker.api.*;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;

import java.util.*;
import java.util.function.Predicate;

public class VariableTrackerImpl extends BaseTracker implements VariableTracker {
    private final ChangeHistory<Variable> variableChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final String variableName;
    private final int variableDeclarationLineNumber;

    public VariableTrackerImpl(Repository repository, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber, String variableName, int variableDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.variableName = variableName;
        this.variableDeclarationLineNumber = variableDeclarationLineNumber;
    }

    private boolean isStartVariable(Variable variable) {
        return variable.getVariableDeclaration().getVariableName().equals(variableName) &&
                variable.getVariableDeclaration().getLocationInfo().getStartLine() <= variableDeclarationLineNumber &&
                variable.getVariableDeclaration().getLocationInfo().getEndLine() >= variableDeclarationLineNumber;
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }


    @Override
    public History<Variable> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {

            Version startVersion = gitRepository.getVersion(startCommitId);

            UMLModel umlModel = getUMLModel(startCommitId, Collections.singletonList(filePath));

            Method startMethod = getMethod(umlModel, startVersion, this::isStartMethod);
            if (startMethod == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }
            Variable startVariable = startMethod.findVariable(this::isStartVariable);
            if (startVariable == null) {
                throw new CodeElementNotFoundException(filePath, variableName, variableDeclarationLineNumber);
            }

            variableChangeHistory.addNode(startVariable);

            ArrayDeque<Variable> variables = new ArrayDeque<>();
            variables.addFirst(startVariable);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!variables.isEmpty()) {
                Variable currentVariable = variables.poll();
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
                    System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);

                    Version currentVersion = gitRepository.getVersion(commitId);
                    String parentCommitId = gitRepository.getParentId(commitId);
                    Version parentVersion = gitRepository.getVersion(parentCommitId);

                    Method currentMethod = Method.of(currentVariable.getOperation(), currentVersion);

                    UMLModel rightModel = getUMLModel(commitId, Collections.singletonList(currentMethod.getFilePath()));
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
                        variableChangeHistory.handleAdd(leftVariable, rightVariable, "Initial commit!");
                        variableChangeHistory.connectRelatedNodes();
                        variables.add(leftVariable);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singletonList(currentMethod.getFilePath()));

                    //NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    if (leftMethod != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel, new HashMap<>());
                    {
                        //Local Refactoring
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        //CHANGE BODY OR DOCUMENT
                        leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
                        if (leftMethod != null) {
                            if (checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, leftMethod, currentVersion, parentVersion))) {
                                historyReport.step3PlusPlus();
                                break;
                            }
                        }

                        boolean found = checkForExtractionOrInline(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }

                        //Check if refactored
                        found = isVariableRefactored(refactorings, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }

                        found = checkRefactoredMethod(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                        found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }

                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, currentMethod.getFilePath(), s -> true, true);
                            UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight(), commitModel.renamedFilesHint);
                            List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();

                            boolean found;
                            found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffPartial, rightMethod, currentVersion, parentVersion));
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight(), commitModel.renamedFilesHint);

                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
                            boolean flag = false;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_AND_RENAME_OPERATION.equals(refactoring.getRefactoringType())) {
                                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                                    Method movedOperation = Method.of(moveOperationRefactoring.getMovedOperation(), currentVersion);
                                    if (rightMethod.equalIdentifierIgnoringVersion(movedOperation)) {
                                        fileNames.add(moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath());
                                        umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
                                        umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight(), commitModel.renamedFilesHint);
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                            if (flag) {
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            boolean found = checkForExtractionOrInline(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = isVariableRefactored(refactorings, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }

                            found = checkRefactoredMethod(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                            if (found) {
                                historyReport.step5PlusPlus();
                                break;
                            }


                            UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightMethodClassName);
                            if (umlClassDiff != null) {
                                found = checkClassDiffForVariableChange(variables, currentVersion, parentVersion, equalMethod, equalVariable, umlClassDiff);

                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }

                            if (isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), rightMethod::equalIdentifierIgnoringVersion, method -> {
                            }, currentVersion)) {
                                Variable variableBefore = Variable.of(rightVariable.getVariableDeclaration(), rightVariable.getOperation(), parentVersion);
                                variableChangeHistory.handleAdd(variableBefore, rightVariable, "added with method");
                                variables.add(variableBefore);
                                variableChangeHistory.connectRelatedNodes();
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(variableChangeHistory.findSubGraph(startVariable), historyReport);
        }
    }

    private boolean checkClassDiffForVariableChange(ArrayDeque<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Variable> equalVariable, UMLClassBaseDiff umlClassDiff) {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getOperation2(), currentVersion);
            if (equalMethod.test(method2)) {
                if (isVariableRefactored(operationBodyMapper.getRefactorings(), variables, currentVersion, parentVersion, equalVariable))
                    return true;

                // check if it is in the matched
                if (isMatched(operationBodyMapper, variables, currentVersion, parentVersion, equalVariable))
                    return true;

                //Check if is added
                if (isAdded(operationBodyMapper, variables, currentVersion, parentVersion, equalVariable))
                    return true;
            }
        }
        return false;
    }


    private boolean checkForExtractionOrInline(ArrayDeque<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Variable rightVariable, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
        for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        Variable variableBefore = Variable.of(rightVariable.getVariableDeclaration(), rightVariable.getOperation(), parentVersion);
                        variableChangeHistory.handleAdd(variableBefore, rightVariable, extractOperationRefactoring.toString());
                        variables.add(variableBefore);
                        variableChangeHistory.connectRelatedNodes();
                        return true;
                    }
                    break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    Method targetOperationAfterInline = Method.of(inlineOperationRefactoring.getTargetOperationAfterInline(), currentVersion);
                    if (equalMethod.test(targetOperationAfterInline)) {
                        UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                        for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : bodyMapper.getMatchedVariables()) {
                            Variable matchedVariableInsideInlinedMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getOperation2(), currentVersion);
                            if (matchedVariableInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                Variable variableBefore = Variable.of(matchedVariablePair.getLeft(), bodyMapper.getOperation1(), parentVersion);
                                variableChangeHistory.handleAdd(variableBefore, matchedVariableInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                variables.add(variableBefore);
                                variableChangeHistory.connectRelatedNodes();
                                return true;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean checkBodyOfMatchedOperations(Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) {
        if (umlOperationBodyMapper == null)
            return false;
        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactorings();

        //Check if refactored
        if (isVariableRefactored(refactorings, variables, currentVersion, parentVersion, equalOperator))
            return true;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, variables, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, variables, currentVersion, parentVersion, equalOperator);
    }

    private boolean isVariableRefactored(Collection<Refactoring> refactorings, Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        Set<Variable> leftVariableSet = analyseVariableRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        for (Variable leftVariable : leftVariableSet) {
            variables.add(leftVariable);
            variableChangeHistory.connectRelatedNodes();
            return true;
        }
        return false;
    }

    private boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : umlOperationBodyMapper.getMatchedVariables()) {
            Variable variableAfter = Variable.of(matchedVariablePair.getRight(), umlOperationBodyMapper.getOperation2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(matchedVariablePair.getLeft(), umlOperationBodyMapper.getOperation1(), parentVersion);
                variableChangeHistory.addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                variables.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        for (VariableDeclaration addedVariable : umlOperationBodyMapper.getAddedVariables()) {
            Variable variableAfter = Variable.of(addedVariable, umlOperationBodyMapper.getOperation2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(addedVariable, umlOperationBodyMapper.getOperation2(), parentVersion);
                variableChangeHistory.handleAdd(variableBefore, variableAfter, "new variable");
                variables.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private Set<Variable> analyseVariableRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        Set<Variable> leftVariableSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            Variable variableBefore = null;
            Variable variableAfter = null;
            Change.Type changeType = null;

            switch (refactoring.getRefactoringType()) {
                case RENAME_VARIABLE:
                case RENAME_PARAMETER: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    variableBefore = Variable.of(renameVariableRefactoring.getOriginalVariable(), renameVariableRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(renameVariableRefactoring.getRenamedVariable(), renameVariableRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.RENAME;
                    break;
                }
                case PARAMETERIZE_VARIABLE:
                case LOCALIZE_PARAMETER: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    variableBefore = Variable.of(renameVariableRefactoring.getOriginalVariable(), renameVariableRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(renameVariableRefactoring.getRenamedVariable(), renameVariableRefactoring.getOperationAfter(), currentVersion);
                    if (variableBefore.getName().equals(variableAfter.getName()))
                        changeType = Change.Type.NO_CHANGE;
                    else
                        changeType = Change.Type.RENAME;
                    break;
                }
                case CHANGE_VARIABLE_TYPE:
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    variableBefore = Variable.of(changeVariableTypeRefactoring.getOriginalVariable(), changeVariableTypeRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(changeVariableTypeRefactoring.getChangedTypeVariable(), changeVariableTypeRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
                case ADD_VARIABLE_MODIFIER:
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    variableBefore = Variable.of(addVariableModifierRefactoring.getVariableBefore(), addVariableModifierRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(addVariableModifierRefactoring.getVariableAfter(), addVariableModifierRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_VARIABLE_MODIFIER:
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    variableBefore = Variable.of(removeVariableModifierRefactoring.getVariableBefore(), removeVariableModifierRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(removeVariableModifierRefactoring.getVariableAfter(), removeVariableModifierRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case ADD_VARIABLE_ANNOTATION:
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    variableBefore = Variable.of(addVariableAnnotationRefactoring.getVariableBefore(), addVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(addVariableAnnotationRefactoring.getVariableAfter(), addVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_VARIABLE_ANNOTATION:
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    variableBefore = Variable.of(modifyVariableAnnotationRefactoring.getVariableBefore(), modifyVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(modifyVariableAnnotationRefactoring.getVariableAfter(), modifyVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_VARIABLE_ANNOTATION:
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    variableBefore = Variable.of(removeVariableAnnotationRefactoring.getVariableBefore(), removeVariableAnnotationRefactoring.getOperationBefore(), parentVersion);
                    variableAfter = Variable.of(removeVariableAnnotationRefactoring.getVariableAfter(), removeVariableAnnotationRefactoring.getOperationAfter(), currentVersion);
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case SPLIT_PARAMETER:
                case SPLIT_VARIABLE: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    for (VariableDeclaration splitVariable : splitVariableRefactoring.getSplitVariables()) {
                        Variable addedVariableAfter = Variable.of(splitVariable, splitVariableRefactoring.getOperationAfter(), parentVersion);
                        if (equalOperator.test(addedVariableAfter)) {
                            Variable addedVariableBefore = Variable.of(splitVariable, splitVariableRefactoring.getOperationAfter(), parentVersion);
                            addedVariableBefore.setAdded(true);
                            variableChangeHistory.addChange(addedVariableBefore, addedVariableAfter, ChangeFactory.forVariable(Change.Type.INTRODUCED).comment(splitVariableRefactoring.toString()).refactoring(splitVariableRefactoring).codeElement(addedVariableBefore));
                            leftVariableSet.add(addedVariableBefore);
                            variableChangeHistory.connectRelatedNodes();
                            return leftVariableSet;
                        }
                    }
                    break;
                }
                case MERGE_PARAMETER:
                case MERGE_VARIABLE: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    Variable addedVariableAfter = Variable.of(mergeVariableRefactoring.getNewVariable(), mergeVariableRefactoring.getOperationAfter(), currentVersion);
                    if (equalOperator.test(addedVariableAfter)) {
                        Variable addedVariableBefore = Variable.of(mergeVariableRefactoring.getNewVariable(), mergeVariableRefactoring.getOperationAfter(), parentVersion);
                        addedVariableBefore.setAdded(true);
                        variableChangeHistory.addChange(addedVariableBefore, addedVariableAfter, ChangeFactory.forVariable(Change.Type.INTRODUCED).comment(mergeVariableRefactoring.toString()).refactoring(mergeVariableRefactoring).codeElement(addedVariableBefore));
                        leftVariableSet.add(addedVariableBefore);
                        variableChangeHistory.connectRelatedNodes();
                        return leftVariableSet;
                    }
                    break;
                }
                case ADD_PARAMETER: {
                    AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) refactoring;
                    Variable addedVariableAfter = Variable.of(addParameterRefactoring.getParameter().getVariableDeclaration(), addParameterRefactoring.getOperationAfter(), currentVersion);
                    if (equalOperator.test(addedVariableAfter)) {
                        Variable addedVariableBefore = Variable.of(addParameterRefactoring.getParameter().getVariableDeclaration(), addParameterRefactoring.getOperationAfter(), parentVersion);
                        addedVariableBefore.setAdded(true);
                        variableChangeHistory.addChange(addedVariableBefore, addedVariableAfter, ChangeFactory.forVariable(Change.Type.INTRODUCED).comment(addParameterRefactoring.toString()).refactoring(addParameterRefactoring).codeElement(addedVariableBefore));
                        leftVariableSet.add(addedVariableBefore);
                        variableChangeHistory.connectRelatedNodes();
                        return leftVariableSet;
                    }
                    break;
                }
                case EXTRACT_VARIABLE: {
                    ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                    Variable extractedVariableAfter = Variable.of(extractVariableRefactoring.getVariableDeclaration(), extractVariableRefactoring.getOperationAfter(), currentVersion);
                    if (equalOperator.test(extractedVariableAfter)) {
                        Variable extractedVariableBefore = Variable.of(extractVariableRefactoring.getVariableDeclaration(), extractVariableRefactoring.getOperationAfter(), parentVersion);
                        extractedVariableBefore.setAdded(true);
                        variableChangeHistory.addChange(extractedVariableBefore, extractedVariableAfter, ChangeFactory.forVariable(Change.Type.INTRODUCED).comment(extractVariableRefactoring.toString()).refactoring(extractVariableRefactoring).codeElement(extractedVariableAfter));
                        leftVariableSet.add(extractedVariableBefore);
                        variableChangeHistory.connectRelatedNodes();
                        return leftVariableSet;
                    }
                    break;
                }
            }
            if (changeType != null) {
                if (equalOperator.test(variableAfter)) {
                    variableChangeHistory.addChange(variableBefore, variableAfter, ChangeFactory.forVariable(changeType).refactoring(refactoring));
                    leftVariableSet.add(variableBefore);
                }
            }
        }
        variableChangeHistory.connectRelatedNodes();
        return leftVariableSet;
    }

    private boolean checkRefactoredMethod(ArrayDeque<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Variable rightVariable, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {

        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            UMLOperationBodyMapper umlOperationBodyMapper = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pullUpOperationRefactoring.getBodyMapper();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pushDownOperationRefactoring.getBodyMapper();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = moveOperationRefactoring.getBodyMapper();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    umlOperationBodyMapper = renameOperationRefactoring.getBodyMapper();
                    break;
                }
            }
            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (equalMethod.test(methodAfter)) {
                    boolean found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    public static class Builder {
        private Repository repository;
        private String startCommitId;
        private String filePath;
        private String methodName;
        private int methodDeclarationLineNumber;
        private String variableName;
        private int variableDeclarationLineNumber;

        public Builder repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Builder startCommitId(String startCommitId) {
            this.startCommitId = startCommitId;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder methodDeclarationLineNumber(int methodDeclarationLineNumber) {
            this.methodDeclarationLineNumber = methodDeclarationLineNumber;
            return this;
        }

        public Builder variableDeclarationLineNumber(int variableDeclarationLineNumber) {
            this.variableDeclarationLineNumber = variableDeclarationLineNumber;
            return this;
        }

        public Builder variableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        private void checkInput() {

        }
    }
}
