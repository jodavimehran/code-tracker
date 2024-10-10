package org.codetracker.blame.convertor.model;

import java.util.Arrays;
import java.util.function.Predicate;

public enum CodeLinePredicate implements Predicate<String> {
    OPENING_CURLY_BRACKET("{"),
    CLOSING_CURLY_BRACKET("}"),
    OPENING_AND_CLOSING_CURLY_BRACKET("{", "}"),
    BLANK_LINE(""),
    ANY() // Having empty reprs means it will always return true
    ;

    private final String[] reprs;

    @Override
    public boolean test(String stringRepr) {
        if (reprs == null || reprs.length == 0)
            return true;
        Predicate<String> condition = str -> str.equals(stringRepr.trim());
        return Arrays.stream(reprs).anyMatch(condition);
    }

    CodeLinePredicate(String... reprs) {
        this.reprs = reprs;
    }
}
