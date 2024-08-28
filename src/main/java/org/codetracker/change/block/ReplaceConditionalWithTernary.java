package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class ReplaceConditionalWithTernary extends BlockChange {
    private final Refactoring refactoring;

    public ReplaceConditionalWithTernary(Refactoring refactoring) {
        super(Type.REPLACE_LOOP_WITH_PIPELINE);
        this.refactoring = refactoring;
    }

    public Refactoring getReplaceConditionalWithTernaryRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
