package org.codetracker.change;

import org.codetracker.api.CodeElement;

public class EvolutionHook<C extends CodeElement> {
    private final C elementBefore;
    private final C elementAfter;

    public EvolutionHook(C elementBefore, C elementAfter) {
        this.elementBefore = elementBefore;
        this.elementAfter = elementAfter;
    }

    public C getElementBefore() {
        return elementBefore;
    }

    public C getElementAfter() {
        return elementAfter;
    }
}
