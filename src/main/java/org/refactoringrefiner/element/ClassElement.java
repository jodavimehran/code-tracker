package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Version;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClassElement extends BaseCodeElement {

    protected final UMLClass info;

    public ClassElement(UMLClass info, Version version) {
        super(version);
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassElement that = (ClassElement) o;
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
                .toHashCode();
    }

    @Override
    public String toString() {
        return this.getFullName();
    }

    @Override
    public String getIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getSourceFolder());
        sb.append(this.info.getName());
        sb.append(this.getVersion().toString());
        return sb.toString();
    }

    @Override
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getSourceFolder());
        sb.append(this.info.getName());
        return sb.toString();
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
    
}
