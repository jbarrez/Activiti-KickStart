package org.activiti.kickstart.dto;


public abstract class KickstartTask {

  protected String id;

  protected String name;

  protected String description;

  protected boolean startWithPrevious;

  public KickstartTask() {
    super();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getStartsWithPrevious() {
    return startWithPrevious;
  }

  public void setStartWithPrevious(boolean startWithPrevious) {
    this.startWithPrevious = startWithPrevious;
  }
  
}
