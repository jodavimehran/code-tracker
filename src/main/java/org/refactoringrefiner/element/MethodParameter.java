package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLParameter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MethodParameter {
    private final UMLParameter info;
    private final Set<UMLAnnotation> annotations;

    public MethodParameter(UMLParameter info) {
        this.info = info;
        annotations = new HashSet<>(info.getAnnotations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodParameter that = (MethodParameter) o;
        return Objects.equals(info, that.info) &&
                Objects.equals(info.getName(), that.info.getName()) &&
                Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, info.getName(), annotations);
    }

    @Override
    public String toString() {
        return info.toString().replace(" ", ":") + BaseCodeElement.annotationsToString(info.getAnnotations());
    }
}
