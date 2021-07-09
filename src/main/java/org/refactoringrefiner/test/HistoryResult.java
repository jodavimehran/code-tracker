package org.refactoringrefiner.test;

import javax.persistence.*;

@Entity
@Table(name = "history_dataset")
public class HistoryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "repository")
    private String repository;

    @Column(name = "element_type")
    private String elementType;

    @Column(name = "element_key")
    private String elementKey;

    @Column(name = "element_file_before")
    private String elementFileBefore;

    @Column(name = "element_name_before")
    private String elementNameBefore;

    @Column(name = "element_version_id_before")
    private String elementVersionIdBefore;

    @Column(name = "element_version_time_before")
    private long elementVersionTimeBefore;

    @Column(name = "element_file_after")
    private String elementFileAfter;

    @Column(name = "element_name_after")
    private String elementNameAfter;

    @Column(name = "element_version_id_after")
    private String elementVersionIdAfter;

    @Column(name = "element_version_time_after")
    private long elementVersionTimeAfter;

    @Column(name = "change_type")
    private String changeType;

    @Column(name = "refactoring_miner_vote")
    private int refactoringMinerVote = -1;

    @Column(name = "refactoring_miner_desc")
    private String refactoringMinerDesc;

    @Column(name = "code_shovel_vote")
    private int codeShovelVote = -1;

    @Column(name = "code_shovel_desc")
    private String codeShovelDesc;

    @Column(name = "refdiff_vote")
    private int refdiffVote = -1;

    @Column(name = "refdiff_desc")
    private String refDiffDesc;

    @Column(name = "code_shovel_oracle_vote")
    private int codeShovelOracleVote = -1;

    @Column(name = "oracle")
    private String oracle;

    @Column(name = "final_decision")
    private int finalDecision;

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

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public String getElementKey() {
        return elementKey;
    }

    public void setElementKey(String elementKey) {
        this.elementKey = elementKey;
    }

    public String getElementFileBefore() {
        return elementFileBefore;
    }

    public void setElementFileBefore(String elementFileBefore) {
        this.elementFileBefore = elementFileBefore;
    }

    public String getElementNameBefore() {
        return elementNameBefore;
    }

    public void setElementNameBefore(String elementNameBefore) {
        this.elementNameBefore = elementNameBefore;
    }

    public String getElementVersionIdBefore() {
        return elementVersionIdBefore;
    }

    public void setElementVersionIdBefore(String elementVersionIdBefore) {
        this.elementVersionIdBefore = elementVersionIdBefore;
    }

    public long getElementVersionTimeBefore() {
        return elementVersionTimeBefore;
    }

    public void setElementVersionTimeBefore(long elementVersionTimeBefore) {
        this.elementVersionTimeBefore = elementVersionTimeBefore;
    }

    public String getElementFileAfter() {
        return elementFileAfter;
    }

    public void setElementFileAfter(String elementFileAfter) {
        this.elementFileAfter = elementFileAfter;
    }

    public String getElementNameAfter() {
        return elementNameAfter;
    }

    public void setElementNameAfter(String elementNameAfter) {
        this.elementNameAfter = elementNameAfter;
    }

    public String getElementVersionIdAfter() {
        return elementVersionIdAfter;
    }

    public void setElementVersionIdAfter(String elementVersionIdAfter) {
        this.elementVersionIdAfter = elementVersionIdAfter;
    }

    public long getElementVersionTimeAfter() {
        return elementVersionTimeAfter;
    }

    public void setElementVersionTimeAfter(long elementVersionTimeAfter) {
        this.elementVersionTimeAfter = elementVersionTimeAfter;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public int getRefactoringMinerVote() {
        return refactoringMinerVote;
    }

    public void setRefactoringMinerVote(int refactoringMinerVote) {
        this.refactoringMinerVote = refactoringMinerVote;
    }

    public String getRefactoringMinerDesc() {
        return refactoringMinerDesc;
    }

    public void setRefactoringMinerDesc(String refactoringMinerDesc) {
        this.refactoringMinerDesc = refactoringMinerDesc;
    }

    public int getCodeShovelVote() {
        return codeShovelVote;
    }

    public void setCodeShovelVote(int codeShovelVote) {
        this.codeShovelVote = codeShovelVote;
    }

    public String getCodeShovelDesc() {
        return codeShovelDesc;
    }

    public void setCodeShovelDesc(String codeShovelDesc) {
        this.codeShovelDesc = codeShovelDesc;
    }

    public int getRefdiffVote() {
        return refdiffVote;
    }

    public void setRefdiffVote(int refdiffVote) {
        this.refdiffVote = refdiffVote;
    }

    public String getRefDiffDesc() {
        return refDiffDesc;
    }

    public void setRefDiffDesc(String refDiffDesc) {
        this.refDiffDesc = refDiffDesc;
    }

    public int getCodeShovelOracleVote() {
        return codeShovelOracleVote;
    }

    public void setCodeShovelOracleVote(int codeShovelOracleVote) {
        this.codeShovelOracleVote = codeShovelOracleVote;
    }

    public String getOracle() {
        return oracle;
    }

    public void setOracle(String oracle) {
        this.oracle = oracle;
    }

    public int getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(int finialDecision) {
        this.finalDecision = finialDecision;
    }
}
