package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBlockBodyAdded extends BlockBodyChange {
	public ElseBlockBodyAdded() {
		super(Change.Type.ELSE_BLOCK_BODY_ADDED);
	}

    @Override
    public String toString() {
        return "Else Block Body Added";
    }
}
