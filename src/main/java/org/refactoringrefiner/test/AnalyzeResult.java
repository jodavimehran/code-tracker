package org.refactoringrefiner.test;

public class AnalyzeResult {
    private final String repository, startTag, endTag;
    private final int totalCommit;
    private final double distanceRMAC, distanceRMFLC, distanceRM, sameCodeElementRate;
    private final long ptRMAC, ptRMFLC, ptRR;


    public AnalyzeResult(String repository, String startTag, String endTag, int totalCommit, double sameCodeElementRate,
                         double distanceRMAC, double distanceRMFLC, double distanceRM,
                         long ptRMAC, long ptRMFLC, long ptRR) {
        this.repository = repository;
        this.startTag = startTag;
        this.endTag = endTag;
        this.totalCommit = totalCommit;
        this.distanceRMAC = distanceRMAC;
        this.distanceRMFLC = distanceRMFLC;
        this.distanceRM = distanceRM;
        this.ptRMAC = ptRMAC;
        this.ptRMFLC = ptRMFLC;
        this.ptRR = ptRR;
        this.sameCodeElementRate = sameCodeElementRate;
    }

    public String getRepository() {
        return repository;
    }

    public String getStartTag() {
        return startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public int getTotalCommit() {
        return totalCommit;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d" + System.lineSeparator(), repository, startTag, endTag, totalCommit, sameCodeElementRate, distanceRMAC, distanceRMFLC, distanceRM, ptRMAC, ptRMFLC, ptRR);
    }
}
