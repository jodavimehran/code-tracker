package org.codetracker.change.block;

import org.codetracker.change.Change;

public class CatchBodyChange extends BlockChange {
    public CatchBodyChange() {
        super(Change.Type.CATCH_BODY_CHANGE);
    }

    @Override
    public String toString() {
        return "Catch Body Change";
    }
}
