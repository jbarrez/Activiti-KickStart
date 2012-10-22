/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.kickstart.dto;


/**
 * @author Joram Barrez
 */
public class KickstartUserTask extends KickstartTask {

  protected boolean isAssigneeInitiator;
  protected String assignee;

  protected String groups;

  protected KickstartForm form;

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getGroups() {
    return groups;
  }

  public void setGroups(String groups) {
    this.groups = groups;
  }

  public KickstartForm getForm() {
    return form;
  }

  public void setForm(KickstartForm formDto) {
    this.form = formDto;
  }
  
  public boolean isAssigneeInitiator() {
    return isAssigneeInitiator;
  }
  
  public void setAssigneeInitiator(boolean isAssigneeInitiator) {
    this.isAssigneeInitiator = isAssigneeInitiator;
  }

  public String generateDefaultFormName() {
    return name.replace(" ", "_") + ".form";
  }

}
