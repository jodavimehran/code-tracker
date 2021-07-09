package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.CodeElement;

public class Added extends AbstractChange {
    private final CodeElement addedElement;
    public Added(CodeElement addedElement, String description) {
        super(Type.ADDED, description);
        this.addedElement = addedElement;
    }

    protected Added(Type type, CodeElement addedElement, String description) {
        super(type, description);
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
        return String.format("A code element with name [%s] is added.", addedElement.getName());
    }
}
