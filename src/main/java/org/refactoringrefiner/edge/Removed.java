package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.CodeElement;

public class Removed extends AbstractChange {
    private final CodeElement removedElement;

    public Removed(CodeElement removedElement, String description) {
        super(Type.REMOVED, description);
        this.removedElement = removedElement;
    }

    protected Removed(Type type, CodeElement removedElement, String description) {
        super(type, description);
        this.removedElement = removedElement;
    }

    public CodeElement getRemovedElement() {
        return removedElement;
    }

    @Override
    public String toSummary() {
        return String.format("[%s] is removed", removedElement.getName());
    }

    @Override
    public String toString() {
        return String.format("A code element with name [%s] is removed.", removedElement.getName());
    }
}
