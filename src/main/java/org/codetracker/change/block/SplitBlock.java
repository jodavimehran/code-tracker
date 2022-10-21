package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class SplitBlock extends BlockChange {
    private final Refactoring refactoring;

    public SplitBlock(Refactoring refactoring) {
        super(Type.BLOCK_SPLIT);
        this.refactoring = refactoring;
    }

    public Refactoring getSplitRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
