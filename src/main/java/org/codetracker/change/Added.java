package org.codetracker.change;

import org.refactoringminer.api.Refactoring;
import org.codetracker.api.CodeElement;

public class Added extends AbstractChange {
    protected final CodeElement addedElement;
    protected final String comment;
    protected final Refactoring refactoring;

    public Added(CodeElement addedElement, String comment, Refactoring refactoring) {
        super(Type.INTRODUCED);
        this.addedElement = addedElement;
        this.comment = comment;
        this.refactoring = refactoring;
    }

    protected Added(Type type, CodeElement addedElement, Refactoring refactoring) {
        super(type);
        this.addedElement = addedElement;
        this.comment = refactoring.toString();
        this.refactoring = refactoring;
    }

    public CodeElement getAddedElement() {
        return addedElement;
    }

    @Override
    public String toString() {
        return comment;
    }
}
