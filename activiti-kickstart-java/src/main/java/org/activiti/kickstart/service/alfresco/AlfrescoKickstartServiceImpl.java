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
import java.util.logging.Logger;

import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;
import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.MarshallingService;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * @author Joram Barrez
 */
public class AlfrescoKickstartServiceImpl implements KickstartService {
	
	private static final Logger LOGGER = Logger.getLogger(AlfrescoKickstartServiceImpl.class.getName());
	
	private static final String WORKFLOW_DEFINITION_FOLDER = "/Data Dictionary/Workflow Definitions";

	protected String cmisUser;
	protected String cmisPassword;
	protected String cmisAtompubUrl;
	protected Session cachedSession;

	protected MarshallingService marshallingService;
	
	public AlfrescoKickstartServiceImpl(String cmisUser, String cmisPassword, String cmisAtompubUrl) {
		this.cmisUser = cmisUser;
		this.cmisPassword = cmisPassword;
		this.cmisAtompubUrl = cmisAtompubUrl;
	}
	
	protected Session getCmisSession() {
		if (cachedSession == null) {
			synchronized (this) {
				if (cachedSession == null) {
					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(SessionParameter.USER, cmisUser);
					parameters.put(SessionParameter.PASSWORD, cmisPassword);
					parameters.put(SessionParameter.ATOMPUB_URL, cmisAtompubUrl);
					parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

					// First need to detect the repository id
					SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
					List<Repository> repositories = sessionFactory.getRepositories(parameters);
					String repositoryId = repositories.get(0).getId();
					parameters.put(SessionParameter.REPOSITORY_ID, repositoryId);
					
					cachedSession = sessionFactory.createSession(parameters);
				}
			}
		}
		return cachedSession;
	}

	public String deployWorkflow(KickstartWorkflow kickstartWorkflow) {
		LOGGER.info("deploying process");
		String processDocumentId = deployProcess(kickstartWorkflow);

//		LOGGER.info("deploying task model");
//		deployTaskModel();
//
//		LOGGER.info("deploying form");
//		deployForm();
//
//		LOGGER.info("Workflow deployment finished");
		
		return processDocumentId; // Can't get the deployment id, so returning the document id
	}

	protected String deployProcess(KickstartWorkflow kickstartWorkflow) {
		
		// Deploying bpmn20.xml files to the workflow definition folder
		// of the Alfresco data dictionary will automatically deploy them
		Session cmisSession = getCmisSession();
		Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
		if (workflowDefinitionFolder == null) {
			throw new RuntimeException("Cannot find workflow definition folder '" + WORKFLOW_DEFINITION_FOLDER + "'");
		}

		// Create cmis document version of the workflow
		String processName = generateBpmnResourceName(kickstartWorkflow.getName());
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("cmis:name", processName);
		properties.put("cmis:objectTypeId", "D:bpm:workflowDefinition");
		properties.put("bpm:definitionDeployed", true);
		properties.put("bpm:engineId", "activiti");

		// Upload the file
		InputStream inputStream = new ByteArrayInputStream(marshallingService.marshallWorkflow(kickstartWorkflow).getBytes()); 
		ContentStream contentStream = new ContentStreamImpl(processName, null, "application/xml", inputStream);
		Document document = workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
		
		return document.getId();
	}

	protected void deployTaskModel() {
//		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
//		Map<String, String> parameter = new HashMap<String, String>();
//		parameter.put(SessionParameter.USER, "admin");
//		parameter.put(SessionParameter.PASSWORD, "admin");
//		parameter.put(SessionParameter.ATOMPUB_URL, "http://localhost:8080/alfresco/service/api/cmis");
////		parameter.put(SessionParameter.REPOSITORY_ID, findRepositoryId());
//		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
//
//		Session session = sessionFactory.createSession(parameter);
//		Folder modelFolder = (Folder) session.getObjectByPath("/Data Dictionary/Models");
//
//		HashMap<String, Object> properties = new HashMap<String, Object>();
//		properties.put("cmis:name", "task_model.xml");
//		properties.put("cmis:objectTypeId", "D:cm:dictionaryModel");
//		properties.put("cm:modelActive", true);

//		FileInputStream fis = new FileInputStream("task-model.xml");

//		ContentStream contentStream = new ContentStreamImpl("task-model.xml", null, "application/xml", fis);
//		Document document = modelFolder.createDocument(properties,contentStream, VersioningState.MAJOR);
	}

	protected void deployForm() {
		HttpState state = new HttpState();
		state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));

		PostMethod postMethod = new PostMethod("http://localhost:8081/share/page/modules/module");

		try {

//			String formConfig = FileUtils.readFileToString(new File("test-form-config.xml"));
//			postMethod.setRequestEntity(new StringRequestEntity(formConfig, "application/xml", "UTF-8"));

			// postMethod.setRequestHeader("Content-type", "text/xml");
			HttpClient httpClient = new HttpClient();
			int result = httpClient.executeMethod(null, postMethod, state);

			// Display status code
			System.out.println("Response status code: " + result);

			// Display response
			System.out.println("Response body: ");
			System.out.println(postMethod.getResponseBodyAsString());
		} catch (Throwable t) {
			System.err.println("Error: " + t.getMessage());
			t.printStackTrace();
		} finally {
			postMethod.releaseConnection();
		}
	}

	public List<KickstartWorkflowInfo> findWorkflowInformation() {
		throw new UnsupportedOperationException();
	}

	public KickstartWorkflow findWorkflowById(String id) {
		throw new UnsupportedOperationException();
	}

	public InputStream getProcessImage(String processDefinitionId) {
		throw new UnsupportedOperationException();
	}

	public InputStream getBpmnXml(String processDefinitionId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Generates a valid bpmn 2.0 file name for the given process name.
	 */
	protected String generateBpmnResourceName(String processName) {
		return processName.replace(" ", "_") + ".bpmn20.xml";
	}

	// Getters & Setters

	public String getCmisUser() {
		return cmisUser;
	}

	public void setCmisUser(String cmisUser) {
		this.cmisUser = cmisUser;
	}

	public String getCmisPassword() {
		return cmisPassword;
	}

	public void setCmisPassword(String cmisPassword) {
		this.cmisPassword = cmisPassword;
	}

	public String getCmisAtompubUrl() {
		return cmisAtompubUrl;
	}

	public void setCmisAtompubUrl(String cmisAtompubUrl) {
		this.cmisAtompubUrl = cmisAtompubUrl;
	}

	public MarshallingService getMarshallingService() {
		return marshallingService;
	}

	public void setMarshallingService(MarshallingService marshallingService) {
		this.marshallingService = marshallingService;
	}

}