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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
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
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 */
public class AlfrescoKickstartServiceImpl implements KickstartService {
	      
  private static final Logger LOGGER = Logger.getLogger(AlfrescoKickstartServiceImpl.class.getName());

  // Constants
  // /////////////////////////////////////////////////////////////////////

  private static final String KICKSTART_PREFIX = "ks:";
  
  // URLS
  // ////////////////////////////////////////////
  
  private static final String ALFRESCO_BASE_URL = "http://localhost:8080/alfresco/service/";

  private static final String SHARE_BASE_URL = "http://localhost:8081/share/";
  

  // Alfresco specific folders and urls
  // //////////////////////////////////////////

  private static final String WORKFLOW_DEFINITION_FOLDER = "/Data Dictionary/Workflow Definitions";

  private static final String DATA_DICTIONARY_FOLDER = "/Data Dictionary/Models";

  private static final String FORM_CONFIG_UPLOAD_URL = SHARE_BASE_URL + "page/modules/module";

  // Task Model templates
  // /////////////////////////////////////////////////////////

  private static final String TEMPLATE_FOLDER = "/org/activiti/kickstart/service/alfresco/";

  private static final String TASK_MODEL_TEMPLATE_FILE = TEMPLATE_FOLDER + "task-model-template.xml";
  private static String TASK_MODEL_TEMPLATE;

  private static final String TASK_MODEL_TYPE_TEMPLATE_FILE = TEMPLATE_FOLDER + "task-model-type-template.xml";
  private static String TASK_MODEL_TYPE_TEMPLATE;

  private static final String TASK_MODEL_PROPERTY_TEMPLATE_FILE = TEMPLATE_FOLDER + "task-model-property-template.xml";
  private static String TASK_MODEL_PROPERTY_TEMPLATE;

  // Form Config templates
  // /////////////////////////////////////////////////////////

  private static final String FORM_CONFIG_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-template.xml";
  private static String FORM_CONFIG_TEMPLATE;

  private static final String FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-evaluator-config-template.xml";
  private static String FORM_CONFIG_EVALUATOR_CONFIG_TEMPLATE;

  private static final String FORM_CONFIG_FIELD_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-field-template.xml";
  private static String FORM_CONFIG_FIELD_TEMPLATE;

  private static final String FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-field-visibility-template.xml";
  private static String FORM_CONFIG_FIELD_VISIBILITY_TEMPLATE;
  
  private static final String FORM_CONFIG_FIELD_INFO_TEMPLATE_FILE = TEMPLATE_FOLDER + "form-config-field-info-control-template.xml";
  private static String FORM_CONFIG_FIELD_INFO_TEMPLATE;

  // Service parameters
  // ///////////////////////////////////////////////////////////

  protected String cmisUser;
  protected String cmisPassword;
  protected String cmisAtompubUrl;

  // Service members
  // /////////////////////////////////////////////////////////////

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
	  
	  // Validate metadata
	  String jsonSource = metadata.get(MetaDataKeys.WORKFLOW_JSON_SOURCE);
	  if (jsonSource == null) {
	    throw new RuntimeException("Missing metadata " + MetaDataKeys.WORKFLOW_JSON_SOURCE);
	  }

	  // Create base name for all files that will be stored
	  String baseName = kickstartWorkflow.getId();
	  
	  // Following stringbuilders will construct a valid content model and form config
    StringBuilder taskModelsString = new StringBuilder();
    StringBuilder evaluatorConfigStringBuilder = new StringBuilder();

    // Needs to go first, as the formkey will be filled in here
    for (KickstartTask task : kickstartWorkflow.getTasks()) {
      if (task instanceof KickstartUserTask) { // Only need to generte a form for user tasks
        generateTaskAndFormConfigForUserTask((KickstartUserTask) task, taskModelsString, evaluatorConfigStringBuilder);
      }
    }

    // Upload results to Alfresco
    uploadTaskModel(taskModelsString, baseName);
    uploadFormConfig(evaluatorConfigStringBuilder, kickstartWorkflow, baseName);
	  
