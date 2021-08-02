package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.change.AbstractChange;

public class Added extends AbstractChange {
    protected final CodeElement addedElement;
    public Added(CodeElement addedElement) {
        super(Type.INTRODUCED);
        this.addedElement = addedElement;
    }

    protected Added(Type type, CodeElement addedElement) {
        super(type);
        this.addedElement = addedElement;
    }

    public CodeElement getAddedElement() {
        return addedElement;
    }

    @Override
    public String toString() {
        return String.format("A code element with name [%s] is added.", addedElement.getName());
    }
}
