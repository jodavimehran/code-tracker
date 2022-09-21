package org.codetracker.change.block;

import org.codetracker.change.Change;

public class CatchBlockRemoved extends BlockChange {
    public CatchBlockRemoved() {
        super(Change.Type.CATCH_BLOCK_REMOVED);
    }

    @Override
    public String toString() {
        return "Catch Block Removed";
    }
}
