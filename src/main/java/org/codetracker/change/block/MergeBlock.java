package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class MergeBlock extends BlockChange {
    private final Refactoring refactoring;

    public MergeBlock(Refactoring refactoring) {
        super(Type.BLOCK_MERGE);
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
