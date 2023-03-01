package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.change.Change;
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

            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));

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

                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethod.getFilePath()));
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
                            if (involvedInVariableRefactoring(refactorings, rightVariable) && containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
                                bodyMapper = null;
                            }
                        }
                        else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
                            UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
                            bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
                            refactorings = bodyMapper.getRefactorings();
                            if (involvedInVariableRefactoring(refactorings, rightVariable) && containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
                                bodyMapper = null;
                            }
                        }
                        if (checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, refactorings)) {
                            historyReport.step3PlusPlus();
                            break;
                        }
                    }
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        //Local Refactoring
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        boolean found = checkForExtractionOrInline(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }

                        found = checkRefactoredMethod(variables, currentVersion, parentVersion, equalMethod, rightVariable, refactorings);
                        if (found) {
                            historyReport.step4PlusPlus();
                            break;
                        }
                        UMLOperationBodyMapper bodyMapper = findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion);
                        Set<Refactoring> bodyMapperRefactorings = bodyMapper != null ? bodyMapper.getRefactoringsAfterPostProcessing() : Collections.emptySet();
                        found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, bodyMapperRefactorings);
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
                                                found = isMatched(operation, rightVariable, variables, parentVersion);
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
                                found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, bodyMapper, bodyMapperRefactorings);
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
                                boolean found = checkForExtractionOrInline(variables, currentVersion, parentVersion, equalMethod, rightVariable, classLevelRefactorings);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }

                                found = isVariableRefactored(classLevelRefactorings, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }

                                found = checkRefactoredMethod(variables, currentVersion, parentVersion, equalMethod, rightVariable, classLevelRefactorings);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }

                                found = checkClassDiffForVariableChange(variables, currentVersion, parentVersion, equalMethod, equalVariable, classDiff);
                                if (found) {
                                    historyReport.step5PlusPlus();
                                    break;
                                }
                            }
                            List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();
                            boolean flag = false;
                            for (Refactoring refactoring : refactorings) {
                                if (RefactoringType.MOVE_AND_RENAME_OPERATION.equals(refactoring.getRefactoringType())) {
                                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                                    Method movedOperation = Method.of(moveOperationRefactoring.getMovedOperation(), currentVersion);
                                    if (rightMethod.equalIdentifierIgnoringVersion(movedOperation)) {
                                        fileNames.add(moveOperationRefactoring.getOriginalOperation().getLocationInfo().getFilePath());
                                        umlModelPairAll = getUMLModelPair(commitModel, currentMethod.getFilePath(), fileNames::contains, false);
                                        umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
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

    private boolean checkClassDiffForVariableChange(ArrayDeque<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Variable> equalVariable, UMLClassBaseDiff umlClassDiff) throws RefactoringMinerTimedOutException {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                if (isVariableRefactored(operationBodyMapper.getRefactoringsAfterPostProcessing(), variables, currentVersion, parentVersion, equalVariable))
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


    private boolean checkForExtractionOrInline(ArrayDeque<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Variable rightVariable, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        VariableDeclaration matchedVariableFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                        for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : bodyMapper.getMatchedVariables()) {
                            Variable matchedVariableInsideExtractedMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getContainer2(), currentVersion);
                            if (matchedVariableInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                matchedVariableFromSourceMethod = matchedVariablePair.getLeft();
                                break;
                            }
                        }
                        Variable variableBefore = Variable.of(rightVariable.getVariableDeclaration(), rightVariable.getOperation(), parentVersion);
                        if (matchedVariableFromSourceMethod == null) {
                            variableChangeHistory.handleAdd(variableBefore, rightVariable, extractOperationRefactoring.toString());
                        }
                        else {
                            VariableDeclarationContainer sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            variableChangeHistory.addChange(variableBefore, rightVariable, ChangeFactory.forVariable(Change.Type.INTRODUCED)
                                    .refactoring(extractOperationRefactoring).codeElement(rightVariable).hookedElement(Variable.of(matchedVariableFromSourceMethod, sourceMethod)));
                            variableBefore.setAdded(true);
                        }
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
                            Variable matchedVariableInsideInlinedMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getContainer2(), currentVersion);
                            if (matchedVariableInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                Variable variableBefore = Variable.of(matchedVariablePair.getLeft(), bodyMapper.getContainer1(), parentVersion);
                                variableChangeHistory.handleAdd(variableBefore, matchedVariableInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                variables.add(variableBefore);
                                variableChangeHistory.connectRelatedNodes();
                                return true;
                            }
                        }
                    }
                    break;
                }
                case MERGE_OPERATION: {
                    MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                    Method methodAfter = Method.of(mergeOperationRefactoring.getNewMethodAfterMerge(), currentVersion);
                    if (equalMethod.test(methodAfter)) {
                        for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                            for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : bodyMapper.getMatchedVariables()) {
                                Variable matchedVariableInsideMergedMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedVariableInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                    if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                        return true;
                                    if (isMatched(bodyMapper, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                        return true;
                                }
                            }
                            for (VariableDeclaration addedVariable : bodyMapper.getAddedVariables()) {
                                Variable matchedVariableInsideMergedMethodBody = Variable.of(addedVariable, bodyMapper.getContainer2(), currentVersion);
                                if (matchedVariableInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                    if (isAdded(bodyMapper, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case SPLIT_OPERATION: {
                    SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                    for (VariableDeclarationContainer splitMethod : splitOperationRefactoring.getSplitMethods()) {
                        Method methodAfter = Method.of(splitMethod, currentVersion);
                        if (equalMethod.test(methodAfter)) {
                            for (UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
                                for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : bodyMapper.getMatchedVariables()) {
                                    Variable matchedVariableInsideSplitMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedVariableInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                        if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                            return true;
                                        if (isMatched(bodyMapper, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                            return true;
                                    }
                                }
                                for (VariableDeclaration addedVariable : bodyMapper.getAddedVariables()) {
                                    Variable matchedVariableInsideSplitMethodBody = Variable.of(addedVariable, bodyMapper.getContainer2(), currentVersion);
                                    if (matchedVariableInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                        if (isAdded(bodyMapper, variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean checkBodyOfMatchedOperations(Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper, Set<Refactoring> refactorings) {
        if (umlOperationBodyMapper == null)
            return false;
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
            Variable variableAfter = Variable.of(matchedVariablePair.getRight(), umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(matchedVariablePair.getLeft(), umlOperationBodyMapper.getContainer1(), parentVersion);
                variableChangeHistory.addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                variables.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private boolean isMatched(VariableDeclarationContainer leftMethod, Variable rightVariable, Queue<Variable> variables, Version parentVersion) {
        for (VariableDeclaration leftVariable : leftMethod.getAllVariableDeclarations()) {
            if (matchingVariables(leftMethod, rightVariable, variables, parentVersion, leftVariable)) return true;
        }
        for (UMLAnonymousClass anonymousClass : leftMethod.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (VariableDeclaration leftVariable : operation.getAllVariableDeclarations()) {
                    if (matchingVariables(leftMethod, rightVariable, variables, parentVersion, leftVariable)) return true;
                }
            }
        }
        for (LambdaExpressionObject lambda : leftMethod.getAllLambdas()) {
            for (VariableDeclaration leftVariable : lambda.getParameters()) {
                if (matchingVariables(leftMethod, rightVariable, variables, parentVersion, leftVariable)) return true;
            }
        }
        return false;
    }

    private boolean matchingVariables(VariableDeclarationContainer leftMethod, Variable rightVariable, Queue<Variable> variables, Version parentVersion, VariableDeclaration leftVariable) {
        if (leftVariable.equalVariableDeclarationType(rightVariable.getVariableDeclaration()) && leftVariable.getVariableName().equals(rightVariable.getVariableDeclaration().getVariableName())) {
            Set<AbstractCodeFragment> leftStatementsInScope = leftVariable.getStatementsInScopeUsingVariable();
            Set<AbstractCodeFragment> rightStatementsInScope = rightVariable.getVariableDeclaration().getStatementsInScopeUsingVariable();
            boolean identicalStatementsInScope = false;
            if (leftStatementsInScope.size() == rightStatementsInScope.size()) {
                int identicalStatementCount = 0;
                Iterator<AbstractCodeFragment> leftStatementIterator = leftStatementsInScope.iterator();
                Iterator<AbstractCodeFragment> rightStatementIterator = rightStatementsInScope.iterator();
                while (leftStatementIterator.hasNext() && rightStatementIterator.hasNext()) {
                    AbstractCodeFragment leftFragment = leftStatementIterator.next();
                    AbstractCodeFragment rightFragment = rightStatementIterator.next();
                    if (leftFragment.getString().equals(rightFragment.getString())) {
                        identicalStatementCount++;
                    }
                }
                identicalStatementsInScope = identicalStatementCount == leftStatementsInScope.size();
            }
            if (identicalStatementsInScope) {
                Variable variableBefore = Variable.of(leftVariable, leftMethod, parentVersion);
                variableChangeHistory.addChange(variableBefore, rightVariable, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                variables.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Variable> variables, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        for (VariableDeclaration addedVariable : umlOperationBodyMapper.getAddedVariables()) {
            Variable variableAfter = Variable.of(addedVariable, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(addedVariable, umlOperationBodyMapper.getContainer2(), parentVersion);
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
                    if (variableBefore.getVariableDeclaration().getVariableName().equals(variableAfter.getVariableDeclaration().getVariableName()))
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
                        Variable addedVariableAfter = Variable.of(splitVariable, splitVariableRefactoring.getOperationAfter(), currentVersion);
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
                    VariableDeclaration newVariable = mergeVariableRefactoring.getNewVariable();
                    Variable addedVariableAfter = Variable.of(newVariable, mergeVariableRefactoring.getOperationAfter(), currentVersion);
                    for (VariableDeclaration originalVariableDeclaration : mergeVariableRefactoring.getOperationBefore().getAllVariableDeclarations()) {
                        if (originalVariableDeclaration.getVariableName().equals(newVariable.getVariableName()) && originalVariableDeclaration.isParameter() == newVariable.isParameter()) {
                            if (originalVariableDeclaration.getType().equals(newVariable.getType())) {
                                variableBefore = Variable.of(originalVariableDeclaration, mergeVariableRefactoring.getOperationBefore(), parentVersion);
                                variableAfter = addedVariableAfter;
                                changeType = Change.Type.NO_CHANGE;
                                break;
                            }
                            else {
                                boolean sameNameAsNewVariableInMergedVariables = false;
                                for (VariableDeclaration mergedVariable : mergeVariableRefactoring.getMergedVariables()) {
                                    if (mergedVariable.getVariableName().equals(newVariable.getVariableName())) {
                                        sameNameAsNewVariableInMergedVariables = true;
                                        break;
                                    }
                                }
                                if (sameNameAsNewVariableInMergedVariables) {
                                    variableBefore = Variable.of(originalVariableDeclaration, mergeVariableRefactoring.getOperationBefore(), parentVersion);
                                    variableAfter = addedVariableAfter;
                                    changeType = Change.Type.TYPE_CHANGE;
                                    break;
                                }
                            }
                        }
                    }
                    if (equalOperator.test(addedVariableAfter) && variableBefore == null) {
                        Variable addedVariableBefore = Variable.of(newVariable, mergeVariableRefactoring.getOperationAfter(), parentVersion);
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
            UMLOperation operationAfter = null;
            UMLOperationBodyMapper umlOperationBodyMapper = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pullUpOperationRefactoring.getBodyMapper();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pushDownOperationRefactoring.getBodyMapper();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = moveOperationRefactoring.getBodyMapper();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    umlOperationBodyMapper = renameOperationRefactoring.getBodyMapper();
                    break;
                }
            }
            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (equalMethod.test(methodAfter)) {
                    Set<Refactoring> bodyMapperRefactorings = umlOperationBodyMapper != null ? umlOperationBodyMapper.getRefactoringsAfterPostProcessing() : Collections.emptySet();
                    boolean found = checkBodyOfMatchedOperations(variables, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, umlOperationBodyMapper, bodyMapperRefactorings);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    private static boolean involvedInVariableRefactoring(Collection<Refactoring> refactorings, Variable rightVariable) {
        for (Refactoring refactoring : refactorings) {
            VariableDeclaration variableAfter = null;
            switch (refactoring.getRefactoringType()) {
                case RENAME_VARIABLE:
                case RENAME_PARAMETER:
                case PARAMETERIZE_VARIABLE:
                case LOCALIZE_PARAMETER: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    variableAfter = renameVariableRefactoring.getRenamedVariable();
                    break;
                }
                case CHANGE_VARIABLE_TYPE:
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    variableAfter = changeVariableTypeRefactoring.getChangedTypeVariable();
                    break;
                }
                case ADD_VARIABLE_MODIFIER:
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    variableAfter = addVariableModifierRefactoring.getVariableAfter();
                    break;
                }
                case REMOVE_VARIABLE_MODIFIER:
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    variableAfter = removeVariableModifierRefactoring.getVariableAfter();
                    break;
                }
                case ADD_VARIABLE_ANNOTATION:
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    variableAfter = addVariableAnnotationRefactoring.getVariableAfter();
                    break;
                }
                case MODIFY_VARIABLE_ANNOTATION:
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    variableAfter = modifyVariableAnnotationRefactoring.getVariableAfter();
                    break;
                }
                case REMOVE_VARIABLE_ANNOTATION:
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    variableAfter = removeVariableAnnotationRefactoring.getVariableAfter();
                    break;
                }
                case SPLIT_PARAMETER:
                case SPLIT_VARIABLE: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    for (VariableDeclaration splitVariable : splitVariableRefactoring.getSplitVariables()) {
                        if (splitVariable.equals(rightVariable.getVariableDeclaration())) {
                            return true;
                        }
                    }
                    break;
                }
                case MERGE_PARAMETER:
                case MERGE_VARIABLE: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    variableAfter = mergeVariableRefactoring.getNewVariable();
                    break;
                }
                case ADD_PARAMETER: {
                    AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) refactoring;
                    variableAfter = addParameterRefactoring.getParameter().getVariableDeclaration();
                    break;
                }
                case EXTRACT_VARIABLE: {
                    ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) refactoring;
                    variableAfter = extractVariableRefactoring.getVariableDeclaration();
                    break;
                }
            }
            if (rightVariable.getVariableDeclaration().equals(variableAfter)) {
                return true;
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
