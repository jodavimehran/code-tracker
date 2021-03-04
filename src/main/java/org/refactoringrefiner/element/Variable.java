package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Version;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Variable extends BaseCodeElement<VariableDeclaration> {
    private final Method method;

    public Variable(VariableDeclaration info, UMLOperation operation, Version version) {
        super(info, version);
        this.method = new Method(operation, version);
    }

    @Override
    public String getIdentifierExcludeVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName());
        sb.append(":");
        sb.append(this.info.getType());
        return sb.toString();
    }

    @Override
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.method.getIdentifierExcludeVersion());
        sb.append("$");
        sb.append(this.info.getVariableName());
        return sb.toString();
    }

    @Override
    public String getName() {
        return this.info.getVariableName();
    }

    @Override
    public String getContainerName() {
        return this.method.getIdentifierExcludeVersion();
    }

    @Override
    public String getPackageName() {
        return this.method.getPackageName();
    }

    @Override
    public String getSourceFolder() {
        return this.method.getSourceFolder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable that = (Variable) o;

        return new EqualsBuilder()
                .append(this.info.getVariableName(), that.info.getVariableName())
                .append(this.info.getType(), that.info.getType())
                .append(this.info.getScope().getParentSignature(), that.info.getScope().getParentSignature())
                .append(this.method, that.method)
                .append(this.getVersion().getId(), that.getVersion().getId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 57)
                .append(this.info)
                .append(this.info.getType())
                .append(this.info.getScope().getParentSignature())
                .append(this.method)
                .append(this.getVersion().getId())
                .toHashCode();
    }

    @Override
    public String toString() {
        return getIdentifier();
    }

    @Override
    protected List<UMLAnnotation> getAnnotations() {
        return this.info.getAnnotations();
    }

    public static class VariableElementDiff extends BaseCodeElement.BaseElementDiff<Variable> {

        public VariableElementDiff(Variable leftSide, Variable rightSide) {
            super(leftSide, rightSide);
        }

        public Set<Refactoring> getRefactorings() {
            Set<Refactoring> results = new HashSet<>();

            boolean isTypeChanged = !this.leftSide.info.getType().equals(this.rightSide.info.getType());
            if (isTypeChanged) {
                results.add(new ChangeVariableTypeRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo(), Collections.EMPTY_SET));
            }

            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
            if (isRenamed) {
                results.add(new RenameVariableRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo(), Collections.EMPTY_SET));
            }

            if (!(isRenamed || isTypeChanged)) {
//                results.add(new ChangeVariableScopeRefactoring(this.leftSide.info, this.rightSide.info, this.leftSide.method.getInfo(), this.rightSide.method.getInfo()));
            }
            return results;
        }

    }
}
