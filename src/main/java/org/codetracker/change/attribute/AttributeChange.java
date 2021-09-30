package org.codetracker.change.attribute;

import org.codetracker.change.AbstractChange;
import org.refactoringminer.api.Refactoring;

public abstract class AttributeChange extends AbstractChange {
    private final Refactoring refactoring;

    public AttributeChange(Type type, Refactoring refactoring) {
        super(type);
        this.refactoring = refactoring;
    }

    public final Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
