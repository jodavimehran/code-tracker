package org.codetracker.change.block;

import org.codetracker.change.Change;

public class CatchBlockChange extends BlockChange {
    public CatchBlockChange() {
        super(Change.Type.CATCH_BLOCK_CHANGE);
    }

    @Override
    public String toString() {
        return "Catch Block Change";
    }
}
