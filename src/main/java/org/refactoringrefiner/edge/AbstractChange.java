package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.Change;

import java.util.Objects;

public abstract class AbstractChange implements Change {
    protected final Type type;

    public AbstractChange(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toSummary() {
        return type.getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractChange change = (AbstractChange) o;
        return type == change.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
