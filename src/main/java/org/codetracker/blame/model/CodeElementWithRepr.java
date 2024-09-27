package org.codetracker.blame.model;

import gr.uom.java.xmi.LocationInfo;
import org.codetracker.api.CodeElement;
import org.codetracker.api.Version;

public class CodeElementWithRepr {

    CodeElement codeElement;
    String representation;


    public CodeElementWithRepr(CodeElement codeElement, String representation) {
        this.codeElement = codeElement;
        this.representation = representation.trim();
    }

    public String getRepresentation() {
        return representation;
    }

    public CodeElement getCodeElement(){
        return codeElement;
    }

    @Override
    public String toString() {
        return representation;
    }
}
