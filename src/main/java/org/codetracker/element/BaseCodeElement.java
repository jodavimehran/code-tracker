package org.codetracker.element;

import org.codetracker.api.CodeElement;
import org.codetracker.api.Version;

import java.util.Objects;
import java.util.TreeSet;

public abstract class BaseCodeElement implements CodeElement {
    protected final String identifier;
    protected final String identifierIgnoringVersion;
    protected final String name;
    protected final String filePath;
    protected final Version version;
    protected boolean isRemoved;
    protected boolean isAdded;
    protected boolean isStart;
    private boolean closingCurlyBracket;

    public BaseCodeElement(String identifierIgnoringVersion, String name, String filePath, Version version) {
        this.identifier = version != null ? identifierIgnoringVersion + version : identifierIgnoringVersion;
        this.identifierIgnoringVersion = identifierIgnoringVersion;
        this.name = name;
        this.filePath = filePath;
        this.version = version;
    }

    public boolean isClosingCurlyBracket() {
    	return closingCurlyBracket;
    }

    public void setClosingCurlyBracket(boolean closingCurlyBracket) {
    	this.closingCurlyBracket = closingCurlyBracket;
    }

    @Override
    public final int compareTo(CodeElement o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    public final boolean isRemoved() {
        return isRemoved;
    }

    public final void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public final boolean isAdded() {
        return isAdded;
    }

    public final void setAdded(boolean added) {
        isAdded = added;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean equalIdentifierIgnoringVersion(BaseCodeElement codeElement) {
        return this.identifierIgnoringVersion.equals(codeElement.identifierIgnoringVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseCodeElement that = (BaseCodeElement) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        if (version != null)
            return String.format("%s%s", name, version);
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIdentifierIgnoringVersion() {
        return identifierIgnoringVersion;
    }

    public static class ModifiersBuilder {
        private final TreeSet<String> modifiers = new TreeSet<>();

        public ModifiersBuilder isFinal(boolean isFinal) {
            if (isFinal)
                modifiers.add("final");
            return this;
        }

        public ModifiersBuilder isStatic(boolean isStatic) {
            if (isStatic)
                modifiers.add("static");
            return this;
        }

        public ModifiersBuilder isTransient(boolean isTransient) {
            if (isTransient)
                modifiers.add("transient");
            return this;
        }

        public ModifiersBuilder isVolatile(boolean isVolatile) {
            if (isVolatile)
                modifiers.add("volatile");
            return this;
        }

        public ModifiersBuilder isAbstract(boolean isAbstract) {
            if (isAbstract)
                modifiers.add("abstract");
            return this;
        }


        public String build(){
            return modifiers.isEmpty() ? "" : "(" + String.join(",", modifiers) + ")";
        }
    }
}
