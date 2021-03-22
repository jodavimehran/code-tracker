package org.refactoringrefiner;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.edge.*;
import org.refactoringrefiner.element.*;
import org.refactoringrefiner.element.Class;
import org.refactoringrefiner.util.*;

import java.util.*;

public class RefactoringHandlerImpl extends RefactoringHandler {
    private final MutableValueGraph<CodeElement, Edge> attributeChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final MutableValueGraph<CodeElement, Edge> classChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final MutableValueGraph<CodeElement, Edge> methodChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    private final MutableValueGraph<CodeElement, Edge> variableChangeHistoryGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    private final List<Refactoring> refactorings = new ArrayList<>();
    private final IRepository repository;
    //file -> class -> method
    private final HashMap<String, Method> methodElements = new HashMap<>();
    private final HashMap<String, Method> methodElementsByIdentifier = new HashMap<>();
    //filters:
    private final Set<String> files = new HashSet<>();
    private final Set<String> classNames = new HashSet<>();
    private final Set<String> methodNames = new HashSet<>();
    private boolean trackVariables = true;
    private boolean trackClasses = true;
    private boolean trackMethods = true;
    private boolean trackAttributes = true;
    private int refactoringsCount, commitsCount, errorCommitsCount;

    public RefactoringHandlerImpl(IRepository repository) {
        this.repository = repository;
    }

    private static ImmutableValueGraph<CodeElement, Edge> getGraph(MutableValueGraph<CodeElement, Edge> graph) {
        HashMap<String, Set<CodeElement>> leafElements = new HashMap<>();
        HashMap<String, Set<CodeElement>> rootElement = new HashMap<>();
        for (EndpointPair<CodeElement> edge : graph.edges()) {
            CodeElement source = edge.source();
            if (graph.predecessors(source).isEmpty() && !source.isAdded())
                addCodeElementToMap(source, rootElement);

            CodeElement target = edge.target();
            if (graph.successors(target).isEmpty() && !target.isRemoved())
                addCodeElementToMap(target, leafElements);
        }
        for (Map.Entry<String, Set<CodeElement>> leafEntry : leafElements.entrySet()) {
            if (!rootElement.containsKey(leafEntry.getKey())) {
                continue;
            }
            List<CodeElement> leafCodeElementsList = new ArrayList<>(leafEntry.getValue());
            Collections.sort(leafCodeElementsList, (o1, o2) -> Long.compare(o2.getVersion().getTime(), o1.getVersion().getTime()));
            Set<CodeElement> rootCodeElements = rootElement.get(leafEntry.getKey());
            for (CodeElement leafCodeElement : leafCodeElementsList) {
                List<CodeElement> matched = new ArrayList<>();
                for (CodeElement rootCodeElement : rootCodeElements) {
                    if (!rootCodeElement.getVersion().getId().equals(leafCodeElement.getVersion().getId()) && rootCodeElement.getVersion().getTime() >= leafCodeElement.getVersion().getTime()) {
                        matched.add(rootCodeElement);
                    }
                }
                if (!matched.isEmpty()) {
                    Collections.sort(matched, Comparator.comparingLong(o -> o.getVersion().getTime()));
                    graph.putEdgeValue(leafCodeElement, matched.get(0), ChangeFactory.of(AbstractChange.Type.NO_CHANGE).asEdge());
                    rootCodeElements.remove(matched.get(0));
                }
            }
        }
        return ImmutableValueGraph.copyOf(graph);
    }

    private static void addCodeElementToMap(CodeElement codeElement, HashMap<String, Set<CodeElement>> elementsMap) {
        Set<CodeElement> codeElements;
        if (elementsMap.containsKey(codeElement.getIdentifierExcludeVersion())) {
            codeElements = elementsMap.get(codeElement.getIdentifierExcludeVersion());
        } else {
            codeElements = new HashSet<>();
            elementsMap.put(codeElement.getIdentifierExcludeVersion(), codeElements);
        }
        codeElements.add(codeElement);
    }

