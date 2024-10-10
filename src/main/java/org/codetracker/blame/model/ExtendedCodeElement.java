package org.codetracker.blame.model;

import org.codetracker.api.CodeElement;

public interface ExtendedCodeElement extends CodeElement {
    String getRepresentation();
}
