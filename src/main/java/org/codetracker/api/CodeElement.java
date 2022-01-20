package org.codetracker.api;

import gr.uom.java.xmi.LocationInfo;

public interface CodeElement extends Comparable<CodeElement> {

    String getIdentifier();

    String getIdentifierIgnoringVersion();

    String getName();

    Version getVersion();

    boolean isAdded();

    boolean isRemoved();

    String getFilePath();

    LocationInfo getLocation();
}
