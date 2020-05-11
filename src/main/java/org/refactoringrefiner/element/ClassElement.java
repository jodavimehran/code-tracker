package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringrefiner.api.Version;

public class ClassElement extends BaseCodeElement {

    private final UMLClass info;

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
                .isEquals();
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
        return getFullName();
    }

    @Override
    public String getIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName());
        sb.append(this.getVersion().toString());
        return sb.toString();
    }

    @Override
    public String getFullName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getName()) + info.toString();
    }

    @Override
    public String getShortName() {
        return info.getName();
    }

    @Override
    public String getContainerName() {
        String srcFile = getPath(this.info.getLocationInfo().getFilePath(), this.info.getName());
        return srcFile + info.getPackageName();
    }

    @Override
    public String getPackageName() {
        return getContainerName();
    }


}
