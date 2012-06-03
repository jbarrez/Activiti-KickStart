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
package org.activiti.kickstart.service;

import java.util.List;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.kickstart.bpmn20.model.Definitions;
import org.activiti.kickstart.bpmn20.model.activity.Task;
import org.activiti.kickstart.bpmn20.model.activity.type.ScriptTask;
import org.activiti.kickstart.bpmn20.model.activity.type.ServiceTask;
import org.activiti.kickstart.bpmn20.model.activity.type.UserTask;
import org.activiti.kickstart.dto.TaskDto;
import org.activiti.kickstart.dto.MailTaskDto;
import org.activiti.kickstart.dto.ScriptTaskDto;
import org.activiti.kickstart.dto.ServiceTaskDto;
import org.activiti.kickstart.dto.UserTaskDto;
import org.activiti.kickstart.dto.WorkflowDto;
import org.activiti.kickstart.dto.WorkflowInfo;

/**
 * Implementations of this interface are responsible for converting the {@link WorkflowDto} workflow
 * representations into valid executable processes.
 * 
 * @author jbarrez
 */
public interface TransformationService {
	
	/**
	 * Convert the given list of Activiti {@link ProcessDefinition} instances to
	 * a lost of {@link WorkflowInfo} instances, representing information of those processes.
	 */
	List<WorkflowInfo> convertToWorkflowInfoList(List<ProcessDefinition> processDefinitions);

	/**
	 * Converts the given JAXB {@link Definitions} object to a {@link WorkflowDto} which is 
	 * usable by the KickStart operations.
	 */
	WorkflowDto convertToWorkflowDto(Definitions definitions);

	/**
	 * Converts the given JAXB {@link Task} to a {@link TaskDto} which is usable
	 * by the KickStart operations.
	 */
	TaskDto convertToTaskDto(Task task);

	/**
	 * Converts the given JAXB {@link UserTask} to a {@link UserTaskDto} which
	 * is usable by the KickStart operations.
	 */
	UserTaskDto convertToUserTaskDto(UserTask userTask);

	/**
	 * Converts the given JAXB {@link ServiceTask} to a {@link ServiceTaskDto} which
	 * is usable by the KickStart operations.
	 */
	ServiceTaskDto convertToServiceTaskDto(ServiceTask serviceTask);

	/**
	 * Converts the given JAXB {@link ServiceTask} to a {@link MailTaskDto} which
	 * is usable by the KickStart operations.
	 */
	MailTaskDto convertToMailTaskDto(ServiceTask serviceTask);

	/**
	 * Converts the given JAXB {@link ScriptTask} to a {@link ScriptTaskDto} which
	 * is usable by the KickStart operations.
	 */
	ScriptTaskDto convertToScriptTaskDto(ScriptTask serviceTask);
	
}
