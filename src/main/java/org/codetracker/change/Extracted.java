package org.codetracker.change;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.refactoringminer.api.Refactoring;
import org.codetracker.api.CodeElement;

import java.util.Optional;

public class Extracted extends Introduced {
    private CodeElement hookedElement;

    public Extracted(Refactoring extractRefactoring, CodeElement addedElement, CodeElement hookedElement) {
        super(Type.INTRODUCED, addedElement, extractRefactoring);
        this.hookedElement = hookedElement;
    }

    public Refactoring getExtractRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring != null ? refactoring.toString() : String.format("A code element with name [%s] is extracted.", addedElement.getName());
    }

    @Override
    public Optional<EvolutionHook<? extends CodeElement>> getEvolutionHook() {
        if (refactoring != null) {
            ExtractOperationRefactoring extractRefactoring = (ExtractOperationRefactoring) getExtractRefactoring();
            if (addedElement instanceof Method && hookedElement instanceof Method) {
                EvolutionHook<Method> hook = new EvolutionHook<>((Method) hookedElement, (Method) addedElement);
                return Optional.of(hook);
            }
            else if (addedElement instanceof Variable && hookedElement instanceof Variable) {
                EvolutionHook<Variable> hook = new EvolutionHook<>((Variable) hookedElement, (Variable) addedElement);
                return Optional.of(hook);
            }
            else if (addedElement instanceof Block && hookedElement instanceof Block) {
                EvolutionHook<Block> hook = new EvolutionHook<>((Block) hookedElement, (Block) addedElement);
                return Optional.of(hook);
            }
        }
        return Optional.empty();
    }
}
