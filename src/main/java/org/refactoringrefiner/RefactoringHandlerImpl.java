package org.refactoringrefiner;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.element.AttributeElement;
import org.refactoringrefiner.element.ClassElement;
import org.refactoringrefiner.element.MethodElement;

import java.util.*;
import java.util.function.Function;

public class RefactoringHandlerImpl extends RefactoringHandler {
    private final Function<String, String> parentCommitIdResolver;
    private final Function<String, Integer> commitTimeResolver;

    private final MutableValueGraph<CodeElement, Edge> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    private final MetaInfoImpl metaInfo = new MetaInfoImpl();

    public RefactoringHandlerImpl(Function<String, String> parentCommitIdResolver, Function<String, Integer> commitTimeResolver) {
        this.parentCommitIdResolver = parentCommitIdResolver;
        this.commitTimeResolver = commitTimeResolver;
    }

    public ImmutableValueGraph<CodeElement, Edge> getGraph() {
        HashMap<String, Set<CodeElement>> leafElements = new HashMap<>();
        HashMap<String, Set<CodeElement>> rootElement = new HashMap<>();
        for (EndpointPair<CodeElement> edge : graph.edges()) {
            if (graph.predecessors(edge.source()).isEmpty())
                addCodeElementToMap(edge.source(), rootElement);

            if (graph.successors(edge.target()).isEmpty())
                addCodeElementToMap(edge.target(), leafElements);
        }
        for (Map.Entry<String, Set<CodeElement>> leafEntry : leafElements.entrySet()) {
            if (!rootElement.containsKey(leafEntry.getKey())) {
                continue;
            }
            List<CodeElement> leafCodeElementsList = new ArrayList<>(leafEntry.getValue());
            Collections.sort(leafCodeElementsList, (o1, o2) -> Integer.compare(commitTimeResolver.apply(o2.getVersion().getId()), commitTimeResolver.apply(o1.getVersion().getId())));
            Set<CodeElement> rootCodeElements = rootElement.get(leafEntry.getKey());
            for (CodeElement leafCodeElement : leafCodeElementsList) {
                List<CodeElement> matched = new ArrayList<>();
                for (CodeElement rootCodeElement : rootCodeElements) {
                    if (commitTimeResolver.apply(rootCodeElement.getVersion().getId()) > commitTimeResolver.apply(leafCodeElement.getVersion().getId())) {
                        matched.add(rootCodeElement);
                    }
                }
                if (!matched.isEmpty()) {
                    Collections.sort(matched, Comparator.comparingInt(o -> commitTimeResolver.apply(o.getVersion().getId())));
                    graph.putEdgeValue(leafCodeElement, matched.get(0), RefactoringEdge.of(null, null));
                    rootCodeElements.remove(matched.get(0));
                }
            }
        }
        return ImmutableValueGraph.copyOf(graph);
    }

    private void addCodeElementToMap(CodeElement codeElement, HashMap<String, Set<CodeElement>> elementsMap) {
        Set<CodeElement> codeElements;
        if (elementsMap.containsKey(codeElement.toString())) {
            codeElements = elementsMap.get(codeElement.toString());
        } else {
            codeElements = new HashSet<>();
            elementsMap.put(codeElement.toString(), codeElements);
        }
        codeElements.add(codeElement);
    }

    public MetaInfoImpl getMetaInfo() {
        return metaInfo;
    }

    private void addOperationChange(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        addOperationChange(ref, parentCommitId, childCommitId, leftSideOperation, rightSideOperation, true);
    }

    private void addOperationChange(Refactoring ref, String parentCommitId, String childCommitId, UMLOperation leftSideOperation, UMLOperation rightSideOperation, boolean isActualRefactoring) {
        MethodElement leftSideMethodElement = new MethodElement(leftSideOperation, new VersionImpl(parentCommitId, commitTimeResolver.apply(parentCommitId)));
        MethodElement rightSideMethodElement = new MethodElement(rightSideOperation, new VersionImpl(childCommitId, commitTimeResolver.apply(childCommitId)));
        if (!leftSideMethodElement.equals(rightSideMethodElement))
            addChange(leftSideMethodElement, rightSideMethodElement, ref, childCommitId, isActualRefactoring);
    }

    private void addAttributeChange(Refactoring ref, String parentCommitId, String childCommitId, UMLAttribute leftSideAttribute, UMLAttribute rightSideAttribute) {
        addAttributeChange(ref, parentCommitId, childCommitId, leftSideAttribute, rightSideAttribute, true);
    }

