package org.codetracker.element;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLPackage;

public class Package extends BaseCodeElement {
	private final UMLPackage umlPackage;
	private final UMLAbstractClass umlClass;
	
	public Package(UMLPackage umlPackage, UMLAbstractClass umlClass, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.umlPackage = umlPackage;
		this.umlClass = umlClass;
	}

    public UMLPackage getUmlPackage() {
		return umlPackage;
	}

	public UMLAbstractClass getUmlClass() {
		return umlClass;
	}

	public static Package of(UMLPackage umlPackage, UMLAbstractClass clazz, Version version) {
        Class c = Class.of(clazz, version);
        return of(umlPackage, c);
    }

	public static Package of(UMLPackage umlPackage, Class clazz) {
		LocationInfo packageLocationInfo = umlPackage.getLocationInfo();
		String statementType = packageLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", clazz.getName(), statementType, packageLocationInfo.getStartLine(), packageLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(umlPackage.getName());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s}",
				clazz.getIdentifierIgnoringVersion(),
				statementType,
				sha512
				);
		return new Package(umlPackage, clazz.getUmlClass(), identifierExcludeVersion, name, packageLocationInfo.getFilePath(), clazz.getVersion());
	}

	@Override
	public LocationInfo getLocation() {
		return umlPackage.getLocationInfo();
	}
}
