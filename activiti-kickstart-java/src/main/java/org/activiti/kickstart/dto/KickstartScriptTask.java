package org.activiti.kickstart.dto;


public class KickstartScriptTask extends KickstartTask {

  private String scriptFormat;

  public String getScriptFormat() {
    return scriptFormat;
  }
  public void setScriptFormat(String scriptFormat) {
    this.scriptFormat = scriptFormat;
  }

  private String resultVariableName;

  public String getResultVariableName() {
    return resultVariableName;
  }
  public void setResultVariableName(String resultVariableName) {
    this.resultVariableName = resultVariableName;
  }

  private String script;

  public String getScript() {
    return script;
  }
  public void setScript(String script) {
    this.script = script;
  }

}
