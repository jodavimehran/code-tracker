package org.codetracker.change;

import org.codetracker.api.CodeElement;

public class Removed extends AbstractChange {
    private final CodeElement removedElement;

    public Removed(CodeElement removedElement) {
        super(Type.REMOVED);
        this.removedElement = removedElement;
    }

    protected Removed(Type type, CodeElement removedElement) {
        super(type);
        this.removedElement = removedElement;
    }

    public CodeElement getRemovedElement() {
        return removedElement;
    }

    @Override
    public String toString() {
        return String.format("A code element with name [%s] is removed.", removedElement.getName());
    }
}
