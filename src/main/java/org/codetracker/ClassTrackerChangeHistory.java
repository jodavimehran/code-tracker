package org.codetracker;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Class;
import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.AddClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddClassModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeClassAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeTypeDeclarationKindRefactoring;
import gr.uom.java.xmi.diff.ExtractClassRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.ModifyClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.MovedClassToAnotherSourceFolder;
import gr.uom.java.xmi.diff.RemoveClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveClassModifierRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class ClassTrackerChangeHistory extends AbstractChangeHistory<Class> {
	private final ChangeHistory<Class> classChangeHistory = new ChangeHistory<>();
    private final String className;
    private final int classDeclarationLineNumber;

	public ClassTrackerChangeHistory(String className, int classDeclarationLineNumber) {
		this.className = className;
		this.classDeclarationLineNumber = classDeclarationLineNumber;
	}

	public ChangeHistory<Class> get() {
		return classChangeHistory;
	}

	public String getClassName() {
		return className;
	}

	public int getClassDeclarationLineNumber() {
		return classDeclarationLineNumber;
	}

    protected static Class getClass(UMLModel umlModel, Version version, Predicate<Class> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Class clazz = Class.of(umlClass, version);
                if (predicate.test(clazz))
                    return clazz;
            }
        return null;
    }

    public Set<Class> analyseClassRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        Set<Class> leftClassSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            UMLAbstractClass leftUMLClass = null;
            UMLAbstractClass rightUMLClass = null;
            Change.Type changeType = null;
            Change.Type changeType2 = null;
            switch (refactoring.getRefactoringType()) {
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        Class classAfter = Class.of(movedClassToAnotherSourceFolder.getMovedClass(), currentVersion);
                        if (equalOperator.test(classAfter)) {
                            leftUMLClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            rightUMLClass = movedClassToAnotherSourceFolder.getMovedClass();
                            changeType = Change.Type.CONTAINER_CHANGE;
                            break;
                        }
                    }
                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    leftUMLClass = moveClassRefactoring.getOriginalClass();
                    rightUMLClass = moveClassRefactoring.getMovedClass();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case RENAME_CLASS: {
                    RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                    leftUMLClass = renameClassRefactoring.getOriginalClass();
                    rightUMLClass = renameClassRefactoring.getRenamedClass();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    leftUMLClass = moveAndRenameClassRefactoring.getOriginalClass();
                    rightUMLClass = moveAndRenameClassRefactoring.getRenamedClass();
                    changeType = Change.Type.RENAME;
                    changeType2 = Change.Type.MOVED;
                    break;
                }
                case ADD_CLASS_ANNOTATION: {
                    AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) refactoring;
                    leftUMLClass = addClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = addClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_CLASS_ANNOTATION: {
                    RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) refactoring;
                    leftUMLClass = removeClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = removeClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_CLASS_ANNOTATION: {
                    ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) refactoring;
                    leftUMLClass = modifyClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = modifyClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case ADD_CLASS_MODIFIER: {
                    AddClassModifierRefactoring addClassModifierRefactoring = (AddClassModifierRefactoring) refactoring;
                    leftUMLClass = addClassModifierRefactoring.getClassBefore();
                    rightUMLClass = addClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_CLASS_MODIFIER: {
                    RemoveClassModifierRefactoring removeClassModifierRefactoring = (RemoveClassModifierRefactoring) refactoring;
                    leftUMLClass = removeClassModifierRefactoring.getClassBefore();
                    rightUMLClass = removeClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case CHANGE_CLASS_ACCESS_MODIFIER: {
                    ChangeClassAccessModifierRefactoring changeClassAccessModifierRefactoring = (ChangeClassAccessModifierRefactoring) refactoring;
                    leftUMLClass = changeClassAccessModifierRefactoring.getClassBefore();
                    rightUMLClass = changeClassAccessModifierRefactoring.getClassAfter();
                    changeType = Change.Type.ACCESS_MODIFIER_CHANGE;
                    break;
                }
                case EXTRACT_INTERFACE:
                case EXTRACT_SUPERCLASS: {
                    ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) refactoring;
                    leftUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    rightUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case EXTRACT_SUBCLASS:
                case EXTRACT_CLASS: {
                    ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) refactoring;
                    leftUMLClass = extractClassRefactoring.getExtractedClass();
                    rightUMLClass = extractClassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case CHANGE_TYPE_DECLARATION_KIND: {
                    ChangeTypeDeclarationKindRefactoring changeTypeDeclarationKindRefactoring = (ChangeTypeDeclarationKindRefactoring)refactoring;
                    leftUMLClass = changeTypeDeclarationKindRefactoring.getClassBefore();
                    rightUMLClass = changeTypeDeclarationKindRefactoring.getClassAfter();
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
            }

            if (rightUMLClass != null) {
                Class classAfter = Class.of(rightUMLClass, currentVersion);
                if (equalOperator.test(classAfter)) {
                    Class classBefore = Class.of(leftUMLClass, parentVersion);
                    if (Change.Type.INTRODUCED.equals(changeType)) {
                        classChangeHistory.handleAdd(classBefore, classAfter, refactoring.toString());
                    } else {
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType).refactoring(refactoring));
                    }
                    if (changeType2 != null)
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType2).refactoring(refactoring));
                    leftClassSet.add(classBefore);
                }
            }
        }

        if (!leftClassSet.isEmpty())
            classChangeHistory.connectRelatedNodes();
        return leftClassSet;
    }

    public boolean isClassAdded(UMLModelDiff modelDiff, ArrayDeque<Class> classes, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        List<UMLClass> addedClasses = modelDiff.getAddedClasses();
        for (UMLClass umlClass : addedClasses) {
            Class rightClass = Class.of(umlClass, currentVersion);
            if (equalOperator.test(rightClass)) {
                Class leftClass = Class.of(umlClass, parentVersion);
                classChangeHistory.handleAdd(leftClass, rightClass, "new class");
                classChangeHistory.connectRelatedNodes();
                classes.addFirst(leftClass);
                return true;
            }
        }
        return false;
    }
}
