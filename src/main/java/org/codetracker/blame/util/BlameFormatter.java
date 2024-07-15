package org.codetracker.blame.util;

import org.codetracker.api.History.HistoryInfo;
import org.codetracker.blame.model.LineBlameResult;
import org.codetracker.element.BaseCodeElement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/* Created by pourya on 2024-07-02*/
public class BlameFormatter {
    private final String NOT_FOUND_PLACEHOLDER;
    private final List<String> lines;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    private int fromLine;

    public BlameFormatter(List<String> lines) {
    	this("", lines);
    }

    public BlameFormatter(List<String> lines, int fromLine) {
    	this("", lines);
    	this.fromLine = fromLine;
    }

    public BlameFormatter(String NOT_FOUND_PLACEHOLDER, List<String> lines) {
        this.NOT_FOUND_PLACEHOLDER = NOT_FOUND_PLACEHOLDER;
        this.lines = lines;
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT0:00"));
    }

    public List<String[]> make(List<LineBlameResult> lineBlameResults) {
        List<String[]> result = new java.util.ArrayList<>();
        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            LineBlameResult lineBlameResult = null;
            if (lineBlameResults != null && lineNumber <= lineBlameResults.size()) {
                lineBlameResult = lineBlameResults.get(lineNumber - 1);
            }
            result.add(make(lineBlameResult, fromLine > 0 ? fromLine + lineNumber - 1 : lineNumber, lines.get(lineNumber - 1)));
        }
        return result;
    }

    public List<String[]> make(Map<Integer, HistoryInfo<? extends BaseCodeElement>> blameInfo) {
        List<String[]> result = new java.util.ArrayList<>();
        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            HistoryInfo<? extends BaseCodeElement> historyInfo = blameInfo.get(lineNumber);
            LineBlameResult lineBlameResult = null;
            if (historyInfo != null) {
                lineBlameResult = LineBlameResult.of(historyInfo, lineNumber);
            }
            result.add(make(lineBlameResult, fromLine > 0 ? fromLine + lineNumber - 1 : lineNumber, lines.get(lineNumber - 1)));
        }
        return result;
    }

    public String[] make(LineBlameResult lineBlameResult, int lineNumber, String content) {
        String shortCommitId = NOT_FOUND_PLACEHOLDER;
        String committer = NOT_FOUND_PLACEHOLDER;
        String commitDate = NOT_FOUND_PLACEHOLDER;
        String beforeFilePath = NOT_FOUND_PLACEHOLDER;
        if (lineBlameResult != null) {
            shortCommitId = lineBlameResult.getShortCommitId();
            beforeFilePath = lineBlameResult.getBeforeFilePath();
            committer = "(" + lineBlameResult.getCommitter();
            commitDate = simpleDateFormat.format(new Date(lineBlameResult.getCommitDate() * 1000L));
        }
        return new String[]
                {
                        shortCommitId,
                        beforeFilePath,
                        committer,
                        commitDate,
                        lineNumber + ")",
                        content
                };
    }

}
