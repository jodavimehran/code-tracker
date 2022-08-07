package org.codetracker.change;

import java.util.Optional;

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

    public Optional<EvolutionHook> getEvolutionHook() { return Optional.empty(); }
}
