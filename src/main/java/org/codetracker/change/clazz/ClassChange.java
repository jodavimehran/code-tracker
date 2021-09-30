package org.codetracker.change.clazz;

import org.codetracker.change.AbstractChange;
import org.refactoringminer.api.Refactoring;

public abstract class ClassChange extends AbstractChange {
    private final Refactoring refactoring;

    public ClassChange(Type type, Refactoring refactoring) {
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
