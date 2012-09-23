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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.kickstart.bpmn20.model.Definitions;
import org.activiti.kickstart.diagram.ProcessDiagramGenerator;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;

/**
 * @author Joram Barrez
 */
public class KickstartServiceImpl implements KickstartService {

	protected RepositoryService repositoryService;
	protected TransformationService transformationService;
	protected Bpmn20MarshallingService marshallingService;
	
	// Kickstart operations //////////////////////////////////////////////////////////////


	public String deployWorkflow(KickstartWorkflow kickstartWorkflow) {
		String deploymentName = "Process " + kickstartWorkflow.getName();
		String bpmn20XmlResourceName = generateBpmnResourceName(kickstartWorkflow.getName());
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(deploymentName);

		// png image (must go first, since it will add DI to the process xml)
		ProcessDiagramGenerator diagramGenerator = new ProcessDiagramGenerator(kickstartWorkflow, marshallingService);
		deploymentBuilder.addInputStream(bpmn20XmlResourceName.replace(".bpmn20.xml", ".png"), diagramGenerator.execute());

		// bpmn 2.0 xml
    	String workflowXml = marshallingService.marshallWorkflow(kickstartWorkflow);
		deploymentBuilder.addString(bpmn20XmlResourceName,workflowXml);
		Deployment deployment = deploymentBuilder.deploy();
		return deployment.getId();
	}

	public List<KickstartWorkflowInfo> findWorkflowInformation() {
		List<ProcessDefinition> processDefinitions = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionKeyLike("adhoc_%")
				.orderByProcessDefinitionName().asc()
				.orderByProcessDefinitionVersion().desc().list();
		return transformationService.convertToWorkflowInfoList(processDefinitions);
	}

	public KickstartWorkflow findWorkflowById(String id) {
		// Get process definition for key
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery().processDefinitionId(id)
				.singleResult();

		// Get BPMN 2.0 XML file from database and parse it with JAXB
		InputStream is = null;
		Definitions definitions = null;
		try {
			is = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());

			JAXBContext jc = JAXBContext.newInstance(Definitions.class);
			Unmarshaller um = jc.createUnmarshaller();
			definitions = (Definitions) um.unmarshal(is);
			
		} catch (JAXBException e) {
			throw new RuntimeException("Could not unmarshall workflow xml", e);
		} finally {
			IoUtil.closeSilently(is);
		}

		// Convert JAXB to internal model
		return transformationService.convertToKickstartWorkflow( definitions);
	}

	public InputStream getProcessImage(String processDefinitionId) {
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		return repositoryService.getResourceAsStream(
				processDefinition.getDeploymentId(),
				processDefinition.getDiagramResourceName());
	};

	public InputStream getBpmnXml(String processDefinitionId) {
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		return repositoryService.getResourceAsStream(
				processDefinition.getDeploymentId(),
				processDefinition.getResourceName());
	}

	// Helper methods
	// ///////////////////////////////////////////////////////////////////

	/**
	 * Generates a valid bpmn 2.0 file name for the given process name.
	 */
	protected String generateBpmnResourceName(String processName) {
		return processName.replace(" ", "_") + ".bpmn20.xml";
	}
	
	// Getters and Setters //////////////////////////////////////////////////////////////
	
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public TransformationService getTransformationService() {
		return transformationService;
	}

	public void setTransformationService(TransformationService transformationService) {
		this.transformationService = transformationService;
	}
	
	public Bpmn20MarshallingService getMarshallingService() {
		return marshallingService;
	}
	
	public void setMarshallingService(Bpmn20MarshallingService marshallingService) {
		this.marshallingService = marshallingService;
	}

}