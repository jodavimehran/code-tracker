package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringrefiner.api.Version;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MethodElement extends BaseCodeElement {
    private final UMLOperation info;

    public MethodElement(UMLOperation info, Version version) {
        super(version);
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodElement that = (MethodElement) o;

        boolean equals = new EqualsBuilder()
                .append(this.info.getName(), that.info.getName())
                .append(this.info.getClassName(), that.info.getClassName())
                .append(this.info.getLocationInfo().getFilePath(), that.info.getLocationInfo().getFilePath())
                .append(this.getVersion().getId(), that.getVersion().getId())
                .isEquals()
                && this.info.equalParameterTypes(that.info)
                && this.info.equalParameterNames(that.info)
                && new HashSet<>(this.info.getAnnotations()).containsAll(new HashSet<>(that.info.getAnnotations()));
        return equals;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 57)
                .append(this.info.getName())
                .append(this.info.getTypeParameters())
                .append(this.info.getReturnParameter())
                .append(this.info.getClassName())
                .append(this.info.getLocationInfo().getFilePath())
                .append(this.getVersion().getId())
                .append(this.info.getAnnotations())
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
        UMLParameter returnParameter = this.info.getReturnParameter();
        List<UMLParameter> parameters = new ArrayList(this.info.getParameters());
        parameters.remove(returnParameter);
        sb.append("(");
        for (int i = 0; i < parameters.size(); ++i) {
            UMLParameter parameter = parameters.get(i);
            if (parameter.getKind().equals("in")) {
                sb.append(parameter.toString().replace(" ", ":"));
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        sb.append(":");
        sb.append(returnParameter);
        if (!this.info.getAnnotations().isEmpty()) {
            sb.append("[");
            this.info.getAnnotations().stream().map(umlAnnotation -> umlAnnotation.toString()).collect(Collectors.joining(";"));
            sb.append("]");
        }
        sb.append(this.getVersion().toString());
        return sb.toString();
    }

    @Override
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()));
        sb.append(this.info.getClassName());
        sb.append('#');
        sb.append(this.info.getName());
        return sb.toString();
    }

    @Override
    public String getName() {
        return this.info.getName();
    }

    @Override
    public String getContainerName() {
        return getPath(this.info.getLocationInfo().getFilePath(), this.info.getClassName()) + this.info.getClassName();
    }

    public String getPackageName() {
        return getPackage(this.info.getLocationInfo().getFilePath(), this.info.getClassName());
    }

}
