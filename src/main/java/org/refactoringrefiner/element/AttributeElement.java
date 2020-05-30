package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringrefiner.api.Version;

public class AttributeElement extends BaseCodeElement {
    private final String className;
    private final VariableDeclaration variableInfo;

    public AttributeElement(UMLAttribute info, Version version) {
        super(version);
        this.className = info.getClassName();
        variableInfo = info.getVariableDeclaration();
    }

    public AttributeElement(VariableDeclaration variableInfo, String className, Version version) {
        super(version);
        this.className = className;
        this.variableInfo = variableInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeElement that = (AttributeElement) o;

        boolean equals = new EqualsBuilder()
                .append(getPath(this.variableInfo.getLocationInfo().getFilePath(), this.className), getPath(that.variableInfo.getLocationInfo().getFilePath(), that.className))
                .append(this.className, that.className)
                .append(this.variableInfo.getVariableName(), that.variableInfo.getVariableName())
                .append(this.variableInfo.getType(), that.variableInfo.getType())
                .append(this.getVersion().getId(), that.getVersion().getId())
                .isEquals();
        return equals;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(87, 93)
                .append(getPath(this.variableInfo.getLocationInfo().getFilePath(), this.className))
                .append(this.className)
                .append(this.variableInfo.getVariableName())
                .append(this.variableInfo.getType())
                .append(this.getVersion().getId())
                .toHashCode();
    }

    @Override
    public String toString() {
        return getIdentifier();
    }

    @Override
    public String getIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName());
        sb.append(":");
        sb.append(this.variableInfo.getType());
        sb.append(this.getVersion().toString());
        return sb.toString();
    }

    @Override
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPath(this.variableInfo.getLocationInfo().getFilePath(), this.className));
        sb.append(this.className);
        sb.append("@");
        sb.append(this.variableInfo.getVariableName());
        return sb.toString();
    }

    @Override
    public String getName() {
        return this.variableInfo.getVariableName();
    }

    @Override
    public String getContainerName() {
        return getPath(this.variableInfo.getLocationInfo().getFilePath(), this.className) + this.className;
    }

    @Override
    public String getPackageName() {
        return getPackage(this.variableInfo.getLocationInfo().getFilePath(), this.className);
    }
}
