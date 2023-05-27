package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Method;
import org.codetracker.util.Util;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MethodTrackerImpl extends BaseTracker implements MethodTracker {
    private final ChangeHistory<Method> methodChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;

    public MethodTrackerImpl(Repository repository, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
    }

    public static boolean checkOperationBodyChanged(OperationBody body1, OperationBody body2) {
        if (body1 == null && body2 == null) return false;

        if (body1 == null || body2 == null) {
            return true;
        }
        return body1.getBodyHashCode() != body2.getBodyHashCode();
    }

    public static boolean checkOperationDocumentationChanged(VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
        String comments1 = Util.getSHA512(operation1.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        String comments2 = Util.getSHA512(operation2.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        return !comments1.equals(comments2);
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    @Override
    public History<Method> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method start = getMethod(umlModel, startVersion, this::isStartMethod);
            if (start == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }
            methodChangeHistory.addNode(start);

            ArrayDeque<Method> methods = new ArrayDeque<>();
            methods.addFirst(start);
            HashSet<String> analysedCommits = new HashSet<>();
            List<String> commits = null;
            String lastFileName = null;
            while (!methods.isEmpty()) {
                Method currentMethod = methods.poll();
                if (currentMethod.isAdded() || currentMethod.getVersion().getId().equals("0")) {
                    commits = null;
                    continue;
                }
                final String currentMethodFilePath = currentMethod.getFilePath();
                if (commits == null || !currentMethodFilePath.equals(lastFileName)) {
                    lastFileName = currentMethodFilePath;
                    commits = getCommits(repository, currentMethod.getVersion().getId(), lastFileName, git);
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


                    UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentMethodFilePath));
                    Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
                    if (rightMethod == null) {
                        continue;
                    }
                    historyReport.analysedCommitsPlusPlus();
                    if ("0".equals(parentCommitId)) {
                        Method leftMethod = Method.of(rightMethod.getUmlOperation(), parentVersion);
                        methodChangeHistory.handleAdd(leftMethod, rightMethod, "Initial commit!");
                        methodChangeHistory.connectRelatedNodes();
                        methods.add(leftMethod);
                        break;
                    }
                    UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(currentMethodFilePath));

                    //NO CHANGE
                    Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                    if (leftMethod != null) {
                        historyReport.step2PlusPlus();
                        continue;
                    }

                    //CHANGE BODY OR DOCUMENT
                    leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);

                    if (leftMethod != null) {
                        if (!leftMethod.equalBody(rightMethod))
                            methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                        if (!leftMethod.equalDocuments(rightMethod))
                            methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                        methodChangeHistory.connectRelatedNodes();
                        currentMethod = leftMethod;
                        historyReport.step3PlusPlus();
                        continue;
                    }

                    //Local Refactoring
                    UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
                    {
                        List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
                        Set<Method> leftSideMethods = analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                        boolean refactored = !leftSideMethods.isEmpty();
                        if (refactored) {
                            leftSideMethods.forEach(methods::addFirst);
                            historyReport.step4PlusPlus();
                            break;
                        }
                    }
                    //All refactorings
                    {
                        CommitModel commitModel = getCommitModel(commitId);
                        if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
                            Set<Method> methodContainerChanged = null;
                            boolean containerChanged = false;
                            boolean found = false;
                            for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : commitModel.moveSourceFolderRefactorings) {
                                if (found)
                                    break;
                                for (Map.Entry<String, String> identicalPath : moveSourceFolderRefactoring.getIdenticalFilePaths().entrySet()) {
                                    if (identicalPath.getValue().equals(currentMethodFilePath)) {
                                        String leftSideFileName = identicalPath.getKey();

                                        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesBefore);
                                        UMLClass originalClass = null;
                                        for(UMLClass leftSideClass : leftSideUMLModel.getClassList()){
                                            if(leftSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                originalClass = leftSideClass;
                                                break;
                                            }
                                        }
                                        UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(currentMethodFilePath)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
                                        UMLClass movedClass = null;
                                        for(UMLClass rightSideClass : rightSideUMLModel.getClassList()){
                                            if(rightSideClass.getName().equals(currentMethod.getUmlOperation().getClassName())){
                                                movedClass = rightSideClass;
                                                break;
                                            }
                                        }
                                        moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder().add(new MovedClassToAnotherSourceFolder(originalClass, movedClass, identicalPath.getKey(), identicalPath.getValue()));
                                        methodContainerChanged = isMethodContainerChanged(null, Collections.singletonList(moveSourceFolderRefactoring), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                                        containerChanged = !methodContainerChanged.isEmpty();
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (containerChanged) {
                                methodContainerChanged.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                        {
                            Set<String> fileNames = getRightSideFileNames(currentMethod, commitModel, umlModelDiffLocal);
                            Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                            UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

                            Set<Refactoring> moveRenameClassRefactorings = umlModelDiffAll.getMoveRenameClassRefactorings();
                            Set<Method> methodContainerChanged = isMethodContainerChanged(umlModelDiffAll, moveRenameClassRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            if (!methodContainerChanged.isEmpty()) {
                                UMLClassBaseDiff classDiff = umlModelDiffAll.getUMLClassDiff(rightMethod.getUmlOperation().getClassName());
                                if (classDiff != null) {
                                    List<Refactoring> classLevelRefactorings = classDiff.getRefactorings();
                                    analyseMethodRefactorings(classLevelRefactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                                }
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
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
                                umlModelPairAll = getUMLModelPair(commitModel, currentMethodFilePath, fileNames::contains, false);
                                umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());
                                refactorings = umlModelDiffAll.getRefactorings();
                            }

                            methodContainerChanged = isMethodContainerChanged(umlModelDiffAll, refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            boolean containerChanged = !methodContainerChanged.isEmpty();

                            Set<Method> methodRefactored = analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
                            boolean refactored = !methodRefactored.isEmpty();

                            if (containerChanged || refactored) {
                                Set<Method> leftMethods = new HashSet<>();
                                leftMethods.addAll(methodContainerChanged);
                                leftMethods.addAll(methodRefactored);
                                leftMethods.forEach(methods::addFirst);
                                historyReport.step5PlusPlus();
                                break;
                            }

                            if (isMethodAdded(umlModelDiffAll, methods, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion)) {
                                historyReport.step5PlusPlus();
                                break;
                            }
                        }
                    }
                }
            }
            return new HistoryImpl<>(methodChangeHistory.findSubGraph(start), historyReport);
        }
    }

    public Set<Method> analyseMethodRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        Set<Method> leftMethodSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            VariableDeclarationContainer operationBefore = null;
            VariableDeclarationContainer operationAfter = null;
            Change.Type changeType = null;

            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case MOVE_AND_RENAME_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.MOVED;
                    addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, operationBefore, operationAfter, Change.Type.RENAME);
                    break;
                }
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case ADD_METHOD_ANNOTATION: {
                    AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) refactoring;
                    operationBefore = addMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = addMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_METHOD_ANNOTATION: {
                    ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
                    operationBefore = modifyMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_METHOD_ANNOTATION: {
                    RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) refactoring;
                    operationBefore = removeMethodAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeMethodAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case CHANGE_RETURN_TYPE: {
                    ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) refactoring;
                    operationBefore = changeReturnTypeRefactoring.getOperationBefore();
                    operationAfter = changeReturnTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.RETURN_TYPE_CHANGE;
                    break;
                }
                case SPLIT_PARAMETER: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) refactoring;
                    operationBefore = splitVariableRefactoring.getOperationBefore();
                    operationAfter = splitVariableRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case MERGE_PARAMETER: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) refactoring;
                    operationBefore = mergeVariableRefactoring.getOperationBefore();
                    operationAfter = mergeVariableRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case RENAME_PARAMETER:
                case PARAMETERIZE_ATTRIBUTE:
                case PARAMETERIZE_VARIABLE:
                case LOCALIZE_PARAMETER: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) refactoring;
                    if (!renameVariableRefactoring.isInsideExtractedOrInlinedMethod()) {
                        operationBefore = renameVariableRefactoring.getOperationBefore();
                        operationAfter = renameVariableRefactoring.getOperationAfter();
                        changeType = Change.Type.PARAMETER_CHANGE;
                    }
                    break;
                }
                case CHANGE_PARAMETER_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) refactoring;
                    if (!changeVariableTypeRefactoring.isInsideExtractedOrInlinedMethod()) {
                        operationBefore = changeVariableTypeRefactoring.getOperationBefore();
                        operationAfter = changeVariableTypeRefactoring.getOperationAfter();
                        changeType = Change.Type.PARAMETER_CHANGE;
                    }
                    break;
                }
                case ADD_PARAMETER: {
                    AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) refactoring;
                    operationBefore = addParameterRefactoring.getOperationBefore();
                    operationAfter = addParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER: {
                    RemoveParameterRefactoring removeParameterRefactoring = (RemoveParameterRefactoring) refactoring;
                    operationBefore = removeParameterRefactoring.getOperationBefore();
                    operationAfter = removeParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REORDER_PARAMETER: {
                    ReorderParameterRefactoring reorderParameterRefactoring = (ReorderParameterRefactoring) refactoring;
                    operationBefore = reorderParameterRefactoring.getOperationBefore();
                    operationAfter = reorderParameterRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_PARAMETER_MODIFIER: {
                    AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) refactoring;
                    operationBefore = addVariableModifierRefactoring.getOperationBefore();
                    operationAfter = addVariableModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER_MODIFIER: {
                    RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) refactoring;
                    operationBefore = removeVariableModifierRefactoring.getOperationBefore();
                    operationAfter = removeVariableModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_PARAMETER_ANNOTATION: {
                    AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) refactoring;
                    operationBefore = addVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = addVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case REMOVE_PARAMETER_ANNOTATION: {
                    RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) refactoring;
                    operationBefore = removeVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = removeVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case MODIFY_PARAMETER_ANNOTATION: {
                    ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) refactoring;
                    operationBefore = modifyVariableAnnotationRefactoring.getOperationBefore();
                    operationAfter = modifyVariableAnnotationRefactoring.getOperationAfter();
                    changeType = Change.Type.PARAMETER_CHANGE;
                    break;
                }
                case ADD_THROWN_EXCEPTION_TYPE: {
                    AddThrownExceptionTypeRefactoring addThrownExceptionTypeRefactoring = (AddThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = addThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = addThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case CHANGE_THROWN_EXCEPTION_TYPE: {
                    ChangeThrownExceptionTypeRefactoring changeThrownExceptionTypeRefactoring = (ChangeThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = changeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = changeThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case REMOVE_THROWN_EXCEPTION_TYPE: {
                    RemoveThrownExceptionTypeRefactoring removeThrownExceptionTypeRefactoring = (RemoveThrownExceptionTypeRefactoring) refactoring;
                    operationBefore = removeThrownExceptionTypeRefactoring.getOperationBefore();
                    operationAfter = removeThrownExceptionTypeRefactoring.getOperationAfter();
                    changeType = Change.Type.EXCEPTION_CHANGE;
                    break;
                }
                case CHANGE_OPERATION_ACCESS_MODIFIER: {
                    ChangeOperationAccessModifierRefactoring changeOperationAccessModifierRefactoring = (ChangeOperationAccessModifierRefactoring) refactoring;
                    operationBefore = changeOperationAccessModifierRefactoring.getOperationBefore();
                    operationAfter = changeOperationAccessModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case ADD_METHOD_MODIFIER: {
                    AddMethodModifierRefactoring addMethodModifierRefactoring = (AddMethodModifierRefactoring) refactoring;
                    operationBefore = addMethodModifierRefactoring.getOperationBefore();
                    operationAfter = addMethodModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_METHOD_MODIFIER: {
                    RemoveMethodModifierRefactoring removeMethodModifierRefactoring = (RemoveMethodModifierRefactoring) refactoring;
                    operationBefore = removeMethodModifierRefactoring.getOperationBefore();
                    operationAfter = removeMethodModifierRefactoring.getOperationAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    operationBefore = inlineOperationRefactoring.getTargetOperationBeforeInline();
                    operationAfter = inlineOperationRefactoring.getTargetOperationAfterInline();
                    changeType = Change.Type.BODY_CHANGE;
                    break;
                }
                case SPLIT_OPERATION: {
                    SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                    operationBefore = splitOperationRefactoring.getOriginalMethodBeforeSplit();
                    Method originalOperationBefore = Method.of(operationBefore, parentVersion);
                    for (VariableDeclarationContainer container : splitOperationRefactoring.getSplitMethods()) {
                        Method splitOperationAfter = Method.of(container, currentVersion);
                        if (equalOperator.test(splitOperationAfter)) {
                            leftMethodSet.add(originalOperationBefore);
                            changeType = Change.Type.METHOD_SPLIT;
                            methodChangeHistory.addChange(originalOperationBefore, splitOperationAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                            methodChangeHistory.connectRelatedNodes();
                            return leftMethodSet;
                        }
                    }
                    break;
                }
                case MERGE_OPERATION: {
                    MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                    operationAfter = mergeOperationRefactoring.getNewMethodAfterMerge();
                    Method newOperationAfter = Method.of(operationAfter, currentVersion);
                    if (equalOperator.test(newOperationAfter)) {
                        for (VariableDeclarationContainer container : mergeOperationRefactoring.getMergedMethods()) {
                            Method mergedOperationBefore = Method.of(container, parentVersion);
                            leftMethodSet.add(mergedOperationBefore);
                            changeType = Change.Type.METHOD_MERGE;
                            methodChangeHistory.addChange(mergedOperationBefore, newOperationAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                        }
                        methodChangeHistory.connectRelatedNodes();
                        return leftMethodSet;
                    }
                    break;
                }
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    operationBefore = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                    if (extractOperationRefactoring.getBodyMapper().isNested()) {
                        UMLOperationBodyMapper parentMapper = extractOperationRefactoring.getBodyMapper().getParentMapper();
                        while (parentMapper.getParentMapper() != null) {
                            parentMapper = parentMapper.getParentMapper();
                        }
                        operationAfter = parentMapper.getContainer2();
                    }
                    else {
                        operationAfter = extractOperationRefactoring.getSourceOperationAfterExtraction();
                    }
                    changeType = Change.Type.BODY_CHANGE;

                    UMLOperation extractedOperation = extractOperationRefactoring.getExtractedOperation();
                    Method extractedOperationAfter = Method.of(extractedOperation, currentVersion);
                    if (equalOperator.test(extractedOperationAfter)) {
                        Method extractedOperationBefore = Method.of(extractedOperation, parentVersion);
                        extractedOperationBefore.setAdded(true);
                        methodChangeHistory.addChange(extractedOperationBefore, extractedOperationAfter, ChangeFactory.forMethod(Change.Type.INTRODUCED)
                                .refactoring(extractOperationRefactoring).codeElement(extractedOperationAfter).hookedElement(Method.of(operationBefore, parentVersion)));
                        methodChangeHistory.connectRelatedNodes();
                        leftMethodSet.add(extractedOperationBefore);
                        return leftMethodSet;
                    }
                    break;
                }
            }

            addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, operationBefore, operationAfter, changeType);
        }
        methodChangeHistory.connectRelatedNodes();
        return leftMethodSet;
    }

    public boolean addMethodChange(Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, Set<Method> leftMethodSet, Refactoring refactoring, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Change.Type changeType) {
        if (operationAfter != null) {
            Method methodAfter = Method.of(operationAfter, currentVersion);
            if (equalOperator.test(methodAfter)) {
                Method methodBefore = Method.of(operationBefore, parentVersion);
                methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                if (checkOperationBodyChanged(methodBefore.getUmlOperation().getBody(), methodAfter.getUmlOperation().getBody())) {
                    methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                }
                if (checkOperationDocumentationChanged(methodBefore.getUmlOperation(), methodAfter.getUmlOperation())) {
                    methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                }
                leftMethodSet.add(methodBefore);
                return true;
            }
        }
        return false;
    }

    private boolean isMethodAdded(UMLModelDiff modelDiff, ArrayDeque<Method> methods, String className, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        List<UMLOperation> addedOperations = getAllClassesDiff(modelDiff)
                .stream()
                .map(UMLClassBaseDiff::getAddedOperations)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLOperation operation : addedOperations) {
            if (handleAddOperation(methods, currentVersion, parentVersion, equalOperator, operation, "new method"))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (handleAddOperation(methods, currentVersion, parentVersion, equalOperator, operation, "added with new class"))
                    return true;
            }
        }

        for (UMLClassRenameDiff classRenameDiff : modelDiff.getClassRenameDiffList()) {
            for (UMLAnonymousClass addedAnonymousClasses : classRenameDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClasses.getOperations()) {
                    if (handleAddOperation(methods, currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassMoveDiff classMoveDiff : modelDiff.getClassMoveDiffList()) {
            for (UMLAnonymousClass addedAnonymousClasses : classMoveDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClasses.getOperations()) {
                    if (handleAddOperation(methods, currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassDiff classDiff : modelDiff.getCommonClassDiffList()) {
            for (UMLAnonymousClass addedAnonymousClasses : classDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClasses.getOperations()) {
                    if (handleAddOperation(methods, currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean handleAddOperation(ArrayDeque<Method> methods, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, UMLOperation operation, String comment) {
        Method rightMethod = Method.of(operation, currentVersion);
        if (equalOperator.test(rightMethod)) {
            Method leftMethod = Method.of(operation, parentVersion);
            methodChangeHistory.handleAdd(leftMethod, rightMethod, comment);
            methodChangeHistory.connectRelatedNodes();
            methods.addFirst(leftMethod);
            return true;
        }
        return false;
    }

    private Set<Method> isMethodContainerChanged(UMLModelDiff umlModelDiffAll, Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator) {
        Set<Method> leftMethodSet = new HashSet<>();
        boolean found = false;
        Change.Type changeType = Change.Type.CONTAINER_CHANGE;
        for (Refactoring refactoring : refactorings) {
            if (found)
                break;
            switch (refactoring.getRefactoringType()) {
                case RENAME_CLASS: {
                    RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                    UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                    UMLClass renamedClass = renameClassRefactoring.getRenamedClass();

                    found = isMethodMatched(originalClass.getOperations(), renamedClass.getOperations(), leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                    UMLClass movedClass = moveClassRefactoring.getMovedClass();

                    found = isMethodMatched(originalClass.getOperations(), movedClass.getOperations(), leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                    UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();

                    found = isMethodMatched(originalClass.getOperations(), renamedClass.getOperations(), leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                        UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                        found = isMethodMatched(originalClass.getOperations(), movedClass.getOperations(), leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                        if (found)
                            break;
                    }
                    break;
                }
            }
        }
        if (umlModelDiffAll != null) {
            for (UMLClassRenameDiff classRenameDiffList : umlModelDiffAll.getClassRenameDiffList()) {
                if (found)
                    break;
                for (UMLOperationBodyMapper umlOperationBodyMapper : classRenameDiffList.getOperationBodyMapperList()) {
                    found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, new RenameClassRefactoring(classRenameDiffList.getOriginalClass(), classRenameDiffList.getRenamedClass()), umlOperationBodyMapper.getContainer1(), umlOperationBodyMapper.getContainer2(), changeType);
                    if (found)
                        break;
                }
            }
            for (UMLClassMoveDiff classMoveDiff : getClassMoveDiffList(umlModelDiffAll)) {
                if (found)
                    break;
                for (UMLOperationBodyMapper umlOperationBodyMapper : classMoveDiff.getOperationBodyMapperList()) {
                    found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, new MoveClassRefactoring(classMoveDiff.getOriginalClass(), classMoveDiff.getMovedClass()), umlOperationBodyMapper.getContainer1(), umlOperationBodyMapper.getContainer2(), changeType);
                    if (found)
                        break;
                }
            }
        }
        if (found) {
            methodChangeHistory.connectRelatedNodes();
            return leftMethodSet;
        }
        return Collections.emptySet();
    }


    private boolean isMethodMatched(List<UMLOperation> leftSide, List<UMLOperation> rightSide, Set<Method> leftMethodSet, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, Refactoring refactoring, Change.Type changeType) {
        Set<UMLOperation> leftMatched = new HashSet<>();
        Set<UMLOperation> rightMatched = new HashSet<>();
        for (UMLOperation leftOperation : leftSide) {
            if (leftMatched.contains(leftOperation))
                continue;
            for (UMLOperation rightOperation : rightSide) {
                if (rightMatched.contains(rightOperation))
                    continue;
                if (leftOperation.equalSignature(rightOperation)) {
                    if (addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, leftOperation, rightOperation, changeType))
                        return true;
                    leftMatched.add(leftOperation);
                    rightMatched.add(rightOperation);
                    break;
                }
            }
        }
        return false;
    }
}
