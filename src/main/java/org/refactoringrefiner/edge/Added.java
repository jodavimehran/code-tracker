package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.CodeElement;

public class Added extends AbstractChange {
    private final CodeElement addedElement;
    public Added(CodeElement addedElement) {
        super(Type.ADDED);
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
    public String toSummary(){
        return String.format("[%s] is added", addedElement.getName());
    }

    @Override
    public String toString() {
        return String.format("A code element with full name [%s] is added.", addedElement.getFullName());
    }
}
