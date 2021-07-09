package org.refactoringrefiner.edge;

import org.refactoringrefiner.api.Change;

import java.util.HashMap;
import java.util.Map;

public class ChangeImpl implements Change {
    protected final Type type;
    protected final String description;
    protected final Map<String, String> extraInfo = new HashMap<>();

    public ChangeImpl(Type type, String description) {
        this.type = type;
        this.description = description;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void addExtraInfo(String key, String description) {
        extraInfo.put(key, description);
    }
}
