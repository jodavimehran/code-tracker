package org.codetracker;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;

public class CommentTrackerChangeHistory {
	private final ChangeHistory<Comment> commentChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType commentType;
    private final int commentStartLineNumber;
    private final int commentEndLineNumber;

	public CommentTrackerChangeHistory(String methodName, int methodDeclarationLineNumber, CodeElementType commentType,
			int commentStartLineNumber, int commentEndLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
		this.commentType = commentType;
		this.commentStartLineNumber = commentStartLineNumber;
		this.commentEndLineNumber = commentEndLineNumber;
	}

	public ChangeHistory<Comment> get() {
		return commentChangeHistory;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getMethodDeclarationLineNumber() {
		return methodDeclarationLineNumber;
	}

	public CodeElementType getCommentType() {
		return commentType;
	}

	public int getCommentStartLineNumber() {
		return commentStartLineNumber;
	}

	public int getCommentEndLineNumber() {
		return commentEndLineNumber;
	}

    public boolean isStartComment(Comment comment) {
        return comment.getComment().getLocationInfo().getCodeElementType().equals(commentType) &&
                comment.getComment().getLocationInfo().getStartLine() == commentStartLineNumber &&
                comment.getComment().getLocationInfo().getEndLine() == commentEndLineNumber;
    }

    public boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getName().equals(methodName) &&
        		clazz.getUmlClass().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
        		clazz.getUmlClass().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean checkClassDiffForCommentChange(ArrayDeque<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Comment> equalComment, UMLClassBaseDiff umlClassDiff) throws RefactoringMinerTimedOutException {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                // check if it is in the matched
                if (isMatched(operationBodyMapper, comments, currentVersion, parentVersion, equalComment))
                    return true;
                //Check if is added
                if (isAdded(operationBodyMapper, comments, currentVersion, parentVersion, equalComment))
                    return true;
            }
        }
        return false;
    }

    public boolean checkForExtractionOrInline(ArrayDeque<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Comment rightComment, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
        int extractMatches = 0;
    	for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        UMLComment matchedCommentFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                        for (Pair<UMLComment, UMLComment> mapping : bodyMapper.getCommentListDiff().getCommonComments()) {
                            Comment matchedCommentInsideExtractedMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                            if (matchedCommentInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                matchedCommentFromSourceMethod = mapping.getLeft();
                                Comment commentBefore = Comment.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
                                if (!commentBefore.getComment().getText().equals(matchedCommentInsideExtractedMethodBody.getComment().getText())) {
                                    commentChangeHistory.addChange(commentBefore, matchedCommentInsideExtractedMethodBody, ChangeFactory.forComment(Change.Type.BODY_CHANGE));
                                }
                                break;
                            }
                        }
                        Comment commentBefore;
                        if (rightComment.getOperation().isPresent())
                        	commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
                        else
                        	commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
                        if (matchedCommentFromSourceMethod == null) {
                            commentChangeHistory.handleAdd(commentBefore, rightComment, extractOperationRefactoring.toString());
                            if(extractMatches == 0) {
                            	comments.add(commentBefore);
                            }
                        }
                        else {
                            VariableDeclarationContainer sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            Comment leftComment = Comment.of(matchedCommentFromSourceMethod, sourceMethod);
                            if(extractMatches == 0) {
                            	comments.add(leftComment);
                            }
                        }
                        commentChangeHistory.connectRelatedNodes();
                        extractMatches++;
                    }
                    break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    Method targetOperationAfterInline = Method.of(inlineOperationRefactoring.getTargetOperationAfterInline(), currentVersion);
                    if (equalMethod.test(targetOperationAfterInline)) {
                        UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                        for (Pair<UMLComment, UMLComment> mapping : bodyMapper.getCommentListDiff().getCommonComments()) {
                            Comment matchedCommentInsideInlinedMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                            if (matchedCommentInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                Comment commentBefore = Comment.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
                                commentChangeHistory.handleAdd(commentBefore, matchedCommentInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                comments.add(commentBefore);
                                commentChangeHistory.connectRelatedNodes();
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
                    	int mergeMatches = 0;
                        for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                            for (Pair<UMLComment, UMLComment> mapping : bodyMapper.getCommentListDiff().getCommonComments()) {
                                Comment matchedCommentInsideMergedMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedCommentInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                    // implementation for introduced
                                    /*
                                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                    blocks.add(blockBefore);
                                    blockChangeHistory.connectRelatedNodes();
                                    return true;
                                    */
                                    // check if it is in the matched
                                    if (isMatched(bodyMapper, comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion))
                                    	mergeMatches++;
                                }
                            }
                        }
                        if(mergeMatches > 0) {
                        	return true;
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
                                for (Pair<UMLComment, UMLComment> mapping : bodyMapper.getCommentListDiff().getCommonComments()) {
                                    Comment matchedCommentInsideSplitMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedCommentInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                    // implementation for introduced
                                    /*
                                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                    blocks.add(blockBefore);
                                    blockChangeHistory.connectRelatedNodes();
                                    return true;
                                    */
                                    // check if it is in the matched
                                    if (isMatched(bodyMapper, comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion))
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    	if(extractMatches > 0) {
    		return true;
    	}
        return false;
    }

    public boolean checkBodyOfMatchedOperations(Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) throws RefactoringMinerTimedOutException {
        if (umlOperationBodyMapper == null)
            return false;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, comments, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, comments, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	int matches = 0;
    	for (Pair<UMLComment, UMLComment> mapping : umlOperationBodyMapper.getCommentListDiff().getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), umlOperationBodyMapper.getContainer2(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), umlOperationBodyMapper.getContainer1(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.forComment(Change.Type.BODY_CHANGE));
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	comments.add(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
        for (UMLComment composite : umlOperationBodyMapper.getCommentListDiff().getAddedComments()) {
            Comment commentAfter = Comment.of(composite, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(composite, umlOperationBodyMapper.getContainer2(), parentVersion);
                boolean commentedCode = false;
                for (Pair<AbstractCodeFragment, UMLComment> pair : umlOperationBodyMapper.getCommentedCode()) {
                	if (pair.getRight().equals(composite)) {
                		commentedCode = true;
                		break;
                	}
                }
                if (commentedCode)
                	commentChangeHistory.handleAdd(commentBefore, commentAfter, "commented code");
                else
                	commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                comments.add(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    public boolean checkBodyOfMatchedClasses(Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
        if (classDiff == null)
            return false;
        // check if it is in the matched
        if (isMatched(classDiff, comments, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(classDiff, comments, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLAbstractClassDiff classDiff, Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	int matches = 0;
    	for (Pair<UMLComment, UMLComment> mapping : classDiff.getCommentListDiff().getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.forComment(Change.Type.BODY_CHANGE));
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	comments.add(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(UMLAbstractClassDiff classDiff, Queue<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
        for (UMLComment composite : classDiff.getCommentListDiff().getAddedComments()) {
            Comment commentAfter = Comment.of(composite, classDiff.getNextClass(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(composite, classDiff.getNextClass(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                comments.add(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    public boolean checkRefactoredMethod(ArrayDeque<Comment> comments, Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Comment rightComment, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            UMLOperationBodyMapper umlOperationBodyMapper = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pullUpOperationRefactoring.getBodyMapper();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pushDownOperationRefactoring.getBodyMapper();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = moveOperationRefactoring.getBodyMapper();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    umlOperationBodyMapper = renameOperationRefactoring.getBodyMapper();
                    break;
                }
            }
            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (equalMethod.test(methodAfter)) {
                    boolean found = checkBodyOfMatchedOperations(comments, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }
}
