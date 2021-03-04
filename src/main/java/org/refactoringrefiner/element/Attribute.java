package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Version;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Attribute extends BaseCodeElement<UMLAttribute> {

    public Attribute(UMLAttribute umlAttribute, Version version) {
        super(umlAttribute, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute that = (Attribute) o;

        return new EqualsBuilder()
                .append(getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()), getPath(that.info.getLocationInfo().getFilePath(), that.info.getClassName()))
                .append(this.info.getClassName(), that.info.getClassName())
                .append(this.info.getName(), that.info.getName())
                .append(this.info.getType(), that.info.getType())
                .append(this.getVersion().getId(), that.getVersion().getId())
                .isEquals()
                && new HashSet<>(this.info.getAnnotations()).containsAll(new HashSet<>(that.info.getAnnotations()));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 93)
                .append(getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()))
                .append(this.info.getClassName())
                .append(this.info.getName())
                .append(this.info.getType())
                .append(this.getVersion().getId())
                .append(this.info.getAnnotations())
                .toHashCode();
    }

    @Override
    public String toString() {
        return getIdentifierExcludeVersion();
    }

    @Override
    public String getIdentifierExcludeVersion() {
        return getFullName() +
                ":" +
                this.info.getType() +
                annotationsToString();
    }

    @Override
    public String getFullName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()) +
                this.info.getClassName() +
                "@" +
                this.info.getName();
    }

    @Override
    public String getName() {
        return this.info.getName();
    }

    @Override
    public String getContainerName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()) + this.info.getClassName();
    }

    @Override
    public String getSourceFolder() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getName());
    }

    @Override
    public String getPackageName() {
        return getPackage(this.info.getLocationInfo().getFilePath(), this.info.getClassName());
    }

    @Override
    protected List<UMLAnnotation> getAnnotations() {
        return this.info.getAnnotations();
    }

    public static class AttributeElementDiff extends BaseClassMemberElementDiff<Attribute> {

        public AttributeElementDiff(Attribute leftSide, Attribute rightSide) {
            super(leftSide, rightSide);
        }

        protected Refactoring getRenameRefactoring() {
            return new RenameAttributeRefactoring(leftSide.info, rightSide.info, new HashSet<>());
        }

        protected Refactoring getMoveRefactoring() {
            MoveAttributeRefactoring pullUpOrPushDownRefactoring = getPullUpOrPushDownRefactoring();
            if (pullUpOrPushDownRefactoring != null) return pullUpOrPushDownRefactoring;
            return new MoveAttributeRefactoring(leftSide.info, rightSide.info);
        }

        private MoveAttributeRefactoring getPullUpOrPushDownRefactoring() {
            if (isSameType(rightSide.info.getClassName(), leftSide.info.getSuperclass())) {
                return new PullUpAttributeRefactoring(leftSide.info, rightSide.info);
            }
            if (isSameType(leftSide.info.getClassName(), rightSide.info.getSuperclass())) {
                return new PushDownAttributeRefactoring(leftSide.info, rightSide.info);
            }
            return null;
        }

        protected Refactoring getMoveAndRenameRefactoring() {
            MoveAttributeRefactoring pullUpOrPushDownRefactoring = getPullUpOrPushDownRefactoring();
            if (pullUpOrPushDownRefactoring != null) return pullUpOrPushDownRefactoring;
            return new MoveAndRenameAttributeRefactoring(leftSide.info, rightSide.info, new HashSet<>());
        }

        protected Set<Refactoring> getOtherRefactorings() {
            return new UMLAttributeDiff(leftSide.info, rightSide.info, new ArrayList<>()).getRefactorings();
        }
    }
}
