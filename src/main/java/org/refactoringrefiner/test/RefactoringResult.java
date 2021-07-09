package org.refactoringrefiner.test;

import com.google.common.hash.Hashing;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class RefactoringResult {
    private final Set<Refactoring> refactoringMinerAllCommits = new HashSet<>();
    private final Set<Refactoring> refactoringMinerFirstAndLast = new HashSet<>();
    private final Set<Refactoring> refactoringRefiner = new HashSet<>();
    private long refactoringMinerAllCommitProcessTime, refactoringMinerFirstLastCommitProcessTime, refactoringRefinerProcessTime;
    private int numberOfCommits, sRMAC, sRMFLC, sRR, iRMFLCRR, iRMACRR, iRMFLCRMAC;
    private double sameCodeElementChangeRate;


    private static boolean isSupportedRefactoring(RefactoringType refactoringType) {
       return true;
//        switch (refactoringType) {
//            case MOVE_CLASS:
//            case RENAME_CLASS:
//            case ADD_CLASS_ANNOTATION:
//            case MODIFY_CLASS_ANNOTATION:
//            case REMOVE_CLASS_ANNOTATION:
//
//            case MOVE_ATTRIBUTE:
//            case MOVE_RENAME_ATTRIBUTE:
//            case RENAME_ATTRIBUTE:
//            case CHANGE_ATTRIBUTE_TYPE:
//            case ADD_ATTRIBUTE_ANNOTATION:
//            case REMOVE_ATTRIBUTE_ANNOTATION:
//            case MODIFY_ATTRIBUTE_ANNOTATION:
//
//            case RENAME_METHOD:
//            case MOVE_OPERATION:
//            case MOVE_AND_RENAME_OPERATION:
//            case ADD_PARAMETER:
//            case REMOVE_PARAMETER:
//            case RENAME_PARAMETER:
//            case REORDER_PARAMETER:
//            case CHANGE_RETURN_TYPE:
//            case CHANGE_PARAMETER_TYPE:
//            case ADD_METHOD_ANNOTATION:
//            case REMOVE_METHOD_ANNOTATION:
//            case MODIFY_METHOD_ANNOTATION:
//
//            case MOVE_SOURCE_FOLDER:
//            case CHANGE_VARIABLE_TYPE:
//            case RENAME_VARIABLE:
////            case ADD_PARAMETER_ANNOTATION:
////            case REMOVE_PARAMETER_ANNOTATION:
////            case MODIFY_PARAMETER_ANNOTATION:
//
//                return true;
//        }
//        return false;
    }

    public Set<Refactoring> getRefactoringMinerAllCommits() {
        return refactoringMinerAllCommits;
    }

    public Set<Refactoring> getRefactoringMinerFirstAndLast() {
        return refactoringMinerFirstAndLast;
    }

    public Set<Refactoring> getRefactoringRefiner() {
        return refactoringRefiner;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RefactoringMiner [All Commits] says :").append(System.lineSeparator());
        sb.append("=====================================================================================================").append(System.lineSeparator());
        sb.append(refactoringMinerAllCommits.stream().map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
        sb.append("=====================================================================================================").append(System.lineSeparator());
        sb.append("RefactoringMiner [Compare First and Last] says :").append(System.lineSeparator());
        sb.append("=====================================================================================================").append(System.lineSeparator());
        sb.append(refactoringMinerFirstAndLast.stream().map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
        sb.append("=====================================================================================================").append(System.lineSeparator());
        sb.append("RefactoringRefiner says :").append(System.lineSeparator());
        sb.append("=====================================================================================================").append(System.lineSeparator());
        sb.append(refactoringMinerFirstAndLast.stream().map(Refactoring::toString).collect(Collectors.joining(System.lineSeparator())));
        sb.append("=====================================================================================================").append(System.lineSeparator());
        return sb.toString();
    }

    public static List<String> minus(List<String> left, List<String> right) {
        ArrayList<String> leftCopy = new ArrayList<>(left);
        right.forEach(leftCopy::remove);
        return leftCopy;
    }

    public void calculateOtherAttributes() {
        List<String> rmac = refactoringMinerAllCommits.stream().filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType())).map(Refactoring::toString).collect(Collectors.toList());
        List<String> rmflc = refactoringMinerFirstAndLast.stream().filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType())).map(Refactoring::toString).collect(Collectors.toList());
        List<String> rr = refactoringRefiner.stream().filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType())).map(Refactoring::toString).collect(Collectors.toList());

        sRR = rr.size();
        sRMAC = rmac.size();
        sRMFLC = rmflc.size();

        iRMACRR = sRMAC - minus(rmac, rr).size();
        iRMFLCRR = sRMFLC - minus(rmflc, rr).size();
        iRMFLCRMAC = sRMFLC - minus(rmflc, rmac).size();

    }

    public List<Result> getResults(Input input) {
        HashMap<String, Refactoring> rr = new HashMap<>();
        refactoringRefiner.stream()
                .filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType()))
                .forEach(refactoring -> rr.put(Hashing.sha256().hashString(refactoring.toJSON(), StandardCharsets.UTF_8).toString(), refactoring));

        HashMap<String, Refactoring> rmac = new HashMap<>();
        refactoringMinerAllCommits.stream()
                .filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType()))
                .forEach(refactoring -> rmac.put(Hashing.sha256().hashString(refactoring.toJSON(), StandardCharsets.UTF_8).toString(), refactoring));

        HashMap<String, Refactoring> rmfl = new HashMap<>();
        refactoringMinerFirstAndLast.stream()
                .filter(refactoring -> isSupportedRefactoring(refactoring.getRefactoringType()))
                .forEach(refactoring -> rmfl.put(Hashing.sha256().hashString(refactoring.toJSON(), StandardCharsets.UTF_8).toString(), refactoring));

        Set<String> added = new HashSet<>();

        List<Result> results = new ArrayList<>();

        rr.entrySet().stream()
                .forEach(entry -> {
                    Result result = getResults(input.getRepositoryWebURL(), input.getStartTag(), input.getEndTag(), entry.getKey(), entry.getValue());
                    result.setDetectedByRR(true);
                    sRR++;
                    result.setDetectedByRMAC(rmac.containsKey(entry.getKey()));
                    if (result.isDetectedByRMAC()) {
                        sRMAC++;
                        iRMACRR++;
                    }

                    result.setDetectedByRMFL(rmfl.containsKey(entry.getKey()));
                    if (result.isDetectedByRMFL()) {
                        sRMFLC++;
                        iRMFLCRR++;
                    }

                    if (result.isDetectedByRMAC() && result.isDetectedByRMFL())
                        iRMFLCRMAC++;

                    results.add(result);
                    added.add(entry.getKey());
                });

        rmac.entrySet().stream()
                .filter(entry -> !added.contains(entry.getKey()))
                .forEach(entry -> {
                    Result result = getResults(input.getRepositoryWebURL(), input.getStartTag(), input.getEndTag(), entry.getKey(), entry.getValue());
                    result.setDetectedByRR(false);
                    result.setDetectedByRMAC(true);
                    sRMAC++;
                    result.setDetectedByRMFL(rmfl.containsKey(entry.getKey()));
                    if (result.isDetectedByRMFL()) {
                        sRMFLC++;
                        iRMFLCRMAC++;
                    }
                    results.add(result);
                    added.add(entry.getKey());
                });

        rmfl.entrySet().stream()
                .filter(entry -> !added.contains(entry.getKey()))
                .forEach(entry -> {
                    Result result = getResults(input.getRepositoryWebURL(), input.getStartTag(), input.getEndTag(), entry.getKey(), entry.getValue());
                    result.setDetectedByRR(false);
                    result.setDetectedByRMAC(false);
                    result.setDetectedByRMFL(true);
                    sRMFLC++;
                    results.add(result);
                    added.add(entry.getKey());
                });

        return results;
    }

    private Result getResults(String repositoryWebUrl, String startTag, String endTag, String sha256, Refactoring refactoring) {
        Result result = new Result();
        result.setRepository(repositoryWebUrl);
        result.setStartTag(startTag);
        result.setEndTag(endTag);
        result.setRefactoringType(refactoring.getRefactoringType());
        result.setRefactoringDesc(refactoring.toString());
        result.setRefactoringJSON(refactoring.toJSON());
        result.setHash(sha256);
        return result;
    }

    public double getDistanceRMACRR() {
        return getDistance(sRMAC, sRR, iRMACRR);
    }

    public double getDistanceRMFLCRR() {
        return getDistance(sRMFLC, sRR, iRMFLCRR);
    }

    public double getDistanceRMFLCRMAC() {
        return getDistance(sRMFLC, sRMAC, iRMFLCRMAC);
    }

    public long getRefactoringMinerAllCommitProcessTime() {
        return refactoringMinerAllCommitProcessTime;
    }

    public void setRefactoringMinerAllCommitProcessTime(long refactoringMinerAllCommitProcessTime) {
        this.refactoringMinerAllCommitProcessTime = refactoringMinerAllCommitProcessTime;
    }

    public long getRefactoringMinerFirstLastCommitProcessTime() {
        return refactoringMinerFirstLastCommitProcessTime;
    }

    public void setRefactoringMinerFirstLastCommitProcessTime(long refactoringMinerFirstLastCommitProcessTime) {
        this.refactoringMinerFirstLastCommitProcessTime = refactoringMinerFirstLastCommitProcessTime;
    }

    public int getNumberOfCommits() {
        return numberOfCommits;
    }

    public void setNumberOfCommits(int numberOfCommits) {
        this.numberOfCommits = numberOfCommits;
    }

    public long getRefactoringRefinerProcessTime() {
        return refactoringRefinerProcessTime;
    }

    public void setRefactoringRefinerProcessTime(long refactoringRefinerProcessTime) {
        this.refactoringRefinerProcessTime = refactoringRefinerProcessTime;
    }

    public double getSameCodeElementChangeRate() {
        return sameCodeElementChangeRate;
    }

    public void setSameCodeElementChangeRate(double sameCodeElementChangeRate) {
        this.sameCodeElementChangeRate = sameCodeElementChangeRate;
    }

    private double getDistance(int s1, int s2, int intersection) {
        double a, b, c;
        a = intersection;
        b = s1 - intersection;
        c = s2 - intersection;
        double jaccard, sorenson, sokalSneath2, psc, kulczynski, Ochiai;
        jaccard = a / (a + b + c);
        sorenson = 2.0 * a / (2.0 * a + b + c);
        sokalSneath2 = a / (a + 2.0 * (b + c));
        psc = (a * a) / ((b + a) * (c + a));
        kulczynski = 0.5 * ((a / (a + b)) + (a / (a + c)));
        return 1 - jaccard;
    }

}
