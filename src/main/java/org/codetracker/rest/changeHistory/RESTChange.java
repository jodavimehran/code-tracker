package org.codetracker.rest.changeHistory;

import org.codetracker.api.CodeElement;

import java.util.ArrayList;

// CodeTracker History Element For REST API
public class RESTChange extends OracleChange {
    public String date;
    public String before;
    public Integer beforeLine;
    public String beforePath;
    public String after;
    public Integer afterLine;
    public String afterPath;
    public String committer;
    public ArrayList<String> changes;
    public String evolutionHook;
    public Integer evolutionHookLine;
    public String evolutionHookPath;
    public String evolutionHookCommit;
    public String type;

    public RESTChange(
            String commitId,
            String parentCommitId,
            String date,
            CodeElement before,
            CodeElement after,
            String committer,
            Long commitTime,
            ArrayList<String> changes,
            Boolean evolutionPresent,
            CodeElement evolutionHook
    ) {
        super(
                parentCommitId,
                commitId,
                commitTime,
                null,
                before,
                after,
                null
        );
        super.changeType = getChangeType(changes);
        super.comment = getComment(changes);

        this.date = date;
        this.before = before.getName();
        this.beforeLine = before.getLocation().getStartLine();
        this.beforePath = before.getLocation().getFilePath();

        this.after = after.getName();
        this.afterLine = after.getLocation().getStartLine();
        this.afterPath = after.getLocation().getFilePath();

        this.committer = committer;

        this.changes = changes;

        if (evolutionPresent) {
            this.evolutionHook = getEvolutionHook(evolutionHook);
            this.evolutionHookLine = evolutionHook.getLocation().getStartLine();
            this.evolutionHookPath = evolutionHook.getLocation().getFilePath();
            this.evolutionHookCommit = evolutionHook.getVersion().getId();
        }

        this.type = before.getClass().getSimpleName().toLowerCase();
    }

    private String getComment(ArrayList<String> changes) {
        String comment = null;
        try {
            comment = changes.get(0).split(": ", 2)[1].replaceAll("\t", " ");
        } catch (Exception ignored) {
        }
        return comment;
    }

    private String getChangeType(ArrayList<String> changes) {
        return changes.get(0).split(": ", 2)[0].toLowerCase();
    }

    private String getEvolutionHook(CodeElement evolutionHook) {
        return evolutionHook.getName().split("\\$")[1].split("\\(")[0];
    }

}