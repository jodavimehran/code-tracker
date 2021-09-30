package org.codetracker.experiment.oracle.history;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"repositoryName", "repositoryWebURL", "startCommitId", "filePath", "branchName", "attributeName", "attributeKey", "attributeDeclarationLine", "expectedChanges"})
public class AttributeHistoryInfo extends AbstractHistoryInfo {
    private String attributeName;
    private String attributeKey;
    private int attributeDeclarationLine;

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public int getAttributeDeclarationLine() {
        return attributeDeclarationLine;
    }

    public void setAttributeDeclarationLine(int attributeDeclarationLine) {
        this.attributeDeclarationLine = attributeDeclarationLine;
    }

    @Override
    public String getElementKey() {
        return attributeKey;
    }
}
