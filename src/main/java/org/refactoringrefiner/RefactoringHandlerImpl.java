package org.refactoringrefiner;

import com.google.common.collect.Sets;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.Hashing;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.edge.AbstractChange;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.element.Attribute;
import org.refactoringrefiner.element.Class;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.element.Variable;
import org.refactoringrefiner.util.IRepository;
import org.refactoringrefiner.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class RefactoringHandlerImpl extends RefactoringHandler {
    private final static Set<RefactoringType> CLASS_LEVEL_REFACTORING = Sets.newHashSet(RefactoringType.RENAME_CLASS, RefactoringType.MOVE_CLASS, RefactoringType.MOVE_RENAME_CLASS, RefactoringType.ADD_CLASS_ANNOTATION, RefactoringType.REMOVE_CLASS_ANNOTATION, RefactoringType.MODIFY_CLASS_ANNOTATION, RefactoringType.EXTRACT_INTERFACE, RefactoringType.EXTRACT_SUPERCLASS, RefactoringType.EXTRACT_SUBCLASS, RefactoringType.EXTRACT_CLASS);
    private final ChangeHistory attributeChangeHistory = new ChangeHistory();
    //    private final MutableValueGraph<CodeElement, Edge> attributeChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final ChangeHistory classChangeHistory = new ChangeHistory();
    //    private final MutableValueGraph<CodeElement, Edge> classChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
//    private final MutableValueGraph<CodeElement, Edge> methodChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final ChangeHistory methodChangeHistory = new ChangeHistory();
    //    private final MutableValueGraph<CodeElement, Edge> variableChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final ChangeHistory variableChangeHistory = new ChangeHistory();
    private final List<Refactoring> refactorings = new ArrayList<>();
    private final IRepository repository;
    //filters:
    private final Set<String> files = new HashSet<>();
    private final HashMap<UMLOperation, Refactoring> relatedRefactoringsToOperations = new HashMap<>();
    private final HashMap<UMLAttribute, Refactoring> relatedRefactoringsToAttributes = new HashMap<>();
    private boolean trackVariables = true;
    private boolean trackClasses = true;
    private boolean trackMethods = true;
    private boolean trackAttributes = true;
    private int refactoringsCount, commitsCount, errorCommitsCount;

    public RefactoringHandlerImpl(IRepository repository) {
        this.repository = repository;
    }

    private static ImmutableValueGraph<CodeElement, Edge> getGraph(MutableValueGraph<CodeElement, Edge> graph) {
        return ImmutableValueGraph.copyOf(graph);
    }

    private static void addRefactored(ChangeHistory changeHistory, CodeElement leftSide, CodeElement rightSide, Refactoring refactoring) {
        changeHistory.addRefactored(leftSide, rightSide, refactoring, null);
    }

    public static boolean checkOperationBodyChanged(OperationBody body1, OperationBody body2) {
        if (body1 == null && body2 == null) return false;

        if (body1 == null || body2 == null) {
            return true;
        }
        return !body1.getSha512().equals(body2.getSha512());
    }

    public static boolean checkOperationDocumentationChanged(UMLOperation operation1, UMLOperation operation2) {
        String comments1 = Hashing.getSHA512(operation1.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        String comments2 = Hashing.getSHA512(operation2.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        return !comments1.equals(comments2);
    }

    public IRepository getRepository() {
        return repository;
    }

    private void addMethodChange(String parentCommitId, String commitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation, ChangeFactory changeFactory) {
        if (!trackMethods)
            return;
        if (!files.isEmpty() && !(files.contains(leftSideOperation.getLocationInfo().getFilePath()) || files.contains(rightSideOperation.getLocationInfo().getFilePath())))
            return;
        Method leftSideMethod = getMethod(parentCommitId, leftSideOperation);
        Method rightSideMethod = getMethod(commitId, rightSideOperation);
        if (AbstractChange.Type.EXTRACTED.equals(changeFactory.getType())) {
            if (leftSideMethod != null) {
                leftSideMethod.setAdded(true);
            }
            changeFactory.codeElement(rightSideMethod);
        }
        if (AbstractChange.Type.INLINED.equals(changeFactory.getType())) {
            if (rightSideMethod != null) {
                rightSideMethod.setRemoved(true);
            }
            changeFactory.codeElement(leftSideMethod);
        }

        methodChangeHistory.addChange(leftSideMethod, rightSideMethod, changeFactory);
    }

    public int getNumberOfEdge() {
        return attributeChangeHistory.getNumberOfEdge() + classChangeHistory.getNumberOfEdge() + methodChangeHistory.getNumberOfEdge() + variableChangeHistory.getNumberOfEdge();
    }

    public ChangeHistory getAttributeChangeHistory() {
        return attributeChangeHistory;
    }

    public ChangeHistory getClassChangeHistoryGraph() {
        return classChangeHistory;
    }

    public ChangeHistory getMethodChangeHistoryGraph() {
        return methodChangeHistory;
    }

    public ChangeHistory getVariableChangeHistoryGraph() {
        return variableChangeHistory;
    }

    private Class getClass(String commitId, UMLClass umlClass) {
        if (!trackClasses)
            return null;
        return (Class) classChangeHistory.addNode(Class.of(umlClass, repository.getVersion(commitId)));
    }

    private Method getMethod(String commitId, UMLOperation umlOperation) {
        if (!trackMethods)
            return null;
        return (Method) methodChangeHistory.addNode(Method.of(umlOperation, repository.getVersion(commitId)));
    }

    private Variable getVariable(VariableDeclaration variableDeclaration, UMLOperation umlOperation, String commitId) {
        if (!trackVariables)
            return null;
        return (Variable) variableChangeHistory.addNode(Variable.of(variableDeclaration, umlOperation, repository.getVersion(commitId)));
    }

    private Attribute getAttributeElement(String commitId, UMLAttribute attribute) {
        if (!trackAttributes)
            return null;
        return (Attribute) attributeChangeHistory.addNode(Attribute.of(attribute, repository.getVersion(commitId)));
    }

    private void addOperationRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        if (!files.isEmpty() && !(files.contains(leftSideOperation.getLocationInfo().getFilePath()) || files.contains(rightSideOperation.getLocationInfo().getFilePath())))
            return;
        Method leftSideMethod = getMethod(parentCommitId, leftSideOperation);
        Method rightSideMethod = getMethod(childCommitId, rightSideOperation);

        if (leftSideMethod != null && rightSideMethod != null && !leftSideMethod.equals(rightSideMethod)) {
            Refactoring relatedRefactoring = relatedRefactoringsToOperations.getOrDefault(leftSideOperation, null);
            methodChangeHistory.addRefactored(leftSideMethod, rightSideMethod, ref, relatedRefactoring);
            matchVariables(ref, parentCommitId, childCommitId, leftSideOperation, rightSideOperation);
        }
    }

    private void addAttributeRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLAttribute leftSideUMLAttribute, UMLAttribute rightSideUMLAttribute) {
        if (!files.isEmpty() && !(files.contains(leftSideUMLAttribute.getLocationInfo().getFilePath()) || files.contains(rightSideUMLAttribute.getLocationInfo().getFilePath())))
            return;

        Attribute leftSideAttribute = getAttributeElement(parentCommitId, leftSideUMLAttribute);
        Attribute rightSideAttribute = getAttributeElement(childCommitId, rightSideUMLAttribute);
        if (leftSideAttribute != null && rightSideAttribute != null && !leftSideAttribute.equals(rightSideAttribute)) {
            addRefactored(attributeChangeHistory, leftSideAttribute, rightSideAttribute, ref);
        }
    }

    private void addVariableRefactored(Refactoring ref, String parentCommitId, String commitId, VariableDeclaration leftSideVariable, UMLOperation leftSideOperation, VariableDeclaration rightSideVariable, UMLOperation rightSideOperation) {
        if (!files.isEmpty() && !(files.contains(leftSideVariable.getLocationInfo().getFilePath()) || files.contains(rightSideVariable.getLocationInfo().getFilePath())))
            return;

        Variable leftSideAttributeElement = getVariable(leftSideVariable, leftSideOperation, parentCommitId);
        Variable rightSideAttributeElement = getVariable(rightSideVariable, rightSideOperation, commitId);
        if (leftSideAttributeElement != null && rightSideAttributeElement != null && !leftSideAttributeElement.equals(rightSideAttributeElement)) {
            addRefactored(variableChangeHistory, leftSideAttributeElement, rightSideAttributeElement, ref);
//            addMethodChange(parentCommitId, commitId, leftSideOperation, rightSideOperation, ChangeFactory.of(AbstractChange.Type.MODIFIED).description(ref.toString()).refactoring(ref));
        }
    }

    private void addClassRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLClass leftSideClass, UMLClass rightSideClass) {
        if (!files.isEmpty() && !(files.contains(leftSideClass.getLocationInfo().getFilePath()) || files.contains(rightSideClass.getLocationInfo().getFilePath())))
            return;

        Class leftSideClassElement = getClass(parentCommitId, leftSideClass);
        Class rightSideClassElement = getClass(childCommitId, rightSideClass);
        if (leftSideClassElement != null && rightSideClassElement != null && !leftSideClassElement.equals(rightSideClassElement)) {
            addRefactored(classChangeHistory, leftSideClassElement, rightSideClassElement, ref);
        }
    }

    public void analyze(String commitId, Collection<Refactoring> refactorings) {
        if (!refactorings.isEmpty()) {
            this.refactorings.addAll(refactorings);
            String parentCommitId = repository.getParentId(commitId);
            for (Refactoring ref : refactorings) {
                RefactoringType refactoringType = ref.getRefactoringType();
                switch (refactoringType) {
                    case PULL_UP_OPERATION: {
                        PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) ref;
                        addOperationRefactored(pullUpOperationRefactoring, parentCommitId, commitId, pullUpOperationRefactoring.getOriginalOperation(), pullUpOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case PUSH_DOWN_OPERATION: {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) ref;
                        addOperationRefactored(pushDownOperationRefactoring, parentCommitId, commitId, pushDownOperationRefactoring.getOriginalOperation(), pushDownOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_RENAME_OPERATION:
                    case MOVE_OPERATION: {
                        MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) ref;
                        UMLOperation leftSideOperation = moveOperationRefactoring.getOriginalOperation();
                        UMLOperation rightSideOperation = moveOperationRefactoring.getMovedOperation();
                        addOperationRefactored(moveOperationRefactoring, parentCommitId, commitId, leftSideOperation, rightSideOperation);
                        if (checkOperationBodyChanged(leftSideOperation.getBody(), rightSideOperation.getBody())) {
                            addMethodChange(parentCommitId, commitId, leftSideOperation, rightSideOperation, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
                        }
                        break;
                    }
                    case MOVE_AND_INLINE_OPERATION:
                    case INLINE_OPERATION: {
                        InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) ref;
                        UMLOperation inlinedOperation = inlineOperationRefactoring.getInlinedOperation();
                        UMLOperation operationAfterInline = inlineOperationRefactoring.getTargetOperationAfterInline();
                        UMLOperation operationBeforeInline = inlineOperationRefactoring.getTargetOperationBeforeInline();

                        addMethodChange(parentCommitId, commitId, inlinedOperation, inlinedOperation, ChangeFactory.of(AbstractChange.Type.INLINED).refactoring(inlineOperationRefactoring));
//                        addMethodChange(parentCommitId, commitId, inlinedOperation, operationAfterInline, ChangeFactory.of(AbstractChange.Type.MERGED).refactoring(inlineOperationRefactoring));
                        addMethodChange(parentCommitId, commitId, operationBeforeInline, operationAfterInline, ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(inlineOperationRefactoring));
                        break;
                    }
                    case EXTRACT_AND_MOVE_OPERATION:
                    case EXTRACT_OPERATION: {
                        ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
                        UMLOperation operationBeforeExtraction = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                        UMLOperation operationAfterExtraction = extractOperationRefactoring.getSourceOperationAfterExtraction();
                        UMLOperation extractedOperation = extractOperationRefactoring.getExtractedOperation();

                        addMethodChange(parentCommitId, commitId, extractedOperation, extractedOperation, ChangeFactory.of(AbstractChange.Type.EXTRACTED).refactoring(extractOperationRefactoring));
//                        addMethodChange(parentCommitId, commitId, operationBeforeExtraction, extractedOperation, ChangeFactory.of(AbstractChange.Type.BRANCHED).refactoring(extractOperationRefactoring));
                        addMethodChange(parentCommitId, commitId, operationBeforeExtraction, operationAfterExtraction, ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(ref));
                        break;
                    }
                    case RENAME_METHOD: {
                        RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) ref;
                        addOperationRefactored(renameOperationRefactoring, parentCommitId, commitId, renameOperationRefactoring.getOriginalOperation(), renameOperationRefactoring.getRenamedOperation());
                        break;
                    }
                    case ADD_METHOD_ANNOTATION: {
                        AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                        addOperationRefactored(addMethodAnnotationRefactoring, parentCommitId, commitId, addMethodAnnotationRefactoring.getOperationBefore(), addMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case MODIFY_METHOD_ANNOTATION: {
                        ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) ref;
                        addOperationRefactored(modifyMethodAnnotationRefactoring, parentCommitId, commitId, modifyMethodAnnotationRefactoring.getOperationBefore(), modifyMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_METHOD_ANNOTATION: {
                        RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                        addOperationRefactored(removeMethodAnnotationRefactoring, parentCommitId, commitId, removeMethodAnnotationRefactoring.getOperationBefore(), removeMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_RETURN_TYPE: {
                        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) ref;
                        addOperationRefactored(changeReturnTypeRefactoring, parentCommitId, commitId, changeReturnTypeRefactoring.getOperationBefore(), changeReturnTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case SPLIT_PARAMETER: {
                        SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) ref;
                        addOperationRefactored(splitVariableRefactoring, parentCommitId, commitId, splitVariableRefactoring.getOperationBefore(), splitVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case MERGE_PARAMETER: {
                        MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) ref;
                        addOperationRefactored(mergeVariableRefactoring, parentCommitId, commitId, mergeVariableRefactoring.getOperationBefore(), mergeVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_PARAMETER:
                    case PARAMETERIZE_VARIABLE: {
                        RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) ref;
                        if (!renameVariableRefactoring.isExtraction())
                            addOperationRefactored(renameVariableRefactoring, parentCommitId, commitId, renameVariableRefactoring.getOperationBefore(), renameVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_PARAMETER_TYPE: {
                        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                        addOperationRefactored(changeVariableTypeRefactoring, parentCommitId, commitId, changeVariableTypeRefactoring.getOperationBefore(), changeVariableTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_PARAMETER: {
                        AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) ref;
                        addOperationRefactored(addParameterRefactoring, parentCommitId, commitId, addParameterRefactoring.getOperationBefore(), addParameterRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_PARAMETER: {
                        RemoveParameterRefactoring removeParameterRefactoring = (RemoveParameterRefactoring) ref;
                        addOperationRefactored(removeParameterRefactoring, parentCommitId, commitId, removeParameterRefactoring.getOperationBefore(), removeParameterRefactoring.getOperationAfter());
                        break;
                    }
                    case REORDER_PARAMETER: {
                        ReorderParameterRefactoring reorderParameterRefactoring = (ReorderParameterRefactoring) ref;
                        addOperationRefactored(reorderParameterRefactoring, parentCommitId, commitId, reorderParameterRefactoring.getOperationBefore(), reorderParameterRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_PARAMETER_MODIFIER: {
                        AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) ref;
                        addOperationRefactored(addVariableModifierRefactoring, parentCommitId, commitId, addVariableModifierRefactoring.getOperationBefore(), addVariableModifierRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_PARAMETER_MODIFIER: {
                        RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) ref;
                        addOperationRefactored(removeVariableModifierRefactoring, parentCommitId, commitId, removeVariableModifierRefactoring.getOperationBefore(), removeVariableModifierRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_PARAMETER_ANNOTATION: {
                        AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) ref;
                        addOperationRefactored(addVariableAnnotationRefactoring, parentCommitId, commitId, addVariableAnnotationRefactoring.getOperationBefore(), addVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_PARAMETER_ANNOTATION: {
                        RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) ref;
                        addOperationRefactored(removeVariableAnnotationRefactoring, parentCommitId, commitId, removeVariableAnnotationRefactoring.getOperationBefore(), removeVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case MODIFY_PARAMETER_ANNOTATION: {
                        ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) ref;
                        addOperationRefactored(modifyVariableAnnotationRefactoring, parentCommitId, commitId, modifyVariableAnnotationRefactoring.getOperationBefore(), modifyVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_THROWN_EXCEPTION_TYPE: {
                        AddThrownExceptionTypeRefactoring addThrownExceptionTypeRefactoring = (AddThrownExceptionTypeRefactoring) ref;
                        addOperationRefactored(addThrownExceptionTypeRefactoring, parentCommitId, commitId, addThrownExceptionTypeRefactoring.getOperationBefore(), addThrownExceptionTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_THROWN_EXCEPTION_TYPE: {
                        ChangeThrownExceptionTypeRefactoring changeThrownExceptionTypeRefactoring = (ChangeThrownExceptionTypeRefactoring) ref;
                        addOperationRefactored(changeThrownExceptionTypeRefactoring, parentCommitId, commitId, changeThrownExceptionTypeRefactoring.getOperationBefore(), changeThrownExceptionTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_THROWN_EXCEPTION_TYPE: {
                        RemoveThrownExceptionTypeRefactoring removeThrownExceptionTypeRefactoring = (RemoveThrownExceptionTypeRefactoring) ref;
                        addOperationRefactored(removeThrownExceptionTypeRefactoring, parentCommitId, commitId, removeThrownExceptionTypeRefactoring.getOperationBefore(), removeThrownExceptionTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_OPERATION_ACCESS_MODIFIER: {
                        ChangeOperationAccessModifierRefactoring changeOperationAccessModifierRefactoring = (ChangeOperationAccessModifierRefactoring) ref;
                        addOperationRefactored(changeOperationAccessModifierRefactoring, parentCommitId, commitId, changeOperationAccessModifierRefactoring.getOperationBefore(), changeOperationAccessModifierRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_METHOD_MODIFIER: {
                        AddMethodModifierRefactoring addMethodModifierRefactoring = (AddMethodModifierRefactoring) ref;
                        addOperationRefactored(addMethodModifierRefactoring, parentCommitId, commitId, addMethodModifierRefactoring.getOperationBefore(), addMethodModifierRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_METHOD_MODIFIER: {
                        RemoveMethodModifierRefactoring removeMethodModifierRefactoring = (RemoveMethodModifierRefactoring) ref;
                        addOperationRefactored(removeMethodModifierRefactoring, parentCommitId, commitId, removeMethodModifierRefactoring.getOperationBefore(), removeMethodModifierRefactoring.getOperationAfter());
                        break;
                    }
                    //=======================================CLASS===========================================================
                    case RENAME_CLASS: {
                        RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) ref;
                        UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
                        addClassRefactored(renameClassRefactoring, parentCommitId, commitId, originalClass, renamedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case MOVE_CLASS: {
                        MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) ref;
                        UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                        UMLClass movedClass = moveClassRefactoring.getMovedClass();
                        addClassRefactored(moveClassRefactoring, parentCommitId, commitId, originalClass, movedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), movedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), movedClass.getAttributes());
                        break;
                    }
                    case MOVE_RENAME_CLASS: {
                        MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) ref;
                        UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                        addClassRefactored(moveAndRenameClassRefactoring, parentCommitId, commitId, originalClass, renamedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case ADD_CLASS_ANNOTATION: {
                        AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) ref;
                        addClassRefactored(addClassAnnotationRefactoring, parentCommitId, commitId, addClassAnnotationRefactoring.getClassBefore(), addClassAnnotationRefactoring.getClassAfter());
                        break;
                    }
                    case REMOVE_CLASS_ANNOTATION: {
                        RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) ref;
                        addClassRefactored(removeClassAnnotationRefactoring, parentCommitId, commitId, removeClassAnnotationRefactoring.getClassBefore(), removeClassAnnotationRefactoring.getClassAfter());
                        break;
                    }
                    case MODIFY_CLASS_ANNOTATION: {
                        ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) ref;
                        addClassRefactored(modifyClassAnnotationRefactoring, parentCommitId, commitId, modifyClassAnnotationRefactoring.getClassBefore(), modifyClassAnnotationRefactoring.getClassAfter());
                        break;
                    }
                    case ADD_CLASS_MODIFIER: {
                        AddClassModifierRefactoring addClassModifierRefactoring = (AddClassModifierRefactoring) ref;
                        addClassRefactored(addClassModifierRefactoring, parentCommitId, commitId, addClassModifierRefactoring.getClassBefore(), addClassModifierRefactoring.getClassAfter());
                        break;
                    }
                    case REMOVE_CLASS_MODIFIER: {
                        RemoveClassModifierRefactoring removeClassModifierRefactoring = (RemoveClassModifierRefactoring) ref;
                        addClassRefactored(removeClassModifierRefactoring, parentCommitId, commitId, removeClassModifierRefactoring.getClassBefore(), removeClassModifierRefactoring.getClassAfter());
                        break;
                    }
                    case CHANGE_CLASS_ACCESS_MODIFIER: {
                        ChangeClassAccessModifierRefactoring changeClassAccessModifierRefactoring = (ChangeClassAccessModifierRefactoring) ref;
                        addClassRefactored(changeClassAccessModifierRefactoring, parentCommitId, commitId, changeClassAccessModifierRefactoring.getClassBefore(), changeClassAccessModifierRefactoring.getClassAfter());
                        break;
                    }
                    case EXTRACT_INTERFACE:
                    case EXTRACT_SUPERCLASS: {
                        //TODO: Change ExtractSuperclassRefactoring in a way that contains the information of original class before and after extraction
                        ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                        UMLClass extractedClass = extractSuperclassRefactoring.getExtractedClass();
                        Class leftSideExtractedClass = getClass(parentCommitId, extractedClass);
                        Class rightSideExtractedClass = getClass(commitId, extractedClass);
                        if (leftSideExtractedClass != null)
                            leftSideExtractedClass.setAdded(true);
                        classChangeHistory.addChange(leftSideExtractedClass, rightSideExtractedClass, ChangeFactory.of(AbstractChange.Type.EXTRACTED).refactoring(extractSuperclassRefactoring).codeElement(rightSideExtractedClass));
                        for (UMLClass originalClass : extractSuperclassRefactoring.getUMLSubclassSet()) {
                            classChangeHistory.addChange(getClass(parentCommitId, originalClass), getClass(commitId, originalClass), ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(extractSuperclassRefactoring));
                        }
                        break;
                    }
                    case EXTRACT_SUBCLASS:
                    case EXTRACT_CLASS: {
                        //TODO: Change ExtractClassRefactoring in a way that contains the information of original class before and after extraction
                        ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) ref;
                        UMLClass originalClass = extractClassRefactoring.getOriginalClass();
                        UMLClass extractedClass = extractClassRefactoring.getExtractedClass();

                        Class leftSideSourceClass = getClass(parentCommitId, originalClass);
                        Class rightSideSourceClass = getClass(commitId, originalClass);
                        Class leftSideExtractedClass = getClass(parentCommitId, extractedClass);
                        Class rightSideExtractedClass = getClass(commitId, extractedClass);
                        if (leftSideExtractedClass != null)
                            leftSideExtractedClass.setAdded(true);
                        classChangeHistory.addChange(leftSideExtractedClass, rightSideExtractedClass, ChangeFactory.of(AbstractChange.Type.EXTRACTED).refactoring(extractClassRefactoring).codeElement(rightSideExtractedClass));
                        classChangeHistory.addChange(leftSideSourceClass, rightSideSourceClass, ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(extractClassRefactoring));

                        for (UMLOperation extractedOperation : extractClassRefactoring.getExtractedOperations()) {
                            relatedRefactoringsToOperations.put(extractedOperation, ref);
                        }
                        for (UMLAttribute extractedAttribute : extractClassRefactoring.getExtractedAttributes()) {
                            relatedRefactoringsToAttributes.put(extractedAttribute, ref);
                        }
                        break;
                    }
                    case MOVE_SOURCE_FOLDER: {
                        MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) ref;
                        for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                            UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                            addClassRefactored(moveSourceFolderRefactoring, parentCommitId, commitId, originalClass, movedClass);
                            matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), movedClass.getAttributes());
                            matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), movedClass.getOperations());
                        }
                        break;
                    }
                    //======================================ATTRIBUTE============================================================
                    case MOVE_ATTRIBUTE: {
                        MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) ref;
                        addAttributeRefactored(moveAttributeRefactoring, parentCommitId, commitId, moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case PULL_UP_ATTRIBUTE: {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) ref;
                        addAttributeRefactored(pullUpAttributeRefactoring, parentCommitId, commitId, pullUpAttributeRefactoring.getOriginalAttribute(), pullUpAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case PUSH_DOWN_ATTRIBUTE: {
                        PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) ref;
                        addAttributeRefactored(pushDownAttributeRefactoring, parentCommitId, commitId, pushDownAttributeRefactoring.getOriginalAttribute(), pushDownAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case RENAME_ATTRIBUTE: {
                        RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) ref;
                        addAttributeRefactored(renameAttributeRefactoring, parentCommitId, commitId, renameAttributeRefactoring.getOriginalAttribute(), renameAttributeRefactoring.getRenamedAttribute());
                        break;
                    }
                    case CHANGE_ATTRIBUTE_TYPE: {
                        ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) ref;
                        addAttributeRefactored(changeAttributeTypeRefactoring, parentCommitId, commitId, changeAttributeTypeRefactoring.getOriginalAttribute(), changeAttributeTypeRefactoring.getChangedTypeAttribute());
                        break;
                    }
                    case ADD_ATTRIBUTE_ANNOTATION: {
                        AddAttributeAnnotationRefactoring addAttributeAnnotationRefactoring = (AddAttributeAnnotationRefactoring) ref;
                        addAttributeRefactored(addAttributeAnnotationRefactoring, parentCommitId, commitId, addAttributeAnnotationRefactoring.getAttributeBefore(), addAttributeAnnotationRefactoring.getAttributeAfter());
                        break;
                    }
                    case MODIFY_ATTRIBUTE_ANNOTATION: {
                        ModifyAttributeAnnotationRefactoring modifyAttributeAnnotationRefactoring = (ModifyAttributeAnnotationRefactoring) ref;
                        addAttributeRefactored(modifyAttributeAnnotationRefactoring, parentCommitId, commitId, modifyAttributeAnnotationRefactoring.getAttributeBefore(), modifyAttributeAnnotationRefactoring.getAttributeAfter());
                        break;
                    }
                    case REMOVE_ATTRIBUTE_ANNOTATION: {
                        RemoveAttributeAnnotationRefactoring removeAttributeAnnotationRefactoring = (RemoveAttributeAnnotationRefactoring) ref;
                        addAttributeRefactored(removeAttributeAnnotationRefactoring, parentCommitId, commitId, removeAttributeAnnotationRefactoring.getAttributeBefore(), removeAttributeAnnotationRefactoring.getAttributeAfter());
                        break;
                    }
                    case CHANGE_ATTRIBUTE_ACCESS_MODIFIER: {
                        ChangeAttributeAccessModifierRefactoring changeAttributeAccessModifierRefactoring = (ChangeAttributeAccessModifierRefactoring) ref;
                        addAttributeRefactored(changeAttributeAccessModifierRefactoring, parentCommitId, commitId, changeAttributeAccessModifierRefactoring.getAttributeBefore(), changeAttributeAccessModifierRefactoring.getAttributeAfter());
                        break;
                    }
                    case SPLIT_ATTRIBUTE: {
                        SplitAttributeRefactoring splitAttributeRefactoring = (SplitAttributeRefactoring) ref;
                        for (UMLAttribute rightSide : splitAttributeRefactoring.getSplitAttributes()) {
                            addAttributeRefactored(splitAttributeRefactoring, parentCommitId, commitId, splitAttributeRefactoring.getOldAttribute(), rightSide);
                        }
                        break;
                    }
                    case MERGE_ATTRIBUTE: {
                        MergeAttributeRefactoring mergeAttributeRefactoring = (MergeAttributeRefactoring) ref;
                        for (UMLAttribute leftSide : mergeAttributeRefactoring.getMergedAttributes()) {
                            addAttributeRefactored(mergeAttributeRefactoring, parentCommitId, commitId, leftSide, mergeAttributeRefactoring.getNewAttribute());
                        }
                        break;
                    }
                    case REPLACE_ATTRIBUTE: {
                        ReplaceAttributeRefactoring replaceAttributeRefactoring = (ReplaceAttributeRefactoring) ref;
                        addAttributeRefactored(replaceAttributeRefactoring, parentCommitId, commitId, replaceAttributeRefactoring.getOriginalAttribute(), replaceAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case MOVE_RENAME_ATTRIBUTE: {
                        MoveAndRenameAttributeRefactoring moveAndRenameAttributeRefactoring = (MoveAndRenameAttributeRefactoring) ref;
                        addAttributeRefactored(moveAndRenameAttributeRefactoring, parentCommitId, commitId, moveAndRenameAttributeRefactoring.getOriginalAttribute(), moveAndRenameAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case ADD_ATTRIBUTE_MODIFIER: {
                        AddAttributeModifierRefactoring addAttributeModifierRefactoring = (AddAttributeModifierRefactoring) ref;
                        addAttributeRefactored(addAttributeModifierRefactoring, parentCommitId, commitId, addAttributeModifierRefactoring.getAttributeBefore(), addAttributeModifierRefactoring.getAttributeAfter());
                        break;
                    }
                    case REMOVE_ATTRIBUTE_MODIFIER: {
                        RemoveAttributeModifierRefactoring removeAttributeModifierRefactoring = (RemoveAttributeModifierRefactoring) ref;
                        addAttributeRefactored(removeAttributeModifierRefactoring, parentCommitId, commitId, removeAttributeModifierRefactoring.getAttributeBefore(), removeAttributeModifierRefactoring.getAttributeAfter());
                        break;
                    }
                    //=VARIABLE=========================================================================================
                    case RENAME_VARIABLE: {
                        RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) ref;
                        addVariableRefactored(renameVariableRefactoring, parentCommitId, commitId, renameVariableRefactoring.getOriginalVariable(), renameVariableRefactoring.getOperationBefore(), renameVariableRefactoring.getRenamedVariable(), renameVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_VARIABLE_TYPE: {
                        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                        addVariableRefactored(changeVariableTypeRefactoring, parentCommitId, commitId, changeVariableTypeRefactoring.getOriginalVariable(), changeVariableTypeRefactoring.getOperationBefore(), changeVariableTypeRefactoring.getChangedTypeVariable(), changeVariableTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case MERGE_VARIABLE: {
                        MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) ref;
                        for (VariableDeclaration mergedVariable : mergeVariableRefactoring.getMergedVariables()) {
                            addVariableRefactored(mergeVariableRefactoring, parentCommitId, commitId, mergedVariable, mergeVariableRefactoring.getOperationBefore(), mergeVariableRefactoring.getNewVariable(), mergeVariableRefactoring.getOperationAfter());
                        }
                        break;
                    }
                    case SPLIT_VARIABLE: {
                        SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) ref;
                        for (VariableDeclaration splitVariable : splitVariableRefactoring.getSplitVariables()) {
                            addVariableRefactored(splitVariableRefactoring, parentCommitId, commitId, splitVariableRefactoring.getOldVariable(), splitVariableRefactoring.getOperationBefore(), splitVariable, splitVariableRefactoring.getOperationAfter());
                        }
                        break;
                    }
                    case INLINE_VARIABLE: {
                        InlineVariableRefactoring inlineVariableRefactoring = (InlineVariableRefactoring) ref;
                        inlineVariableRefactoring.toString();
                        break;
                    }
                    case EXTRACT_VARIABLE: {
                        ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring) ref;
                        extractVariableRefactoring.toString();
                        break;
                    }
                    case ADD_VARIABLE_ANNOTATION: {
                        AddVariableAnnotationRefactoring addVariableAnnotationRefactoring = (AddVariableAnnotationRefactoring) ref;
                        addVariableRefactored(addVariableAnnotationRefactoring, parentCommitId, commitId, addVariableAnnotationRefactoring.getVariableBefore(), addVariableAnnotationRefactoring.getOperationBefore(), addVariableAnnotationRefactoring.getVariableAfter(), addVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case MODIFY_VARIABLE_ANNOTATION: {
                        ModifyVariableAnnotationRefactoring modifyVariableAnnotationRefactoring = (ModifyVariableAnnotationRefactoring) ref;
                        addVariableRefactored(modifyVariableAnnotationRefactoring, parentCommitId, commitId, modifyVariableAnnotationRefactoring.getVariableBefore(), modifyVariableAnnotationRefactoring.getOperationBefore(), modifyVariableAnnotationRefactoring.getVariableAfter(), modifyVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_VARIABLE_ANNOTATION: {
                        RemoveVariableAnnotationRefactoring removeVariableAnnotationRefactoring = (RemoveVariableAnnotationRefactoring) ref;
                        addVariableRefactored(removeVariableAnnotationRefactoring, parentCommitId, commitId, removeVariableAnnotationRefactoring.getVariableBefore(), removeVariableAnnotationRefactoring.getOperationBefore(), removeVariableAnnotationRefactoring.getVariableAfter(), removeVariableAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_VARIABLE_MODIFIER: {
                        AddVariableModifierRefactoring addVariableModifierRefactoring = (AddVariableModifierRefactoring) ref;
                        addVariableRefactored(addVariableModifierRefactoring, parentCommitId, commitId, addVariableModifierRefactoring.getVariableBefore(), addVariableModifierRefactoring.getOperationBefore(), addVariableModifierRefactoring.getVariableAfter(), addVariableModifierRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_VARIABLE_MODIFIER: {
                        RemoveVariableModifierRefactoring removeVariableModifierRefactoring = (RemoveVariableModifierRefactoring) ref;
                        addVariableRefactored(removeVariableModifierRefactoring, parentCommitId, commitId, removeVariableModifierRefactoring.getVariableBefore(), removeVariableModifierRefactoring.getOperationBefore(), removeVariableModifierRefactoring.getVariableAfter(), removeVariableModifierRefactoring.getOperationAfter());
                        break;
                    }
                    //==================================================================================================
                    case RENAME_PACKAGE: {
                        //All moved classes are reported in separate refactoring
                        RenamePackageRefactoring renamePackageRefactoring = (RenamePackageRefactoring) ref;
                        renamePackageRefactoring.toString();
                        break;
                    }
                    case EXTRACT_ATTRIBUTE:
                    case MERGE_OPERATION:
                    case CONVERT_ANONYMOUS_CLASS_TO_TYPE:
                    case INTRODUCE_POLYMORPHISM:
                    case REPLACE_VARIABLE_WITH_ATTRIBUTE:

                    default: {
                    }
                }
            }
        }
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        relatedRefactoringsToAttributes.clear();
        relatedRefactoringsToOperations.clear();

        List<Refactoring> classLevelRefactorings = refactorings.stream().filter(refactoring -> CLASS_LEVEL_REFACTORING.contains(refactoring.getRefactoringType())).collect(Collectors.toList());
        analyze(commitId, classLevelRefactorings);
        List<Refactoring> otherRefactorings = refactorings.stream().filter(refactoring -> !CLASS_LEVEL_REFACTORING.contains(refactoring.getRefactoringType())).collect(Collectors.toList());
        analyze(commitId, otherRefactorings);
    }


    @Override
    public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
        this.refactoringsCount = refactoringsCount;
        this.commitsCount = commitsCount;
        this.errorCommitsCount = errorCommitsCount;
    }

    @Override
    public void handleException(String commitId, Exception e) {

    }

    @Override
    public void handleExtraInfo(String commitId, UMLModelDiff umlModelDiff) {
        String parentCommitId = repository.getParentId(commitId);
        for (Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair : umlModelDiff.getMatchedVariables()) {
            handleMatchedVariable(commitId, parentCommitId, matchedVariablePair);
        }
        for (UMLClass removedClass : umlModelDiff.getRemovedClasses()) {
            handleRemovedClassChange(commitId, parentCommitId, removedClass);
        }
        List<UMLOperation> removedOperations = umlModelDiff.getRemovedOperations();

        List<UMLOperation> addedOperations = umlModelDiff.getAddedOperations();
        HashMap<String, UMLOperation> addedOperationMap = new HashMap<>();
        HashMap<String, Integer> addedOperationMapCount = new HashMap<>();
        for (UMLOperation addedOperation : addedOperations) {
            String key = String.format("%s%s#%s", Util.getPath(addedOperation.getLocationInfo().getFilePath(), addedOperation.getClassName()), addedOperation.getClassName(), addedOperation.getName());
            addedOperationMap.putIfAbsent(key, addedOperation);
            addedOperationMapCount.merge(key, 1, Integer::sum);
        }
        HashMap<String, UMLOperation> removedOperationMap = new HashMap<>();
        HashMap<String, Integer> removedOperationMapCount = new HashMap<>();
        for (UMLOperation removedOperation : removedOperations) {
            String key = String.format("%s%s#%s", Util.getPath(removedOperation.getLocationInfo().getFilePath(), removedOperation.getClassName()), removedOperation.getClassName(), removedOperation.getName());
            removedOperationMap.put(key, removedOperation);
            removedOperationMapCount.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<String, UMLOperation> entry : addedOperationMap.entrySet()) {
            String key = entry.getKey();
            if (addedOperationMapCount.get(key) > 1)
                continue;
            if (removedOperationMap.containsKey(key) && removedOperationMapCount.get(key) == 1) {
                UMLOperation leftOperation = removedOperationMap.get(key);
                UMLOperation rightOperation = entry.getValue();
                UMLOperationDiff umlOperationDiff = new UMLOperationDiff(leftOperation, rightOperation);
                analyze(commitId, umlOperationDiff.getRefactorings());
                if (checkOperationBodyChanged(leftOperation.getBody(), rightOperation.getBody())) {
                    addMethodChange(parentCommitId, commitId, leftOperation, rightOperation, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
                }
                if (checkOperationDocumentationChanged(leftOperation, rightOperation)) {
                    addMethodChange(parentCommitId, commitId, leftOperation, rightOperation, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("Some comments inside the body of the method element is changed."));
                }
                removedOperations.remove(leftOperation);
                addedOperations.remove(rightOperation);
            }
        }

        for (UMLOperation removedOperation : removedOperations) {
            handleRemovedMethod(commitId, parentCommitId, removedOperation);
        }
        for (UMLOperation addedOperation : addedOperations) {
            handleAddedMethod(commitId, parentCommitId, addedOperation);
        }
        for (UMLAttribute removedAttribute : umlModelDiff.getRemovedAttributes()) {
            handleRemovedAttribute(commitId, parentCommitId, removedAttribute);
        }
        for (Pair<VariableDeclaration, UMLOperation> removedVariable : umlModelDiff.getRemovedVariables()) {
            handleRemovedVariable(commitId, parentCommitId, removedVariable.getRight(), removedVariable.getLeft());
        }
        List<UMLClass> addedClasses = umlModelDiff.getAddedClasses();
        for (UMLClass addedClass : addedClasses) {
            handleAddedClassChange(commitId, parentCommitId, addedClass);
        }

        for (UMLAttribute addedAttribute : umlModelDiff.getAddedAttributes()) {
            handleAddedAttribute(commitId, parentCommitId, addedAttribute);
        }
        for (Pair<VariableDeclaration, UMLOperation> addedVariable : umlModelDiff.getAddedVariables()) {
            handleAddedVariable(commitId, parentCommitId, addedVariable.getRight(), addedVariable.getLeft());
        }
        Set<Pair<UMLOperation, UMLOperation>> changedBodyOperations = umlModelDiff.getChangedBodyOperations();
        for (Pair<UMLOperation, UMLOperation> changedBodyOperation : changedBodyOperations) {
            addMethodChange(parentCommitId, commitId, changedBodyOperation.getLeft(), changedBodyOperation.getRight(), ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
        }

        Set<Pair<UMLOperation, UMLOperation>> changedCommentOperations = umlModelDiff.getChangedCommentOperations();
        for (Pair<UMLOperation, UMLOperation> changedCommentOperation : changedCommentOperations) {
            addMethodChange(parentCommitId, commitId, changedCommentOperation.getLeft(), changedCommentOperation.getRight(), ChangeFactory.of(AbstractChange.Type.MODIFIED).description("Some comments inside the body of the method element is changed."));
        }
        for (UMLClassMoveDiff umlClassMoveDiff : umlModelDiff.getInnerClassMoveDiffList()) {
            MoveClassRefactoring moveClassRefactoring = new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass());
            UMLClass originalClass = moveClassRefactoring.getOriginalClass();
            UMLClass movedClass = moveClassRefactoring.getMovedClass();
            addClassRefactored(moveClassRefactoring, parentCommitId, commitId, originalClass, movedClass);
            String desc = umlClassMoveDiff.toString();
            for (UMLOperationBodyMapper umlOperationBodyMapper : umlClassMoveDiff.getOperationBodyMapperList()) {
                addMethodChange(parentCommitId, commitId, umlOperationBodyMapper.getOperation1(), umlOperationBodyMapper.getOperation2(), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).description(desc));
            }
        }
        methodChangeHistory.connectRelatedNodes();
        classChangeHistory.connectRelatedNodes();
        attributeChangeHistory.connectRelatedNodes();
        variableChangeHistory.connectRelatedNodes();

    }

    public void handleMatchedVariable(String commitId, String parentCommitId, Pair<Pair<VariableDeclaration, UMLOperation>, Pair<VariableDeclaration, UMLOperation>> matchedVariablePair) {
        VariableDeclaration leftSideVariable = matchedVariablePair.getLeft().getLeft();
        UMLOperation leftSideOperation = matchedVariablePair.getLeft().getRight();

        VariableDeclaration rightSideVariable = matchedVariablePair.getRight().getLeft();
        UMLOperation rightSideOperation = matchedVariablePair.getRight().getRight();

        variableChangeHistory.addChange(getVariable(leftSideVariable, leftSideOperation, parentCommitId), getVariable(rightSideVariable, rightSideOperation, commitId), ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
    }

    private void handleRemovedMethod(String commitId, String parentCommitId, UMLOperation operation) {
        Method leftSideMethod = getMethod(parentCommitId, operation);
        Method rightSideMethod = getMethod(commitId, operation);
        methodChangeHistory.handleRemoved(leftSideMethod, rightSideMethod);
        if (operation.getBody() != null) {
            for (VariableDeclaration variableDeclaration : operation.getBody().getAllVariableDeclarations()) {
                handleRemovedVariable(commitId, parentCommitId, operation, variableDeclaration);
            }
        }
    }

    public void handleAddedMethod(String commitId, String parentCommitId, UMLOperation operation) {
        Method leftSideMethod = getMethod(parentCommitId, operation);
        Method rightSideMethod = getMethod(commitId, operation);
        methodChangeHistory.handleAdd(leftSideMethod, rightSideMethod);
        if (operation.getBody() != null) {
            for (VariableDeclaration variableDeclaration : operation.getBody().getAllVariableDeclarations()) {
                handleAddedVariable(commitId, parentCommitId, operation, variableDeclaration);
            }
        }
    }

    public void handleRemovedVariable(String commitId, String parentCommitId, UMLOperation operation, VariableDeclaration variableDeclaration) {
        Variable leftSideVariable = getVariable(variableDeclaration, operation, parentCommitId);
        Variable rightSideVariable = getVariable(variableDeclaration, operation, commitId);
        variableChangeHistory.handleRemoved(leftSideVariable, rightSideVariable);
    }

    public void handleAddedVariable(String commitId, String parentCommitId, UMLOperation operation, VariableDeclaration variableDeclaration) {
        Variable leftSideVariable = getVariable(variableDeclaration, operation, parentCommitId);
        Variable rightSideVariable = getVariable(variableDeclaration, operation, commitId);
        variableChangeHistory.handleAdd(leftSideVariable, rightSideVariable);
    }

    private void handleRemovedClassChange(String commitId, String parentCommitId, UMLClass removedClass) {
        Class leftSideClass = getClass(parentCommitId, removedClass);
        Class rightSideClass = getClass(commitId, removedClass);
        classChangeHistory.handleRemoved(leftSideClass, rightSideClass);
        for (UMLOperation operation : removedClass.getOperations()) {
            handleRemovedMethod(commitId, parentCommitId, operation);
        }
        for (UMLAttribute attribute : removedClass.getAttributes()) {
            handleRemovedAttribute(commitId, parentCommitId, attribute);
        }
    }

    private void handleAddedClassChange(String commitId, String parentCommitId, UMLClass addedClass) {
        Class leftSideClass = getClass(parentCommitId, addedClass);
        Class rightSideClass = getClass(commitId, addedClass);
        classChangeHistory.handleAdd(leftSideClass, rightSideClass);
        for (UMLOperation operation : addedClass.getOperations()) {
            handleAddedMethod(commitId, parentCommitId, operation);
        }
        for (UMLAttribute attribute : addedClass.getAttributes()) {
            handleAddedAttribute(commitId, parentCommitId, attribute);
        }
    }

    private void handleRemovedAttribute(String commitId, String parentCommitId, UMLAttribute attribute) {
        Attribute leftSideAttribute = getAttributeElement(parentCommitId, attribute);
        Attribute rightSideAttribute = getAttributeElement(commitId, attribute);
        attributeChangeHistory.handleRemoved(leftSideAttribute, rightSideAttribute);
    }

    private void handleAddedAttribute(String commitId, String parentCommitId, UMLAttribute attribute) {
        Attribute leftSideAttribute = getAttributeElement(parentCommitId, attribute);
        Attribute rightSideAttribute = getAttributeElement(commitId, attribute);
        attributeChangeHistory.handleAdd(leftSideAttribute, rightSideAttribute);
    }

    private void matchVariables(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        for (VariableDeclaration variableBefore : leftSideOperation.getAllVariableDeclarations())
            for (VariableDeclaration variableAfter : rightSideOperation.getAllVariableDeclarations())
                if (variableBefore.equals(variableAfter))
                    variableChangeHistory.addChange(getVariable(variableBefore, leftSideOperation, parentCommitId), getVariable(variableAfter, rightSideOperation, childCommitId), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
    }

    private void matchAttributes(Refactoring ref, String parentCommitId, String childCommitId, List<UMLAttribute> leftSide, List<UMLAttribute> rightSide) {
        for (UMLAttribute attributeBefore : leftSide)
            for (UMLAttribute attributeAfter : rightSide)
                if (attributeBefore.getName().equals(attributeAfter.getName())) {
                    attributeChangeHistory.addChange(getAttributeElement(parentCommitId, attributeBefore), getAttributeElement(childCommitId, attributeAfter), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
                    break;
                }
    }

    public void matchOperations(Refactoring ref, String parentCommitId, String commitId, List<UMLOperation> leftSide, List<UMLOperation> rightSide) {
        Set<UMLOperation> leftMatched = new HashSet<>();
        Set<UMLOperation> rightMatched = new HashSet<>();
        matchOperation(ref, parentCommitId, commitId, leftSide, rightSide, leftMatched, rightMatched);
    }

    private void matchOperation(Refactoring ref, String parentCommitId, String commitId, List<UMLOperation> leftSide, List<UMLOperation> rightSide, Set<UMLOperation> leftMatched, Set<UMLOperation> rightMatched) {
        for (UMLOperation leftOperation : leftSide) {
            if (leftMatched.contains(leftOperation))
                continue;
//            Method leftMethod = Method.of(leftOperation, null);
            for (UMLOperation rightOperation : rightSide) {
                if (rightMatched.contains(rightOperation))
                    continue;
//                Method rightMethod = Method.of(rightOperation, null);
//                String leftMethodIdentifier = containsBody ? leftMethod.getIdentifierExcludeVersion() : leftMethod.getIdentifierExcludeVersionAndBody();
//                String rightIdentifier = containsBody ? rightMethod.getIdentifierExcludeVersion() : rightMethod.getIdentifierExcludeVersionAndBody();
//                if (leftMethodIdentifier
//                        .replace(Util.getPath(leftOperation.getLocationInfo().getFilePath(), leftOperation.getClassName()), Util.getPath(rightOperation.getLocationInfo().getFilePath(), rightOperation.getClassName()))
//                        .replace(leftOperation.getClassName(), rightOperation.getClassName())
//                        .equals(rightIdentifier)) {
                if (leftOperation.equalSignature(rightOperation)) {
                    addMethodChange(parentCommitId, commitId, leftOperation, rightOperation, ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
                    if (checkOperationBodyChanged(leftOperation.getBody(), rightOperation.getBody())) {
                        addMethodChange(parentCommitId, commitId, leftOperation, rightOperation, ChangeFactory.of(AbstractChange.Type.MODIFIED).description("The body of the method element is changed."));
                    }
                    leftMatched.add(leftOperation);
                    rightMatched.add(rightOperation);
                    break;
                }
            }
        }
    }

    public List<Refactoring> getRefactorings() {
        return refactorings;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public Set<String> getFiles() {
        return files;
    }

    public void setTrackVariables(boolean trackVariables) {
        this.trackVariables = trackVariables;
    }

    public void setTrackClasses(boolean trackClasses) {
        this.trackClasses = trackClasses;
    }

    public void setTrackMethods(boolean trackMethods) {
        this.trackMethods = trackMethods;
    }

    public void setTrackAttributes(boolean trackAttributes) {
        this.trackAttributes = trackAttributes;
    }
}
