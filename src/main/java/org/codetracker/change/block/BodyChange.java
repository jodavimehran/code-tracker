package org.codetracker.change.block;
import org.codetracker.change.Change;

public class BodyChange extends BlockChange {
    public BodyChange() {
        super(Change.Type.BODY_CHANGE);
    }

    @Override
    public String toString() {
        return "Body Change";
    }
}
