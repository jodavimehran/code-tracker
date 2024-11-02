package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBlockBodyRemoved extends BlockBodyChange {
	public ElseBlockBodyRemoved() {
		super(Change.Type.ELSE_BLOCK_BODY_REMOVED);
	}

    @Override
    public String toString() {
        return "Else Block Body Removed";
    }
}
