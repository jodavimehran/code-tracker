package org.codetracker.element;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLImport;

public class Import extends BaseCodeElement {
	private final UMLImport umlImport;
	private final UMLAbstractClass clazz;
	
	public Import(UMLImport umlImport, UMLAbstractClass clazz, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.umlImport = umlImport;
		this.clazz = clazz;
	}

    public UMLImport getUmlImport() {
		return umlImport;
	}

	public UMLAbstractClass getClazz() {
		return clazz;
	}

	public static Import of(UMLImport umlImport, UMLAbstractClass clazz, Version version) {
        Class c = Class.of(clazz, version);
        return of(umlImport, c);
    }

	public static Import of(UMLImport umlImport, Class clazz) {
		LocationInfo commentLocationInfo = umlImport.getLocationInfo();
		String statementType = commentLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", clazz.getName(), statementType, commentLocationInfo.getStartLine(), commentLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(umlImport.getName());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s}",
				clazz.getIdentifierIgnoringVersion(),
				statementType,
				sha512
				);
		return new Import(umlImport, clazz.getUmlClass(), identifierExcludeVersion, name, commentLocationInfo.getFilePath(), clazz.getVersion());
	}

	@Override
	public LocationInfo getLocation() {
		return umlImport.getLocationInfo();
	}
}
