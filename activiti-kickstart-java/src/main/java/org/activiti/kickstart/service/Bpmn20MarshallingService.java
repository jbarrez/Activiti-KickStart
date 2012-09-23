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

import org.activiti.kickstart.bpmn20.model.Definitions;
import org.activiti.kickstart.bpmn20.model.FlowElement;
import org.activiti.kickstart.bpmn20.model.activity.type.ScriptTask;
import org.activiti.kickstart.bpmn20.model.activity.type.ServiceTask;
import org.activiti.kickstart.bpmn20.model.activity.type.UserTask;
import org.activiti.kickstart.dto.KickstartMailTask;
import org.activiti.kickstart.dto.KickstartScriptTask;
import org.activiti.kickstart.dto.KickstartServiceTask;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;

/**
 * Responsible for marshalling Kickstart workflows and task into valid, executable BPMN 2.0.
 * 
 * @author jbarrez
 */
public interface Bpmn20MarshallingService {
	
	/**
	 * Marshalls the given {@link KickstartWorkflow} to a BPMN 2.0 compatible XML.
	 */
	String marshallWorkflow(KickstartWorkflow kickstartWorkflowDto);
	
	/**
	 * Converts the given {@link KickstartWorkflow} into a JAXB representation of the BPMN 2.0 business process.
	 */
	Definitions convertToBpmn(KickstartWorkflow kickstartWorkflow);
	
	/**
	 * Converts the given {@link KickstartTask} into a JAXB representation of the BPMN 2.0 counterpart.
	 */
	FlowElement convertToBPMN(KickstartTask kickstartTask);
	
	/**
	 * Converts the given {@link KickstartUserTask} into a JAXB representation of the BPMN 2.0 counterpart.
	 */
	UserTask convertToBPMN(KickstartUserTask kickstartUserTask);
	
	/**
	 * Converts the given {@link KickstartServiceTask} into a JAXB representation of the BPMN 2.0 counterpart.
	 */
	ServiceTask convertToBPMN(KickstartServiceTask kickstartServiceTask);
	
	/**
	 * Converts the given {@link KickstartScriptTask} into a JAXB representation of the BPMN 2.0 counterpart.
	 */
	ScriptTask convertToBPMN(KickstartScriptTask kickstartScriptTask);
	
	/**
	 * Converts the given {@link KickstartMailTask} into a JAXB representation of the BPMN 2.0 counterpart.
	 */
	ServiceTask convertToBPMN(KickstartMailTask kickstartMailTask);

}
