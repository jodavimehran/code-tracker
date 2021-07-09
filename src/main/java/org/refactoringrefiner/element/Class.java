package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import org.refactoringrefiner.api.Version;
import org.refactoringrefiner.util.Util;

import java.util.List;

public class Class extends BaseCodeElement {
    private final UMLClass umlClass;

    private Class(UMLClass umlClass, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlClass = umlClass;
    }

    public static Class of(UMLClass umlClass, Version version) {
        String sourceFolder = Util.getPath(umlClass.getLocationInfo().getFilePath(), umlClass.getName());
        String packageName = umlClass.getPackageName();
        String name = umlClass.getName().replace(umlClass.getPackageName(), "").replace(".", "");
        String identifierExcludeVersion = String.format("%s%s.%s%s", sourceFolder, packageName, name, Util.annotationsToString(umlClass.getAnnotations()));
        return new Class(umlClass, identifierExcludeVersion, String.format("%s%s.%s", sourceFolder, packageName, name), umlClass.getLocationInfo().getFilePath(), version);
    }

    public List<UMLOperation> getOperations() {
        return umlClass.getOperations();
    }

    public List<UMLAttribute> getAttributes() {
        return umlClass.getAttributes();
    }


//    /**
//     *
//     */
//    public static class ClassElementDiff extends BaseElementDiff<Class> {
//
//        public ClassElementDiff(Class leftSide, Class rightSide) {
//            super(leftSide, rightSide);
//        }
//
//        public Set<Refactoring> getRefactorings(HashMap<String, MoveSourceFolderRefactoring> moveSourceFolderRefactoringMap) {
//            Set<Refactoring> results = new HashSet<>();
//
//
//            boolean isSrcFolderChanged = !this.leftSide.getSourceFolder().equals(this.rightSide.getSourceFolder());
//            if (isSrcFolderChanged) {
//                MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder = new MovedClassToAnotherSourceFolder(this.leftSide.info, this.rightSide.info, this.leftSide.getSourceFolder(), this.rightSide.getSourceFolder());
//                String renamePattern = movedClassToAnotherSourceFolder.getRenamePattern().toString();
//                if (moveSourceFolderRefactoringMap.containsKey(renamePattern)) {
//                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = moveSourceFolderRefactoringMap.get(renamePattern);
//                    moveSourceFolderRefactoring.addMovedClassToAnotherSourceFolder(movedClassToAnotherSourceFolder);
//                } else {
//                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = new MoveSourceFolderRefactoring(movedClassToAnotherSourceFolder);
//                    results.add(moveSourceFolderRefactoring);
//                    moveSourceFolderRefactoringMap.put(renamePattern, moveSourceFolderRefactoring);
//                }
//            }
//
//            boolean isMoved = !this.leftSide.getPackageName().equals(this.rightSide.getPackageName());
//            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
//            if (isMoved && isRenamed) {
//                results.add(new MoveAndRenameClassRefactoring(this.leftSide.info, this.rightSide.info));
//            } else if (isRenamed) {
//                results.add(new RenameClassRefactoring(this.leftSide.info, this.rightSide.info));
//            } else if (isMoved) {
//                results.add(new MoveClassRefactoring(this.leftSide.info, this.rightSide.info));
//            }
//
//
//            UMLAnnotationListDiff annotationListDiff = new UMLAnnotationListDiff(this.leftSide.info.getAnnotations(), this.rightSide.info.getAnnotations());
//            for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
//                AddClassAnnotationRefactoring refactoring = new AddClassAnnotationRefactoring(annotation, this.leftSide.info, this.rightSide.info);
//                results.add(refactoring);
//            }
//            for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
//                RemoveClassAnnotationRefactoring refactoring = new RemoveClassAnnotationRefactoring(annotation, this.leftSide.info, this.rightSide.info);
//                results.add(refactoring);
//            }
//            for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
//                ModifyClassAnnotationRefactoring refactoring = new ModifyClassAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), this.leftSide.info, this.rightSide.info);
//                results.add(refactoring);
//            }
//
//            return results;
//        }
//    }
}
