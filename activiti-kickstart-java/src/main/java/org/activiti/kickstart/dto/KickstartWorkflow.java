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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.activiti.kickstart.bpmn20.model.Definitions;

/**
 * @author Joram Barrez
 */
public class KickstartWorkflow {

  public static final String START_NAME = "theStart";
  public static final String END_NAME = "theEnd";

  protected String name;
  protected String description;
  protected List<KickstartTask> tasks = new ArrayList<KickstartTask>();
  protected List<KickstartTaskBlock> taskBlocks;

  // Cached version of the BPMN JAXB counterpart
  protected Definitions cachedDefinitions;

  public KickstartWorkflow() {
  }
  
  public Definitions getCachedDefinitions() {
	  return cachedDefinitions;
  }
  
  public void setCachedDefinitions(Definitions definitions) {
	  this.cachedDefinitions = definitions;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    this.cachedDefinitions = null;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
    this.cachedDefinitions = null;
  }

  public List<KickstartTask> getTasks() {
    return Collections.unmodifiableList(tasks);
  }

  public void setTasks(List<KickstartTask> tasks) {
    this.tasks = tasks;
    this.taskBlocks = null;
    this.cachedDefinitions = null;
  }

  public void addTask(KickstartTask task) {
    tasks.add(task);
    
    // Reset any previously generated taskblocks
    this.taskBlocks = null;
    this.cachedDefinitions = null;
  }

  public List<KickstartTaskBlock> getTaskBlocks() {
    if (taskBlocks == null) {
      generateTaskBlocks();
    }
    return taskBlocks;
  }

  protected void generateTaskBlocks() {
    taskBlocks = new ArrayList<KickstartTaskBlock>();
    for (int i = 0; i < tasks.size(); i++) {
      KickstartTask task = tasks.get(i);
      // Parallel tasks are grouped in the same task block
      if (task.getStartsWithPrevious() && (i != 0)) {
        taskBlocks.get(taskBlocks.size() - 1).addTask(task);
      } else {
        KickstartTaskBlock taskBlock = new KickstartTaskBlock();
        taskBlock.addTask(task);
        taskBlocks.add(taskBlock);
      }
    }
  }

}
