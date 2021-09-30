package org.codetracker.element;

import gr.uom.java.xmi.UMLAttribute;
import org.codetracker.api.Version;

import static org.codetracker.util.Util.annotationsToString;
import static org.codetracker.util.Util.getPath;

public class Attribute extends BaseCodeElement {
    private final UMLAttribute umlAttribute;

    private Attribute(UMLAttribute umlAttribute, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlAttribute = umlAttribute;
    }

    public static Attribute of(UMLAttribute umlAttribute, Version version) {
        String sourceFolder = getPath(umlAttribute.getLocationInfo().getFilePath(), umlAttribute.getClassName());
        String name = umlAttribute.getName();
        String className = umlAttribute.getClassName();
        String visibility = umlAttribute.getVisibility();
        String type = umlAttribute.getType().toString();
        int startLine = umlAttribute.getLocationInfo().getStartLine();

        String modifiersString = new ModifiersBuilder()
                .isFinal(umlAttribute.isFinal())
                .isStatic(umlAttribute.isStatic())
                .isTransient(umlAttribute.isTransient())
                .isVolatile(umlAttribute.isVolatile())
                .build();

        String identifierExcludeVersion = String.format("%s%s@%s(%s)%s:%s%s", sourceFolder, className, modifiersString, visibility, name, type, annotationsToString(umlAttribute.getAnnotations()));
        return new Attribute(umlAttribute, identifierExcludeVersion, String.format("%s%s@%s(%s)%s:%s(%d)", sourceFolder, className, modifiersString, visibility, name, type, startLine), umlAttribute.getLocationInfo().getFilePath(), version);
    }

    public UMLAttribute getUmlAttribute() {
        return umlAttribute;
    }
}
