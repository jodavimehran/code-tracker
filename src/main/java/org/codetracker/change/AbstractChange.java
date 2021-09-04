package org.codetracker.change;

import org.codetracker.api.Change;

public abstract class AbstractChange implements Change {
    protected final Type type;

    public AbstractChange(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
