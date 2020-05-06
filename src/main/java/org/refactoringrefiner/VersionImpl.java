package org.refactoringrefiner;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringrefiner.api.Version;

public class VersionImpl implements Version {
    private final String id;
    private final int time;

    public VersionImpl(String id, int time) {
        this.id = id;
        this.time = time;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VersionImpl version = (VersionImpl) o;

        return new EqualsBuilder()
                .append(id, version.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
