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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.activiti.kickstart.diagram.ProcessDiagramGenerator;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;
import org.activiti.kickstart.service.Bpmn20MarshallingService;
import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.MetaDataKeys;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
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
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

/**
 * @author Joram Barrez
 */
public class AlfrescoKickstartServiceImpl implements KickstartService {
	      
    private static final Logger LOGGER = Logger.getLogger(AlfrescoKickstartServiceImpl.class.getName());
    
    // Constants /////////////////////////////////////////////////////////////////////
    
    private static final String KICKSTART_PREFIX = "ks:";
	
	// Alfresco specific folders and urls //////////////////////////////////////////
	
    private static final String WORKFLOW_DEFINITION_FOLDER = "/Data Dictionary/Workflow Definitions";
	
	private static final String DATA_DICTIONARY_FOLDER = "/Data Dictionary/Models";
	
	private static final String FORM_CONFIG_UPLOAD_URL = "http://localhost:8081/share/page/modules/module";
	
	 // Task Model templates /////////////////////////////////////////////////////////
	
    private static final String TEMPLATE_FOLDER = "/org/activiti/kickstart/service/alfresco/";
	
	private static final String TASK_MODEL_TEMPLATE_FILE = TEMPLATE_FOLDER + "task-model-template.xml";
	private static String TASK_MODEL_TEMPLATE;
	
	private static final String TASK_MODEL_TYPE_TEMPLATE_FILE =  TEMPLATE_FOLDER + "task-model-type-template.xml";
	private static String TASK_MODEL_TYPE_TEMPLATE;

	private static final String TASK_MODEL_PROPERTY_TEMPLATE_FILE =  TEMPLATE_FOLDER + "task-model-property-template.xml";
	private static String TASK_MODEL_PROPERTY_TEMPLATE;
	 
    // Form Config templates /////////////////////////////////////////////////////////
	 
	private static final String FORM_CONFIG_TEMPLATE_FILE =  TEMPLATE_FOLDER + "form-config-template.xml";
	private static String FORM_CONFIG_TEMPLATE;
	 
	private static final String FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE_FILE =  TEMPLATE_FOLDER + "form-config-evaluator-config-template.xml";
	private static String FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE;
	 
	private static final String FORM_CONFIG_FIELD_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-field-template.xml";
	private static String FORM_CONFIG_FIELD_TEMPLATE;
	
	private static final String FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-field-visibility-template.xml";
    private static String FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE;

    // Service parameters /////////////////////////////////////////////////////////// 
    
    protected String cmisUser;
	protected String cmisPassword;
	protected String cmisAtompubUrl;
	
	// Service members /////////////////////////////////////////////////////////////
	
	protected Session cachedSession;
	protected Bpmn20MarshallingService marshallingService;
	
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
					
					// We're using the Alfresco extensions
					parameters.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

					// First need to fetch the repository info to know the repo id
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

	public String deployWorkflow(KickstartWorkflow kickstartWorkflow, Map<String, String> metadata) {
	  
	  String jsonSource = metadata.get(MetaDataKeys.WORKFLOW_JSON_SOURCE);
	  if (jsonSource == null) {
	    throw new RuntimeException("Missing metadata " + MetaDataKeys.WORKFLOW_JSON_SOURCE);
	  }
	  
		deployTaskModel(kickstartWorkflow); // needs to go first, as the formkey will be filled in here
		return deployProcess(kickstartWorkflow, jsonSource); // Can't get the deployment id, so returning process definition id
	}

