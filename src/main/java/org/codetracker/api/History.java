package org.codetracker.api;

import org.codetracker.change.Change;

import java.util.List;
import java.util.Set;

public interface History<C extends CodeElement> {
  Graph<C, Edge> getGraph();

  HistoryReport getHistoryReport();

  List<HistoryInfo<C>> getHistoryInfoList();

  interface HistoryReport {
    int getAnalysedCommits();

    int getGitLogCommandCalls();

    int getStep2();

    int getStep3();

    int getStep4();

    int getStep5();
  }

  interface HistoryInfo<C extends CodeElement> extends Comparable<HistoryInfo<C>> {
    C getElementBefore();

    C getElementAfter();

    Set<Change> getChangeList();

    Change.Type getChangeType();

    String getCommitId();

    long getCommitTime();

    long getAuthoredTime();

    String getCommitterName();
  }
}
