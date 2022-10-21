package org.codetracker.change.block;

import org.codetracker.api.CodeElement;
import org.codetracker.change.EvolutionHook;
import org.codetracker.change.Introduced;
import org.codetracker.element.Block;
import org.refactoringminer.api.Refactoring;

import java.util.Optional;

public class SplitBlockWithEvolutionHook extends Introduced {
    private final CodeElement hookedElement;

    public SplitBlockWithEvolutionHook(Refactoring refactoring, CodeElement addedElement, CodeElement hookedElement) {
        super(Type.BLOCK_SPLIT, addedElement, refactoring);
        this.hookedElement = hookedElement;
    }

    public Refactoring getSplitRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }

    @Override
    public Optional<EvolutionHook<? extends CodeElement>> getEvolutionHook() {
        if (refactoring != null) {
            if (addedElement instanceof Block && hookedElement instanceof Block) {
                EvolutionHook<Block> hook = new EvolutionHook<>((Block) hookedElement, (Block) addedElement);
                return Optional.of(hook);
            }
        }
        return Optional.empty();
    }
}