    private void addAttributeChange(Refactoring ref, String parentCommitId, String childCommitId, UMLAttribute leftSideAttribute, UMLAttribute rightSideAttribute, boolean isActualRefactoring) {
        AttributeElement leftSideAttributeElement = new AttributeElement(leftSideAttribute, new VersionImpl(parentCommitId, commitTimeResolver.apply(parentCommitId)));
        AttributeElement rightSideAttributeElement = new AttributeElement(rightSideAttribute, new VersionImpl(childCommitId, commitTimeResolver.apply(childCommitId)));

        addChange(leftSideAttributeElement, rightSideAttributeElement, ref, childCommitId, isActualRefactoring);
    }

    private void addAttributeChange(Refactoring ref, String parentCommitId, String childCommitId, VariableDeclaration leftSideAttribute, String leftSideClassName, VariableDeclaration rightSideAttribute, String rightSideClassName) {
        addAttributeChange(ref, parentCommitId, childCommitId, leftSideAttribute, leftSideClassName, rightSideAttribute, rightSideClassName, true);
    }

    private void addAttributeChange(Refactoring ref, String parentCommitId, String childCommitId, VariableDeclaration leftSideAttribute, String leftSideClassName, VariableDeclaration rightSideAttribute, String rightSideClassName, boolean isActualRefactoring) {
        AttributeElement leftSideAttributeElement = new AttributeElement(leftSideAttribute, leftSideClassName, new VersionImpl(parentCommitId, commitTimeResolver.apply(parentCommitId)));
        AttributeElement rightSideAttributeElement = new AttributeElement(rightSideAttribute, rightSideClassName, new VersionImpl(childCommitId, commitTimeResolver.apply(childCommitId)));

        addChange(leftSideAttributeElement, rightSideAttributeElement, ref, childCommitId, isActualRefactoring);
    }

    private void addClassChange(Refactoring ref, String parentCommitId, String childCommitId, UMLClass leftSideClass, UMLClass rightSideClass) {
        ClassElement leftSideClassElement = new ClassElement(leftSideClass, new VersionImpl(parentCommitId, commitTimeResolver.apply(parentCommitId)));
        ClassElement rightSideClassElement = new ClassElement(rightSideClass, new VersionImpl(childCommitId, commitTimeResolver.apply(childCommitId)));

        addChange(leftSideClassElement, rightSideClassElement, ref, childCommitId, true);
    }

    private void addChange(CodeElement leftSide, CodeElement rightSide, Refactoring ref, String commitId, boolean isActualRefactoring) {
        Optional<Edge> edgeValue = graph.edgeValue(leftSide, rightSide);
        if (edgeValue.isPresent()) {
            Edge edge = edgeValue.get();
            edge.addRefactoring(ref, isActualRefactoring);
        } else {
            Edge edge = RefactoringEdge.of(commitId, ref);
            graph.putEdgeValue(leftSide, rightSide, edge);
        }
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        analyze(commitId, refactorings);
    }

