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

import java.io.InputStream;
import java.util.List;

import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;

/**
 * This is the service you'll typically use if you want to integrate the Activiti Kickstart functionality. 
 * 
 * @author Joram Barrez
 */
public interface KickstartService {

	/**
	 * Deploys the given workflow representation to the configured Activiti engine.
	 */
	String deployWorkflow(KickstartWorkflow kickstartWorkflowDto);

	/**
	 * Retrieves a list of {@link KickstartWorkflowInfo} instances which
	 * correspond with all the processes that are deployed to the configured
	 * Activiti engine and which can are compatible with KickStart.
	 */
	List<KickstartWorkflowInfo> findWorkflowInformation();

	/**
	 * Fetches the process definition for the KickStart workflow from
	 * the configured Activiti engine data store.
	 */
	KickstartWorkflow findWorkflowById(String id);

	/**
	 * Returns an {@link InputStream} to the process image for the process definition
	 * with the given id (convenience method - this is already possible with Activiti)
	 */
	InputStream getProcessImage(String processDefinitionId);

	/**
	 * Returns an {@link InputStream} to the BPMN 2.0 xml for the process definition
	 * with the given id (convenience method - this is already possible with Activiti).
	 */
	InputStream getBpmnXml(String processDefinitionId);

}
