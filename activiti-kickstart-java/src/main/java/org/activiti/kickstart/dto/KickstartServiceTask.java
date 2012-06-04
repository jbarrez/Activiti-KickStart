package org.activiti.kickstart.dto;


public class KickstartServiceTask extends KickstartTask {

  private String className;

  public String getClassName() {
    return className;
  }
  public void setClassName(String className) {
    this.className = className;
  }

  private String delegateExpression;

  public String getDelegateExpression() {
    return delegateExpression;
  }
  public void setDelegateExpression(String delegateExpression) {
    this.delegateExpression = delegateExpression;
  }

  private String expression;

  public String getExpression() {
    return expression;
  }
  public void setExpression(String expression) {
    this.expression = expression;
  }

}