	protected String deployProcess(KickstartWorkflow kickstartWorkflow, String jsonSource) {
		
		// TODO: hack (until we get real users in there)
		for (KickstartTask kickstartTask : kickstartWorkflow.getTasks()) {
			((KickstartUserTask) kickstartTask).setAssignee("admin");
		}
		// TODO: hack
		
		// Process name needs to follow certain rules
		String processFileName = generateBpmnResourceName(kickstartWorkflow.getName());
		String baseName = processNameToBaseName(processFileName);
		
		// Process image (must go first, since it will add DI to the process xml)
		LOGGER.info("Generating process image...");
		ProcessDiagramGenerator diagramGenerator = new ProcessDiagramGenerator(kickstartWorkflow, marshallingService);
		InputStream diagramInputStream = diagramGenerator.execute();
		
		// Diagram is deployed next to the process xml
		Session cmisSession = getCmisSession();
		Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
		if (workflowDefinitionFolder == null) {
			throw new RuntimeException("Cannot find workflow definition folder '" + WORKFLOW_DEFINITION_FOLDER + "'");
		}
		
		HashMap<String, Object> diagramProperties = new HashMap<String, Object>();
		String diagramFileName = baseName + ".png";
		diagramProperties.put(PropertyIds.NAME, diagramFileName);
		diagramProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		
		ContentStream diagramContentStream = new ContentStreamImpl(diagramFileName, null, "image/png", diagramInputStream);
		workflowDefinitionFolder.createDocument(diagramProperties, diagramContentStream, VersioningState.MAJOR);
		
		// Upload json source of workflow
		LOGGER.info("Upload json source...");
		HashMap<String, Object> jsonSrcProperties = new HashMap<String, Object>();
		String jsonSrcFileName = baseName + ".json"; 
		jsonSrcProperties.put(PropertyIds.NAME, jsonSrcFileName);
		jsonSrcProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		
    ContentStream jsonSrcContentStream = new ContentStreamImpl(jsonSrcFileName, null, "application/json", new ByteArrayInputStream(jsonSource.getBytes()));
    workflowDefinitionFolder.createDocument(jsonSrcProperties, jsonSrcContentStream, VersioningState.MAJOR);
		
		// Deploying bpmn20.xml files to the workflow definition folder
		// of the Alfresco Data Dictionary will automatically deploy them

		// Create cmis document version of the workflow
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("cmis:name", processFileName);
		properties.put("cmis:objectTypeId", "D:bpm:workflowDefinition,P:cm:titled"); // Important! Process won't be deployed otherwise
		properties.put("bpm:definitionDeployed", true);
		properties.put("bpm:engineId", "activiti"); // Also vital for correct deployment!
		properties.put("cm:description", kickstartWorkflow.getName());

		// Upload the file
		String workflowXML = marshallingService.marshallWorkflow(kickstartWorkflow);
		InputStream inputStream = new ByteArrayInputStream(workflowXML.getBytes()); 
		
		LOGGER.info("Uploading process definition xml...");
		prettyLogXml(workflowXML);
		
		ContentStream contentStream = new ContentStreamImpl(processFileName, null, "application/xml", inputStream);
		Document document = workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
		
		LOGGER.info("Process definition uploaded to '" + document.getPaths() + "'");
		return baseName;
	}
	
	protected String processNameToBaseName(String processName) {
	  return processName.replace(".bpmn20.xml", "");
	}

	private void deployTaskModel(KickstartWorkflow workflow) {

		// Following stringbuilders will construct a valid content model and form config
		StringBuilder taskModelsString = new StringBuilder();
		StringBuilder evaluatorConfigStringBuilder = new StringBuilder();

		// XML generation
		for (KickstartTask task : workflow.getTasks()) {
			if (task instanceof KickstartUserTask) { // Only need to generte a form for user tasks
				generateTaskAndFormConfigForUserTask((KickstartUserTask) task, taskModelsString, evaluatorConfigStringBuilder);
			}
		}

		// Upload results to Alfresco
		uploadTaskModel(taskModelsString);
		uploadFormConfig(evaluatorConfigStringBuilder, workflow);
	}

	protected void generateTaskAndFormConfigForUserTask(KickstartUserTask userTask,
			StringBuilder taskModelsString, StringBuilder formConfigString) {

		if (userTask.getForm() != null) {

			String formId = KICKSTART_PREFIX + UUID.randomUUID().toString();
			userTask.getForm().setFormKey(formId);

			StringBuilder typeString = new StringBuilder();
			StringBuilder formAppearanceString = new StringBuilder();
			StringBuilder formVisibilityString = new StringBuilder();

			if (userTask.getForm().getFormProperties() != null
					&& userTask.getForm().getFormProperties().size() > 0) {

				typeString.append("<properties>");

				// Get form-propertes
				for (KickstartFormProperty formProperty : userTask.getForm().getFormProperties()) {
					// Property in type-definition
					typeString.append(MessageFormat.format(
							getTaskModelPropertyTemplate(),
							createFriendlyName(formProperty.getProperty()),
							getAlfrescoModelType(formProperty.getType()),
							formProperty.isRequired()));

					// Visibility in form-config
					formVisibilityString.append(MessageFormat.format(
							getFormConfigFieldVisibilityTemplate(),
							createFriendlyName(formProperty.getProperty())));

					// Appearance on screen in form-config
					formAppearanceString.append(MessageFormat.format(
							getFormConfigFieldTemplate(),
							createFriendlyName(formProperty.getProperty()),
							formProperty.getProperty()));

				}
				typeString.append("</properties>");
			}

			// Add name and all form-properties to model XML
			taskModelsString.append(MessageFormat.format(
					getTaskModelTypeTemplate(), formId,
					typeString.toString()));

			// Add task-form-config
			formConfigString.append(MessageFormat.format(
					getFormConfigEvaluatorConfigTemplate(), formId,
					formVisibilityString.toString(),
					formAppearanceString.toString()));
		}
	}
	
