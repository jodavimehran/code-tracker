package org.refactoringrefiner.change.method;

import org.refactoringrefiner.api.Change;

public class DocumentationChange extends MethodChange {
    public DocumentationChange() {
        super(Change.Type.DOCUMENTATION_CHANGE);
    }

    @Override
    public String toString() {
        return "The documentation of the method element is changed.";
    }
}
