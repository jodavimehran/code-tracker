package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.Change;

public abstract class AbstractChange implements Change {
    protected final Type type;
    protected final String description;

    public AbstractChange(Type type, String description) {
        this.type = type;
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public String toSummary() {
        return type.getTitle();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
