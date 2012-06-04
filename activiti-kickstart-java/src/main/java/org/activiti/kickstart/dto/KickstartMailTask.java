package org.activiti.kickstart.dto;


public class KickstartMailTask extends KickstartTask {

  public static class Field {

    public Field(String name) {
      this.name = name;
    }

    private String name;

    public String getName() {
      return name;
    }

    private String stringValue;

    public String getStringValue() {
      return stringValue;
    }
    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    private String expression;

    public String getExpression() {
      return expression;
    }
    public void setExpression(String expression) {
      this.expression = expression;
    }
  }

  private Field to = new Field("to");
  private Field from = new Field("from");
  private Field cc = new Field("cc");
  private Field subject = new Field("subject");
  private Field bcc = new Field("bcc");
  private Field html = new Field("html");
  private Field text = new Field("text");

  public Field getTo() {
    return to;
  }
  public Field getFrom() {
    return from;
  }
  public Field getCc() {
    return cc;
  }
  public Field getSubject() {
    return subject;
  }
  public Field getBcc() {
    return bcc;
  }
  public Field getHtml() {
    return html;
  }
  public Field getText() {
    return text;
  }

}
