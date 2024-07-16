package org.codetracker;

import java.util.Collections;
import java.util.List;

import org.codetracker.api.History.HistoryInfo;
import org.codetracker.element.BaseCodeElement;

public abstract class AbstractChangeHistory<T extends BaseCodeElement> {

	public abstract ChangeHistory<T> get();
	
	public List<HistoryInfo<T>> getHistory() {
		List<HistoryInfo<T>> history = HistoryImpl.processHistory(get().getCompleteGraph());
        Collections.reverse(history);
        return history;
	}
}
