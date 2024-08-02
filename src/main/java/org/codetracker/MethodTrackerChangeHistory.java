package org.codetracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.codetracker.api.History;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.MethodAnnotationChange;
import org.codetracker.change.method.MethodSignatureChange;
import org.codetracker.element.Method;
import org.codetracker.util.Util;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AddMethodAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddMethodModifierRefactoring;
import gr.uom.java.xmi.diff.AddParameterRefactoring;
import gr.uom.java.xmi.diff.AddThrownExceptionTypeRefactoring;
import gr.uom.java.xmi.diff.AddVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddVariableModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeOperationAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeThrownExceptionTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring;
import gr.uom.java.xmi.diff.ModifyVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.MovedClassToAnotherSourceFolder;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RemoveMethodAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveMethodModifierRefactoring;
import gr.uom.java.xmi.diff.RemoveParameterRefactoring;
import gr.uom.java.xmi.diff.RemoveThrownExceptionTypeRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableModifierRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.ReorderParameterRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLClassRenameDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class MethodTrackerChangeHistory extends AbstractChangeHistory<Method> {
	private final ChangeHistory<Method> methodChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;

	public MethodTrackerChangeHistory(String methodName, int methodDeclarationLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
	}

	public ChangeHistory<Method> get() {
		return methodChangeHistory;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getMethodDeclarationLineNumber() {
		return methodDeclarationLineNumber;
	}

	boolean isStartMethod(Method method) {
	    return method.getUmlOperation().getName().equals(methodName) &&
	            method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
	            method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
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
                    UMLOperationBodyMapper mapper = extractOperationRefactoring.getBodyMapper();
                    Set<UMLAnonymousClassDiff> anonymousClassDiffs = mapper.getAnonymousClassDiffs();
                    for (UMLAnonymousClassDiff diff : anonymousClassDiffs) {
                        for (UMLOperationBodyMapper anonymousMapper : diff.getOperationBodyMapperList()) {
                            Method anonymousExtractedOperationAfter = Method.of(anonymousMapper.getContainer2(), currentVersion);
                            if (equalOperator.test(anonymousExtractedOperationAfter)) {
                            	Method anonymousExtractedOperationBefore = Method.of(anonymousMapper.getContainer1(), parentVersion);
                            	boolean bodyChange = false;
                            	if (checkOperationBodyChanged(anonymousExtractedOperationBefore.getUmlOperation().getBody(), anonymousExtractedOperationAfter.getUmlOperation().getBody())) {
                                    methodChangeHistory.addChange(anonymousExtractedOperationBefore, anonymousExtractedOperationAfter, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                                    bodyChange = true;
                                }
                            	boolean docChange = false;
                                if (checkOperationDocumentationChanged(anonymousExtractedOperationBefore.getUmlOperation(), anonymousExtractedOperationAfter.getUmlOperation())) {
                                    methodChangeHistory.addChange(anonymousExtractedOperationBefore, anonymousExtractedOperationAfter, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                                    docChange = true;
                                }
                                if (!docChange && !bodyChange) {
                                	methodChangeHistory.addChange(anonymousExtractedOperationBefore, anonymousExtractedOperationAfter, ChangeFactory.forMethod(Change.Type.NO_CHANGE));
                                }
                            	methodChangeHistory.connectRelatedNodes();
                            	leftMethodSet.add(anonymousExtractedOperationBefore);
                                return leftMethodSet;
                            }
                        }
                    }
                    break;
                }
            }

            addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, refactoring, operationBefore, operationAfter, changeType);
        }
        methodChangeHistory.connectRelatedNodes();
        return leftMethodSet;
    }

    private static boolean checkOperationBodyChanged(OperationBody body1, OperationBody body2) {
        if (body1 == null && body2 == null) return false;

        if (body1 == null || body2 == null) {
            return true;
        }
        return body1.getBodyHashCode() != body2.getBodyHashCode();
    }

    private static boolean checkOperationDocumentationChanged(VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
        String comments1 = Util.getSHA512(operation1.getComments().stream().map(UMLComment::getFullText).collect(Collectors.joining(";")));
        String comments2 = Util.getSHA512(operation2.getComments().stream().map(UMLComment::getFullText).collect(Collectors.joining(";")));
        return !comments1.equals(comments2);
    }

    private boolean addMethodChange(Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, Set<Method> leftMethodSet, Refactoring refactoring, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter, Change.Type changeType) {
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

    public Set<Method> isMethodContainerChanged(UMLModelDiff umlModelDiffAll, Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, List<UMLClassMoveDiff> classMovedDiffList) {
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
                    List<UMLOperation> leftOperations = new ArrayList<UMLOperation>();
                    leftOperations.addAll(originalClass.getOperations());
                    for (UMLAnonymousClass anonymous : originalClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		leftOperations.add(operation);
                    	}
                    }
                    List<UMLOperation> rightOperations = new ArrayList<UMLOperation>();
                    rightOperations.addAll(renamedClass.getOperations());
                    for (UMLAnonymousClass anonymous : renamedClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		rightOperations.add(operation);
                    	}
                    }

                    found = isMethodMatched(leftOperations, rightOperations, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                    UMLClass movedClass = moveClassRefactoring.getMovedClass();
                    List<UMLOperation> leftOperations = new ArrayList<UMLOperation>();
                    leftOperations.addAll(originalClass.getOperations());
                    for (UMLAnonymousClass anonymous : originalClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		leftOperations.add(operation);
                    	}
                    }
                    List<UMLOperation> rightOperations = new ArrayList<UMLOperation>();
                    rightOperations.addAll(movedClass.getOperations());
                    for (UMLAnonymousClass anonymous : movedClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		rightOperations.add(operation);
                    	}
                    }

                    found = isMethodMatched(leftOperations, rightOperations, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                    UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                    List<UMLOperation> leftOperations = new ArrayList<UMLOperation>();
                    leftOperations.addAll(originalClass.getOperations());
                    for (UMLAnonymousClass anonymous : originalClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		leftOperations.add(operation);
                    	}
                    }
                    List<UMLOperation> rightOperations = new ArrayList<UMLOperation>();
                    rightOperations.addAll(renamedClass.getOperations());
                    for (UMLAnonymousClass anonymous : renamedClass.getAnonymousClassList()) {
                    	for (UMLOperation operation : anonymous.getOperations()) {
                    		rightOperations.add(operation);
                    	}
                    }

                    found = isMethodMatched(leftOperations, rightOperations, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
                    break;
                }
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                        UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                        List<UMLOperation> leftOperations = new ArrayList<UMLOperation>();
                        leftOperations.addAll(originalClass.getOperations());
                        for (UMLAnonymousClass anonymous : originalClass.getAnonymousClassList()) {
                        	for (UMLOperation operation : anonymous.getOperations()) {
                        		leftOperations.add(operation);
                        	}
                        }
                        List<UMLOperation> rightOperations = new ArrayList<UMLOperation>();
                        rightOperations.addAll(movedClass.getOperations());
                        for (UMLAnonymousClass anonymous : movedClass.getAnonymousClassList()) {
                        	for (UMLOperation operation : anonymous.getOperations()) {
                        		rightOperations.add(operation);
                        	}
                        }

                        found = isMethodMatched(leftOperations, rightOperations, leftMethodSet, currentVersion, parentVersion, equalOperator, refactoring, changeType);
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
            for (UMLClassMoveDiff classMoveDiff : classMovedDiffList) {
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

    public boolean isMethodAdded(UMLModelDiff modelDiff, String className, Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, List<UMLClassBaseDiff> allClassesDiff) {
        List<UMLOperation> addedOperations = allClassesDiff
                .stream()
                .map(UMLClassBaseDiff::getAddedOperations)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLOperation operation : addedOperations) {
            if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "new method"))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new class"))
                    return true;
            }
        }

        for (UMLClassRenameDiff classRenameDiff : modelDiff.getClassRenameDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classRenameDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
            for (UMLAnonymousClass addedAnonymousClass : classRenameDiff.getNextClass().getAnonymousClassList()) {
            	for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassMoveDiff classMoveDiff : modelDiff.getClassMoveDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classMoveDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
            for (UMLAnonymousClass addedAnonymousClass : classMoveDiff.getNextClass().getAnonymousClassList()) {
            	for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassDiff classDiff : modelDiff.getCommonClassDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classDiff.getAddedAnonymousClasses()) {
                for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
            for (UMLAnonymousClass addedAnonymousClass : classDiff.getNextClass().getAnonymousClassList()) {
            	for (UMLOperation operation : addedAnonymousClass.getOperations()) {
                    if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean handleAddOperation( Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, UMLOperation operation, String comment) {
        Method rightMethod = Method.of(operation, currentVersion);
        if (equalOperator.test(rightMethod)) {
            Method leftMethod = Method.of(operation, parentVersion);
            methodChangeHistory.handleAdd(leftMethod, rightMethod, comment);
            methodChangeHistory.connectRelatedNodes();
            elements.addFirst(leftMethod);
            return true;
        }
        return false;
    }

	public HistoryInfo<Method> blameReturn(Method startMethod) {
		List<HistoryInfo<Method>> history = getHistory();
		for (History.HistoryInfo<Method> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (startMethod.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if ((change instanceof MethodSignatureChange && !(change instanceof MethodAnnotationChange)) || change instanceof Introduced) {
						return historyInfo;
					}
				}
			}
		}
		return null;
	}
}
