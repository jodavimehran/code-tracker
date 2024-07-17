package org.codetracker;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import org.codetracker.api.History.HistoryInfo;
import org.codetracker.element.BaseCodeElement;

public abstract class AbstractChangeHistory<T extends BaseCodeElement> {
	protected final ArrayDeque<T> elements = new ArrayDeque<>();
	public abstract ChangeHistory<T> get();
	
	public List<HistoryInfo<T>> getHistory() {
		List<HistoryInfo<T>> history = HistoryImpl.processHistory(get().getCompleteGraph());
        Collections.reverse(history);
        return history;
	}

	public void addFirst(T element) {
		elements.addFirst(element);
	}

	public void add(T element) {
		elements.add(element);
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public T poll() {
		return elements.poll();
	}
}