    // Upload process
		deployProcess(kickstartWorkflow, baseName, jsonSource); // Can't get the deployment id, so returning process definition id
		return baseName;
	}

	protected void deployProcess(KickstartWorkflow kickstartWorkflow, String baseFileName,String jsonSource) {
		
		// TODO: hack (until we get real users in there)
		for (KickstartTask kickstartTask : kickstartWorkflow.getTasks()) {
			((KickstartUserTask) kickstartTask).setAssignee("admin");
		}
		// TODO: hack
	
		Session cmisSession = getCmisSession();
		Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
		
		uploadDiagramFile(kickstartWorkflow, baseFileName, workflowDefinitionFolder); // Process image (must go first, since it will add DI to the process xml)
		uploadJsonFile(baseFileName, jsonSource, workflowDefinitionFolder);
		uploadProcessFile(kickstartWorkflow, baseFileName, workflowDefinitionFolder);
	}

  private void uploadDiagramFile(KickstartWorkflow kickstartWorkflow, String baseFileName, Folder workflowDefinitionFolder) {
    LOGGER.info("Generating process image...");
		ProcessDiagramGenerator diagramGenerator = new ProcessDiagramGenerator(kickstartWorkflow, marshallingService);
		InputStream diagramInputStream = diagramGenerator.execute();
		
		// Diagram is deployed next to the process xml
		if (workflowDefinitionFolder == null) {
			throw new RuntimeException("Cannot find workflow definition folder '" + WORKFLOW_DEFINITION_FOLDER + "'");
		}
		
		String diagramFileName = baseFileName + ".png";
		ContentStream diagramContentStream = new ContentStreamImpl(diagramFileName, null, "image/png", diagramInputStream);
		Document diagramDocument = getDocumentFromFolder(workflowDefinitionFolder.getPath(), diagramFileName);
		if (diagramDocument == null) {
		
  		HashMap<String, Object> diagramProperties = new HashMap<String, Object>();
  		diagramProperties.put(PropertyIds.NAME, diagramFileName);
  		diagramProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
  		
  		workflowDefinitionFolder.createDocument(diagramProperties, diagramContentStream, VersioningState.MAJOR);
  		
		} else {
		  
		  diagramDocument.setContentStream(diagramContentStream, true);
		  
		}
  }

  private void uploadJsonFile(String baseFileName, String jsonSource, Folder workflowDefinitionFolder) {
    LOGGER.info("Upload json source...");
		String jsonSrcFileName = baseFileName + ".json"; 
		ContentStream jsonSrcContentStream = new ContentStreamImpl(jsonSrcFileName, null, "application/json", new ByteArrayInputStream(jsonSource.getBytes()));
		Document jsonDocument = getDocumentFromFolder(workflowDefinitionFolder.getPath(), jsonSrcFileName);
		if (jsonDocument == null) {
		
  		HashMap<String, Object> jsonSrcProperties = new HashMap<String, Object>();
  		jsonSrcProperties.put(PropertyIds.NAME, jsonSrcFileName);
  		jsonSrcProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
  		
      workflowDefinitionFolder.createDocument(jsonSrcProperties, jsonSrcContentStream, VersioningState.MAJOR);
      
		} else {
		  
		  jsonDocument.setContentStream(jsonSrcContentStream, true);
		  
		}
  }

  private void uploadProcessFile(KickstartWorkflow kickstartWorkflow, String baseFileName, Folder workflowDefinitionFolder) {
    String processFileName = baseFileName + ".bpmn20.xml";
		
    String workflowXML = marshallingService.marshallWorkflow(kickstartWorkflow);
    InputStream inputStream = new ByteArrayInputStream(workflowXML.getBytes()); 
    LOGGER.info("Uploading process definition xml...");
    prettyLogXml(workflowXML);
    ContentStream processContentStream = new ContentStreamImpl(processFileName, null, "application/xml", inputStream);
    
    Document processDocument = getDocumentFromFolder(workflowDefinitionFolder.getPath(), processFileName);
    if (processDocument == null) {
      
      HashMap<String, Object> properties = new HashMap<String, Object>();
      properties.put("cmis:name", processFileName);
      properties.put("cmis:objectTypeId", "D:bpm:workflowDefinition,P:cm:titled"); // Important! Process won't be deployed otherwise
      properties.put("bpm:definitionDeployed", true);
      properties.put("bpm:engineId", "activiti"); // Also vital for correct deployment!
      properties.put("cm:description", kickstartWorkflow.getName());
      
      processDocument = workflowDefinitionFolder.createDocument(properties, processContentStream, VersioningState.MAJOR);
      
    } else {
      
      processDocument.setContentStream(processContentStream, true);
      
    }
    
		LOGGER.info("Process definition uploaded to '" + processDocument.getPaths() + "'");
  }
	
	protected String processNameToBaseName(String processName) {
	  return processName.replace(".bpmn20.xml", "");
	}

	protected void generateTaskAndFormConfigForUserTask(KickstartUserTask userTask,
			StringBuilder taskModelsString, StringBuilder formConfigString) {

		if (userTask.getForm() != null) {

		  String uuid = UUID.randomUUID().toString(); 
			String formId = KICKSTART_PREFIX + uuid;
			userTask.getForm().setFormKey(formId);

			StringBuilder typeString = new StringBuilder();
			StringBuilder formAppearanceString = new StringBuilder();
			StringBuilder formVisibilityString = new StringBuilder();
			
			if (userTask.getDescription() != null) {
			  String propertyValue = "ks:description_" + uuid;
			  formVisibilityString.append(MessageFormat.format(getFormConfigFieldVisibilityTemplate(), propertyValue));
			  formAppearanceString.append(MessageFormat.format(getFormConfigInfoTemplate(), propertyValue, "Description"));
			}
			
			if (userTask.getForm().getFormProperties() != null
					&& userTask.getForm().getFormProperties().size() > 0) {

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
			}
			
			// Add name and all form-properties to model XML
			taskModelsString.append(MessageFormat.format(
					getTaskModelTypeTemplate(), 
					formId,
					uuid,
					userTask.getDescription(),
					typeString.toString()));

			// Add task-form-config
			formConfigString.append(MessageFormat.format(
					getFormConfigEvaluatorConfigTemplate(), formId,
					formVisibilityString.toString(),
					formAppearanceString.toString()));
		}
	}
	
	protected void uploadTaskModel(StringBuilder taskModelsString, String baseFileName) {
		Session session = getCmisSession();
		Folder modelFolder = (Folder) session.getObjectByPath(DATA_DICTIONARY_FOLDER);

		String taskModelFileName = baseFileName + "-task-model.xml";
		
    // Finally, wrap all taskdefinitions is right XML -> this is the FULL
    // model file, including generic start-task
    String taskModelId = UUID.randomUUID().toString();
    String taskModelXML = MessageFormat.format(getTaskModelTemplate(), taskModelId, taskModelsString.toString());
    LOGGER.info("Deploying task model XML:");
    prettyLogXml(taskModelXML);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(taskModelXML.getBytes());
    ContentStream contentStream = new ContentStreamImpl(taskModelFileName, null, "application/xml", inputStream);
		
    // Verify whether it is an update or a new workflow
		Document taskModelDocument = getDocumentFromFolder(DATA_DICTIONARY_FOLDER, taskModelFileName);
		if (taskModelDocument == null) {
		  HashMap<String, Object> properties = new HashMap<String, Object>();
	    properties.put("cmis:name", taskModelFileName);
	    properties.put("cmis:objectTypeId", "D:cm:dictionaryModel");
	    properties.put("cm:modelActive", true);

	    LOGGER.info("Task model file : " + taskModelFileName);
	    modelFolder.createDocument(properties,contentStream, VersioningState.MAJOR);
		} else {
		  LOGGER.info("Updating content of " + taskModelFileName);
		  taskModelDocument.setContentStream(contentStream, true);
		}
	}
	
	protected void uploadFormConfig(StringBuilder formConfigStringBuilder, KickstartWorkflow workflow, String baseFileName) {
	  int version = 0;
    String formConfig = generateFormConfig(formConfigStringBuilder, workflow, version, baseFileName);

    int result = executeFormConfigUpload(formConfig);
    
    // Okay, Okay, this is pretty hackish. But it was the fastest way to get it working.
    // In an ideal world, with plenty of time and pink unicorns, we would fetch the form config
    // xml, adapt it with the new forms and save that back.
    // Or even beter: we could query Share to know the actual latest version number
    while (result == 409) {
      LOGGER.info("Found deployed form config with version " + version + " Trying now with " + (version + 1));
      version = version + 1;
      formConfig = generateFormConfig(formConfigStringBuilder, workflow, version, baseFileName);
      result = executeFormConfigUpload(formConfig);
    }
    
    // We're also uploading it to the workflow definition folder, for future use
    LOGGER.info("Uploading formconfig to " + WORKFLOW_DEFINITION_FOLDER);
    uploadStringToDocument(formConfig, WORKFLOW_DEFINITION_FOLDER, baseFileName + "-form-config.xml", "application/xml");
	}

  protected int executeFormConfigUpload(String formConfig) {
    HttpState state = new HttpState();
    state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials(cmisUser, cmisPassword));

    LOGGER.info("Deploying form config XML: ");
    prettyLogXml(formConfig);
    
    PostMethod postMethod = new PostMethod(FORM_CONFIG_UPLOAD_URL);
    try {
      postMethod.setRequestEntity(new StringRequestEntity(formConfig, "application/xml", "UTF-8"));
      
      HttpClient httpClient = new HttpClient();
      int result = httpClient.executeMethod(null, postMethod, state);
      
      // Display status code
      LOGGER.info("Response status code: " + result);

      // Display response
      LOGGER.info("Response body: ");
      LOGGER.info(postMethod.getResponseBodyAsString());
      
      return result;
      
    } catch (Throwable t) {
      System.err.println("Error: " + t.getMessage());
      t.printStackTrace();
    } finally {
      postMethod.releaseConnection();
    }
    
    throw new RuntimeException("Programmatic error. You shouldn't be here.");
  }

  private String generateFormConfig(StringBuilder evaluatorConfigStringBuilder, KickstartWorkflow workflow, int version, String baseFileName) {
    String formId = "kickstart_form_" + baseFileName;
    
    if (version > 0) {
      formId += "_" + version;
    }
    
    String formConfig = MessageFormat.format(getFormConfigTemplate(),
    		formId, 
    		workflow.getId(),
    		evaluatorConfigStringBuilder.toString());
    return formConfig;
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
	
	public String getWorkflowMetaData(String processDefinitionId, String metadataKey) {
	  String metadataFile = processDefinitionId;
	  if (metadataKey.equals(MetaDataKeys.WORKFLOW_JSON_SOURCE)) {
	    metadataFile = metadataFile + ".json";
	  }
	  
	  Document document = getDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, metadataFile);
	  StringBuilder strb = new StringBuilder();
	  
	  BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(document.getContentStream().getStream()));
	  try {
	    String line = bufferedReader.readLine();
	    while (line != null) {
	      strb.append(line);
	      line = bufferedReader.readLine();
	    }
	  } catch (Exception e) {
	    LOGGER.log(Level.SEVERE, "Could not read metadata '" + metadataKey + "' : " + e.getMessage());
	    e.printStackTrace();
	  } finally {
	    if (bufferedReader != null) {
	      try {
	        bufferedReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
	    }
	  }
	  
	  return strb.toString();
	}

	public List<KickstartWorkflowInfo> findWorkflowInformation(boolean includeCounts) {
	  
	  if (includeCounts) {
	    // Not yet implemented, cause it would be an 1+n call ...
	    throw new UnsupportedOperationException();
	  }
	  
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
	
	public KickstartWorkflowInfo findWorkflowInformation(String processDefinitionId, boolean includeCounts) {
	  KickstartWorkflowInfo kickstartWorkflowInfo = new KickstartWorkflowInfo();
	  
	  // Get general info
	  Session cmisSession = getCmisSession();
    Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(WORKFLOW_DEFINITION_FOLDER);
    
    try {
      Document bpmn20Document = (Document) cmisSession.getObjectByPath(workflowDefinitionFolder.getPath() + "/" + generateBpmnResourceName(processDefinitionId));
      if (bpmn20Document != null) {
        kickstartWorkflowInfo.setId(processNameToBaseName((String) bpmn20Document.getPropertyValue(PropertyIds.NAME)));
        kickstartWorkflowInfo.setName((String) bpmn20Document.getPropertyValue("cm:description"));
        kickstartWorkflowInfo.setCreateTime(( (GregorianCalendar)bpmn20Document.getPropertyValue(PropertyIds.CREATION_DATE)).getTime());
      } else {
        return null;
      }
    } catch (CmisObjectNotFoundException e) {
      return null;
    }
	  
	  // Get counts
	  if (includeCounts) {
	    kickstartWorkflowInfo.setNrOfRuntimeInstances(retrieveWorkflowInstanceIds(kickstartWorkflowInfo.getName()).size());
	  }
	  
	  return kickstartWorkflowInfo;
  }

	public KickstartWorkflow findWorkflowById(String id) {
		throw new UnsupportedOperationException();
	}
	
	public void deleteWorkflow(String processDefinitionId) {
    // Delete all workflow instances, as this will block the undeployment of the process otherwise
	  deleteWorkflowInstances(processDefinitionId);
	  
	  // TODO: make constants for the files, both for use in creation/removal
	  
	  // Remove all files in the workflow definition folder
	  deleteDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, processDefinitionId + ".png");
	  deleteDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, processDefinitionId + "_image.png");
	  deleteDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, processDefinitionId + ".json");
	  deleteDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, processDefinitionId + ".bpmn20.xml");
	  
	  // Remove task model
	  deleteDocumentFromFolder(DATA_DICTIONARY_FOLDER, processDefinitionId + "-task-model.xml");
	  
	  // Remove form config
	  deleteFormConfig(processDefinitionId);
	  deleteDocumentFromFolder(WORKFLOW_DEFINITION_FOLDER, processDefinitionId + "-form-config.xml");
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
    
    String fileName = processDefinitionIdToProcessImage(processDefinitionId); 
    ContentStream contentStream = new ContentStreamImpl(fileName, null, "image/png", processImageStream);

    Document imageDocument = getDocumentFromFolder(workflowDefinitionFolder.getPath(), fileName);
    if (imageDocument == null) {
      
      HashMap<String, Object> properties = new HashMap<String, Object>();
      properties.put("cmis:name", fileName);
      properties.put("cmis:objectTypeId", "cmis:document");
      
      workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
      
    } else {
      
      imageDocument.setContentStream(contentStream, true);
      
    }
    
	}
	
	// Helpets //////////////////////

	/**
	 * Generates a valid bpmn 2.0 file name for the given process name.
	 */
	protected String generateBpmnResourceName(String string) {
		return string.replace(" ", "_") + ".bpmn20.xml";
	}
	
	 protected String processDefinitionIdToProcessImage(String processDefinitionId) {
	    return processDefinitionId + "_image.png";
	 }
	  
	 public InputStream getBpmnXml(String processDefinitionId) {
	   throw new UnsupportedOperationException();
	 }
	 
	 // CMIS helper methods  //////////////////////////////////////////////////////////////////////////////////////////////
	 
  protected void deleteDocumentFromFolder(String folderPath, String documentName) {
    Document document = getDocumentFromFolder(folderPath, documentName);
    if (document != null) {
      document.delete(true);
      LOGGER.info("Removed document " + folderPath + "/" + documentName);
    }
  }

  protected Document getDocumentFromFolder(String folderPath, String documentName) {
    try {
      Session cmisSession = getCmisSession();
      Folder workflowDefinitionFolder = (Folder) cmisSession.getObjectByPath(folderPath);
      String path = workflowDefinitionFolder.getPath() + "/" + documentName;
      return (Document) cmisSession.getObjectByPath(path);
    } catch (CmisObjectNotFoundException e) {
      return null;
    }
  }
  
  protected void uploadStringToDocument(String string, String folderPath, String documentName, String mimetype) {
    Session cmisSession = getCmisSession();
    Folder folder = (Folder) cmisSession.getObjectByPath(folderPath);
    
    ContentStream contentStream = new ContentStreamImpl(documentName, null, mimetype, new ByteArrayInputStream(string.getBytes()));
    Document document = getDocumentFromFolder(folder.getPath(), documentName); 
    
    if (document == null) {
      HashMap<String, Object> properties = new HashMap<String, Object>();
      properties.put("cmis:name", documentName);
      properties.put("cmis:objectTypeId", "cmis:document");
      folder.createDocument(properties, contentStream, VersioningState.MAJOR);
    } else {
      document.setContentStream(contentStream, true);
    }
  }

	 // Helper methods for REST calls /////////////////////////////////////////////////////////////////////////////////////
	 
	 protected List<String> retrieveWorkflowInstanceIds(String workflowName) {
	   String url = ALFRESCO_BASE_URL + "api/workflow-instances?state=active"; 
	   JsonNode json = executeGet(url);
	   ArrayNode data = (ArrayNode) json.get("data");
	   
	   ArrayList<String> workflowInstanceIds = new ArrayList<String>();
	   for (int i=0; i<data.size(); i++) {
	     String title = data.get(i).get("title").asText();
	     if (title.equalsIgnoreCase(workflowName)) {
	       workflowInstanceIds.add(data.get(i).get("id").asText());
	     }
	   }
	   
	   return workflowInstanceIds;
	 }
	 
	 protected JsonNode executeGet(String url) {
	   HttpState state = new HttpState();
     state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials(cmisUser, cmisPassword));
 
     GetMethod getMethod = new GetMethod(url);
     LOGGER.info("Executing GET '" + url + "'");
 
     try {
       HttpClient httpClient = new HttpClient();
       int result = httpClient.executeMethod(null, getMethod, state);
 
       // Display Response
       String responseJson = getMethod.getResponseBodyAsString();
       LOGGER.info("Response status code: " + result);
       LOGGER.info("Response body: " + responseJson);
       
       ObjectMapper mapper = new ObjectMapper();
       JsonNode json = mapper.readTree(responseJson);
       return json;
       
     } catch (Throwable t) {
       System.err.println("Error: " + t.getMessage());
       t.printStackTrace();
     } finally {
       getMethod.releaseConnection();
     }
     return null;
	 }
	 
	 protected void deleteWorkflowInstances(String workflowName) {
	   List<String> workflowInstanceIds = retrieveWorkflowInstanceIds(workflowName);
	   for (String workflowInstanceId : workflowInstanceIds) {
	     deleteWorkflowInstance(workflowInstanceId);
	     LOGGER.info("Deleted workflow instance '" + workflowInstanceId + "'");
	   }
	 }
	 
	 protected void deleteWorkflowInstance(String workflowInstanceId) {
	   HttpState state = new HttpState();
     state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials(cmisUser, cmisPassword));
 
     // Only fetching one, we're only interested in the paging information, after all
     String url = ALFRESCO_BASE_URL + "api/workflow-instances/" + workflowInstanceId + "?forced=true"; 
     DeleteMethod deleteMethod = new DeleteMethod(url);
     LOGGER.info("Executing DELETE '" + url + "'");
 
     try {
       HttpClient httpClient = new HttpClient();
       int result = httpClient.executeMethod(null, deleteMethod, state);
 
       // Display Response
       String responseJson = deleteMethod.getResponseBodyAsString();
       LOGGER.info("Response status code: " + result);
       LOGGER.info("Response body: " + responseJson);
       
     } catch (Throwable t) {
       System.err.println("Error: " + t.getMessage());
       t.printStackTrace();
     } finally {
       deleteMethod.releaseConnection();
     }
	 }
	 
  protected void deleteFormConfig(String workflowId) {
    String url = SHARE_BASE_URL + "page/modules/module/delete?moduleId=" + URLEncoder.encode("kickstart_form_" + workflowId);
    int statusCode = executeDelete(url);
    
    // Cont. hackyness from the upload. Read all about it there.
    int version = 1;
    while (statusCode == 200) {
      statusCode = executeDelete(url + "_" + version);
      version++;
    }
  }
  
  private int executeDelete(String url) {
    LOGGER.info("Executed module delete: " + url);
    GetMethod method = new GetMethod(url);
    try {
      HttpState httpState = new HttpState();
      httpState.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));
      
      HttpClient httpClient = new HttpClient();
      int statusCode = httpClient.executeMethod(null, method, httpState);
      LOGGER.info("Status code result for delete: " + statusCode);
      return statusCode;
    } catch (Throwable t) {
      LOGGER.log(Level.SEVERE, "Error: " + t.getMessage());
      t.printStackTrace();
    } finally {
      method.releaseConnection();
    }
    
    throw new RuntimeException("Programmatic error. You shouldn't be here.");
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
	
	// Helper methods for XML templates /////////////////////////////////////////////////////////////////
	
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
	
	protected String getFormConfigInfoTemplate() {
	  if (FORM_CONFIG_FIELD_INFO_TEMPLATE == null) {
	    FORM_CONFIG_FIELD_INFO_TEMPLATE = readTemplateFile(FORM_CONFIG_FIELD_INFO_TEMPLATE_FILE);
	  }
	  return FORM_CONFIG_FIELD_INFO_TEMPLATE;
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