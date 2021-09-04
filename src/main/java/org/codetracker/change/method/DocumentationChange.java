package org.codetracker.change.method;

import org.codetracker.api.Change;

public class DocumentationChange extends MethodChange {
    public DocumentationChange() {
        super(Change.Type.DOCUMENTATION_CHANGE);
    }

    @Override
    public String toString() {
        return "Documentation Change";
    }
}
