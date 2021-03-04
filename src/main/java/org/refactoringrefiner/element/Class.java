package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Class extends BaseCodeElement<UMLClass> {

    public Class(UMLClass info, Version version) {
        super(info, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Class that = (Class) o;
        return new EqualsBuilder()
                .append(getPath(this.info.getLocationInfo().getFilePath(), this.info.getName()), getPath(that.info.getLocationInfo().getFilePath(), that.info.getName()))
                .append(this.info.getPackageName(), that.info.getPackageName())
                .append(this.info.getName(), that.info.getName())
                .append(this.getVersion().getId(), that.getVersion().getId())
                .isEquals()
                && new HashSet<>(this.info.getAnnotations()).containsAll(new HashSet<>(that.info.getAnnotations()));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getPath(this.info.getLocationInfo().getFilePath(), this.info.getName()))
                .append(this.info.getPackageName())
                .append(this.info.getName())
                .append(this.getVersion().getId())
                .append(this.info.getAnnotations())
                .toHashCode();
    }

    @Override
    public String toString() {
        return this.getFullName();
    }

    @Override
    public String getIdentifierExcludeVersion() {
        return this.getSourceFolder() + this.info.getName() + annotationsToString();
    }

    @Override
    protected List<UMLAnnotation> getAnnotations() {
        return this.info.getAnnotations();
    }

    @Override
    public String getFullName() {
        return this.getSourceFolder() + this.info.getName();
    }

    @Override
    public String getName() {
        return this.info.getName().replace(this.info.getPackageName(), "").replace(".", "");
    }

    @Override
    public String getSourceFolder() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getName());
    }

    @Override
    public String getPackageName() {
        return this.info.getPackageName();
    }

    @Override
    public String getContainerName() {
        return this.getSourceFolder() + this.getPackageName();
    }

    /**
     *
     */
    public static class ClassElementDiff extends BaseElementDiff<Class> {

        public ClassElementDiff(Class leftSide, Class rightSide) {
            super(leftSide, rightSide);
        }

        public Set<Refactoring> getRefactorings(HashMap<String, MoveSourceFolderRefactoring> moveSourceFolderRefactoringMap) {
            Set<Refactoring> results = new HashSet<>();


            boolean isSrcFolderChanged = !this.leftSide.getSourceFolder().equals(this.rightSide.getSourceFolder());
            if (isSrcFolderChanged) {
                MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder = new MovedClassToAnotherSourceFolder(this.leftSide.info, this.rightSide.info, this.leftSide.getSourceFolder(), this.rightSide.getSourceFolder());
                String renamePattern = movedClassToAnotherSourceFolder.getRenamePattern().toString();
                if (moveSourceFolderRefactoringMap.containsKey(renamePattern)) {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = moveSourceFolderRefactoringMap.get(renamePattern);
                    moveSourceFolderRefactoring.addMovedClassToAnotherSourceFolder(movedClassToAnotherSourceFolder);
                } else {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = new MoveSourceFolderRefactoring(movedClassToAnotherSourceFolder);
                    results.add(moveSourceFolderRefactoring);
                    moveSourceFolderRefactoringMap.put(renamePattern, moveSourceFolderRefactoring);
                }
            }

            boolean isMoved = !this.leftSide.getPackageName().equals(this.rightSide.getPackageName());
            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
            if (isMoved && isRenamed) {
                results.add(new MoveAndRenameClassRefactoring(this.leftSide.info, this.rightSide.info));
            } else if (isRenamed) {
                results.add(new RenameClassRefactoring(this.leftSide.info, this.rightSide.info));
            } else if (isMoved) {
                results.add(new MoveClassRefactoring(this.leftSide.info, this.rightSide.info));
            }


            UMLAnnotationListDiff annotationListDiff = new UMLAnnotationListDiff(this.leftSide.info.getAnnotations(), this.rightSide.info.getAnnotations());
            for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
                AddClassAnnotationRefactoring refactoring = new AddClassAnnotationRefactoring(annotation, this.leftSide.info, this.rightSide.info);
                results.add(refactoring);
            }
            for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
                RemoveClassAnnotationRefactoring refactoring = new RemoveClassAnnotationRefactoring(annotation, this.leftSide.info, this.rightSide.info);
                results.add(refactoring);
            }
            for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
                ModifyClassAnnotationRefactoring refactoring = new ModifyClassAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), this.leftSide.info, this.rightSide.info);
                results.add(refactoring);
            }

            return results;
        }
    }
}
