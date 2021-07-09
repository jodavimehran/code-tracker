package org.refactoringrefiner.test;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Input implements Comparable<Input> {
    private final String repositoryWebURL;
    private final String startTag;
    private final String endTag;

    public Input(String repositoryWebURL, String startTag, String endTag) {
        this.repositoryWebURL = repositoryWebURL;
        this.startTag = startTag;
        this.endTag = endTag;
    }

    public String getRepositoryWebURL() {
        return repositoryWebURL;
    }

    public String getStartTag() {
        return startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public String getTagRange() {
        return startTag + "..." + endTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Input input = (Input) o;

        return new EqualsBuilder()
                .append(repositoryWebURL, input.repositoryWebURL)
                .append(startTag, input.startTag)
                .append(endTag, input.endTag)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(repositoryWebURL)
                .append(startTag)
                .append(endTag)
                .toHashCode();
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s" + System.lineSeparator(), repositoryWebURL, startTag, endTag);
    }

    @Override
    public int compareTo(Input that) {
        return new CompareToBuilder()
                .append(this.repositoryWebURL, that.repositoryWebURL)
                .append(this.startTag, that.startTag)
                .append(this.startTag, that.startTag)
                .toComparison();
    }
}
