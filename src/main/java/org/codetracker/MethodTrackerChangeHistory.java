package org.codetracker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLInitializer;
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
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
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
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLClassRenameDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class MethodTrackerChangeHistory extends AbstractChangeHistory<Method> {
	private final ChangeHistory<Method> methodChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private Method sourceOperation;

	public MethodTrackerChangeHistory(String methodName, int methodDeclarationLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
	}

	public Method getSourceOperation() {
		return sourceOperation;
	}

	public void setSourceOperation(Method sourceOperation) {
		this.sourceOperation = sourceOperation;
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
                    UMLOperationBodyMapper mapperWithLessMappings = null;
                    boolean debatable = false;
                    for (UMLOperationBodyMapper mapper : splitOperationRefactoring.getMappers()) {
                    	if (mapperWithLessMappings == null) {
                    		mapperWithLessMappings = mapper;
                    	}
                    	else if (looserMapper(mapperWithLessMappings, mapper)) {
                    		debatable = debatable(mapperWithLessMappings, mapper);
                    		mapperWithLessMappings = mapper;
                    	}
                    }
                    operationBefore = splitOperationRefactoring.getOriginalMethodBeforeSplit();
                    Method originalOperationBefore = Method.of(operationBefore, parentVersion);
                    for (VariableDeclarationContainer container : splitOperationRefactoring.getSplitMethods()) {
                        Method splitOperationAfter = Method.of(container, currentVersion);
                        if (equalOperator.test(splitOperationAfter)) {
                            leftMethodSet.add(originalOperationBefore);
                            changeType = Change.Type.METHOD_SPLIT;
                            methodChangeHistory.addChange(originalOperationBefore, splitOperationAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                            methodChangeHistory.connectRelatedNodes();
                            
                            if (!debatable && mapperWithLessMappings.getContainer2().equals(container)) {
	                            Method splitOperationBefore = Method.of(container, parentVersion);
	                            splitOperationBefore.setAdded(true);
	                            methodChangeHistory.addChange(splitOperationBefore, splitOperationAfter, ChangeFactory.forMethod(Change.Type.INTRODUCED)
	                                    .refactoring(splitOperationRefactoring).codeElement(splitOperationAfter).hookedElement(Method.of(operationBefore, parentVersion)));
	                            methodChangeHistory.connectRelatedNodes();
	                            Method sourceOperationBefore = Method.of(operationBefore, parentVersion);
	                            setSourceOperation(sourceOperationBefore);
                            }
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
                        Method sourceOperationBefore = Method.of(operationBefore, parentVersion);
                        setSourceOperation(sourceOperationBefore);
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
                case MOVE_CODE: {
                	MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                	operationBefore = moveCodeRefactoring.getSourceContainer();
                	VariableDeclarationContainer extractedOperation = moveCodeRefactoring.getTargetContainer();
                	Method extractedOperationAfter = Method.of(extractedOperation, currentVersion);
                    if (equalOperator.test(extractedOperationAfter) && moveCodeRefactoring.getMoveType().equals(MoveCodeRefactoring.Type.MOVE_TO_ADDED)) {
                    	Method extractedOperationBefore = Method.of(extractedOperation, parentVersion);
                        extractedOperationBefore.setAdded(true);
                        methodChangeHistory.addChange(extractedOperationBefore, extractedOperationAfter, ChangeFactory.forMethod(Change.Type.INTRODUCED)
                                .refactoring(moveCodeRefactoring).codeElement(extractedOperationAfter).hookedElement(Method.of(operationBefore, parentVersion)));
                        methodChangeHistory.connectRelatedNodes();
                        leftMethodSet.add(extractedOperationBefore);
                        Method sourceOperationBefore = Method.of(operationBefore, parentVersion);
                        setSourceOperation(sourceOperationBefore);
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

    private boolean debatable(UMLOperationBodyMapper previous, UMLOperationBodyMapper current) {
    	Set<String> previousIntersection = parameterIntersection(previous);
		Set<String> currentIntersection = parameterIntersection(current);
		return current.mappingsWithoutBlocks() < previous.mappingsWithoutBlocks() &&
				currentIntersection.size() < previousIntersection.size() &&
				current.getContainer1().getName().equals(current.getContainer2().getName());
    }

	private boolean looserMapper(UMLOperationBodyMapper previous, UMLOperationBodyMapper current) {
		Set<String> previousIntersection = parameterIntersection(previous);
		Set<String> currentIntersection = parameterIntersection(current);
		if (current.mappingsWithoutBlocks() < previous.mappingsWithoutBlocks() &&
				currentIntersection.size() < previousIntersection.size()) {
			return true;
		}
		return current.mappingsWithoutBlocks() < previous.mappingsWithoutBlocks() &&
				!current.getContainer1().getName().equals(current.getContainer2().getName());
	}

	private Set<String> parameterIntersection(UMLOperationBodyMapper mapper) {
		Set<String> parameterTypeIntersection = new LinkedHashSet<>(mapper.getContainer1().getParameterNameList());
		parameterTypeIntersection.retainAll(mapper.getContainer2().getParameterNameList());
		return parameterTypeIntersection;
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
                if (refactoring != null)
                	methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(changeType).refactoring(refactoring));
                else
                	methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(changeType));
                if (checkOperationBodyChanged(methodBefore.getUmlOperation().getBody(), methodAfter.getUmlOperation().getBody())) {
                    methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
                }
                if (checkOperationDocumentationChanged(methodBefore.getUmlOperation(), methodAfter.getUmlOperation())) {
                    methodChangeHistory.addChange(methodBefore, methodAfter, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
                }
                processChange(methodBefore, methodAfter);
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
            for (UMLClassRenameDiff classRenameDiff : umlModelDiffAll.getClassRenameDiffList()) {
                if (found)
                    break;
                for (UMLOperationBodyMapper umlOperationBodyMapper : classRenameDiff.getOperationBodyMapperList()) {
                	if (found)
                        break;
                	found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, new RenameClassRefactoring(classRenameDiff.getOriginalClass(), classRenameDiff.getRenamedClass()), umlOperationBodyMapper.getContainer1(), umlOperationBodyMapper.getContainer2(), changeType);
                    if (found)
                        break;
                    for (UMLAnonymousClassDiff anonymousClassDiff : umlOperationBodyMapper.getAnonymousClassDiffs()) {
            			if (found)
                            break;
            			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
            				 found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                             if (found)
                                 break;
            			}
            		}
                }
                for (UMLAttributeDiff diff : classRenameDiff.getAttributeDiffList()) {
                	if (found)
                        break;
                	if (diff.getInitializerMapper().isPresent()) {
                		for (UMLAnonymousClassDiff anonymousClassDiff : diff.getInitializerMapper().get().getAnonymousClassDiffs()) {
                			if (found)
                                break;
                			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                				found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                                if (found)
                                    break;
                			}
                		}
                	}
                }
                for (Pair<UMLAttribute, UMLAttribute> pair : classRenameDiff.getCommonAtrributes()) {
                	if (found)
                        break;
                	if (pair.getLeft().getAnonymousClassList().size() == pair.getRight().getAnonymousClassList().size() && pair.getLeft().getAnonymousClassList().size() > 0) {
                		for (int i=0; i<pair.getLeft().getAnonymousClassList().size(); i++) {
                			UMLAnonymousClass anonymous1 = pair.getLeft().getAnonymousClassList().get(i);
                			UMLAnonymousClass anonymous2 = pair.getRight().getAnonymousClassList().get(i);
                			if (anonymous1.getOperations().size() == anonymous2.getOperations().size()) {
                				for (int j=0; j<anonymous1.getOperations().size(); j++) {
                					UMLOperation anonymousMethod1 = anonymous1.getOperations().get(j);
                					UMLOperation anonymousMethod2 = anonymous2.getOperations().get(j);
                					found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousMethod1, anonymousMethod2, changeType);
                                    if (found)
                                        break;
                				}
                			}
                		}
                	}
                }
            }
            for (UMLClassMoveDiff classMoveDiff : classMovedDiffList) {
                if (found)
                    break;
                for (UMLOperationBodyMapper umlOperationBodyMapper : classMoveDiff.getOperationBodyMapperList()) {
                	if (found)
                        break;
                	found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, new MoveClassRefactoring(classMoveDiff.getOriginalClass(), classMoveDiff.getMovedClass()), umlOperationBodyMapper.getContainer1(), umlOperationBodyMapper.getContainer2(), changeType);
                    if (found)
                        break;
                    for (UMLAnonymousClassDiff anonymousClassDiff : umlOperationBodyMapper.getAnonymousClassDiffs()) {
            			if (found)
                            break;
            			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
            				 found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                             if (found)
                                 break;
            			}
            		}
                }
                for (UMLAttributeDiff diff : classMoveDiff.getAttributeDiffList()) {
                	if (found)
                        break;
                	if (diff.getInitializerMapper().isPresent()) {
                		for (UMLAnonymousClassDiff anonymousClassDiff : diff.getInitializerMapper().get().getAnonymousClassDiffs()) {
                			if (found)
                                break;
                			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                				found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                                if (found)
                                    break;
                			}
                		}
                	}
                }
                for (Pair<UMLAttribute, UMLAttribute> pair : classMoveDiff.getCommonAtrributes()) {
                	if (found)
                        break;
                	if (pair.getLeft().getAnonymousClassList().size() == pair.getRight().getAnonymousClassList().size() && pair.getLeft().getAnonymousClassList().size() > 0) {
                		for (int i=0; i<pair.getLeft().getAnonymousClassList().size(); i++) {
                			UMLAnonymousClass anonymous1 = pair.getLeft().getAnonymousClassList().get(i);
                			UMLAnonymousClass anonymous2 = pair.getRight().getAnonymousClassList().get(i);
                			if (anonymous1.getOperations().size() == anonymous2.getOperations().size()) {
                				for (int j=0; j<anonymous1.getOperations().size(); j++) {
                					UMLOperation anonymousMethod1 = anonymous1.getOperations().get(j);
                					UMLOperation anonymousMethod2 = anonymous2.getOperations().get(j);
                					found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousMethod1, anonymousMethod2, changeType);
                                    if (found)
                                        break;
                				}
                			}
                		}
                	}
                }
            }
            for (UMLClassDiff classDiff : umlModelDiffAll.getCommonClassDiffList()) {
            	if (found)
                    break;
            	for (UMLOperationBodyMapper umlOperationBodyMapper : classDiff.getOperationBodyMapperList()) {
            		if (found)
                        break;
            		for (UMLAnonymousClassDiff anonymousClassDiff : umlOperationBodyMapper.getAnonymousClassDiffs()) {
            			if (found)
                            break;
            			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
            				 found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                             if (found)
                                 break;
            			}
            		}
            	}
            	for (UMLAttributeDiff diff : classDiff.getAttributeDiffList()) {
                	if (found)
                        break;
                	if (diff.getInitializerMapper().isPresent()) {
                		for (UMLAnonymousClassDiff anonymousClassDiff : diff.getInitializerMapper().get().getAnonymousClassDiffs()) {
                			if (found)
                                break;
                			for (UMLOperationBodyMapper anonymousOperationBodyMapper : anonymousClassDiff.getOperationBodyMapperList()) {
                				found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousOperationBodyMapper.getContainer1(), anonymousOperationBodyMapper.getContainer2(), changeType);
                                if (found)
                                    break;
                			}
                		}
                	}
                }
            	for (Pair<UMLAttribute, UMLAttribute> pair : classDiff.getCommonAtrributes()) {
                	if (found)
                        break;
                	if (pair.getLeft().getAnonymousClassList().size() == pair.getRight().getAnonymousClassList().size() && pair.getLeft().getAnonymousClassList().size() > 0) {
                		for (int i=0; i<pair.getLeft().getAnonymousClassList().size(); i++) {
                			UMLAnonymousClass anonymous1 = pair.getLeft().getAnonymousClassList().get(i);
                			UMLAnonymousClass anonymous2 = pair.getRight().getAnonymousClassList().get(i);
                			if (anonymous1.getOperations().size() == anonymous2.getOperations().size()) {
                				for (int j=0; j<anonymous1.getOperations().size(); j++) {
                					UMLOperation anonymousMethod1 = anonymous1.getOperations().get(j);
                					UMLOperation anonymousMethod2 = anonymous2.getOperations().get(j);
                					found = addMethodChange(currentVersion, parentVersion, equalOperator, leftMethodSet, null, anonymousMethod1, anonymousMethod2, changeType);
                                    if (found)
                                        break;
                				}
                			}
                		}
                	}
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
        for (UMLClassBaseDiff classDiff : allClassesDiff) {
        	for (Refactoring r : classDiff.getRefactoringsBeforePostProcessing()) {
        		if (r instanceof ExtractOperationRefactoring) {
        			ExtractOperationRefactoring extract = (ExtractOperationRefactoring)r;
        			if (extract.getBodyMapper().isNested() && extract.getBodyMapper().getParentMapper() != null) {
        				VariableDeclarationContainer container = extract.getBodyMapper().getParentMapper().getContainer2();
        				if (handleAddOperation(currentVersion, parentVersion, equalOperator, container, "new method"))
        	                return true;
        			}
        		}
        	}
        }

        UMLClass addedClass = modelDiff.getAddedClass(className);
        if (addedClass != null) {
            for (UMLOperation operation : addedClass.getOperations()) {
                if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new class"))
                    return true;
            }
        }
        else {
        	String prefix = new String(className);
        	while (prefix.contains(".")) {
        		prefix = prefix.substring(0, prefix.lastIndexOf("."));
        		addedClass = modelDiff.getAddedClass(prefix);
        		if (addedClass != null) {
        			break;
        		}
        	}
        	if (addedClass != null) {
        		for (UMLAnonymousClass anonymousClass : addedClass.getAnonymousClassList()) {
        			for (UMLOperation operation : anonymousClass.getOperations()) {
                        if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new anonymous class"))
                            return true;
                    }
        		}
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
        
        List<UMLInitializer> addedInitializers = allClassesDiff
                .stream()
                .map(UMLClassBaseDiff::getAddedInitializers)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLInitializer operation : addedInitializers) {
            if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "new initializer"))
                return true;
        }
        if (addedClass != null) {
            for (UMLInitializer operation : addedClass.getInitializers()) {
                if (handleAddOperation(currentVersion, parentVersion, equalOperator, operation, "added with new class"))
                    return true;
            }
        }
        return false;
    }

    public boolean handleAddOperation( Version currentVersion, Version parentVersion, Predicate<Method> equalOperator, VariableDeclarationContainer operation, String comment) {
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

    private Map<Pair<Method, Method>, List<Integer>> lineChangeMap = new LinkedHashMap<>();

	public void processChange(Method methodBefore, Method methodAfter) {
		if (methodBefore.isMultiLine() || methodAfter.isMultiLine()) {
			try {
				Pair<Method, Method> pair = Pair.of(methodBefore, methodAfter);
				Method startMethod = getStart();
				if (startMethod != null) {
					List<String> start = IOUtils.readLines(new StringReader(((UMLOperation)startMethod.getUmlOperation()).getActualSignature()));
					List<String> original = IOUtils.readLines(new StringReader(((UMLOperation)methodBefore.getUmlOperation()).getActualSignature()));
					List<String> revised = IOUtils.readLines(new StringReader(((UMLOperation)methodAfter.getUmlOperation()).getActualSignature()));
		
					Patch<String> patch = DiffUtils.diff(original, revised);
					List<AbstractDelta<String>> deltas = patch.getDeltas();
					for (int i=0; i<deltas.size(); i++) {
						AbstractDelta<String> delta = deltas.get(i);
						Chunk<String> target = delta.getTarget();
						List<String> affectedLines = new ArrayList<>(target.getLines());
						boolean subListFound = false;
						if (affectedLines.size() > 1 && !(delta instanceof InsertDelta)) {
							int index = Collections.indexOfSubList(start, affectedLines);
							if (index != -1) {
								subListFound = true;
								for (int j=0; j<affectedLines.size(); j++) {
									int actualLine = startMethod.signatureStartLine() + index + j;
									if (lineChangeMap.containsKey(pair)) {
										lineChangeMap.get(pair).add(actualLine);
									}
									else {
										List<Integer> list = new ArrayList<>();
										list.add(actualLine);
										lineChangeMap.put(pair, list);
									}
								}
							}
						}
						if (!subListFound) {
							for (String line : affectedLines) {
								List<Integer> matchingIndices = findAllMatchingIndices(start, line);
								for (Integer index : matchingIndices) {
									if (original.size() > index && revised.size() > index &&
											original.get(index).equals(line) && revised.get(index).equals(line)) {
										continue;
									}
									int actualLine = startMethod.signatureStartLine() + index;
									if (lineChangeMap.containsKey(pair)) {
										lineChangeMap.get(pair).add(actualLine);
									}
									else {
										List<Integer> list = new ArrayList<>();
										list.add(actualLine);
										lineChangeMap.put(pair, list);
									}
									break;
								}
							}
						}
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<Integer> findAllMatchingIndices(List<String> startCommentLines, String line) {
		List<Integer> matchingIndices = new ArrayList<>();
		for(int i=0; i<startCommentLines.size(); i++) {
			String element = startCommentLines.get(i).trim();
			if(line.equals(element) || element.contains(line.trim())) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
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

	public HistoryInfo<Method> blameReturn(Method startMethod, int exactLineNumber) {
		List<HistoryInfo<Method>> history = getHistory();
		for (History.HistoryInfo<Method> historyInfo : history) {
			Pair<Method, Method> pair = Pair.of(historyInfo.getElementBefore(), historyInfo.getElementAfter());
			boolean multiLine = startMethod.isMultiLine();
			for (Change change : historyInfo.getChangeList()) {
				if (startMethod.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if ((change instanceof MethodSignatureChange && !(change instanceof MethodAnnotationChange))) {
						if (multiLine) {
							if (lineChangeMap.containsKey(pair)) {
								if (lineChangeMap.get(pair).contains(exactLineNumber)) {
									return historyInfo;
								}
							}
						}
						else {
							return historyInfo;
						}
					}
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
			}
		}
		return null;
	}
}
