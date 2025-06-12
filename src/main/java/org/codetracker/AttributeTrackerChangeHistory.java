package org.codetracker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.codetracker.change.attribute.AttributeAnnotationChange;
import org.codetracker.change.attribute.AttributeCrossFileChange;
import org.codetracker.change.Change.Type;
import org.codetracker.element.Attribute;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AddAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddAttributeModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import gr.uom.java.xmi.diff.ModifyAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.MovedClassToAnotherSourceFolder;
import gr.uom.java.xmi.diff.PullUpAttributeRefactoring;
import gr.uom.java.xmi.diff.PushDownAttributeRefactoring;
import gr.uom.java.xmi.diff.RemoveAttributeAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveAttributeModifierRefactoring;
import gr.uom.java.xmi.diff.RenameAttributeRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithClassRefactoring;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLClassRenameDiff;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class AttributeTrackerChangeHistory extends AbstractChangeHistory<Attribute> {
	private final ChangeHistory<Attribute> attributeChangeHistory = new ChangeHistory<>();
    private final String attributeName;
    private final int attributeDeclarationLineNumber;

	public AttributeTrackerChangeHistory(String attributeName, int attributeDeclarationLineNumber) {
		this.attributeName = attributeName;
		this.attributeDeclarationLineNumber = attributeDeclarationLineNumber;
	}

	public ChangeHistory<Attribute> get() {
		return attributeChangeHistory;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public int getAttributeDeclarationLineNumber() {
		return attributeDeclarationLineNumber;
	}

    public boolean isStartAttribute(Attribute attribute) {
        return attribute.getUmlAttribute().getName().equals(attributeName) &&
                attribute.getUmlAttribute().getLocationInfo().getStartLine() <= attributeDeclarationLineNumber &&
                attribute.getUmlAttribute().getLocationInfo().getEndLine() >= attributeDeclarationLineNumber;
    }

    public Set<Attribute> analyseAttributeRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator) {
        Set<Attribute> leftAttributeSet = new HashSet<>();
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
                    addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, refactoring, attributeBefore, attributeAfter, Change.Type.RENAME);
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
                    if (equalOperator.test(rightAttribute)) {
                        Attribute leftAttribute = Attribute.of(extractAttributeRefactoring.getVariableDeclaration(), parentVersion);
                        attributeChangeHistory.handleAdd(leftAttribute, rightAttribute , refactoring.toString());
                        attributeChangeHistory.connectRelatedNodes();
                        leftAttributeSet.add(leftAttribute);
                        return leftAttributeSet;
                    }
                    break;
                }

            }

            addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, refactoring, attributeBefore, attributeAfter, changeType);
        }
        attributeChangeHistory.connectRelatedNodes();
        return leftAttributeSet;
    }

    private boolean addAttributeChange(Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, Set<Attribute> leftAttributeSet, Refactoring refactoring, UMLAttribute umlAttributeBefore, UMLAttribute umlAttributeAfter, Change.Type changeType) {
        if (umlAttributeAfter != null) {
            Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
            if (equalOperator.test(attributeAfter)) {
                Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
                if (refactoring != null)
                	attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(changeType).refactoring(refactoring));
                else
                	attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(changeType));
                leftAttributeSet.add(attributeBefore);
                AbstractExpression leftInitializer = umlAttributeBefore.getVariableDeclaration().getInitializer();
				AbstractExpression rightInitializer = umlAttributeAfter.getVariableDeclaration().getInitializer();
				if (leftInitializer != null && rightInitializer != null) {
                	if (!leftInitializer.getString().equals(rightInitializer.getString())) {
                		attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Type.INITIALIZER_CHANGE));
                	}
                }
				else if (leftInitializer == null && rightInitializer != null) {
					attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Type.INITIALIZER_ADDED));
				}
				else if (leftInitializer != null && rightInitializer == null) {
					attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Type.INITIALIZER_REMOVED));
				}
				processChange(attributeBefore, attributeAfter);
                return true;
            }
        }
        return false;
    }

    public boolean isAttributeAdded(UMLModelDiff modelDiff, String sourceFolder, String className, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, List<UMLClassBaseDiff> allClassesDiff) {
        List<UMLAttribute> addedAttributes = allClassesDiff
                .stream()
                .map(UMLClassBaseDiff::getAddedAttributes)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLAttribute umlAttribute : addedAttributes) {
            if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "new attribute"))
                return true;
        }
        
        List<UMLEnumConstant> addedEnumConstants = allClassesDiff
                .stream()
                .map(UMLClassBaseDiff::getAddedEnumConstants)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        for (UMLEnumConstant umlAttribute : addedEnumConstants) {
            if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "new enum constant"))
                return true;
        }

        UMLClass addedClass = modelDiff.getAddedClass(sourceFolder, className);
        if (addedClass != null) {
            for (UMLAttribute umlAttribute : addedClass.getAttributes()) {
                if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new class"))
                    return true;
            }
            for (UMLEnumConstant umlAttribute : addedClass.getEnumConstants()) {
                if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new class"))
                    return true;
            }
        }

        for (UMLClassRenameDiff classRenameDiff : modelDiff.getClassRenameDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classRenameDiff.getAddedAnonymousClasses()) {
                for (UMLAttribute umlAttribute : addedAnonymousClass.getAttributes()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
                for (UMLEnumConstant umlAttribute : addedAnonymousClass.getEnumConstants()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassMoveDiff classMoveDiff : modelDiff.getClassMoveDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classMoveDiff.getAddedAnonymousClasses()) {
                for (UMLAttribute umlAttribute : addedAnonymousClass.getAttributes()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
                for (UMLEnumConstant umlAttribute : addedAnonymousClass.getEnumConstants()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
            }
        }

        for (UMLClassDiff classDiff : modelDiff.getCommonClassDiffList()) {
            for (UMLAnonymousClass addedAnonymousClass : classDiff.getAddedAnonymousClasses()) {
                for (UMLAttribute umlAttribute : addedAnonymousClass.getAttributes()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
                for (UMLEnumConstant umlAttribute : addedAnonymousClass.getEnumConstants()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new anonymous class"))
                        return true;
                }
            }
        }
        for (Refactoring r : modelDiff.getDetectedRefactorings()) {
        	if (r instanceof ReplaceAnonymousWithClassRefactoring) {
        		ReplaceAnonymousWithClassRefactoring replaceAnonymous = (ReplaceAnonymousWithClassRefactoring)r;
        		for (UMLAttribute umlAttribute : replaceAnonymous.getAddedClass().getAttributes()) {
                    if (handleAddAttribute(currentVersion, parentVersion, equalOperator, umlAttribute, "added with new class"))
                        return true;
                }
        	}
        }
        return false;
    }

    private boolean handleAddAttribute(Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, UMLAttribute umlAttribute, String comment) {
        Attribute rightAttribute = Attribute.of(umlAttribute, currentVersion);
        if (equalOperator.test(rightAttribute)) {
            Attribute leftAttribute = Attribute.of(umlAttribute, parentVersion);
            attributeChangeHistory.handleAdd(leftAttribute, rightAttribute, comment);
            attributeChangeHistory.connectRelatedNodes();
            elements.addFirst(leftAttribute);
            return true;
        }
        return false;
    }

    public Set<Attribute> isAttributeContainerChanged(UMLModelDiff umlModelDiffAll, Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, List<UMLClassMoveDiff> classMoveDiffList) {
        Set<Attribute> leftAttributeSet = new HashSet<>();
        Change.Type changeType = Change.Type.CONTAINER_CHANGE;

        for (UMLClassMoveDiff umlClassMoveDiff : classMoveDiffList) {
            for (UMLAttributeDiff attributeDiff : umlClassMoveDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLAttribute, UMLAttribute> pair : umlClassMoveDiff.getCommonAtrributes()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (UMLEnumConstantDiff attributeDiff : umlClassMoveDiff.getEnumConstantDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedEnumConstant(), attributeDiff.getAddedEnumConstant(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLEnumConstant, UMLEnumConstant> pair : umlClassMoveDiff.getCommonEnumConstants()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }

        for (UMLClassRenameDiff umlClassRenameDiff : umlModelDiffAll.getClassRenameDiffList()) {
            for (UMLAttributeDiff attributeDiff : umlClassRenameDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new RenameClassRefactoring(umlClassRenameDiff.getOriginalClass(), umlClassRenameDiff.getRenamedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLAttribute, UMLAttribute> pair : umlClassRenameDiff.getCommonAtrributes()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new RenameClassRefactoring(umlClassRenameDiff.getOriginalClass(), umlClassRenameDiff.getRenamedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (UMLEnumConstantDiff attributeDiff : umlClassRenameDiff.getEnumConstantDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new RenameClassRefactoring(umlClassRenameDiff.getOriginalClass(), umlClassRenameDiff.getRenamedClass()), attributeDiff.getRemovedEnumConstant(), attributeDiff.getAddedEnumConstant(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLEnumConstant, UMLEnumConstant> pair : umlClassRenameDiff.getCommonEnumConstants()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new RenameClassRefactoring(umlClassRenameDiff.getOriginalClass(), umlClassRenameDiff.getRenamedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }
        for (UMLClassMoveDiff umlClassMoveDiff : umlModelDiffAll.getInnerClassMoveDiffList()) {
            for (UMLAttributeDiff attributeDiff : umlClassMoveDiff.getAttributeDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLAttribute, UMLAttribute> pair : umlClassMoveDiff.getCommonAtrributes()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (UMLEnumConstantDiff attributeDiff : umlClassMoveDiff.getEnumConstantDiffList()) {
                if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), attributeDiff.getRemovedEnumConstant(), attributeDiff.getAddedEnumConstant(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
            for (Pair<UMLEnumConstant, UMLEnumConstant> pair : umlClassMoveDiff.getCommonEnumConstants()) {
            	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass()), pair.getLeft(), pair.getRight(), changeType)) {
                    attributeChangeHistory.connectRelatedNodes();
                    return leftAttributeSet;
                }
            }
        }
        
        for (UMLClassDiff classDiff : umlModelDiffAll.getCommonClassDiffList()) {
        	for (UMLOperationBodyMapper umlOperationBodyMapper : classDiff.getOperationBodyMapperList()) {
        		for (UMLAnonymousClassDiff anonymousClassDiff : umlOperationBodyMapper.getAnonymousClassDiffs()) {
        			for (UMLAttributeDiff attributeDiff : anonymousClassDiff.getAttributeDiffList()) {
                        if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, null, attributeDiff.getRemovedAttribute(), attributeDiff.getAddedAttribute(), changeType)) {
                            attributeChangeHistory.connectRelatedNodes();
                            return leftAttributeSet;
                        }
                    }
                    for (Pair<UMLAttribute, UMLAttribute> pair : anonymousClassDiff.getCommonAtrributes()) {
                    	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, null, pair.getLeft(), pair.getRight(), changeType)) {
                            attributeChangeHistory.connectRelatedNodes();
                            return leftAttributeSet;
                        }
                    }
                    for (UMLEnumConstantDiff attributeDiff : anonymousClassDiff.getEnumConstantDiffList()) {
                        if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, null, attributeDiff.getRemovedEnumConstant(), attributeDiff.getAddedEnumConstant(), changeType)) {
                            attributeChangeHistory.connectRelatedNodes();
                            return leftAttributeSet;
                        }
                    }
                    for (Pair<UMLEnumConstant, UMLEnumConstant> pair : anonymousClassDiff.getCommonEnumConstants()) {
                    	if (addAttributeChange(currentVersion, parentVersion, equalOperator, leftAttributeSet, null, pair.getLeft(), pair.getRight(), changeType)) {
                            attributeChangeHistory.connectRelatedNodes();
                            return leftAttributeSet;
                        }
                    }
        		}
        	}
        }

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType() == RefactoringType.MOVE_SOURCE_FOLDER) {
                MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                    UMLClass originalClass = movedClassToAnotherSourceFolder.getOriginalClass();
                    UMLClass movedClass = movedClassToAnotherSourceFolder.getMovedClass();
                    if (checkAttributeContainerChangeInMovedClasses(originalClass, movedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                        return leftAttributeSet;
                    }
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.MOVE_CLASS) {
                MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                UMLClass originalClass = moveClassRefactoring.getOriginalClass();
                UMLClass movedClass = moveClassRefactoring.getMovedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, movedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.RENAME_CLASS) {
                RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                UMLClass originalClass = renameClassRefactoring.getOriginalClass();
                UMLClass renamedClass = renameClassRefactoring.getRenamedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, renamedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            } else if (refactoring.getRefactoringType() == RefactoringType.MOVE_RENAME_CLASS) {
                MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                UMLClass originalClass = moveAndRenameClassRefactoring.getOriginalClass();
                UMLClass renamedClass = moveAndRenameClassRefactoring.getRenamedClass();
                if (checkAttributeContainerChangeInMovedClasses(originalClass, renamedClass, currentVersion, parentVersion, equalOperator, refactoring, leftAttributeSet)) {
                    return leftAttributeSet;
                }
            }
        }
        return Collections.emptySet();
    }

    private boolean checkAttributeContainerChangeInMovedClasses(UMLClass originalClass, UMLClass movedClass, Version currentVersion, Version parentVersion, Predicate<Attribute> equalOperator, Refactoring refactoring, Set<Attribute> leftAttributeSet) {
        for (UMLAttribute umlAttributeAfter : movedClass.getAttributes()) {
            Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
            if (equalOperator.test(attributeAfter)) {
                for (UMLAttribute umlAttributeBefore : originalClass.getAttributes()) {
                    if (umlAttributeAfter.equals(umlAttributeBefore)) {
                        Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
                        attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Change.Type.CONTAINER_CHANGE).refactoring(refactoring));
                        attributeChangeHistory.connectRelatedNodes();
                        leftAttributeSet.add(attributeBefore);
                        return true;
                    }
                }
                for (UMLAttribute umlAttributeBefore : originalClass.getEnumConstants()) {
                    if (umlAttributeAfter.equals(umlAttributeBefore)) {
                        Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
                        attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Change.Type.CONTAINER_CHANGE).refactoring(refactoring));
                        attributeChangeHistory.connectRelatedNodes();
                        leftAttributeSet.add(attributeBefore);
                        return true;
                    }
                }
            }
        }
        for (UMLAnonymousClass anonymousClassAfter : movedClass.getAnonymousClassList()) {
        	for (UMLAttribute umlAttributeAfter : anonymousClassAfter.getAttributes()) {
                Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
                if (equalOperator.test(attributeAfter)) {
                	for (UMLAnonymousClass anonymousClassBefore : originalClass.getAnonymousClassList()) {
	                	for (UMLAttribute umlAttributeBefore : anonymousClassBefore.getAttributes()) {
	                        if (umlAttributeAfter.equals(umlAttributeBefore)) {
	                            Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
	                            attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Change.Type.CONTAINER_CHANGE).refactoring(refactoring));
	                            attributeChangeHistory.connectRelatedNodes();
	                            leftAttributeSet.add(attributeBefore);
	                            return true;
	                        }
	                    }
                	}
                }
            }
        	for (UMLAttribute umlAttributeAfter : anonymousClassAfter.getEnumConstants()) {
                Attribute attributeAfter = Attribute.of(umlAttributeAfter, currentVersion);
                if (equalOperator.test(attributeAfter)) {
                	for (UMLAnonymousClass anonymousClassBefore : originalClass.getAnonymousClassList()) {
	                	for (UMLAttribute umlAttributeBefore : anonymousClassBefore.getEnumConstants()) {
	                        if (umlAttributeAfter.equals(umlAttributeBefore)) {
	                            Attribute attributeBefore = Attribute.of(umlAttributeBefore, parentVersion);
	                            attributeChangeHistory.addChange(attributeBefore, attributeAfter, ChangeFactory.forAttribute(Change.Type.CONTAINER_CHANGE).refactoring(refactoring));
	                            attributeChangeHistory.connectRelatedNodes();
	                            leftAttributeSet.add(attributeBefore);
	                            return true;
	                        }
	                    }
                	}
                }
        	}
        }
        return false;
    }

    private Map<Pair<Attribute, Attribute>, List<Integer>> lineChangeMap = new LinkedHashMap<>();

	public void processChange(Attribute attributeBefore, Attribute attributeAfter) {
		if (attributeBefore.isMultiLine() || attributeAfter.isMultiLine()) {
			try {
				Pair<Attribute, Attribute> pair = Pair.of(attributeBefore, attributeAfter);
				Attribute startAttribute = getStart();
				if (startAttribute != null) {
					List<String> start = IOUtils.readLines(new StringReader(startAttribute.getUmlAttribute().getVariableDeclaration().getActualSignature()));
					List<String> original = IOUtils.readLines(new StringReader(attributeBefore.getUmlAttribute().getVariableDeclaration().getActualSignature()));
					List<String> revised = IOUtils.readLines(new StringReader(attributeAfter.getUmlAttribute().getVariableDeclaration().getActualSignature()));
		
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
									int actualLine = startAttribute.signatureStartLine() + index + j;
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
									int actualLine = startAttribute.signatureStartLine() + index;
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

	public HistoryInfo<Attribute> blameReturn() {
		List<HistoryInfo<Attribute>> history = getHistory();
		for (History.HistoryInfo<Attribute> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (!(change instanceof AttributeCrossFileChange) && !(change instanceof AttributeAnnotationChange)) {
					return historyInfo;
				}
				//handle case where annotation is in the same line with the attribute declaration
				if (change instanceof AttributeAnnotationChange) {
					UMLAttribute attributeAfter = historyInfo.getElementAfter().getUmlAttribute();
					if (attributeAfter.getAnnotations().size() > 0) {
						int sameLineAnnotations = 0;
						for (UMLAnnotation annotation : attributeAfter.getAnnotations()) {
							if (annotation.getLocationInfo().getStartLine() == historyInfo.getElementAfter().getLocation().getStartLine()) {
								sameLineAnnotations++;
							}
						}
						if (sameLineAnnotations == attributeAfter.getAnnotations().size()) {
							return historyInfo;
						}
					}
					UMLAttribute attributeBefore = historyInfo.getElementBefore().getUmlAttribute();
					if (attributeBefore.getAnnotations().size() > 0) {
						int sameLineAnnotations = 0;
						for (UMLAnnotation annotation : attributeBefore.getAnnotations()) {
							if (annotation.getLocationInfo().getStartLine() == historyInfo.getElementBefore().getLocation().getStartLine()) {
								sameLineAnnotations++;
							}
						}
						if (sameLineAnnotations == attributeBefore.getAnnotations().size()) {
							return historyInfo;
						}
					}
				}
			}
		}
		return null;
	}

	public HistoryInfo<Attribute> blameReturn(Attribute startAttribute, int exactLineNumber) {
		List<HistoryInfo<Attribute>> history = getHistory();
		for (History.HistoryInfo<Attribute> historyInfo : history) {
			Pair<Attribute, Attribute> pair = Pair.of(historyInfo.getElementBefore(), historyInfo.getElementAfter());
			boolean multiLine = startAttribute.isMultiLine();
			for (Change change : historyInfo.getChangeList()) {
				if (!(change instanceof AttributeCrossFileChange) && !(change instanceof AttributeAnnotationChange)) {
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
				//handle case where annotation is in the same line with the attribute declaration
				if (change instanceof AttributeAnnotationChange) {
					UMLAttribute attributeAfter = historyInfo.getElementAfter().getUmlAttribute();
					if (attributeAfter.getAnnotations().size() > 0) {
						int sameLineAnnotations = 0;
						for (UMLAnnotation annotation : attributeAfter.getAnnotations()) {
							if (annotation.getLocationInfo().getStartLine() == historyInfo.getElementAfter().getLocation().getStartLine()) {
								sameLineAnnotations++;
							}
						}
						if (sameLineAnnotations == attributeAfter.getAnnotations().size()) {
							return historyInfo;
						}
					}
					UMLAttribute attributeBefore = historyInfo.getElementBefore().getUmlAttribute();
					if (attributeBefore.getAnnotations().size() > 0) {
						int sameLineAnnotations = 0;
						for (UMLAnnotation annotation : attributeBefore.getAnnotations()) {
							if (annotation.getLocationInfo().getStartLine() == historyInfo.getElementBefore().getLocation().getStartLine()) {
								sameLineAnnotations++;
							}
						}
						if (sameLineAnnotations == attributeBefore.getAnnotations().size()) {
							return historyInfo;
						}
					}
				}
				if (change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
	}

	public void checkInitializerChange(Attribute rightAttribute, Attribute leftAttribute) {
		AbstractExpression leftInitializer = leftAttribute.getUmlAttribute().getVariableDeclaration().getInitializer();
		AbstractExpression rightInitializer = rightAttribute.getUmlAttribute().getVariableDeclaration().getInitializer();
		if (leftInitializer != null && rightInitializer != null) {
			if (!leftInitializer.getString().equals(rightInitializer.getString())) {
				attributeChangeHistory.addChange(leftAttribute, rightAttribute, ChangeFactory.forAttribute(Type.INITIALIZER_CHANGE));
		        add(leftAttribute);
		        attributeChangeHistory.connectRelatedNodes();
		        processChange(leftAttribute, rightAttribute);
			}
		}
		else if (leftInitializer == null && rightInitializer != null) {
			attributeChangeHistory.addChange(leftAttribute, rightAttribute, ChangeFactory.forAttribute(Type.INITIALIZER_ADDED));
			add(leftAttribute);
			attributeChangeHistory.connectRelatedNodes();
		}
		else if (leftInitializer != null && rightInitializer == null) {
			attributeChangeHistory.addChange(leftAttribute, rightAttribute, ChangeFactory.forAttribute(Type.INITIALIZER_REMOVED));
			add(leftAttribute);
			attributeChangeHistory.connectRelatedNodes();
		}
	}
}
