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
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartMailTask;
import org.activiti.kickstart.dto.KickstartScriptTask;
import org.activiti.kickstart.dto.KickstartServiceTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;

/**
 * Implementations of this interface are responsible for converting the JAXB representations
 * of a BPMN 2.0 business process into objects that are usable by KickStart functionalities.
 * 
 * @author jbarrez
 */
public interface TransformationService {
	
	/**
	 * Convert the given list of Activiti {@link ProcessDefinition} instances to
	 * a lost of {@link KickstartWorkflowInfo} instances, representing information of those processes.
	 */
	List<KickstartWorkflowInfo> convertToWorkflowInfoList(List<ProcessDefinition> processDefinitions, boolean includeCounts);

	/**
	 * Converts the given JAXB {@link Definitions} object to a {@link KickstartWorkflow} which is 
	 * usable by the KickStart operations.
	 */
	KickstartWorkflow convertToKickstartWorkflow(Definitions definitions);

	/**
	 * Converts the given JAXB {@link Task} to a {@link KickstartTask} which is usable
	 * by the KickStart operations.
	 */
	KickstartTask convertToKickstartTask(Task task);

	/**
	 * Converts the given JAXB {@link UserTask} to a {@link KickstartUserTask} which
	 * is usable by the KickStart operations.
	 */
	KickstartUserTask convertToKickstartUserTask(UserTask userTask);

	/**
	 * Converts the given JAXB {@link ServiceTask} to a {@link KickstartServiceTask} which
	 * is usable by the KickStart operations.
	 */
	KickstartServiceTask convertToKickstartServiceTask(ServiceTask serviceTask);

	/**
	 * Converts the given JAXB {@link ServiceTask} to a {@link KickstartMailTask} which
	 * is usable by the KickStart operations.
	 */
	KickstartMailTask convertToKickstartMailTask(ServiceTask serviceTask);

	/**
	 * Converts the given JAXB {@link ScriptTask} to a {@link KickstartScriptTask} which
	 * is usable by the KickStart operations.
	 */
	KickstartScriptTask convertToKickstartScriptTask(ScriptTask serviceTask);
	
}