	protected void uploadTaskModel(StringBuilder taskModelsString) {
		Session session = getCmisSession();
		Folder modelFolder = (Folder) session.getObjectByPath(DATA_DICTIONARY_FOLDER);

		String taskModelId = UUID.randomUUID().toString();
		String taskModelFileName = "task-model-" + taskModelId + ".xml";
		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("cmis:name", taskModelFileName);
		properties.put("cmis:objectTypeId", "D:cm:dictionaryModel");
		properties.put("cm:modelActive", true);

		// Finally, wrap all taskdefinitions is right XML -> this is the FULL
		// model file, including generic start-task
		String taskModelXML = MessageFormat.format(getTaskModelTemplate(),
				taskModelId, taskModelsString.toString());
		LOGGER.info("Deploying task model XML:");
		prettyLogXml(taskModelXML);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(taskModelXML.getBytes());

		LOGGER.info("Task model file : " + taskModelFileName);
		ContentStream contentStream = new ContentStreamImpl(taskModelFileName, null, "application/xml", inputStream);

		modelFolder.createDocument(properties,contentStream, VersioningState.MAJOR);
	}
	
	protected void uploadFormConfig(StringBuilder evaluatorConfigStringBuilder, KickstartWorkflow workflow) {
		HttpState state = new HttpState();
		state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), 
				new UsernamePasswordCredentials(cmisUser, cmisPassword));

		PostMethod postMethod = new PostMethod(FORM_CONFIG_UPLOAD_URL);

		try {

			// Wrap all form-configs in right XML -> this is the FULL form-config
			// file, including generic start-task definition
			String formId = "kickstart_form_" + UUID.randomUUID().toString();
			String formConfig = MessageFormat.format(getFormConfigTemplate(),
					formId, 
					workflow.getName().replace(" ", "_"), 
					evaluatorConfigStringBuilder.toString());
			
			LOGGER.info("Deploying form config XML: ");
			prettyLogXml(formConfig);
			
			postMethod.setRequestEntity(new StringRequestEntity(formConfig, "application/xml", "UTF-8"));

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

	protected Object getAlfrescoModelType(String type) {
		if (type.equals("text")) {
			return "d:text";
		} else if (type.equals("date")) {
			return "d:date";
		} else if (type.equals("number")) {
			return "d:long";
		}
		return null;
	}

	protected Object createFriendlyName(String property) {
		return "ks:" + property.toLowerCase().replace(" ", "_");
	}

	public List<KickstartWorkflowInfo> findWorkflowInformation() {
		// Fetch all BPMN 2.0 xml processes from the definitions folder
	  Session cmisSession = getCmisSession();
    Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
    String query = "select t.cm:description, d." + PropertyIds.NAME + ", d." + PropertyIds.CREATION_DATE +
            " from cmis:document as d join cm:titled as t on d.cmis:objectId = t.cmis:objectId where in_folder(d, '" + workflowDefinitionFolder.getId() +
            "') and d.cmis:name LIKE '%.bpmn20.xml' order by d.cmis:name";
    LOGGER.info("Executing CMIS query '" + query + "'");
    ItemIterable<QueryResult> results = cmisSession.query(query , false);
	  
	  // Transmorph them into the correct KickstartWorkflowInfo object
    ArrayList<KickstartWorkflowInfo> workflowInfos = new ArrayList<KickstartWorkflowInfo>();
    for (QueryResult result : results) {
      // We're using only a fraction of the KickstartWorkflowInfo objects
      KickstartWorkflowInfo kickstartWorkflowInfo = new KickstartWorkflowInfo();
      kickstartWorkflowInfo.setName((String) result.getPropertyValueById("cm:description"));
      kickstartWorkflowInfo.setId(processNameToBaseName((String) result.getPropertyValueById(PropertyIds.NAME)));  
      GregorianCalendar createDate = result.getPropertyValueById(PropertyIds.CREATION_DATE); 
      kickstartWorkflowInfo.setCreateTime(createDate.getTime()) ;
      workflowInfos.add(kickstartWorkflowInfo);
    }
    
    return workflowInfos;
	}

	public KickstartWorkflow findWorkflowById(String id) {
		throw new UnsupportedOperationException();
	}

	public InputStream getProcessImage(String processDefinitionId) {
	  Session cmisSession = getCmisSession();
    Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
    
    Document imageDocument = (Document) cmisSession.getObjectByPath(workflowDefinitionFolder.getPath() + "/" + processDefinitionIdToProcessImage(processDefinitionId));
    return imageDocument.getContentStream().getStream();
	}	
	
	public void setProcessImage(String processDefinitionId, InputStream processImageStream) {
	  Session cmisSession = getCmisSession();
    Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
    
    HashMap<String, Object> properties = new HashMap<String, Object>();
    String fileName = processDefinitionIdToProcessImage(processDefinitionId); 
    properties.put("cmis:name", fileName);
    properties.put("cmis:objectTypeId", "cmis:document");
    
    ContentStream contentStream = new ContentStreamImpl(fileName, null, "image/png", processImageStream);
    workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
	}
	
	protected String processDefinitionIdToProcessImage(String processDefinitionId) {
	  return processDefinitionId + "_image.png";
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

	public Bpmn20MarshallingService getMarshallingService() {
		return marshallingService;
	}

	public void setMarshallingService(Bpmn20MarshallingService marshallingService) {
		this.marshallingService = marshallingService;
	}
	
	// Helper methods /////////////////////////////////////////////////////////////////
	
	protected String getTaskModelTemplate(){
		if (TASK_MODEL_TEMPLATE == null) {
			TASK_MODEL_TEMPLATE = readTemplateFile(TASK_MODEL_TEMPLATE_FILE);
		}
		return TASK_MODEL_TEMPLATE;
	}
	
	protected String getTaskModelTypeTemplate() {
		if (TASK_MODEL_TYPE_TEMPLATE == null)
		{
			TASK_MODEL_TYPE_TEMPLATE = readTemplateFile(TASK_MODEL_TYPE_TEMPLATE_FILE);
		}
		return TASK_MODEL_TYPE_TEMPLATE;
	}
	
	protected String getTaskModelPropertyTemplate() {
		if (TASK_MODEL_PROPERTY_TEMPLATE == null) {
			TASK_MODEL_PROPERTY_TEMPLATE = readTemplateFile(TASK_MODEL_PROPERTY_TEMPLATE_FILE);
		}
		return TASK_MODEL_PROPERTY_TEMPLATE;
	}
	
	protected String getFormConfigTemplate() {
		if (FORM_CONFIG_TEMPLATE == null) {
			FORM_CONFIG_TEMPLATE = readTemplateFile(FORM_CONFIG_TEMPLATE_FILE);
		}
		return FORM_CONFIG_TEMPLATE;
	}
	
	protected String getFormConfigEvaluatorConfigTemplate() {
		if (FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE == null) {
			FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE = readTemplateFile(FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE_FILE);
		}
		return FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE;
	}
	
	protected String getFormConfigFieldTemplate() {
		if (FORM_CONFIG_FIELD_TEMPLATE == null) {
			FORM_CONFIG_FIELD_TEMPLATE = readTemplateFile(FORM_CONFIG_FIELD_TEMPLATE_FILE);
		}
		return FORM_CONFIG_FIELD_TEMPLATE;
	}
	
	protected String getFormConfigFieldVisibilityTemplate() {
		if (FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE == null) {
			FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE = readTemplateFile(FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE_FILE);
		}
		return FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE;
	}
	
	protected String readTemplateFile(String templateFile) {
		LOGGER.info("Reading template file '" + templateFile + "'");
		InputStream inputStream = AlfrescoKickstartServiceImpl.class.getResourceAsStream(templateFile);
		if (inputStream == null) {
			LOGGER.warning("Could not read template file '" + templateFile + "'!");
		} else {
			try {
				return IOUtils.toString(inputStream);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Error while reading '" + templateFile + "' : " + e.getMessage());
			}
		}
		return null;
	}
	
	protected void prettyLogXml(String xml) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer(); 
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			 
			Source xmlInput = new StreamSource(new StringReader(xml));
			 
			StreamResult xmlOutput = new StreamResult(new StringWriter());
			transformer.transform(xmlInput, xmlOutput);
			LOGGER.info(xmlOutput.getWriter().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}