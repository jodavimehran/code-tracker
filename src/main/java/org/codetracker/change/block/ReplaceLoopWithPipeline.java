package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class ReplaceLoopWithPipeline extends BlockChange {
    private final Refactoring refactoring;

    public ReplaceLoopWithPipeline(Refactoring refactoring) {
        super(Type.REPLACE_LOOP_WITH_PIPELINE);
        this.refactoring = refactoring;
    }

    public Refactoring getReplaceLoopWithPipelineRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
