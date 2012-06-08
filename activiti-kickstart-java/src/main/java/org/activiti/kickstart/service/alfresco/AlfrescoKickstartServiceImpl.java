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
package org.activiti.kickstart.service.alfresco;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;
import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.MarshallingService;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

/**
 * @author Joram Barrez
 */
public class AlfrescoKickstartServiceImpl implements KickstartService {
	
	protected Map<String, String> cmisSessionParameters;
	
	protected MarshallingService marshallingService;

	public String deployKickstartWorkflow(KickstartWorkflow kickstartWorkflow) throws JAXBException {
		
		String deploymentName = kickstartWorkflow.getName() + ".bpmn20.xml";
		
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Session session = sessionFactory.createSession(cmisSessionParameters);
		Folder workflowDefinitionFolder = (Folder) session.getObjectByPath("/Data Dictionary/Workflow Definitions");
		
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("cmis:name", deploymentName);
		properties.put("cmis:objectTypeId", "D:bpm:workflowDefinition");
		properties.put("bpm:definitionDeployed", true);
		properties.put("bpm:engineId", "activiti");
		
		String workflowXml = marshallingService.marshallWorkflow(kickstartWorkflow);
		ContentStream contentStream = new ContentStreamImpl("test_upload.bpmn20.xml", null, 
				"application/xml", new ByteArrayInputStream(workflowXml.getBytes()));
		
		Document document = workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
		return document.getId();
	}

	public List<KickstartWorkflowInfo> findKickstartWorkflowInformation() {
		throw new UnsupportedOperationException();
	}

	public KickstartWorkflow findKickstartWorkflowById(String id) throws JAXBException {
		throw new UnsupportedOperationException();
	}

	public InputStream getProcessImage(String processDefinitionId) {
		throw new UnsupportedOperationException();
	}

	public InputStream getProcessBpmnXml(String processDefinitionId) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Generates a valid bpmn 2.0 file name for the given process name.
	 */
	protected String generateBpmnResourceName(String processName) {
		return processName.replace(" ", "_") + ".bpmn20.xml";
	}

	
	// Getters & Setters
	
	public Map<String, String> getCmisSessionParameters() {
		return cmisSessionParameters;
	}
	
	public void setCmisSessionParameters(Map<String, String> cmisSessionParameters) {
		this.cmisSessionParameters = cmisSessionParameters;
	}

	public MarshallingService getMarshallingService() {
		return marshallingService;
	}

	public void setMarshallingService(MarshallingService marshallingService) {
		this.marshallingService = marshallingService;
	}	

}