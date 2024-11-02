package org.codetracker.change.block;

import org.codetracker.change.Change;

public class BlockBodyRemoved extends BlockBodyChange {

	public BlockBodyRemoved() {
		super(Change.Type.BLOCK_BODY_REMOVED);
	}

    @Override
    public String toString() {
        return "Block Body Removed";
    }
}
