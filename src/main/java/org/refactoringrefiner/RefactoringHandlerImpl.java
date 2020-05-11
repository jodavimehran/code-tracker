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
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.element.AttributeElement;
import org.refactoringrefiner.element.ClassElement;
import org.refactoringrefiner.element.MethodElement;

import java.io.IOException;
import java.util.*;

public class RefactoringHandlerImpl extends RefactoringHandler {
    private final Repository repository;
    private final MutableValueGraph<CodeElement, Edge> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    private final MetaInfoImpl metaInfo = new MetaInfoImpl();

    public RefactoringHandlerImpl(Repository repository) {
        this.repository = repository;
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
            Collections.sort(leafCodeElementsList, (o1, o2) -> Integer.compare(o2.getVersion().getTime(), o1.getVersion().getTime()));
            Set<CodeElement> rootCodeElements = rootElement.get(leafEntry.getKey());
            for (CodeElement leafCodeElement : leafCodeElementsList) {
                List<CodeElement> matched = new ArrayList<>();
                for (CodeElement rootCodeElement : rootCodeElements) {
                    if (rootCodeElement.getVersion().getTime() > leafCodeElement.getVersion().getTime()) {
                        matched.add(rootCodeElement);
                    }
                }
                if (!matched.isEmpty()) {
                    Collections.sort(matched, Comparator.comparingInt(o -> o.getVersion().getTime()));
                    graph.putEdgeValue(leafCodeElement, matched.get(0), RefactoringEdge.of(null, null));
                    rootCodeElements.remove(matched.get(0));
                }
            }
        }
        return ImmutableValueGraph.copyOf(graph);
    }

    private void addOperationChange(Refactoring ref, RevCommit revCommit, UMLOperation leftSideOperation, UMLOperation rightSideOperation) {
        String parentCommitId = revCommit.getParent(0).getId().getName();
        MethodElement leftSideMethodElement = new MethodElement(leftSideOperation, new VersionImpl(parentCommitId, getRevCommit(parentCommitId).getCommitTime()));
        MethodElement rightSideMethodElement = new MethodElement(rightSideOperation, new VersionImpl(revCommit.getId().getName(), revCommit.getCommitTime()));
        if (!leftSideMethodElement.equals(rightSideMethodElement))
            addChange(leftSideMethodElement, rightSideMethodElement, ref, revCommit);
    }

    private void addAttributeChange(Refactoring ref, RevCommit revCommit, UMLAttribute leftSideAttribute, UMLAttribute rightSideAttribute) {
        String parentCommitId = revCommit.getParent(0).getId().getName();
        AttributeElement leftSideAttributeElement = new AttributeElement(leftSideAttribute, new VersionImpl(parentCommitId, getRevCommit(parentCommitId).getCommitTime()));
        AttributeElement rightSideAttributeElement = new AttributeElement(rightSideAttribute, new VersionImpl(revCommit.getId().getName(), revCommit.getCommitTime()));

        addChange(leftSideAttributeElement, rightSideAttributeElement, ref, revCommit);
    }

    private void addAttributeChange(Refactoring ref, RevCommit revCommit, VariableDeclaration leftSideAttribute, String leftSideClassName, VariableDeclaration rightSideAttribute, String rightSideClassName) {
        String parentCommitId = revCommit.getParent(0).getId().getName();
        AttributeElement leftSideAttributeElement = new AttributeElement(leftSideAttribute, leftSideClassName, new VersionImpl(parentCommitId, getRevCommit(parentCommitId).getCommitTime()));
        AttributeElement rightSideAttributeElement = new AttributeElement(rightSideAttribute, rightSideClassName, new VersionImpl(revCommit.getId().getName(), revCommit.getCommitTime()));

        addChange(leftSideAttributeElement, rightSideAttributeElement, ref, revCommit);
    }

    private void addClassChange(Refactoring ref, RevCommit revCommit, UMLClass leftSideClass, UMLClass rightSideClass) {
        String parentCommitId = revCommit.getParent(0).getId().getName();
        ClassElement leftSideClassElement = new ClassElement(leftSideClass, new VersionImpl(parentCommitId, getRevCommit(parentCommitId).getCommitTime()));
        ClassElement rightSideClassElement = new ClassElement(rightSideClass, new VersionImpl(revCommit.getId().getName(), revCommit.getCommitTime()));

        addChange(leftSideClassElement, rightSideClassElement, ref, revCommit);
    }

    private void addChange(CodeElement leftSide, CodeElement rightSide, Refactoring ref, RevCommit revCommit) {
        Optional<Edge> edgeValue = graph.edgeValue(leftSide, rightSide);
        if (edgeValue.isPresent()) {
            Edge edge = edgeValue.get();
            edge.addRefactoring(ref);
        } else {
            Edge edge = RefactoringEdge.of(revCommit, ref);
            graph.putEdgeValue(leftSide, rightSide, edge);
        }
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        analyze(commitId, refactorings);
    }

    private void analyze(String commitId, List<Refactoring> refactorings) {
        RevCommit revCommit = getRevCommit(commitId);
        if (refactorings != null && !refactorings.isEmpty()) {
            metaInfo.addCommitTime(revCommit.getCommitTime());
            for (Refactoring ref : refactorings) {
                RefactoringType refactoringType = ref.getRefactoringType();
                metaInfo.addType(refactoringType);
                switch (refactoringType) {
                    case SPLIT_PARAMETER: {
                        SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) ref;
                        addOperationChange(splitVariableRefactoring, revCommit, splitVariableRefactoring.getOperationBefore(), splitVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case MERGE_PARAMETER: {
                        MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) ref;
                        addOperationChange(mergeVariableRefactoring, revCommit, mergeVariableRefactoring.getOperationBefore(), mergeVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case PULL_UP_OPERATION: {
                        PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) ref;
                        addOperationChange(pullUpOperationRefactoring, revCommit, pullUpOperationRefactoring.getOriginalOperation(), pullUpOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case PUSH_DOWN_OPERATION: {
                        PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) ref;
                        addOperationChange(pushDownOperationRefactoring, revCommit, pushDownOperationRefactoring.getOriginalOperation(), pushDownOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_RENAME_OPERATION:
                    case MOVE_OPERATION: {
                        MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) ref;
                        addOperationChange(moveOperationRefactoring, revCommit, moveOperationRefactoring.getOriginalOperation(), moveOperationRefactoring.getMovedOperation());
                        break;
                    }
                    case MOVE_AND_INLINE_OPERATION:
                    case INLINE_OPERATION: {
                        InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) ref;
                        addOperationChange(inlineOperationRefactoring, revCommit, inlineOperationRefactoring.getInlinedOperation(), inlineOperationRefactoring.getTargetOperationAfterInline());
                        break;
                    }
                    case EXTRACT_AND_MOVE_OPERATION:
                    case EXTRACT_OPERATION: {
                        ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
                        addOperationChange(extractOperationRefactoring, revCommit, extractOperationRefactoring.getSourceOperationAfterExtraction(), extractOperationRefactoring.getExtractedOperation());
                        addOperationChange(extractOperationRefactoring, revCommit, extractOperationRefactoring.getSourceOperationBeforeExtraction(), extractOperationRefactoring.getSourceOperationAfterExtraction());
                        break;
                    }
                    case CHANGE_RETURN_TYPE: {
                        ChangeReturnTypeRefactoring changeReturnTypeRefactoring = (ChangeReturnTypeRefactoring) ref;
                        addOperationChange(changeReturnTypeRefactoring, revCommit, changeReturnTypeRefactoring.getOperationBefore(), changeReturnTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_METHOD: {
                        RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) ref;
                        addOperationChange(renameOperationRefactoring, revCommit, renameOperationRefactoring.getOriginalOperation(), renameOperationRefactoring.getRenamedOperation());
                        break;
                    }
                    case RENAME_PARAMETER:
                    case PARAMETERIZE_VARIABLE: {
                        RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) ref;
                        addOperationChange(renameVariableRefactoring, revCommit, renameVariableRefactoring.getOperationBefore(), renameVariableRefactoring.getOperationAfter());
                        break;
                    }
                    case CHANGE_PARAMETER_TYPE: {
                        ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                        addOperationChange(changeVariableTypeRefactoring, revCommit, changeVariableTypeRefactoring.getOperationBefore(), changeVariableTypeRefactoring.getOperationAfter());
                        break;
                    }
                    case ADD_METHOD_ANNOTATION: {
                        AddMethodAnnotationRefactoring addMethodAnnotationRefactoring = (AddMethodAnnotationRefactoring) ref;
                        addOperationChange(addMethodAnnotationRefactoring, revCommit, addMethodAnnotationRefactoring.getOperationBefore(), addMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case MODIFY_METHOD_ANNOTATION: {
                        ModifyMethodAnnotationRefactoring modifyMethodAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) ref;
                        addOperationChange(modifyMethodAnnotationRefactoring, revCommit, modifyMethodAnnotationRefactoring.getOperationBefore(), modifyMethodAnnotationRefactoring.getOperationAfter());
                        break;
                    }
                    case RENAME_CLASS: {
                        RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) ref;
                        UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
                        addClassChange(renameClassRefactoring, revCommit, originalClass, renamedClass);
                        matchOperations(ref, revCommit, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, revCommit, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case MOVE_CLASS: {
                        MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) ref;
                        UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                        UMLClass movedClass = moveClassRefactoring.getMovedClass();
                        addClassChange(moveClassRefactoring, revCommit, originalClass, movedClass);
                        matchOperations(ref, revCommit, originalClass.getOperations(), movedClass.getOperations());
                        matchAttributes(ref, revCommit, originalClass.getAttributes(), movedClass.getAttributes());
                        break;
                    }
                    case MOVE_RENAME_CLASS: {
                        MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) ref;
                        UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                        UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                        addClassChange(moveAndRenameClassRefactoring, revCommit, originalClass, renamedClass);
                        matchOperations(ref, revCommit, originalClass.getOperations(), renamedClass.getOperations());
                        matchAttributes(ref, revCommit, originalClass.getAttributes(), renamedClass.getAttributes());
                        break;
                    }
                    case EXTRACT_INTERFACE:
                    case EXTRACT_SUPERCLASS: {
                        ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                        UMLClass extractedClass = extractSuperclassRefactoring.getExtractedClass();
                        for (UMLClass originalClass : extractSuperclassRefactoring.getUMLSubclassSet()) {
                            addClassChange(extractSuperclassRefactoring, revCommit, originalClass, extractedClass);
                        }
                        break;
                    }
                    case EXTRACT_SUBCLASS:
                    case EXTRACT_CLASS: {
                        ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) ref;
                        UMLClass originalClass = extractClassRefactoring.getOriginalClass();
                        UMLClass extractedClass = extractClassRefactoring.getExtractedClass();
                        addClassChange(extractClassRefactoring, revCommit, originalClass, extractedClass);
                        break;
                    }
                    case RENAME_PACKAGE: {
                        RenamePackageRefactoring renamePackageRefactoring = (RenamePackageRefactoring) ref;
                        for (MoveClassRefactoring moveClassRefactoring : renamePackageRefactoring.getMoveClassRefactorings()) {
                            UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                            UMLClass movedClass = moveClassRefactoring.getMovedClass();
                            matchAttributes(ref, revCommit, originalClass.getAttributes(), movedClass.getAttributes());
                            matchOperations(ref, revCommit, originalClass.getOperations(), movedClass.getOperations());
                        }
                        break;
                    }
                    case MOVE_SOURCE_FOLDER: {
                        MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) ref;
                        for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                            UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                            addClassChange(moveSourceFolderRefactoring, revCommit, originalClass, movedClass);
                            matchAttributes(ref, revCommit, originalClass.getAttributes(), movedClass.getAttributes());
                            matchOperations(ref, revCommit, originalClass.getOperations(), movedClass.getOperations());
                        }
                        break;
                    }
                    case MOVE_ATTRIBUTE: {
                        MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) ref;
                        addAttributeChange(moveAttributeRefactoring, revCommit, moveAttributeRefactoring.getOriginalAttribute(), moveAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case REPLACE_ATTRIBUTE: {
                        ref.toString();
                        break;
                    }
                    case MERGE_ATTRIBUTE: {
                        ref.toString();
                        break;

                    }
                    case EXTRACT_ATTRIBUTE: {
                        ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) ref;
                        break;
                    }
                    case SPLIT_ATTRIBUTE: {
                        ref.toString();
                        break;
                    }
                    case MOVE_RENAME_ATTRIBUTE: {
                        ref.toString();
                        break;
                    }
                    case PULL_UP_ATTRIBUTE: {
                        PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) ref;
                        addAttributeChange(pullUpAttributeRefactoring, revCommit, pullUpAttributeRefactoring.getOriginalAttribute(), pullUpAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case PUSH_DOWN_ATTRIBUTE: {
                        PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) ref;
                        addAttributeChange(pushDownAttributeRefactoring, revCommit, pushDownAttributeRefactoring.getOriginalAttribute(), pushDownAttributeRefactoring.getMovedAttribute());
                        break;
                    }
                    case RENAME_ATTRIBUTE: {
                        RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) ref;
                        addAttributeChange(renameAttributeRefactoring, revCommit, renameAttributeRefactoring.getOriginalAttribute(), renameAttributeRefactoring.getClassNameBefore(), renameAttributeRefactoring.getRenamedAttribute(), renameAttributeRefactoring.getClassNameAfter());
                        break;
                    }
                    case CHANGE_ATTRIBUTE_TYPE: {
                        ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) ref;
                        addAttributeChange(changeAttributeTypeRefactoring, revCommit, changeAttributeTypeRefactoring.getOriginalAttribute(), changeAttributeTypeRefactoring.getClassNameBefore(), changeAttributeTypeRefactoring.getChangedTypeAttribute(), changeAttributeTypeRefactoring.getClassNameAfter());
                        break;
                    }
                    case REPLACE_VARIABLE_WITH_ATTRIBUTE: {
                        ref.toString();
                        break;
                    }
                    case SPLIT_VARIABLE:
                    case MERGE_VARIABLE:
                    case RENAME_VARIABLE:
                    case CONVERT_ANONYMOUS_CLASS_TO_TYPE:
                    case INTRODUCE_POLYMORPHISM:
                    case CHANGE_METHOD_SIGNATURE:
                    case INLINE_VARIABLE:
                    case CHANGE_VARIABLE_TYPE:
                    case MERGE_OPERATION:
                    case EXTRACT_VARIABLE:
                    default: {
                        ref.toString();
                    }
                }
            }
        }
    }

    private RevCommit getRevCommit(String commitId) {
        RevCommit revCommit = null;
        try {
            revCommit = repository.parseCommit(ObjectId.fromString(commitId));
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (MissingObjectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return revCommit;
    }

    @Override
    public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
    }

    @Override
    public void handleException(String commitId, Exception e) {

    }

    private void matchAttributes(Refactoring ref, RevCommit revCommit, List<UMLAttribute> leftSide, List<UMLAttribute> rightSide) {
        for (UMLAttribute attributeBefore : leftSide) {
            for (UMLAttribute attributeAfter : rightSide) {
                if (attributeBefore.getName().equals(attributeAfter.getName())) {
                    addAttributeChange(ref, revCommit, attributeBefore, attributeAfter);
                }
            }
        }


    }

    private void matchOperations(Refactoring ref, RevCommit revCommit, List<UMLOperation> leftSide, List<UMLOperation> rightSide) {
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
                addOperationChange(ref, revCommit, entry.getKey(), entry.getValue().get(0));
            } else {
                for (UMLOperation operationAfter : entry.getValue()) {
                    if (entry.getKey().equalSignature(operationAfter)) {
                        addOperationChange(ref, revCommit, entry.getKey(), operationAfter);
                    }
                }
            }
        }
    }

}
