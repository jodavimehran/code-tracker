package org.codetracker.experiment.oracle.history;

public class BlockHistoryInfo extends AbstractHistoryInfo {

  private String functionName;
  private String functionKey;
  private int functionStartLine;
  private String blockType;
  private String blockKey;
  private int blockStartLine;
  private int blockEndLine;

  public String getFunctionName() {
    return functionName;
  }

  public void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  public String getFunctionKey() {
    return functionKey;
  }

  public void setFunctionKey(String functionKey) {
    this.functionKey = functionKey;
  }

  public int getFunctionStartLine() {
    return functionStartLine;
  }

  public void setFunctionStartLine(int functionStartLine) {
    this.functionStartLine = functionStartLine;
  }

  public String getBlockKey() {
    return blockKey;
  }

  public void setBlockKey(String blockKey) {
    this.blockKey = blockKey;
  }

  public String getBlockType() {
    return blockType;
  }

  public void getBlockType(String blockType) {
    this.blockType = blockType;
  }

  public int getBlockStartLine() {
    return blockStartLine;
  }

  public void setBlockStartLine(int blockStartLine) {
    this.blockStartLine = blockStartLine;
  }

  public int getBlockEndLine() {
    return blockEndLine;
  }

  public void setBlockEndLine(int blockEndLine) {
    this.blockEndLine = blockEndLine;
  }

  @Override
  public String getElementKey() {
    return blockKey;
  }
}
