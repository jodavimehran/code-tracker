package org.codetracker;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.AddParameterRefactoring;
import gr.uom.java.xmi.diff.AddVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddVariableModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.ModifyVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveVariableModifierRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;

public class VariableTrackerChangeHistory extends AbstractChangeHistory<Variable> {
	private final ChangeHistory<Variable> variableChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final String variableName;
    private final int variableDeclarationLineNumber;

	public VariableTrackerChangeHistory(String methodName, int methodDeclarationLineNumber, String variableName, int variableDeclarationLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
		this.variableName = variableName;
		this.variableDeclarationLineNumber = variableDeclarationLineNumber;
	}

	public ChangeHistory<Variable> get() {
		return variableChangeHistory;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getMethodDeclarationLineNumber() {
		return methodDeclarationLineNumber;
	}

	public String getVariableName() {
		return variableName;
	}

	public int getVariableDeclarationLineNumber() {
		return variableDeclarationLineNumber;
	}

    public boolean isStartVariable(Variable variable) {
        return variable.getVariableDeclaration().getVariableName().equals(variableName) &&
                variable.getVariableDeclaration().getLocationInfo().getStartLine() <= variableDeclarationLineNumber &&
                variable.getVariableDeclaration().getLocationInfo().getEndLine() >= variableDeclarationLineNumber;
    }

    public boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean checkClassDiffForVariableChange(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Variable> equalVariable, UMLAbstractClassDiff umlClassDiff) {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                if (isVariableRefactored(operationBodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, equalVariable))
                    return true;

                // check if it is in the matched
                if (isMatched(operationBodyMapper, currentVersion, parentVersion, equalVariable))
                    return true;

                //Check if is added
                if (isAdded(operationBodyMapper, currentVersion, parentVersion, equalVariable))
                    return true;
            }
        }
        return false;
    }

    public boolean checkForExtractionOrInline(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Variable rightVariable, List<Refactoring> refactorings) {
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
                        elements.add(variableBefore);
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
                                elements.add(variableBefore);
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
                                    if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                        return true;
                                    if (isMatched(bodyMapper, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                        return true;
                                }
                            }
                            for (VariableDeclaration addedVariable : bodyMapper.getAddedVariables()) {
                                Variable matchedVariableInsideMergedMethodBody = Variable.of(addedVariable, bodyMapper.getContainer2(), currentVersion);
                                if (matchedVariableInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                    if (isAdded(bodyMapper, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion)) {
                                        return true;
                                    }
                                }
                            }
                            for (Refactoring r : bodyMapper.getRefactoringsAfterPostProcessing()) {
                                if (r instanceof RenameVariableRefactoring) {
                                    RenameVariableRefactoring rename = (RenameVariableRefactoring) r;
                                    Variable matchedVariableInsideMergedMethodBody = Variable.of(rename.getRenamedVariable(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedVariableInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                        if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
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
                                        if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                            return true;
                                        if (isMatched(bodyMapper, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                            return true;
                                    }
                                }
                                for (VariableDeclaration addedVariable : bodyMapper.getAddedVariables()) {
                                    Variable matchedVariableInsideSplitMethodBody = Variable.of(addedVariable, bodyMapper.getContainer2(), currentVersion);
                                    if (matchedVariableInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                        if (isAdded(bodyMapper, currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion)) {
                                            return true;
                                        }
                                    }
                                }
                                for (Refactoring r : bodyMapper.getRefactoringsAfterPostProcessing()) {
                                    if (r instanceof RenameVariableRefactoring) {
                                        RenameVariableRefactoring rename = (RenameVariableRefactoring) r;
                                        Variable matchedVariableInsideMergedMethodBody = Variable.of(rename.getRenamedVariable(), bodyMapper.getContainer2(), currentVersion);
                                        if (matchedVariableInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                            if (isVariableRefactored(bodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion))
                                                return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case MOVE_CODE: {
                    MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                    Method extractedMethod = Method.of(moveCodeRefactoring.getTargetContainer(), currentVersion);
                    if (equalMethod.test(extractedMethod) && moveCodeRefactoring.getMoveType().equals(MoveCodeRefactoring.Type.MOVE_TO_ADDED)) {
                        VariableDeclaration matchedVariableFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
                        for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : bodyMapper.getMatchedVariables()) {
                            Variable matchedVariableInsideExtractedMethodBody = Variable.of(matchedVariablePair.getRight(), bodyMapper.getContainer2(), currentVersion);
                            if (matchedVariableInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightVariable)) {
                                matchedVariableFromSourceMethod = matchedVariablePair.getLeft();
                                break;
                            }
                        }
                        Variable variableBefore = Variable.of(rightVariable.getVariableDeclaration(), rightVariable.getOperation(), parentVersion);
                        if (matchedVariableFromSourceMethod == null) {
                            variableChangeHistory.handleAdd(variableBefore, rightVariable, moveCodeRefactoring.toString());
                        }
                        else {
                            VariableDeclarationContainer sourceOperation = moveCodeRefactoring.getSourceContainer();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            variableChangeHistory.addChange(variableBefore, rightVariable, ChangeFactory.forVariable(Change.Type.INTRODUCED)
                                    .refactoring(moveCodeRefactoring).codeElement(rightVariable).hookedElement(Variable.of(matchedVariableFromSourceMethod, sourceMethod)));
                            variableBefore.setAdded(true);
                        }
                        elements.add(variableBefore);
                        variableChangeHistory.connectRelatedNodes();
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    public boolean checkBodyOfMatchedOperations(Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper, Set<Refactoring> refactorings) {
        if (umlOperationBodyMapper == null)
            return false;
        //Check if refactored
        if (isVariableRefactored(refactorings, currentVersion, parentVersion, equalOperator))
            return true;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator);
    }

    public boolean isVariableRefactored(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        Set<Variable> leftVariableSet = analyseVariableRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        for (Variable leftVariable : leftVariableSet) {
            elements.add(leftVariable);
            variableChangeHistory.connectRelatedNodes();
            return true;
        }
        return false;
    }

    private boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        for (Pair<VariableDeclaration, VariableDeclaration> matchedVariablePair : umlOperationBodyMapper.getMatchedVariables()) {
            Variable variableAfter = Variable.of(matchedVariablePair.getRight(), umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(matchedVariablePair.getLeft(), umlOperationBodyMapper.getContainer1(), parentVersion);
                variableChangeHistory.addChange(variableBefore, variableAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                elements.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    public boolean isMatched(VariableDeclarationContainer leftMethod, Variable rightVariable, Version parentVersion) {
        for (VariableDeclaration leftVariable : leftMethod.getAllVariableDeclarations()) {
            if (matchingVariables(leftMethod, rightVariable, parentVersion, leftVariable)) return true;
        }
        for (UMLAnonymousClass anonymousClass : leftMethod.getAnonymousClassList()) {
            for (UMLOperation operation : anonymousClass.getOperations()) {
                for (VariableDeclaration leftVariable : operation.getAllVariableDeclarations()) {
                    if (matchingVariables(leftMethod, rightVariable, parentVersion, leftVariable)) return true;
                }
            }
        }
        for (LambdaExpressionObject lambda : leftMethod.getAllLambdas()) {
            for (VariableDeclaration leftVariable : lambda.getParameters()) {
                if (matchingVariables(leftMethod, rightVariable, parentVersion, leftVariable)) return true;
            }
        }
        return false;
    }

    private boolean matchingVariables(VariableDeclarationContainer leftMethod, Variable rightVariable, Version parentVersion, VariableDeclaration leftVariable) {
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
                elements.add(variableBefore);
                variableChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Variable> equalOperator) {
        for (VariableDeclaration addedVariable : umlOperationBodyMapper.getAddedVariables()) {
            Variable variableAfter = Variable.of(addedVariable, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(variableAfter)) {
                Variable variableBefore = Variable.of(addedVariable, umlOperationBodyMapper.getContainer2(), parentVersion);
                variableChangeHistory.handleAdd(variableBefore, variableAfter, "new variable");
                elements.add(variableBefore);
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

    public boolean checkRefactoredMethod(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Variable rightVariable, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            VariableDeclarationContainer operationAfter = null;
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
                    boolean found = checkBodyOfMatchedOperations(currentVersion, parentVersion, rightVariable::equalIdentifierIgnoringVersion, umlOperationBodyMapper, bodyMapperRefactorings);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean involvedInVariableRefactoring(Collection<Refactoring> refactorings, Variable rightVariable) {
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
}