    private void analyze(String commitId, List<Refactoring> refactorings) {
        String parentCommitId = parentCommitIdResolver.apply(commitId);
        if (refactorings != null && !refactorings.isEmpty()) {
            metaInfo.addCommitTime(commitTimeResolver.apply(commitId));
            for (Refactoring ref : refactorings) {
                RefactoringType refactoringType = ref.getRefactoringType();
                metaInfo.addType(refactoringType);
                switch (refactoringType) {
                    case SPLIT_PARAMETER: {
                        SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) ref;
                        addOperationChange(splitVariableRefactoring, parentCommitId, commitId, splitVariableRefactoring.getOperationBefore(), splitVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case MERGE_PARAMETER: {
                        MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) ref;
                        addOperationChange(mergeVariableRefactoring, parentCommitId, commitId, mergeVariableRefactoring.getOperationBefore(), mergeVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case PULL_UP_OPERATION: {
                        PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) ref;
                        addOperationChange(pullUpOperationRefactoring, parentCommitId, commitId, pullUpOperationRefactoring.getOriginalOperation(), pullUpOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case PUSH_DOWN_OPERATION: {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) ref;
                        addOperationChange(pushDownOperationRefactoring, parentCommitId, commitId, pushDownOperationRefactoring.getOriginalOperation(), pushDownOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_RENAME_OPERATION:
                    case MOVE_OPERATION: {
                        MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) ref;
                        addOperationChange(moveOperationRefactoring, parentCommitId, commitId, moveOperationRefactoring.getOriginalOperation(), moveOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_INLINE_OPERATION:
                    case INLINE_OPERATION: {
                        InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) ref;
                        addOperationChange(inlineOperationRefactoring, parentCommitId, commitId, inlineOperationRefactoring.getInlinedOperation(), inlineOperationRefactoring.getTargetOperationAfterInline());
                        break;
                    }
                    case EXTRACT_AND_MOVE_OPERATION:
                    case EXTRACT_OPERATION: {
                        ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
                        addOperationChange(extractOperationRefactoring, parentCommitId, commitId, extractOperationRefactoring.getSourceOperationAfterExtraction(), extractOperationRefactoring.getExtractedOperation());
                        addOperationChange(extractOperationRefactoring, parentCommitId, commitId, extractOperationRefactoring.getSourceOperationBeforeExtraction(), extractOperationRefactoring.getSourceOperationAfterExtraction());
                        break;
                    }
                    case CHANGE_RETURN_TYPE: {
                        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) ref;
                        addOperationChange(changeReturnTypeRefactoring, parentCommitId, commitId, changeReturnTypeRefactoring.getOperationBefore(), changeReturnTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_METHOD: {
                        RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) ref;
                        addOperationChange(renameOperationRefactoring, parentCommitId, commitId, renameOperationRefactoring.getOriginalOperation(), renameOperationRefactoring.getRenamedOperation());
                        break;
                    }
                    case RENAME_PARAMETER:
                    case PARAMETERIZE_VARIABLE: {
                        RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) ref;
                        addOperationChange(renameVariableRefactoring, parentCommitId, commitId, renameVariableRefactoring.getOperationBefore(), renameVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_PARAMETER_TYPE: {
                        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                        addOperationChange(changeVariableTypeRefactoring, parentCommitId, commitId, changeVariableTypeRefactoring.getOperationBefore(), changeVariableTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_PARAMETER: {
                        AddParameterRefactoring addParameterRefactoring = (AddParameterRefactoring) ref;
                        addOperationChange(addParameterRefactoring, parentCommitId, commitId, addParameterRefactoring.getOperationBefore(), addParameterRefactoring.getOperationAfter());
                    }
                    case REMOVE_PARAMETER: {
                        RemoveParameterRefactoring removeParameterRefactoring = (RemoveParameterRefactoring) ref;
                        addOperationChange(removeParameterRefactoring, parentCommitId, commitId, removeParameterRefactoring.getOperationBefore(), removeParameterRefactoring.getOperationAfter());
                    }
                    case REORDER_PARAMETER: {
                        ReorderParameterRefactoring reorderParameterRefactoring = (ReorderParameterRefactoring) ref;
                        addOperationChange(reorderParameterRefactoring, parentCommitId, commitId, reorderParameterRefactoring.getOperationBefore(), reorderParameterRefactoring.getOperationAfter());
                    }
                    case ADD_METHOD_ANNOTATION: {
                        AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                        addOperationChange(addMethodAnnotationRefactoring, parentCommitId, commitId, addMethodAnnotationRefactoring.getOperationBefore(), addMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case MODIFY_METHOD_ANNOTATION: {
                        ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) ref;
                        addOperationChange(modifyMethodAnnotationRefactoring, parentCommitId, commitId, modifyMethodAnnotationRefactoring.getOperationBefore(), modifyMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case REMOVE_METHOD_ANNOTATION: {
                        RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring = (RemoveMethodAnnotationRefactoring) ref;
                        addOperationChange(removeMethodAnnotationRefactoring, parentCommitId, commitId, removeMethodAnnotationRefactoring.getOperationBefore(), removeMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_CLASS: {
                        RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) ref;
                        UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
                        addClassChange(renameClassRefactoring, parentCommitId, commitId, originalClass, renamedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case MOVE_CLASS: {
                        MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) ref;
                        UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                        UMLClass movedClass = moveClassRefactoring.getMovedClass();
                        addClassChange(moveClassRefactoring, parentCommitId, commitId, originalClass, movedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), movedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), movedClass.getAttributes());
                        break;
                    }
                    case MOVE_RENAME_CLASS: {
                        MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) ref;
                        UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                        addClassChange(moveAndRenameClassRefactoring, parentCommitId, commitId, originalClass, renamedClass);
                        matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case ADD_CLASS_ANNOTATION: {
                        AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) ref;
                        addClassChange(addClassAnnotationRefactoring, parentCommitId, commitId, addClassAnnotationRefactoring.getClassBefore(), addClassAnnotationRefactoring.getClassAfter());
                    }
                    case REMOVE_CLASS_ANNOTATION: {
                        RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) ref;
                        addClassChange(removeClassAnnotationRefactoring, parentCommitId, commitId, removeClassAnnotationRefactoring.getClassBefore(), removeClassAnnotationRefactoring.getClassAfter());
                    }
                    case MODIFY_CLASS_ANNOTATION: {
                        ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) ref;
                        addClassChange(modifyClassAnnotationRefactoring, parentCommitId, commitId, modifyClassAnnotationRefactoring.getClassBefore(), modifyClassAnnotationRefactoring.getClassAfter());
                    }
                    case EXTRACT_INTERFACE:
                    case EXTRACT_SUPERCLASS: {
                        ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                        UMLClass extractedClass = extractSuperclassRefactoring.getExtractedClass();
                        for (UMLClass originalClass : extractSuperclassRefactoring.getUMLSubclassSet()) {
                            addClassChange(extractSuperclassRefactoring, parentCommitId, commitId, originalClass, extractedClass);
                        }
                        break;
                    }
                    case EXTRACT_SUBCLASS:
                    case EXTRACT_CLASS: {
                        ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) ref;
                        UMLClass originalClass = extractClassRefactoring.getOriginalClass();
                        UMLClass extractedClass = extractClassRefactoring.getExtractedClass();
                        addClassChange(extractClassRefactoring, parentCommitId, commitId, originalClass, extractedClass);
                        break;
                    }
                    case MOVE_SOURCE_FOLDER: {
                        MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) ref;
                        for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                            UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                            addClassChange(moveSourceFolderRefactoring, parentCommitId, commitId, originalClass, movedClass);
                            matchAttributes(ref, parentCommitId, commitId, originalClass.getAttributes(), movedClass.getAttributes());
                            matchOperations(ref, parentCommitId, commitId, originalClass.getOperations(), movedClass.getOperations());
                        }
                        break;
                    }
                    case MOVE_ATTRIBUTE: {
                        MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) ref;
                        addAttributeChange(moveAttributeRefactoring, parentCommitId, commitId, moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case EXTRACT_ATTRIBUTE: {
                        ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) ref;
                        break;
                    }
                    case PULL_UP_ATTRIBUTE: {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) ref;
                        addAttributeChange(pullUpAttributeRefactoring, parentCommitId, commitId, pullUpAttributeRefactoring.getOriginalAttribute(), pullUpAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case PUSH_DOWN_ATTRIBUTE: {
                        PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) ref;
                        addAttributeChange(pushDownAttributeRefactoring, parentCommitId, commitId, pushDownAttributeRefactoring.getOriginalAttribute(), pushDownAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case RENAME_ATTRIBUTE: {
                        RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) ref;
                        addAttributeChange(renameAttributeRefactoring, parentCommitId, commitId, renameAttributeRefactoring.getOriginalAttribute(), renameAttributeRefactoring.getClassNameBefore(), renameAttributeRefactoring.getRenamedAttribute(), renameAttributeRefactoring.getClassNameAfter());
                        break;
                    }
                    case CHANGE_ATTRIBUTE_TYPE: {
                        ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) ref;
                        addAttributeChange(changeAttributeTypeRefactoring, parentCommitId, commitId, changeAttributeTypeRefactoring.getOriginalAttribute(), changeAttributeTypeRefactoring.getClassNameBefore(), changeAttributeTypeRefactoring.getChangedTypeAttribute(), changeAttributeTypeRefactoring.getClassNameAfter());
                        break;
                    }
                    case REPLACE_ATTRIBUTE:
                    case MERGE_ATTRIBUTE:
                    case SPLIT_ATTRIBUTE:
                    case MOVE_RENAME_ATTRIBUTE:
                    case REPLACE_VARIABLE_WITH_ATTRIBUTE:
                    case SPLIT_VARIABLE:
                    case MERGE_VARIABLE:
                    case RENAME_VARIABLE:
                    case CONVERT_ANONYMOUS_CLASS_TO_TYPE:
                    case INTRODUCE_POLYMORPHISM:
                    case INLINE_VARIABLE:
                    case CHANGE_VARIABLE_TYPE:
                    case MERGE_OPERATION:
                    case EXTRACT_VARIABLE:
                    case RENAME_PACKAGE:
                    default: {
                        ref.toString();
                    }
                }
            }
        }
    }


    @Override
    public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
    }

    @Override
    public void handleException(String commitId, Exception e) {

    }

    private void matchAttributes(Refactoring ref, String parentCommitId, String childCommitId, List<UMLAttribute> leftSide, List<UMLAttribute> rightSide) {
        for (UMLAttribute attributeBefore : leftSide) {
            for (UMLAttribute attributeAfter : rightSide) {
                if (attributeBefore.getName().equals(attributeAfter.getName())) {
                    addAttributeChange(ref, parentCommitId, childCommitId, attributeBefore, attributeAfter, false);
                }
            }
        }


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
                addOperationChange(ref, parentCommitId, childCommitId, entry.getKey(), entry.getValue().get(0), false);
            } else {
                for (UMLOperation operationAfter : entry.getValue()) {
                    if (entry.getKey().equalSignature(operationAfter)) {
                        addOperationChange(ref, parentCommitId, childCommitId, entry.getKey(), operationAfter, false);
                    }
                }
            }
        }
    }

}
