package org.codetracker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Attribute;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AddAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddAttributeModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.ModifyAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.PullUpAttributeRefactoring;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import gr.uom.java.xmi.diff.PushDownAttributeRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RemoveAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveAttributeModifierRefactoring;
import gr.uom.java.xmi.diff.RenameAttributeRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLCommentListDiff;
import gr.uom.java.xmi.diff.UMLDocumentationDiffProvider;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;
import gr.uom.java.xmi.diff.UMLJavadocDiff;

public class CommentTrackerChangeHistory extends AbstractChangeHistory<Comment> {
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

    public boolean isStartAttribute(Attribute attribute) {
        return attribute.getUmlAttribute().getName().equals(methodName) &&
        		attribute.getUmlAttribute().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
        		attribute.getUmlAttribute().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getName().equals(methodName) &&
        		clazz.getUmlClass().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
        		clazz.getUmlClass().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean checkClassDiffForCommentChange(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Comment> equalComment, UMLAbstractClassDiff umlClassDiff) {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                // check if it is in the matched
                if (isMatched(operationBodyMapper, currentVersion, parentVersion, equalComment))
                    return true;
                //Check if is added
                if (isAdded(operationBodyMapper, currentVersion, parentVersion, equalComment))
                    return true;
            }
        }
        return false;
    }

    public boolean checkClassDiffForCommentChange(Version currentVersion, Version parentVersion, Attribute rightAttribute, Predicate<Comment> equalComment, UMLAbstractClassDiff umlClassDiff) {
    	if (umlClassDiff == null)
    		return false;
    	boolean found = false;
    	Pair<? extends UMLAttribute, ? extends UMLAttribute> foundPair = null;
    	for (Pair<UMLAttribute, UMLAttribute> pair : umlClassDiff.getCommonAtrributes()) {
    		if (pair.getRight().equals(rightAttribute.getUmlAttribute())) {
    			foundPair = pair;
    			break;
    		}
    	}
    	for (Pair<UMLEnumConstant, UMLEnumConstant> pair : umlClassDiff.getCommonEnumConstants()) {
    		if (pair.getRight().equals(rightAttribute.getUmlAttribute())) {
    			foundPair = pair;
    			break;
    		}
    	}
    	UMLDocumentationDiffProvider provider = null;
    	for (UMLAttributeDiff attributeDiff : umlClassDiff.getAttributeDiffList()) {
    		if (attributeDiff.getContainer2().equals(rightAttribute.getUmlAttribute())) {
    			provider = attributeDiff;
    			break;
    		}
    	}
    	for (UMLEnumConstantDiff attributeDiff : umlClassDiff.getEnumConstantDiffList()) {
    		if (attributeDiff.getContainer2().equals(rightAttribute.getUmlAttribute())) {
    			provider = attributeDiff;
    			break;
    		}
    	}
    	if (foundPair != null) {
    		Pair<UMLAttribute, UMLAttribute> pair = Pair.of(foundPair.getLeft(), foundPair.getRight());
    		found = checkBodyOfMatchedAttributes(currentVersion, parentVersion, equalComment, pair);
    	}
    	if (provider != null) {
    		found = checkBodyOfMatchedOperations(currentVersion, parentVersion, equalComment, provider);
    	}
    	return found;
    }

    public boolean checkForExtractionOrInline(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Comment rightComment, List<Refactoring> refactorings) {
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
                        if(rightComment.getComment() instanceof UMLComment) {
	                        UMLCommentListDiff commentListDiff = bodyMapper.getCommentListDiff();
	                        if (commentListDiff != null) {
	                            for (Pair<UMLComment, UMLComment> mapping : commentListDiff.getCommonComments()) {
	                                Comment matchedCommentInsideExtractedMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
	                                if (matchedCommentInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
	                                    matchedCommentFromSourceMethod = mapping.getLeft();
	                                    Comment commentBefore = Comment.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
	                                    if (!commentBefore.getComment().getText().equals(matchedCommentInsideExtractedMethodBody.getComment().getText())) {
	                                        processChange(commentBefore, matchedCommentInsideExtractedMethodBody);
	                                    }
	                                    break;
	                                }
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
	                            	elements.addFirst(commentBefore);
	                            }
	                        }
	                        else {
	                            VariableDeclarationContainer sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
	                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
	                            Comment leftComment = Comment.of(matchedCommentFromSourceMethod, sourceMethod);
	                            if(extractMatches == 0) {
	                            	elements.addFirst(leftComment);
	                            }
	                        }
                    	}
                        if(bodyMapper.getJavadocDiff().isPresent()) {
                        	UMLJavadocDiff extractedJavadocDiff = bodyMapper.getJavadocDiff().get();
                        	Comment commentAfter = Comment.of(extractedJavadocDiff.getJavadocAfter(), bodyMapper.getContainer2(), currentVersion);
                    		if (commentAfter != null && commentAfter.equalIdentifierIgnoringVersion(rightComment)) {
                                Comment commentBefore = Comment.of(extractedJavadocDiff.getJavadocBefore(), bodyMapper.getContainer1(), parentVersion);
                                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                                    processChange(commentBefore, commentAfter);
                                }
                                else {
                                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                                }
                                if(extractMatches == 0) {
                                	elements.addFirst(commentBefore);
                                }
                    		}
                        }
                        else {
                        	Comment commentBefore;
	                        if (rightComment.getOperation().isPresent())
	                        	commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
	                        else
	                        	commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
	                        if (matchedCommentFromSourceMethod == null) {
	                            commentChangeHistory.handleAdd(commentBefore, rightComment, extractOperationRefactoring.toString());
	                            if(extractMatches == 0) {
	                            	elements.addFirst(commentBefore);
	                            }
	                        }
                        }
                        commentChangeHistory.connectRelatedNodes();
                        extractMatches++;
                    }
                    UMLOperationBodyMapper mapper = extractOperationRefactoring.getBodyMapper();
                    Set<UMLAnonymousClassDiff> anonymousClassDiffs = mapper.getAnonymousClassDiffs();
                    for (UMLAnonymousClassDiff diff : anonymousClassDiffs) {
                        for (UMLOperationBodyMapper anonymousMapper : diff.getOperationBodyMapperList()) {
                            Method anonymousExtractedOperationAfter = Method.of(anonymousMapper.getContainer2(), currentVersion);
                            if (equalMethod.test(anonymousExtractedOperationAfter)) {
                            	UMLComment matchedCommentFromSourceMethod = null;
                            	UMLCommentListDiff commentListDiff = anonymousMapper.getCommentListDiff();
                                if (commentListDiff != null) {
                                    for (Pair<UMLComment, UMLComment> mapping : commentListDiff.getCommonComments()) {
                                        Comment matchedCommentInsideExtractedMethodBody = Comment.of(mapping.getRight(), anonymousMapper.getContainer2(), currentVersion);
                                        if (matchedCommentInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                            matchedCommentFromSourceMethod = mapping.getLeft();
                                            Comment commentBefore = Comment.of(mapping.getLeft(), anonymousMapper.getContainer1(), parentVersion);
                                            if (!commentBefore.getComment().getText().equals(matchedCommentInsideExtractedMethodBody.getComment().getText())) {
                                                processChange(commentBefore, matchedCommentInsideExtractedMethodBody);
                                            }
                                            break;
                                        }
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
                                    	elements.addFirst(commentBefore);
                                    }
                                }
                                else {
                                    VariableDeclarationContainer sourceOperation = anonymousMapper.getContainer1();
                                    Method sourceMethod = Method.of(sourceOperation, parentVersion);
                                    Comment leftComment = Comment.of(matchedCommentFromSourceMethod, sourceMethod);
                                    if(extractMatches == 0) {
                                    	elements.addFirst(leftComment);
                                    }
                                }
                                commentChangeHistory.connectRelatedNodes();
                                extractMatches++;
                            }
                        }
                    }
                    break;
                }
                case EXTRACT_FIXTURE:
                case MOVE_CODE: {
                	MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                	Method extractedMethod = Method.of(moveCodeRefactoring.getTargetContainer(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                    	UMLComment matchedCommentFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
                        UMLCommentListDiff commentListDiff = bodyMapper.getCommentListDiff();
                        if (commentListDiff != null) {
                            for (Pair<UMLComment, UMLComment> mapping : commentListDiff.getCommonComments()) {
                                Comment matchedCommentInsideExtractedMethodBody = Comment.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedCommentInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightComment)) {
                                    matchedCommentFromSourceMethod = mapping.getLeft();
                                    Comment commentBefore = Comment.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
                                    if (!commentBefore.getComment().getText().equals(matchedCommentInsideExtractedMethodBody.getComment().getText())) {
                                        processChange(commentBefore, matchedCommentInsideExtractedMethodBody);
                                    }
                                    break;
                                }
                            }
                        }
                        if (matchedCommentFromSourceMethod != null) {
                            VariableDeclarationContainer sourceOperation = moveCodeRefactoring.getSourceContainer();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            Comment leftComment = Comment.of(matchedCommentFromSourceMethod, sourceMethod);
                            if(extractMatches == 0) {
                            	elements.addFirst(leftComment);
                            }
                            commentChangeHistory.connectRelatedNodes();
                            extractMatches++;
                        }
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
                                elements.addFirst(commentBefore);
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
                                    if (isMatched(bodyMapper, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion))
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
                                    if (isMatched(bodyMapper, currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion))
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

    public boolean checkBodyOfMatchedAttributes(Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator, Pair<? extends UMLAttribute, ? extends UMLAttribute> pair) {
    	if (pair == null)
    		return false;
    	// check if it is in the matched
        if (isMatched(pair, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(pair, currentVersion, parentVersion, equalOperator);
    }

    public boolean checkBodyOfMatchedOperations(Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator, UMLDocumentationDiffProvider umlOperationBodyMapper) {
        if (umlOperationBodyMapper == null)
            return false;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(Pair<? extends UMLAttribute, ? extends UMLAttribute> pair, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	int matches = 0;
    	UMLCommentListDiff commentDiff = new UMLCommentListDiff(pair.getLeft().getComments(), pair.getRight().getComments());
    	for (Pair<UMLComment, UMLComment> mapping : commentDiff.getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), pair.getRight(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), pair.getLeft(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if (pair.getLeft().getJavadoc() != null && pair.getRight().getJavadoc() != null) {
    		UMLJavadocDiff javadocDiff = new UMLJavadocDiff(pair.getLeft().getJavadoc(), pair.getRight().getJavadoc());
    		Comment commentAfter = Comment.of(javadocDiff.getJavadocAfter(), pair.getRight(), currentVersion);
    		if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadocDiff.getJavadocBefore(), pair.getLeft(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
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

    public boolean isMatched(UMLDocumentationDiffProvider umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	int matches = 0;
    	for (Pair<UMLComment, UMLComment> mapping : umlOperationBodyMapper.getCommentListDiff().getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), umlOperationBodyMapper.getContainer2(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), umlOperationBodyMapper.getContainer1(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if (umlOperationBodyMapper.getContainer1().getAnonymousClassList().size() == umlOperationBodyMapper.getContainer2().getAnonymousClassList().size()) {
    		for (int i=0; i<umlOperationBodyMapper.getContainer2().getAnonymousClassList().size(); i++) {
    			UMLAnonymousClass anonymous2 = umlOperationBodyMapper.getContainer2().getAnonymousClassList().get(i);
    			UMLAnonymousClass anonymous1 = umlOperationBodyMapper.getContainer1().getAnonymousClassList().get(i);
    			if (anonymous1.getComments().size() == anonymous2.getComments().size()) {
	    			for (int j=0; j<anonymous2.getComments().size(); j++) {
	    				UMLComment umlComment2 = anonymous2.getComments().get(j);
	    				UMLComment umlComment1 = anonymous1.getComments().get(j);
	    				Comment commentAfter = Comment.of(umlComment2, umlOperationBodyMapper.getContainer2(), currentVersion);
	    				if (commentAfter != null && equalOperator.test(commentAfter)) {
	    					Comment commentBefore = Comment.of(umlComment1, umlOperationBodyMapper.getContainer1(), parentVersion);
	    	                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
	    	                    processChange(commentBefore, commentAfter);
	    	                }
	    	                else {
	    	                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
	    	                }
	    	                if(matches == 0) {
	    	                	elements.addFirst(commentBefore);
	    	                }
	    	                commentChangeHistory.connectRelatedNodes();
	    	                matches++;
	    				}
	    			}
    			}
    		}
    	}
    	if (umlOperationBodyMapper.getJavadocDiff().isPresent()) {
    		UMLJavadocDiff javadocDiff = umlOperationBodyMapper.getJavadocDiff().get();
    		Comment commentAfter = Comment.of(javadocDiff.getJavadocAfter(), umlOperationBodyMapper.getContainer2(), currentVersion);
    		if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadocDiff.getJavadocBefore(), umlOperationBodyMapper.getContainer1(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
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

    public void addedClass(Class rightClass, Comment rightComment, Version parentVersion) {
    	Comment commentBefore = Comment.of(rightComment.getComment(), rightClass.getUmlClass(), parentVersion);
        commentChangeHistory.handleAdd(commentBefore, rightComment, "added with class");
        elements.addFirst(commentBefore);
        commentChangeHistory.connectRelatedNodes();
    }

    public void addedMethod(Method rightMethod, Comment rightComment, Version parentVersion) {
    	Comment commentBefore = Comment.of(rightComment.getComment(), rightMethod.getUmlOperation(), parentVersion);
        commentChangeHistory.handleAdd(commentBefore, rightComment, "added with method");
        elements.addFirst(commentBefore);
        commentChangeHistory.connectRelatedNodes();
    }

    public void addedAttribute(Attribute rightAttribute, Comment rightComment, Version parentVersion) {
    	Comment commentBefore = Comment.of(rightComment.getComment(), rightAttribute.getUmlAttribute(), parentVersion);
        commentChangeHistory.handleAdd(commentBefore, rightComment, "added with attribute");
        elements.addFirst(commentBefore);
        commentChangeHistory.connectRelatedNodes();
    }

    private boolean isAdded(Pair<? extends UMLAttribute, ? extends UMLAttribute> pair, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	UMLCommentListDiff commentDiff = new UMLCommentListDiff(pair.getLeft().getComments(), pair.getRight().getComments());
    	for (UMLComment comment : commentDiff.getAddedComments()) {
            Comment commentAfter = Comment.of(comment, pair.getRight(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(comment, pair.getRight(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        UMLJavadoc javadoc = pair.getRight().getJavadoc();
    	if (javadoc != null) {
    		Comment commentAfter = Comment.of(javadoc, pair.getRight(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadoc, pair.getRight(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new javadoc");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
    	}
        return false;
    }

    private boolean isAdded(UMLDocumentationDiffProvider umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
        for (UMLComment comment : umlOperationBodyMapper.getCommentListDiff().getAddedComments()) {
            Comment commentAfter = Comment.of(comment, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(comment, umlOperationBodyMapper.getContainer2(), parentVersion);
                boolean commentedCode = false;
                if (umlOperationBodyMapper instanceof UMLOperationBodyMapper) {
	                for (Pair<AbstractCodeFragment, UMLComment> pair : ((UMLOperationBodyMapper)umlOperationBodyMapper).getCommentedCode()) {
	                	if (pair.getRight().equals(comment)) {
	                		commentedCode = true;
	                		break;
	                	}
	                }
                }
                if (commentedCode)
                	commentChangeHistory.handleAdd(commentBefore, commentAfter, "commented code");
                else
                	commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        if (umlOperationBodyMapper.getContainer1().getAnonymousClassList().size() < umlOperationBodyMapper.getContainer2().getAnonymousClassList().size() &&
        		umlOperationBodyMapper.getContainer1().getAnonymousClassList().size() == 0) {
        	for (int i=0; i<umlOperationBodyMapper.getContainer2().getAnonymousClassList().size(); i++) {
    			UMLAnonymousClass anonymous2 = umlOperationBodyMapper.getContainer2().getAnonymousClassList().get(i);
	    		for (UMLComment umlComment : anonymous2.getComments()) {
	    			Comment commentAfter = Comment.of(umlComment, umlOperationBodyMapper.getContainer2(), currentVersion);
	                if (commentAfter != null && equalOperator.test(commentAfter)) {
	                	Comment commentBefore = Comment.of(umlComment, umlOperationBodyMapper.getContainer2(), parentVersion);
	                	commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
	                    elements.addFirst(commentBefore);
	                    commentChangeHistory.connectRelatedNodes();
	                    return true;
	                }
	    		}
        	}
        }
        UMLJavadoc javadoc = umlOperationBodyMapper.getContainer2().getJavadoc();
    	if (javadoc != null) {
    		Comment commentAfter = Comment.of(javadoc, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadoc, umlOperationBodyMapper.getContainer2(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new javadoc");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
    	}
        return false;
    }

    public boolean checkBodyOfMatchedClasses(Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator, UMLAbstractClassDiff classDiff) {
        if (classDiff == null)
            return false;
        // check if it is in the matched
        if (isMatched(classDiff, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(classDiff, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
    	int matches = 0;
    	for (Pair<UMLComment, UMLComment> mapping : classDiff.getCommentListDiff().getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	for (Pair<UMLComment, UMLComment> mapping : classDiff.getPackageDeclarationCommentListDiff().getCommonComments()) {
            Comment commentAfter = Comment.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
            if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if (classDiff.getJavadocDiff().isPresent()) {
    		UMLJavadocDiff javadocDiff = classDiff.getJavadocDiff().get();
    		Comment commentAfter = Comment.of(javadocDiff.getJavadocAfter(), classDiff.getNextClass(), currentVersion);
    		if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadocDiff.getJavadocBefore(), classDiff.getOriginalClass(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
                }
                commentChangeHistory.connectRelatedNodes();
                matches++;
    		}
    	}
    	if (classDiff.getPackageDeclarationJavadocDiff().isPresent()) {
    		UMLJavadocDiff javadocDiff = classDiff.getPackageDeclarationJavadocDiff().get();
    		Comment commentAfter = Comment.of(javadocDiff.getJavadocAfter(), classDiff.getNextClass(), currentVersion);
    		if (commentAfter != null && equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(javadocDiff.getJavadocBefore(), classDiff.getOriginalClass(), parentVersion);
                if (!commentBefore.getComment().getText().equals(commentAfter.getComment().getText())) {
                    processChange(commentBefore, commentAfter);
                }
                else {
                    commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                }
                if(matches == 0) {
                	elements.addFirst(commentBefore);
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

    private boolean isAdded(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Comment> equalOperator) {
        for (UMLComment comment : classDiff.getCommentListDiff().getAddedComments()) {
            Comment commentAfter = Comment.of(comment, classDiff.getNextClass(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(comment, classDiff.getNextClass(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        for (UMLComment comment : classDiff.getPackageDeclarationCommentListDiff().getAddedComments()) {
            Comment commentAfter = Comment.of(comment, classDiff.getNextClass(), currentVersion);
            if (equalOperator.test(commentAfter)) {
                Comment commentBefore = Comment.of(comment, classDiff.getNextClass(), parentVersion);
                commentChangeHistory.handleAdd(commentBefore, commentAfter, "new comment");
                elements.addFirst(commentBefore);
                commentChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        if (classDiff.getNextClass() instanceof UMLClass) {
        	UMLJavadoc javadoc = ((UMLClass) classDiff.getNextClass()).getJavadoc();
        	if (javadoc != null) {
        		Comment commentAfter = Comment.of(javadoc, classDiff.getNextClass(), currentVersion);
                if (equalOperator.test(commentAfter)) {
                    Comment commentBefore = Comment.of(javadoc, classDiff.getNextClass(), parentVersion);
                    commentChangeHistory.handleAdd(commentBefore, commentAfter, "new javadoc");
                    elements.addFirst(commentBefore);
                    commentChangeHistory.connectRelatedNodes();
                    return true;
                }
        	}
        	UMLJavadoc packageDeclarationJavadoc = ((UMLClass) classDiff.getNextClass()).getPackageDeclarationJavadoc();
        	if (packageDeclarationJavadoc != null) {
        		Comment commentAfter = Comment.of(packageDeclarationJavadoc, classDiff.getNextClass(), currentVersion);
                if (equalOperator.test(commentAfter)) {
                    Comment commentBefore = Comment.of(packageDeclarationJavadoc, classDiff.getNextClass(), parentVersion);
                    commentChangeHistory.handleAdd(commentBefore, commentAfter, "new javadoc");
                    elements.addFirst(commentBefore);
                    commentChangeHistory.connectRelatedNodes();
                    return true;
                }
        	}
        }
        return false;
    }

    public boolean checkRefactoredMethod(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Comment rightComment, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            VariableDeclarationContainer operationBefore = null;
            VariableDeclarationContainer operationAfter = null;
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
                    boolean found = checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    public boolean checkRefactoredAttribute(Version currentVersion, Version parentVersion, Predicate<Attribute> equalAttribute, Comment rightComment, List<Refactoring> refactorings) {
    	for (Refactoring refactoring : refactorings) {
            UMLAttribute attributeBefore = null;
            UMLAttribute attributeAfter = null;
            Change.Type changeType = null;

            switch (refactoring.getRefactoringType()) {
                case PULL_UP_ATTRIBUTE: {
                    PullUpAttributeRefactoring pullUpAttributeRefactoring = (PullUpAttributeRefactoring) refactoring;
                    attributeBefore = pullUpAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = pullUpAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case PUSH_DOWN_ATTRIBUTE: {
                    PushDownAttributeRefactoring pushDownAttributeRefactoring = (PushDownAttributeRefactoring) refactoring;
                    attributeBefore = pushDownAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = pushDownAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case MOVE_ATTRIBUTE: {
                    MoveAttributeRefactoring moveAttributeRefactoring = (MoveAttributeRefactoring) refactoring;
                    attributeBefore = moveAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = moveAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case MOVE_RENAME_ATTRIBUTE: {
                    MoveAndRenameAttributeRefactoring moveAndRenameAttributeRefactoring = (MoveAndRenameAttributeRefactoring) refactoring;
                    attributeBefore = moveAndRenameAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = moveAndRenameAttributeRefactoring.getMovedAttribute();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case RENAME_ATTRIBUTE: {
                    RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring) refactoring;
                    attributeBefore = renameAttributeRefactoring.getOriginalAttribute();
                    attributeAfter = renameAttributeRefactoring.getRenamedAttribute();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case ADD_ATTRIBUTE_ANNOTATION: {
                    AddAttributeAnnotationRefactoring addAttributeAnnotationRefactoring = (AddAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = addAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = addAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_ATTRIBUTE_ANNOTATION: {
                    ModifyAttributeAnnotationRefactoring modifyAttributeAnnotationRefactoring = (ModifyAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = modifyAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = modifyAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_ATTRIBUTE_ANNOTATION: {
                    RemoveAttributeAnnotationRefactoring removeAttributeAnnotationRefactoring = (RemoveAttributeAnnotationRefactoring) refactoring;
                    attributeBefore = removeAttributeAnnotationRefactoring.getAttributeBefore();
                    attributeAfter = removeAttributeAnnotationRefactoring.getAttributeAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case CHANGE_ATTRIBUTE_TYPE: {
                    ChangeAttributeTypeRefactoring changeAttributeTypeRefactoring = (ChangeAttributeTypeRefactoring) refactoring;
                    attributeBefore = changeAttributeTypeRefactoring.getOriginalAttribute();
                    attributeAfter = changeAttributeTypeRefactoring.getChangedTypeAttribute();
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
                case ADD_ATTRIBUTE_MODIFIER: {
                    AddAttributeModifierRefactoring addAttributeModifierRefactoring = (AddAttributeModifierRefactoring) refactoring;
                    attributeBefore = addAttributeModifierRefactoring.getAttributeBefore();
                    attributeAfter = addAttributeModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_ATTRIBUTE_MODIFIER: {
                    RemoveAttributeModifierRefactoring removeAttributeModifierRefactoring = (RemoveAttributeModifierRefactoring) refactoring;
                    attributeBefore = removeAttributeModifierRefactoring.getAttributeBefore();
                    attributeAfter = removeAttributeModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case CHANGE_ATTRIBUTE_ACCESS_MODIFIER: {
                    ChangeAttributeAccessModifierRefactoring changeAttributeAccessModifierRefactoring = (ChangeAttributeAccessModifierRefactoring) refactoring;
                    attributeBefore = changeAttributeAccessModifierRefactoring.getAttributeBefore();
                    attributeAfter = changeAttributeAccessModifierRefactoring.getAttributeAfter();
                    changeType = Change.Type.ACCESS_MODIFIER_CHANGE;
                    break;
                }
                case EXTRACT_ATTRIBUTE: {
                    ExtractAttributeRefactoring extractAttributeRefactoring = (ExtractAttributeRefactoring) refactoring;
                    Attribute rightAttribute = Attribute.of(extractAttributeRefactoring.getVariableDeclaration(), currentVersion);
                    if (equalAttribute.test(rightAttribute)) {
                    	addedAttribute(rightAttribute, rightComment, parentVersion);
                    	return true;
                    }
                    break;
                }
            }
            if (attributeAfter != null) {
            	Attribute fieldAfter = Attribute.of(attributeAfter, currentVersion);
                if (equalAttribute.test(fieldAfter)) {
                	Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attributeBefore, attributeAfter);
                	boolean found = checkBodyOfMatchedAttributes(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, pair);
                	if (found)
                        return true;
                }
        	}
    	}
    	return false;
    }

    private Map<Pair<Comment, Comment>, List<Integer>> lineChangeMap = new LinkedHashMap<>();

	public void processChange(Comment commentBefore, Comment commentAfter) {
		commentChangeHistory.addChange(commentBefore, commentAfter, ChangeFactory.forComment(Change.Type.BODY_CHANGE));
		if (commentBefore.isMultiLine() || commentAfter.isMultiLine()) {
			try {
				Pair<Comment, Comment> pair = Pair.of(commentBefore, commentAfter);
				Comment startComment = getStart();
				if (startComment != null) {
					List<String> start = IOUtils.readLines(new StringReader(startComment.getComment().getFullText()));
					List<String> original = IOUtils.readLines(new StringReader(commentBefore.getComment().getFullText()));
					List<String> revised = IOUtils.readLines(new StringReader(commentAfter.getComment().getFullText()));
		
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
									int actualLine = startComment.getLocation().getStartLine() + index + j;
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
									int actualLine = startComment.getLocation().getStartLine() + index;
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
			String element = startCommentLines.get(i);
			if(line.equals(element)) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
	}

	public HistoryInfo<Comment> blameReturn() {
		List<HistoryInfo<Comment>> history = getHistory();
		for (History.HistoryInfo<Comment> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof BodyChange || change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
	}

	public HistoryInfo<Comment> blameReturn(Comment startComment, int exactLineNumber) {
		List<HistoryInfo<Comment>> history = getHistory();
		for (History.HistoryInfo<Comment> historyInfo : history) {
			Pair<Comment, Comment> pair = Pair.of(historyInfo.getElementBefore(), historyInfo.getElementAfter());
			boolean multiLine = startComment.isMultiLine();
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof Introduced) {
					return historyInfo;
				}
				else if (change instanceof BodyChange) {
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
			}
		}
		return null;
	}
}
