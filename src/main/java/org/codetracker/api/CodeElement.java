package org.refactoringrefiner.api;

public interface CodeElement extends Comparable<CodeElement> {

    String getIdentifier();

    String getIdentifierIgnoringVersion();

    String getName();

    Version getVersion();

    boolean isAdded();

    boolean isRemoved();

    String getFilePath();

}
