package org.refactoringrefiner.edge;

public class NoChange extends AbstractChange {
    public NoChange() {
        super(Type.NO_CHANGE, null);
    }

    @Override
    public String toString() {
        return "Nothing Really Happened!!!";
    }
}
