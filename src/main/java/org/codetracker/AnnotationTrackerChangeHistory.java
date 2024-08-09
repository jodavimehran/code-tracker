package org.codetracker;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Annotation;
import org.codetracker.element.Attribute;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AddAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddAttributeModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.ModifyAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
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
import gr.uom.java.xmi.diff.UMLAnnotationDiff;
import gr.uom.java.xmi.diff.UMLAnnotationListDiff;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLDocumentationDiffProvider;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;

public class AnnotationTrackerChangeHistory extends AbstractChangeHistory<Annotation> {
	private final ChangeHistory<Annotation> annotationChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType annotationType;
    private final int annotationStartLineNumber;
    private final int annotationEndLineNumber;

	public AnnotationTrackerChangeHistory(String methodName, int methodDeclarationLineNumber, CodeElementType annotationType,
			int annotationStartLineNumber, int annotationEndLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
		this.annotationType = annotationType;
		this.annotationStartLineNumber = annotationStartLineNumber;
		this.annotationEndLineNumber = annotationEndLineNumber;
	}

	public ChangeHistory<Annotation> get() {
		return annotationChangeHistory;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getMethodDeclarationLineNumber() {
		return methodDeclarationLineNumber;
	}

	public CodeElementType getAnnotationType() {
		return annotationType;
	}

	public int getAnnotationStartLineNumber() {
		return annotationStartLineNumber;
	}

	public int getAnnotationEndLineNumber() {
		return annotationEndLineNumber;
	}

    public boolean isStartAnnotation(Annotation annotation) {
        return annotation.getAnnotation().getLocationInfo().getCodeElementType().equals(annotationType) &&
        		annotation.getAnnotation().getLocationInfo().getStartLine() == annotationStartLineNumber &&
                annotation.getAnnotation().getLocationInfo().getEndLine() == annotationEndLineNumber;
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

    public boolean checkForExtractionOrInline(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Annotation rightAnnotation, List<Refactoring> refactorings) {
        int extractMatches = 0;
    	for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        Annotation annotationBefore;
                        if (rightAnnotation.getOperation().isPresent())
                        	annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getOperation().get(), parentVersion);
                        else
                        	annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getClazz().get(), parentVersion);
                    	annotationChangeHistory.handleAdd(annotationBefore, rightAnnotation, extractOperationRefactoring.toString());
                        if(extractMatches == 0) {
                        	elements.addFirst(annotationBefore);
                        }
                        annotationChangeHistory.connectRelatedNodes();
                        extractMatches++;
                    }
                    break;
                }
                case MERGE_OPERATION: {
                    MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                    Method methodAfter = Method.of(mergeOperationRefactoring.getNewMethodAfterMerge(), currentVersion);
                    if (equalMethod.test(methodAfter)) {
                    	int mergeMatches = 0;
                        for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                        	if (bodyMapper.getOperationSignatureDiff().isPresent()) {
                    	    	for (Pair<UMLAnnotation, UMLAnnotation> mapping : bodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getCommonAnnotations()) {
                    	            Annotation annotationAfter = Annotation.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                    	            if (annotationAfter != null && rightAnnotation.equalIdentifierIgnoringVersion(annotationAfter)) {
                    	            	Annotation annotationBefore = Annotation.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
                    	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    	                if(mergeMatches == 0) {
                    	                	elements.addFirst(annotationBefore);
                    	                }
                    	                annotationChangeHistory.connectRelatedNodes();
                    	                mergeMatches++;
                    	            }
                    	        }
                    	    	for (UMLAnnotationDiff mapping : bodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getAnnotationDiffs()) {
                    	            Annotation annotationAfter = Annotation.of(mapping.getAddedAnnotation(), bodyMapper.getContainer2(), currentVersion);
                    	            if (annotationAfter != null && rightAnnotation.equalIdentifierIgnoringVersion(annotationAfter)) {
                    	            	Annotation annotationBefore = Annotation.of(mapping.getRemovedAnnotation(), bodyMapper.getContainer1(), parentVersion);
                    	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.forAnnotation(Change.Type.BODY_CHANGE));
                    	                if(mergeMatches == 0) {
                    	                	elements.addFirst(annotationBefore);
                    	                }
                    	                annotationChangeHistory.connectRelatedNodes();
                    	                mergeMatches++;
                    	            }
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
                            	if (bodyMapper.getOperationSignatureDiff().isPresent()) {
                        	    	for (Pair<UMLAnnotation, UMLAnnotation> mapping : bodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getCommonAnnotations()) {
                        	            Annotation annotationAfter = Annotation.of(mapping.getRight(), bodyMapper.getContainer2(), currentVersion);
                        	            if (annotationAfter != null && rightAnnotation.equalIdentifierIgnoringVersion(annotationAfter)) {
                        	            	Annotation annotationBefore = Annotation.of(mapping.getLeft(), bodyMapper.getContainer1(), parentVersion);
                        	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                        	                elements.addFirst(annotationBefore);
                        	                annotationChangeHistory.connectRelatedNodes();
                        	                return true;
                        	            }
                        	        }
                        	    	for (UMLAnnotationDiff mapping : bodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getAnnotationDiffs()) {
                        	            Annotation annotationAfter = Annotation.of(mapping.getAddedAnnotation(), bodyMapper.getContainer2(), currentVersion);
                        	            if (annotationAfter != null && rightAnnotation.equalIdentifierIgnoringVersion(annotationAfter)) {
                        	            	Annotation annotationBefore = Annotation.of(mapping.getRemovedAnnotation(), bodyMapper.getContainer1(), parentVersion);
                        	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.forAnnotation(Change.Type.BODY_CHANGE));
                        	                elements.addFirst(annotationBefore);
                        	                annotationChangeHistory.connectRelatedNodes();
                        	                return true;
                        	            }
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

    public boolean checkClassDiffForAnnotationChange(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Annotation> equalAnnotation, UMLAbstractClassDiff umlClassDiff) {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                // check if it is in the matched
                if (isMatched(operationBodyMapper, currentVersion, parentVersion, equalAnnotation))
                    return true;
                //Check if is added
                if (isAdded(operationBodyMapper, currentVersion, parentVersion, equalAnnotation))
                    return true;
            }
        }
        return false;
    }

    public boolean checkClassDiffForAnnotationChange(Version currentVersion, Version parentVersion, Attribute rightAttribute, Predicate<Annotation> equalAnnotation, UMLAbstractClassDiff umlClassDiff) {
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
    		Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(foundPair.getLeft(), foundPair.getRight());
    		found = checkBodyOfMatched(currentVersion, parentVersion, equalAnnotation, pair);
    	}
    	if (provider != null) {
    		Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(provider.getContainer1(), provider.getContainer2());
    		found = checkBodyOfMatched(currentVersion, parentVersion, equalAnnotation, pair);
    	}
    	return found;
    }

    public void addedMethod(Method rightMethod, Annotation rightAnnotation, Version parentVersion) {
    	Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightMethod.getUmlOperation(), parentVersion);
        annotationChangeHistory.handleAdd(annotationBefore, rightAnnotation, "added with method");
        elements.addFirst(annotationBefore);
        annotationChangeHistory.connectRelatedNodes();
    }

    public void addedAttribute(Attribute rightAttribute, Annotation rightAnnotation, Version parentVersion) {
    	Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAttribute.getUmlAttribute(), parentVersion);
    	annotationChangeHistory.handleAdd(annotationBefore, rightAnnotation, "added with attribute");
        elements.addFirst(annotationBefore);
        annotationChangeHistory.connectRelatedNodes();
    }

    public boolean checkBodyOfMatchedOperations(Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) {
        if (umlOperationBodyMapper == null)
            return false;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
    	int matches = 0;
    	if (umlOperationBodyMapper.getOperationSignatureDiff().isPresent()) {
	    	for (Pair<UMLAnnotation, UMLAnnotation> mapping : umlOperationBodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getCommonAnnotations()) {
	            Annotation annotationAfter = Annotation.of(mapping.getRight(), umlOperationBodyMapper.getContainer2(), currentVersion);
	            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
	            	Annotation annotationBefore = Annotation.of(mapping.getLeft(), umlOperationBodyMapper.getContainer1(), parentVersion);
	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
	                if(matches == 0) {
	                	elements.addFirst(annotationBefore);
	                }
	                annotationChangeHistory.connectRelatedNodes();
	                matches++;
	            }
	        }
	    	for (UMLAnnotationDiff mapping : umlOperationBodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getAnnotationDiffs()) {
	            Annotation annotationAfter = Annotation.of(mapping.getAddedAnnotation(), umlOperationBodyMapper.getContainer2(), currentVersion);
	            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
	            	Annotation annotationBefore = Annotation.of(mapping.getRemovedAnnotation(), umlOperationBodyMapper.getContainer1(), parentVersion);
	                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.forAnnotation(Change.Type.BODY_CHANGE));
	                if(matches == 0) {
	                	elements.addFirst(annotationBefore);
	                }
	                annotationChangeHistory.connectRelatedNodes();
	                matches++;
	            }
	        }
    	}
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
    	if (umlOperationBodyMapper.getOperationSignatureDiff().isPresent()) {
	    	for (UMLAnnotation annotation : umlOperationBodyMapper.getOperationSignatureDiff().get().getAnnotationListDiff().getAddedAnnotations()) {
	    		Annotation annotationAfter = Annotation.of(annotation, umlOperationBodyMapper.getContainer2(), currentVersion);
	            if (equalOperator.test(annotationAfter)) {
	            	Annotation annotationBefore = Annotation.of(annotation, umlOperationBodyMapper.getContainer2(), parentVersion);
	            	annotationChangeHistory.handleAdd(annotationBefore, annotationAfter, "new annotation");
	                elements.addFirst(annotationBefore);
	                annotationChangeHistory.connectRelatedNodes();
	                return true;
	            }
	        }
    	}
        return false;
    }

    public boolean checkBodyOfMatchedClasses(Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator, UMLAbstractClassDiff classDiff) {
        if (classDiff == null)
            return false;
        // check if it is in the matched
        if (isMatched(classDiff, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(classDiff, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
    	int matches = 0;
    	for (Pair<UMLAnnotation, UMLAnnotation> mapping : classDiff.getAnnotationListDiff().getCommonAnnotations()) {
    		Annotation annotationAfter = Annotation.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                if(matches == 0) {
                	elements.addFirst(annotationBefore);
                }
                annotationChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	for (UMLAnnotationDiff mapping : classDiff.getAnnotationListDiff().getAnnotationDiffs()) {
            Annotation annotationAfter = Annotation.of(mapping.getAddedAnnotation(), classDiff.getNextClass(), currentVersion);
            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(mapping.getRemovedAnnotation(), classDiff.getOriginalClass(), parentVersion);
                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.forAnnotation(Change.Type.BODY_CHANGE));
                if(matches == 0) {
                	elements.addFirst(annotationBefore);
                }
                annotationChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
        for (UMLAnnotation annotation : classDiff.getAnnotationListDiff().getAddedAnnotations()) {
        	Annotation annotationAfter = Annotation.of(annotation, classDiff.getNextClass(), currentVersion);
            if (equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(annotation, classDiff.getNextClass(), parentVersion);
            	annotationChangeHistory.handleAdd(annotationBefore, annotationAfter, "new annotation");
                elements.addFirst(annotationBefore);
                annotationChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        return false;
    }

    public boolean checkBodyOfMatched(Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator, Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair) {
    	if (pair == null)
    		return false;
    	// check if it is in the matched
        if (isMatched(pair, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(pair, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
    	int matches = 0;
    	UMLAnnotationListDiff annotationDiff = new UMLAnnotationListDiff(pair.getLeft().getAnnotations(), pair.getRight().getAnnotations());
    	for (Pair<UMLAnnotation, UMLAnnotation> mapping : annotationDiff.getCommonAnnotations()) {
    		Annotation annotationAfter = Annotation.of(mapping.getRight(), pair.getRight(), currentVersion);
            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(mapping.getLeft(), pair.getLeft(), parentVersion);
                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                if(matches == 0) {
                	elements.addFirst(annotationBefore);
                }
                annotationChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	for (UMLAnnotationDiff mapping : annotationDiff.getAnnotationDiffs()) {
            Annotation annotationAfter = Annotation.of(mapping.getAddedAnnotation(), pair.getRight(), currentVersion);
            if (annotationAfter != null && equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(mapping.getRemovedAnnotation(), pair.getLeft(), parentVersion);
                annotationChangeHistory.addChange(annotationBefore, annotationAfter, ChangeFactory.forAnnotation(Change.Type.BODY_CHANGE));
                if(matches == 0) {
                	elements.addFirst(annotationBefore);
                }
                annotationChangeHistory.connectRelatedNodes();
                matches++;
            }
        }
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair, Version currentVersion, Version parentVersion, Predicate<Annotation> equalOperator) {
    	UMLAnnotationListDiff annotationDiff = new UMLAnnotationListDiff(pair.getLeft().getAnnotations(), pair.getRight().getAnnotations());
    	for (UMLAnnotation annotation : annotationDiff.getAddedAnnotations()) {
    		Annotation annotationAfter = Annotation.of(annotation, pair.getRight(), currentVersion);
            if (equalOperator.test(annotationAfter)) {
            	Annotation annotationBefore = Annotation.of(annotation, pair.getRight(), parentVersion);
            	annotationChangeHistory.handleAdd(annotationBefore, annotationAfter, "new annotation");
                elements.addFirst(annotationBefore);
                annotationChangeHistory.connectRelatedNodes();
                return true;
            }
        }
    	return false;
    }

    public boolean checkRefactoredMethod(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Annotation rightAnnotation, List<Refactoring> refactorings) {
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
                    boolean found = checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    public boolean checkRefactoredAttribute(Version currentVersion, Version parentVersion, Predicate<Attribute> equalAttribute, Annotation rightAnnotation, List<Refactoring> refactorings) {
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
                    	addedAttribute(rightAttribute, rightAnnotation, parentVersion);
                    	return true;
                    }
                    break;
                }
            }
            if (attributeAfter != null) {
            	Attribute fieldAfter = Attribute.of(attributeAfter, currentVersion);
                if (equalAttribute.test(fieldAfter)) {
                	Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(attributeBefore, attributeAfter);
                	boolean found = checkBodyOfMatched(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, pair);
                	if (found)
                        return true;
                }
        	}
    	}
    	return false;
    }

	public HistoryInfo<Annotation> blameReturn() {
		List<HistoryInfo<Annotation>> history = getHistory();
		for (History.HistoryInfo<Annotation> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof BodyChange || change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
	}
}