    private static void handleRemove(MutableValueGraph<CodeElement, Edge> graph, BaseCodeElement leftSide, BaseCodeElement rightSide) {
        if (leftSide == null || rightSide == null)
            return;
        rightSide.setRemoved(true);
        addChange(graph, leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.REMOVED).codeElement(leftSide));
    }

    private static void handleAdd(MutableValueGraph<CodeElement, Edge> graph, BaseCodeElement leftSide, BaseCodeElement rightSide) {
        if (leftSide == null || rightSide == null)
            return;
        leftSide.setAdded(true);
        addChange(graph, leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.ADDED).codeElement(rightSide));
    }

    private static void addRefactored(MutableValueGraph<CodeElement, Edge> graph, CodeElement leftSide, CodeElement rightSide, Refactoring refactoring) {
        addChange(graph, leftSide, rightSide, ChangeFactory.of(AbstractChange.Type.REFACTORED).refactoring(refactoring));
    }

    private static void addChange(MutableValueGraph<CodeElement, Edge> graph, CodeElement leftSide, CodeElement rightSide, ChangeFactory changeFactory) {
        if (leftSide == null || rightSide == null)
            return;
        if (leftSide.equals(rightSide))
            return;
        Optional<Edge> edgeValue = graph.edgeValue(leftSide, rightSide);
        if (edgeValue.isPresent()) {
            EdgeImpl edge = (EdgeImpl) edgeValue.get();
            edge.addChange(changeFactory.build());
        } else {
            graph.putEdgeValue(leftSide, rightSide, changeFactory.asEdge());
        }
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
        addChange(methodChangeHistoryGraph, leftSideMethod, rightSideMethod, changeFactory);
    }

    public int getNumberOfEdge() {
        return attributeChangeHistoryGraph.edges().size() + classChangeHistoryGraph.edges().size() + methodChangeHistoryGraph.edges().size() + variableChangeHistoryGraph.edges().size();
    }

    public ImmutableValueGraph<CodeElement, Edge> getAttributeChangeHistoryGraph() {
        return getGraph(attributeChangeHistoryGraph);
    }

    public ImmutableValueGraph<CodeElement, Edge> getClassChangeHistoryGraph() {
        return getGraph(classChangeHistoryGraph);
    }

    public ImmutableValueGraph<CodeElement, Edge> getMethodChangeHistoryGraph() {
        return getGraph(methodChangeHistoryGraph);
    }

    public ImmutableValueGraph<CodeElement, Edge> getVariableChangeHistoryGraph() {
        return getGraph(variableChangeHistoryGraph);
    }

    private Class getClass(String commitId, UMLClass umlClass) {
        if (!trackClasses)
            return null;
        if (!files.isEmpty() && !files.contains(umlClass.getLocationInfo().getFilePath()))
            return null;
        return new Class(umlClass, getVersion(commitId));
    }

    private Method getMethod(String commitId, UMLOperation umlOperation) {
        Method method = new Method(umlOperation, getVersion(commitId));
        String identifier = method.getIdentifier();
        if (methodElementsByIdentifier.containsKey(identifier)) {
            method = methodElementsByIdentifier.get(identifier);
        } else {
            methodElementsByIdentifier.put(identifier, method);
        }

        methodElements.put(String.format("%s>%s", umlOperation.getLocationInfo().getFilePath(), umlOperation.getKey()), method);
        methodChangeHistoryGraph.addNode(method);
        return method;
    }

    private VersionImpl getVersion(String commitId) {
        return new VersionImpl(commitId, repository.getCommitTime(commitId));
    }

    private Variable getVariable(VariableDeclaration variableDeclaration, UMLOperation umlOperation, String commitId) {
        if (!trackVariables)
            return null;
        if (!files.isEmpty() && !files.contains(umlOperation.getLocationInfo().getFilePath()))
            return null;
        return new Variable(variableDeclaration, umlOperation, getVersion(commitId));
    }

    private Attribute getAttributeElement(String commitId, UMLAttribute attribute) {
        if (!trackAttributes)
            return null;
        return new Attribute(attribute, getVersion(commitId));
    }

    private void addOperationRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        if (!trackMethods)
            return;
        if (!files.isEmpty() && !(files.contains(leftSideOperation.getLocationInfo().getFilePath()) || files.contains(rightSideOperation.getLocationInfo().getFilePath())))
            return;
        Method leftSideMethod = getMethod(parentCommitId, leftSideOperation);
        Method rightSideMethod = getMethod(childCommitId, rightSideOperation);

        if (!leftSideMethod.equals(rightSideMethod)) {
            addRefactored(methodChangeHistoryGraph, leftSideMethod, rightSideMethod, ref);
            matchVariables(ref, parentCommitId, childCommitId, leftSideOperation, rightSideOperation);
        }
    }

    private void addAttributeRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLAttribute leftSideAttribute, UMLAttribute rightSideAttribute) {
        Attribute leftSideAttributeElement = getAttributeElement(parentCommitId, leftSideAttribute);
        Attribute rightSideAttributeElement = getAttributeElement(childCommitId, rightSideAttribute);

        addRefactored(attributeChangeHistoryGraph, leftSideAttributeElement, rightSideAttributeElement, ref);
    }

    private void addVariableRefactored(Refactoring ref, String parentCommitId, String childCommitId, VariableDeclaration leftSideVariable, UMLOperation leftSideOperation, VariableDeclaration rightSideVariable, UMLOperation rightSideOperation) {
        Variable leftSideAttributeElement = getVariable(leftSideVariable, leftSideOperation, parentCommitId);
        Variable rightSideAttributeElement = getVariable(rightSideVariable, rightSideOperation, childCommitId);

        addRefactored(variableChangeHistoryGraph, leftSideAttributeElement, rightSideAttributeElement, ref);
    }

    private void addClassRefactored(Refactoring ref, String parentCommitId, String childCommitId, UMLClass leftSideClass, UMLClass rightSideClass) {
        Class leftSideClassElement = getClass(parentCommitId, leftSideClass);
        Class rightSideClassElement = getClass(childCommitId, rightSideClass);

        addRefactored(classChangeHistoryGraph, leftSideClassElement, rightSideClassElement, ref);
    }

    private void analyze(String commitId, List<Refactoring> refactorings) {
        this.refactorings.addAll(refactorings);
        String parentCommitId = repository.getParentId(commitId);
        if (refactorings != null && !refactorings.isEmpty()) {
            for (Refactoring ref : refactorings) {
                RefactoringType refactoringType = ref.getRefactoringType();
                switch (refactoringType) {
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
                        addOperationRefactored(moveOperationRefactoring, parentCommitId, commitId, moveOperationRefactoring.getOriginalOperation(), moveOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_INLINE_OPERATION:
                    case INLINE_OPERATION: {
                        InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) ref;
                        UMLOperation inlinedOperation = inlineOperationRefactoring.getInlinedOperation();
                        UMLOperation operationAfterInline = inlineOperationRefactoring.getTargetOperationAfterInline();
                        UMLOperation operationBeforeInline = inlineOperationRefactoring.getTargetOperationBeforeInline();

                        addMethodChange(parentCommitId, commitId, inlinedOperation, inlinedOperation, ChangeFactory.of(AbstractChange.Type.INLINED).refactoring(inlineOperationRefactoring));
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
                        addMethodChange(parentCommitId, commitId, operationBeforeExtraction, operationAfterExtraction, ChangeFactory.of(AbstractChange.Type.MODIFIED));
                        break;
                    }
                    case CHANGE_RETURN_TYPE: {
                        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) ref;
                        addOperationRefactored(changeReturnTypeRefactoring, parentCommitId, commitId, changeReturnTypeRefactoring.getOperationBefore(), changeReturnTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_METHOD: {
                        RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) ref;
                        addOperationRefactored(renameOperationRefactoring, parentCommitId, commitId, renameOperationRefactoring.getOriginalOperation(), renameOperationRefactoring.getRenamedOperation());
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
                    case EXTRACT_INTERFACE:
                    case EXTRACT_SUPERCLASS: {
                        //TODO: Change ExtractSuperclassRefactoring in a way that contains the information of original class before and after extraction
                        ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                        UMLClass extractedClass = extractSuperclassRefactoring.getExtractedClass();
                        Class leftSideExtractedClass = getClass(parentCommitId, extractedClass);
                        Class rightSideExtractedClass = getClass(commitId, extractedClass);
                        if (leftSideExtractedClass != null)
                            leftSideExtractedClass.setAdded(true);
                        addChange(classChangeHistoryGraph, leftSideExtractedClass, rightSideExtractedClass, ChangeFactory.of(AbstractChange.Type.EXTRACTED).refactoring(extractSuperclassRefactoring).codeElement(rightSideExtractedClass));
                        for (UMLClass originalClass : extractSuperclassRefactoring.getUMLSubclassSet()) {
                            addChange(classChangeHistoryGraph, getClass(parentCommitId, originalClass), getClass(commitId, originalClass), ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(extractSuperclassRefactoring));
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
                        addChange(classChangeHistoryGraph, leftSideExtractedClass, rightSideExtractedClass, ChangeFactory.of(AbstractChange.Type.EXTRACTED).refactoring(extractClassRefactoring).codeElement(rightSideExtractedClass));
                        addChange(classChangeHistoryGraph, leftSideSourceClass, rightSideSourceClass, ChangeFactory.of(AbstractChange.Type.MODIFIED).refactoring(extractClassRefactoring));
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
                    case RENAME_PACKAGE: {
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
        analyze(commitId, refactorings);
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
        for (VariableDeclarationReplacement variableDeclarationReplacement : umlModelDiff.getChangedScopeVariable()) {
            VariableDeclaration leftSideVariable = variableDeclarationReplacement.getVariableDeclaration1();
            UMLOperation leftSideOperation = variableDeclarationReplacement.getOperation1();

            VariableDeclaration rightSideVariable = variableDeclarationReplacement.getVariableDeclaration2();
            UMLOperation rightSideOperation = variableDeclarationReplacement.getOperation2();

            addChange(variableChangeHistoryGraph, getVariable(leftSideVariable, leftSideOperation, parentCommitId), getVariable(rightSideVariable, rightSideOperation, commitId), ChangeFactory.of(AbstractChange.Type.MODIFIED));
        }
        for (UMLClass removedClass : umlModelDiff.getRemovedClasses()) {
            handleRemovedClassChange(commitId, parentCommitId, removedClass);
        }
        List<UMLOperation> removedOperations = umlModelDiff.getRemovedOperations();
        for (UMLOperation removedOperation : removedOperations) {
            handleRemovedMethod(commitId, parentCommitId, removedOperation);
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
        List<UMLOperation> addedOperations = umlModelDiff.getAddedOperations();
        for (UMLOperation addedOperation : addedOperations) {
            handleAddedMethod(commitId, parentCommitId, addedOperation);
        }
        for (UMLAttribute addedAttribute : umlModelDiff.getAddedAttributes()) {
            handleAddedAttribute(commitId, parentCommitId, addedAttribute);
        }
        for (Pair<VariableDeclaration, UMLOperation> addedVariable : umlModelDiff.getAddedVariables()) {
            handleAddedVariable(commitId, parentCommitId, addedVariable.getRight(), addedVariable.getLeft());
        }
        Set<Pair<UMLOperation, UMLOperation>> changedBodyOperations = umlModelDiff.getChangedBodyOperations();
        for (Pair<UMLOperation, UMLOperation> changedBodyOperation : changedBodyOperations) {
            addMethodChange(parentCommitId, commitId, changedBodyOperation.getLeft(), changedBodyOperation.getRight(), ChangeFactory.of(AbstractChange.Type.MODIFIED));
        }
    }

    private void handleExtractClass() {

    }

    private void handleRemovedMethod(String commitId, String parentCommitId, UMLOperation operation) {
        Method leftSideMethod = getMethod(parentCommitId, operation);
        Method rightSideMethod = getMethod(commitId, operation);
        if (!methodChangeHistoryGraph.successors(leftSideMethod).isEmpty())
            return;
        handleRemove(methodChangeHistoryGraph, leftSideMethod, rightSideMethod);
        if (operation.getBody() != null) {
            for (VariableDeclaration variableDeclaration : operation.getBody().getAllVariableDeclarations()) {
                handleRemovedVariable(commitId, parentCommitId, operation, variableDeclaration);
            }
        }
    }

    private void handleAddedMethod(String commitId, String parentCommitId, UMLOperation operation) {
        Method leftSideMethod = getMethod(parentCommitId, operation);
        Method rightSideMethod = getMethod(commitId, operation);
        if (!methodChangeHistoryGraph.predecessors(rightSideMethod).isEmpty())
            return;
        handleAdd(methodChangeHistoryGraph, leftSideMethod, rightSideMethod);
        if (operation.getBody() != null) {
            for (VariableDeclaration variableDeclaration : operation.getBody().getAllVariableDeclarations()) {
                handleAddedVariable(commitId, parentCommitId, operation, variableDeclaration);
            }
        }
    }

    private void handleRemovedVariable(String commitId, String parentCommitId, UMLOperation operation, VariableDeclaration variableDeclaration) {
        Variable leftSideVariable = getVariable(variableDeclaration, operation, parentCommitId);
        Variable rightSideVariable = getVariable(variableDeclaration, operation, commitId);
        handleRemove(variableChangeHistoryGraph, leftSideVariable, rightSideVariable);
    }

    private void handleAddedVariable(String commitId, String parentCommitId, UMLOperation operation, VariableDeclaration variableDeclaration) {
        Variable leftSideVariable = getVariable(variableDeclaration, operation, parentCommitId);
        Variable rightSideVariable = getVariable(variableDeclaration, operation, commitId);
        handleAdd(variableChangeHistoryGraph, leftSideVariable, rightSideVariable);
    }

    private void handleRemovedClassChange(String commitId, String parentCommitId, UMLClass removedClass) {
        Class leftSideClass = getClass(parentCommitId, removedClass);
        Class rightSideClass = getClass(commitId, removedClass);
        handleRemove(classChangeHistoryGraph, leftSideClass, rightSideClass);
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
        handleAdd(classChangeHistoryGraph, leftSideClass, rightSideClass);
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
        handleRemove(attributeChangeHistoryGraph, leftSideAttribute, rightSideAttribute);
    }

    private void handleAddedAttribute(String commitId, String parentCommitId, UMLAttribute attribute) {
        Attribute leftSideAttribute = getAttributeElement(parentCommitId, attribute);
        Attribute rightSideAttribute = getAttributeElement(commitId, attribute);
        handleAdd(attributeChangeHistoryGraph, leftSideAttribute, rightSideAttribute);
    }

    private void matchVariables(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        for (VariableDeclaration variableBefore : leftSideOperation.getAllVariableDeclarations())
            for (VariableDeclaration variableAfter : rightSideOperation.getAllVariableDeclarations())
                if (variableBefore.equals(variableAfter))
                    addChange(variableChangeHistoryGraph, getVariable(variableBefore, leftSideOperation, parentCommitId), getVariable(variableAfter, rightSideOperation, childCommitId), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
    }

    private void matchAttributes(Refactoring ref, String parentCommitId, String childCommitId, List<UMLAttribute> leftSide, List<UMLAttribute> rightSide) {
        for (UMLAttribute attributeBefore : leftSide)
            for (UMLAttribute attributeAfter : rightSide)
                if (attributeBefore.getName().equals(attributeAfter.getName()))
                    addChange(attributeChangeHistoryGraph, getAttributeElement(parentCommitId, attributeBefore), getAttributeElement(childCommitId, attributeAfter), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
    }

    private void matchOperations(Refactoring ref, String parentCommitId, String childCommitId, List<UMLOperation> leftSide, List<UMLOperation> rightSide) {
        Map<UMLOperation, List<UMLOperation>> matched = new HashMap<>();
        for (UMLOperation operationBefore : leftSide) {
            ArrayList<UMLOperation> maybe = new ArrayList<>();
            matched.put(operationBefore, maybe);
            for (UMLOperation operationAfter : rightSide) {
                if (operationBefore.getName().equals(operationAfter.getName())) {
                    maybe.add(operationAfter);
                }
            }
        }
        for (Map.Entry<UMLOperation, List<UMLOperation>> entry : matched.entrySet()) {
            if (entry.getValue().size() == 1) {
                addMethodChange(parentCommitId, childCommitId, entry.getKey(), entry.getValue().get(0), ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
            } else {
                for (UMLOperation operationAfter : entry.getValue()) {
                    if (entry.getKey().equalSignature(operationAfter)) {
                        addMethodChange(parentCommitId, childCommitId, entry.getKey(), operationAfter, ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).refactoring(ref));
                    }
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

    public HashMap<String, Method> getMethodElements() {
        return methodElements;
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
