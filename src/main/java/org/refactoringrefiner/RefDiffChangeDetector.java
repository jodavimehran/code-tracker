package org.refactoringrefiner;

import gr.uom.java.xmi.UMLModel;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Edge;
import org.refactoringrefiner.api.Graph;
import org.refactoringrefiner.api.RefactoringRefiner;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class RefDiffChangeDetector implements ChangeDetector {
    private final File repository;
    //    private final RefDiff refDiff;
    private final RefactoringMiner refactoringMiner;
    private final ChangeHistory classChangeHistory = new ChangeHistory();
    private final ChangeHistory methodChangeHistory = new ChangeHistory();

    public RefDiffChangeDetector(RefactoringMiner refactoringMiner, File gitRepository) {
//        this.refDiff = new RefDiff(new JavaPlugin(new File("E:\\Data\\temp")));
        this.repository = gitRepository;
        this.refactoringMiner = refactoringMiner;
    }

    @Override
    public void detectAtCommit(String commitId) {
//        CstDiff cstDiff = refDiff.computeDiffForCommit(repository, commitId);
//        Pair<UMLModel, UMLModel> umlModel = refactoringMiner.getUMLModel(commitId);
//        for (Relationship relationship : cstDiff.getRelationships()) {
//            if ("MethodDeclaration".equals(relationship.getNodeBefore().getType()) && "MethodDeclaration".equals(relationship.getNodeAfter().getType())) {
//                methodChange(umlModel, relationship, commitId);
//            }
//        }
        methodChangeHistory.connectRelatedNodes();
        classChangeHistory.connectRelatedNodes();
    }

    @Override
    public void addNode(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
        switch (codeElementType) {
            case CLASS:
                classChangeHistory.addNode(codeElement);
            case METHOD:
                methodChangeHistory.addNode(codeElement);
        }
    }

    @Override
    public List<CodeElement> findMostLeftElement(RefactoringRefiner.CodeElementType codeElementType, String elementKey) {
        switch (codeElementType) {
            case CLASS:
                return classChangeHistory.findMostLeftSide(elementKey);
            case METHOD:
                return methodChangeHistory.findMostLeftSide(elementKey);
        }
        return Collections.emptyList();
    }

    @Override
    public Graph<CodeElement, Edge> findSubGraph(RefactoringRefiner.CodeElementType codeElementType, CodeElement start) {
        switch (codeElementType) {
            case CLASS:
                return classChangeHistory.findSubGraph(start);
            case METHOD:
                return methodChangeHistory.findSubGraph(start);
        }
        return null;
    }


//    private void methodChange(Pair<UMLModel, UMLModel> umlModel, Relationship relationship, String commitId) {
//        RelationshipType relationshipType = relationship.getType();
//        UMLModel leftModel = umlModel.getLeft();
//        UMLModel rightModel = umlModel.getRight();
//        switch (relationshipType) {
//            case EXTRACT:
//            case EXTRACT_MOVE:
//            case EXTRACT_SUPER:
//            case INLINE:
//            case PULL_UP_SIGNATURE:
//                return;
//        }
//        String leftKey = getKey(relationship.getNodeBefore());
//        String rightKey = getKey(relationship.getNodeAfter());
//        Method leftMethod = null; // = RefactoringMiner.getMethod(leftModel, leftKey, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)), true, true);
//        Method rightMethod = null; //RefactoringMiner.getMethod(rightModel, rightKey, refactoringMiner.getRepository().getVersion(commitId), true, true);
//        if (leftMethod == null || rightMethod == null /*|| leftMethod.getIdentifierExcludeVersion().equals(rightMethod.getIdentifierExcludeVersion())*/) {
//            return;
//        }
//        switch (relationshipType) {
//            case INTERNAL_MOVE:
//            case MOVE:
//
//        }
//        methodChangeHistory.addNode(leftMethod);
//        methodChangeHistory.addNode(rightMethod);
//        String description = relationshipType.toString().toLowerCase() + ";" + relationship;
//        switch (relationshipType) {
//            case PULL_UP:
//            case PUSH_DOWN:
//            case INTERNAL_MOVE:
//            case MOVE:
//                methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.REFACTORED).description(RelationshipType.MOVE.toString().toLowerCase() + ";" + relationship));
//                break;
//            case INTERNAL_MOVE_RENAME:
//            case MOVE_RENAME:
//                methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.REFACTORED).description(RelationshipType.MOVE.toString().toLowerCase() + ";" + relationship));
//                methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.REFACTORED).description(RelationshipType.RENAME.toString().toLowerCase() + ";" + relationship));
//                break;
//            case SAME:
//                methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.NO_CHANGE).description(description));
//                break;
//            default:
//                methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.REFACTORED).description(description));
//                break;
//        }


//    }

//    private String getKey(CstNode cstNode) {
//        switch (cstNode.getType()) {
//            case "MethodDeclaration":
//                return String.format("%s#%s", getKey(cstNode.getParent().get()), cstNode.getLocalName());
//            case "ClassDeclaration": {
//                if (cstNode.getParent().isPresent()) {
//                    return String.format("%s.%s", getKey(cstNode.getParent().get()), cstNode.getLocalName());
//                } else {
//                    String className = cstNode.getNamespace() + cstNode.getLocalName();
//                    return Util.getPath(cstNode.getLocation().getFile(), className) + className;
//                }
//            }
//
//        }
//        return "";
//    }
}
