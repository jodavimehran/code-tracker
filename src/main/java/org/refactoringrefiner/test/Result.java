package org.refactoringrefiner.test;

import org.refactoringminer.api.RefactoringType;

import javax.persistence.*;

@Entity
@Table(name = "dataset")
public class Result {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "repository")
    private String repository;

    @Column(name = "start_tag")
    private String startTag;

    @Column(name = "end_tag")
    private String endTag;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private RefactoringType refactoringType;

    @Column(name = "refactoring_desc")
    private String refactoringDesc;

    @Column(name = "refactoring_json")
    private String refactoringJSON;

    @Column(name = "confirmed")
    private boolean confirmed;

    @Column(name = "rm_ac")
    private boolean detectedByRMAC;

    @Column(name = "rm_fl")
    private boolean detectedByRMFL;

    @Column(name = "rr")
    private boolean detectedByRR;

    @Column(name = "sha256_hash")
    private String hash;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getStartTag() {
        return startTag;
    }

    public void setStartTag(String startTag) {
        this.startTag = startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public void setEndTag(String endTag) {
        this.endTag = endTag;
    }

    public RefactoringType getRefactoringType() {
        return refactoringType;
    }

    public void setRefactoringType(RefactoringType refactoringType) {
        this.refactoringType = refactoringType;
    }

    public String getRefactoringDesc() {
        return refactoringDesc;
    }

    public void setRefactoringDesc(String refactoringDesc) {
        this.refactoringDesc = refactoringDesc;
    }

    public String getRefactoringJSON() {
        return refactoringJSON;
    }

    public void setRefactoringJSON(String refactoringJSON) {
        this.refactoringJSON = refactoringJSON;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isDetectedByRMAC() {
        return detectedByRMAC;
    }

    public void setDetectedByRMAC(boolean detectedByRMAC) {
        this.detectedByRMAC = detectedByRMAC;
    }

    public boolean isDetectedByRMFL() {
        return detectedByRMFL;
    }

    public void setDetectedByRMFL(boolean detectedByRMFL) {
        this.detectedByRMFL = detectedByRMFL;
    }

    public boolean isDetectedByRR() {
        return detectedByRR;
    }

    public void setDetectedByRR(boolean detectedByRR) {
        this.detectedByRR = detectedByRR;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
