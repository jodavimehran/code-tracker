package org.codetracker.change.block;

import org.refactoringminer.api.Refactoring;

public class ReplacePipelineWithLoop extends BlockChange {
    private final Refactoring refactoring;

    public ReplacePipelineWithLoop(Refactoring refactoring) {
        super(Type.REPLACE_PIPELINE_WITH_LOOP);
        this.refactoring = refactoring;
    }

    public Refactoring getReplacePipelineWithLoopRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
