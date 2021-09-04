package org.refactoringrefiner.change.method;

import org.refactoringrefiner.api.Change;

public class BodyChange extends MethodChange {
    public BodyChange() {
        super(Change.Type.BODY_CHANGE);
    }

    @Override
    public String toString() {
        return "Body Change";
    }
}
