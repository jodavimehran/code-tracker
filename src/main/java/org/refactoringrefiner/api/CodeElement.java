package org.refactoringrefiner.api;

public interface CodeElement extends Comparable<CodeElement> {

    String getIdentifier();

    String getFullName();

    String getName();

    String getContainerName();

    String getPackageName();

    String getSourceFolder();

    Version getVersion();
}
