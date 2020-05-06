package org.refactoringrefiner.api;

public interface CodeElement {

    String getIdentifier();

    String getFullName();

    String getShortName();

    String getContainerName();

    String getPackageName();

    Version getVersion();
}
