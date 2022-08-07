package org.codetracker.change;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import org.codetracker.api.Version;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;
import org.codetracker.api.CodeElement;

import java.util.Optional;

public class Extracted extends Introduced {
    private Version parentVersion;
    private Version childVersion;

    public Extracted(Refactoring extractRefactoring, CodeElement addedElement, Version parentVersion, Version childVersion) {
        super(Type.INTRODUCED, addedElement, extractRefactoring);
        this.parentVersion = parentVersion;
        this.childVersion = childVersion;
    }

    public Refactoring getExtractRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring != null ? refactoring.toString() : String.format("A code element with name [%s] is extracted.", addedElement.getName());
    }

    @Override
    public Optional<EvolutionHook> getEvolutionHook() {
        if (refactoring != null) {
            ExtractOperationRefactoring extractRefactoring = (ExtractOperationRefactoring) getExtractRefactoring();
            Method methodBefore = Method.of(extractRefactoring.getSourceOperationBeforeExtraction(), parentVersion);
            Method methodAfter = Method.of(extractRefactoring.getSourceOperationAfterExtraction(), childVersion);
            EvolutionHook hook = new EvolutionHook(methodBefore, methodAfter);
            return Optional.of(hook);
        }
        return Optional.empty();
    }
}
