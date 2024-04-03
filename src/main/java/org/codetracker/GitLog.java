package org.codetracker;

import java.util.List;
import java.util.Map;

public class GitLog {
	private Map<String, List<String>> commitLogMap;

	public GitLog() {
		
	}

	public GitLog(Map<String, List<String>> commitLogMap) {
		this.commitLogMap = commitLogMap;
	}

	public Map<String, List<String>> getCommitLogMap() {
		return commitLogMap;
	}
}
