package org.codetracker.change.block;
import org.codetracker.change.Change;

public class LanguageFeatureChange extends BlockChange {
    public LanguageFeatureChange() {
        super(Change.Type.LANGUAGE_FEATURE_CHANGE);
    }

    @Override
    public String toString() {
        return "Language Feature Change";
    }
}
