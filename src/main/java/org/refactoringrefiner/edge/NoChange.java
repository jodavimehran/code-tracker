package org.refactoringrefiner.edge;

import org.refactoringrefiner.change.AbstractChange;

public class NoChange extends AbstractChange {
    public NoChange() {
        super(Type.NO_CHANGE);
    }

    @Override
    public String toString() {
        return "Nothing Really Happened!!!";
    }
}
