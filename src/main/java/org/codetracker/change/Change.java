package org.codetracker.change;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface Change {

    Type getType();

    Optional<EvolutionHook> getEvolutionHook();

    enum Type {
        NO_CHANGE("not changed"),
        INTRODUCED("introduced"),
        REMOVED("removed"),
        CONTAINER_CHANGE("container change"),
        MULTI_CHANGE("changed multiple times"),
        BODY_CHANGE("body change"),
        DOCUMENTATION_CHANGE("documentation change"),
        RENAME("rename"),
        MODIFIER_CHANGE("modifier change"),
        ACCESS_MODIFIER_CHANGE("access modifier change"),
        RETURN_TYPE_CHANGE("return type change"),
        TYPE_CHANGE("type change"),
        EXCEPTION_CHANGE("exception change"),
        PARAMETER_CHANGE("parameter change"),
        ANNOTATION_CHANGE("annotation change"),
        MOVED("moved"),
        PARENTHESIS_CHANGE("parenthesis change"),
        LANGUAGE_FEATURE_CHANGE("language feature change");

        private static final Map<String, Type> lookup = new HashMap<>();

        static {
            for (Type type : Type.values()) {
                lookup.put(type.title, type);
            }
        }

        private final String title;

        Type(String title) {
            this.title = title;
        }

        public static Type get(String title) {
            return lookup.get(title);
        }

        public String getTitle() {
            return title;
        }
    }
}
