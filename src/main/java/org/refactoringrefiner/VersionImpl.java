package org.refactoringrefiner;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.refactoringrefiner.api.Version;

public class VersionImpl implements Version {
    private final String id;

    public VersionImpl(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
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

    @Override
    public String toString() {
        return String.format("<%s>", getId());
    }
}
