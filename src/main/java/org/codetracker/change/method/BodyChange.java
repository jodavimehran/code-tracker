package org.codetracker.change.method;

import org.codetracker.api.Change;

public class BodyChange extends MethodChange {
    public BodyChange() {
        super(Change.Type.BODY_CHANGE);
    }

    @Override
    public String toString() {
        return "Body Change";
    }
}
