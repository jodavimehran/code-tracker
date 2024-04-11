package org.codetracker.util;

import com.google.common.graph.EndpointPair;
import org.codetracker.HistoryImpl;
import org.codetracker.api.CodeElement;
import org.codetracker.api.Edge;
import org.codetracker.api.History;
import org.codetracker.change.Change;
import org.codetracker.experiment.oracle.AbstractOracle;
import org.codetracker.experiment.oracle.history.AbstractHistoryInfo;
import org.codetracker.experiment.oracle.history.ChangeHistory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public abstract class OracleTest {
	private static final Map<String, Integer> expectedTP = new HashMap<>();
	private static final Map<String, Integer> expectedFP = new HashMap<>();
	private static final Map<String, Integer> expectedFN = new HashMap<>();

	protected static void loadExpected(String filePath) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(", ");
				String commitId = tokens[0];
				int tp = Integer.parseInt(tokens[1]);
				int fp = Integer.parseInt(tokens[2]);
				int fn = Integer.parseInt(tokens[3]);
				expectedTP.put(commitId, tp);
				expectedFP.put(commitId, fp);
				expectedFN.put(commitId, fn);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static <H extends AbstractHistoryInfo, E extends CodeElement> Stream<Arguments> codeTrackerTestProvider
			(AbstractOracle<H> oracle, CheckedBiFunction<H, String, History<E>> tracker) {
		Stream.Builder<Arguments> builder = Stream.builder();
		for (Map.Entry<String, H> oracleInstance : oracle.getOracle().entrySet()) {
			String fileName = oracleInstance.getKey();
			H historyInfo = oracleInstance.getValue();
			builder.add(Arguments.of(tracker,historyInfo, fileName));
		}
		return builder.build();
	}

	@ParameterizedTest(name = "{index}: {2}")
	@MethodSource(value = "testProvider")
	public <H extends AbstractHistoryInfo, E extends CodeElement> void testCodeTracker(CheckedBiFunction<H, String, History<E>> tracker, H historyInfo, String fileName) throws Exception {
		String repositoryWebURL = historyInfo.getRepositoryWebURL();
		HashMap<String, ChangeHistory> oracleChanges = oracle(historyInfo.getExpectedChanges());
		History<E> history = tracker.apply(historyInfo, repositoryWebURL);
		HashMap<String, ChangeHistory> detectedChanges = new HashMap<>();
		HashMap<String, ChangeHistory> notDetectedChanges = new HashMap<>(oracleChanges);
		HashMap<String, ChangeHistory> falseDetectedChanges = processHistory((HistoryImpl<E>) history);

		for (Map.Entry<String, ChangeHistory> oracleChangeEntry : oracleChanges.entrySet()) {
			String changeKey = oracleChangeEntry.getKey();
			if (falseDetectedChanges.containsKey(changeKey)) {
				detectedChanges.put(changeKey, falseDetectedChanges.get(changeKey));
				notDetectedChanges.remove(changeKey);
				falseDetectedChanges.remove(changeKey);
			}
		}
		final int actualTP = detectedChanges.size();
		final int actualFP = falseDetectedChanges.size();
		final int actualFN = notDetectedChanges.size();
		Assertions.assertAll(
				() -> Assertions.assertEquals(expectedTP.get(fileName), actualTP, String.format("Should have %s according to %s True Positives,  but has %s", expectedTP.get(fileName), fileName, actualTP)),
				() -> Assertions.assertEquals(expectedFP.get(fileName), actualFP, String.format("Should have %s according to %s False Positives, but has %s", expectedFP.get(fileName), fileName, actualFP)),
				() -> Assertions.assertEquals(expectedFN.get(fileName), actualFN, String.format("Should have %s according to %s False Negatives, but has %s", expectedFN.get(fileName), fileName, actualFN))
				);
	}
	protected static <H extends AbstractHistoryInfo, E extends CodeElement> Stream<Arguments> getArgumentsStream(List<? extends AbstractOracle<H>> all, String expected, CheckedBiFunction<H, String, History<E>> tracker) {
		return all.stream().flatMap(oracle -> {
					loadExpected(expected + oracle.getName() + "-expected.txt");
					return codeTrackerTestProvider(oracle, tracker);
				});
	}

	protected static HashMap<String, ChangeHistory> oracle(List<ChangeHistory> expectedChanges) {
		HashMap<String, ChangeHistory> oracleChanges = new HashMap<>();
		for (ChangeHistory changeHistory : expectedChanges) {
			Change.Type changeType = Change.Type.get(changeHistory.getChangeType());
			String commitId = changeHistory.getCommitId();
			String changeKey = getChangeKey(changeType, commitId);
			oracleChanges.put(changeKey, changeHistory);
		}
		return oracleChanges;
	}

	protected static <T extends CodeElement> HashMap<String, ChangeHistory> processHistory(HistoryImpl<T> historyImpl) {
		HashMap<String, ChangeHistory> historyChanges = new HashMap<>();
		if (historyImpl.getGraph() == null)
			return historyChanges;

		Set<EndpointPair<T>> edges = historyImpl.getGraph().getEdges();

		for (EndpointPair<T> edge : edges) {
			Edge edgeValue = historyImpl.getGraph().getEdgeValue(edge).get();
			Set<Change> changeList = edgeValue.getChangeList();
			for (Change change : changeList) {
				if (Change.Type.NO_CHANGE.equals(change.getType()))
					continue;
				ChangeHistory changeHistory = new ChangeHistory();

				String commitId = edge.target().getVersion().getId();
				changeHistory.setCommitId(commitId);
				changeHistory.setParentCommitId(edge.source().getVersion().getId());
				changeHistory.setCommitTime(edge.target().getVersion().getTime());

				String changeType = change.getType().getTitle();
				changeHistory.setChangeType(changeType);

				String leftFile = edge.source().getFilePath();
				changeHistory.setElementFileBefore(leftFile);
				String leftName = edge.source().getName();
				changeHistory.setElementNameBefore(leftName);

				String rightFile = edge.target().getFilePath();
				changeHistory.setElementFileAfter(rightFile);
				String rightName = edge.target().getName();
				changeHistory.setElementNameAfter(rightName);

				changeHistory.setComment(change.toString().replace("\t", " "));
				historyChanges.put(getChangeKey(change.getType(), commitId), changeHistory);
			}
		}
		return historyChanges;
	}

	protected static String getChangeKey(Change.Type changeType, String commitId) {
		return getChangeKey(changeType.getTitle(), commitId);
	}

	protected static String getChangeKey(String changeType, String commitId) {
		return String.format("%s-%s", commitId, changeType);
	}

	@FunctionalInterface
	public interface CheckedBiFunction<T, U, R> {
		R apply(T t, U u) throws Exception;
	}
}
