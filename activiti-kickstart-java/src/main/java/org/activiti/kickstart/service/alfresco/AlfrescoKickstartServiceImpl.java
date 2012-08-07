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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
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
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 * @author Joram Barrez
 */
public class AlfrescoKickstartServiceImpl implements KickstartService {
	      
	private static final Logger LOGGER = Logger.getLogger(AlfrescoKickstartServiceImpl.class.getName());
	
	private static final String WORKFLOW_DEFINITION_FOLDER = "/Data Dictionary/Workflow Definitions";
	
	private static final String DATA_DICTIONARY_FOLDER = "/Data Dictionary/Models";
	
	private static final String TASK_MODEL_NAME = "$task_model_name$";
	
	private static final String TASK_MODEL_XML =
			"<?xml version=''1.0'' encoding=''UTF-8''?>"
			+ "<model name=''ks:taskModel_{0}'' xmlns=''http://www.alfresco.org/model/dictionary/1.0''>"
			+ "<imports>"
			+ "  <import uri=''http://www.alfresco.org/model/dictionary/1.0'' prefix=''d'' />"
			+ "  <import uri=''http://www.alfresco.org/model/bpm/1.0'' prefix=''bpm'' />"
			+ "</imports>"
			+ "<namespaces>"
			+ "  <namespace uri=''http://www.alfresco.org/model/kickstart/1.0'' prefix=''ks'' />"
			+ "</namespaces>"
			+ "<types>"
			+ "  <type name=''ks:genericStartTask''>"
			+ "    <parent>bpm:startTask</parent>"
			+ "  </type>"
			+ "    {1}"
			+ "</types>"
			+ "</model>";
	
	private static final String TASK_MODEL_TASK_XML = "<type name=''ks:{0}''>" +
      "<parent>bpm:task</parent>" + 
	    "{1}" +    
      "</type>";
	
	 private static final String TASK_MODEL_PROPERTY_XML = "<property name=''ks:{0}''>"+
      "<type>{1}</type>" +
	    "<mandatory>{2}</mandatory>" +
      "</property>";
	 
	 private static final String TASK_FORM_CONFIG_XML = "<config evaluator=''task-type'' condition=''ks:{0}'' replace=''true''>" +
      "<forms>" +
        "<form>" +
          "<field-visibility>"+
           "{1}" +
          "</field-visibility>" +
          "<appearance>"+
            "<set id='''' appearance=''title'' label-id=''General Info'' />"+
            "<set id=''info'' appearance='''' label-id=''Info'' />"+
            // Add bpm_description as read-only 'info' field
            "<field id=''bpm_description'' label-id=''Description'' set=''info'' ><control template=''/org/alfresco/components/form/controls/info.ftl'' /></field>" +
            "{2}" +
          "</appearance>" +
        "</form>" +
      "</forms>"+
    "</config>";
	 
	 private static final String TASK_FORM_CONFIG_VISIBILITY = "<show id=''{0}'' />";
	 
	 private static final String TASK_FORM_CONFIG_APPEARANCE = "<field id=''{0}'' label-id=''{1}'' set=''info''>" +
            "</field>";
	 
	 private static final String FORM_CONFIG_XML = "<?xml version=''1.0'' encoding=''UTF-8'' standalone=''yes''?>" +
	         "<module>"+
	         " <id>test_form</id>"+
	         " <auto-deploy>true</auto-deploy> "+
	         " <configurations>"+
	         "   <!-- Start task form -->"+
	         "    <config evaluator=''string-compare'' condition=''activiti$adhoc_{0}''>"+
	         "       <forms>"+
	         "          <form>"+
	         "             <field-visibility>"+
	         "                <show id=''bpm:workflowDescription'' />"+
	         "                <show id=''bpm:workflowDueDate'' />"+
	         "                <show id=''bpm:workflowPriority'' />"+
	         "                <show id=''packageItems'' />"+
	         "             </field-visibility>"+
	         "             <appearance>"+
	         "                <set id='''' appearance=''title'' label-id=''General info'' />"+
	         "                <set id=''info'' appearance='''' template=''/org/alfresco/components/form/2-column-set.ftl'' />"+
	         "                <set id=''assignee'' appearance=''title'' label-id=''workflow.set.assignee'' />"+
	         "                <set id=''items'' appearance=''title'' label-id=''workflow.set.items'' />"+
	         "                <set id=''other'' appearance=''title'' label-id=''workflow.set.other'' />  "+               
	         "                <field id=''bpm:workflowDescription'' label-id=''workflow.field.message''>"+
	         "                   <control template=''/org/alfresco/components/form/controls/textarea.ftl''>"+
	         "                      <control-param name=''style''>width: 95%</control-param>"+
	         "                   </control>"+
	         "                </field>"+
	         "                <field id=''bpm:workflowDueDate'' label-id=''workflow.field.due'' set=''info''>"+
	         "                   <control template=''/org/alfresco/components/form/controls/date.ftl''>"+
	         "                    <control-param name=''showTime''>false</control-param>"+
	         "                    <control-param name=''submitTime''>false</control-param>"+
	         "                   </control>"+
	         "                </field>"+
	         "                <field id=''bpm:workflowPriority'' label-id=''workflow.field.priority'' set=''info''>"+
	         "                   <control template=''/org/alfresco/components/form/controls/workflow/priority.ftl'' />"+
	         "                </field>"+
	         "                <field id=''packageItems'' set=''items'' />"+
	         "             </appearance>"+
	         "          </form>"+
	         "       </forms>"+
	         "    </config>"+
	         "      <!-- Other task forms -->"+
	         "      {1}"+
	         " </configurations>"+
	         "</module>";


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
		LOGGER.info("deploying task model");
		deployTaskModel(kickstartWorkflow);
		
		LOGGER.info("deploying process");
		String processDocumentId = deployProcess(kickstartWorkflow);
		
		return processDocumentId; // Can't get the deployment id, so returning the document id
	}

	protected String deployProcess(KickstartWorkflow kickstartWorkflow) {
		
		// TODO: hack
		for (KickstartTask kickstartTask : kickstartWorkflow.getTasks()) {
			((KickstartUserTask) kickstartTask).setAssignee("admin");
		}
		// TODO: hack
		
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
		String workflowXML = marshallingService.marshallWorkflow(kickstartWorkflow);
		InputStream inputStream = new ByteArrayInputStream(workflowXML.getBytes()); 
		
		LOGGER.info("Deploying process definition xml: " + workflowXML);
		
		ContentStream contentStream = new ContentStreamImpl(processName, null, "application/xml", inputStream);
		Document document = workflowDefinitionFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
		
		LOGGER.info("Process definition deployed: " + document.getPaths());
		
		return document.getId();
	}

	private void deployTaskModel(KickstartWorkflow workflow) {
		
		// TODO: add to other service!

	  // All task-models
	  StringBuilder taskModelsString = new StringBuilder();
	  
	  // All form models
	  StringBuilder formConfigString = new StringBuilder();
	  
	  for(KickstartTask task : workflow.getTasks()) {
	    if(task instanceof KickstartUserTask) {
	      KickstartUserTask userTask = (KickstartUserTask) task;
	      
	      if (userTask.getForm() != null) {
	    	  
	    	  String formId = UUID.randomUUID().toString();
	    	  userTask.getForm().setFormKey(formId);
	      
		      StringBuilder typeString = new StringBuilder();
		      StringBuilder formAppearanceString = new StringBuilder();
		      StringBuilder formVisibilityString = new StringBuilder();
		      
		      
		      if (userTask.getForm().getFormProperties() != null && userTask.getForm().getFormProperties().size() > 0) {
		    	  
		    	  typeString.append("<properties>");
		    	  
			      // Get form-propertes
			      for(KickstartFormProperty prop : userTask.getForm().getFormProperties()) {
			        // Property in type-definition
			        typeString.append(
			                MessageFormat.format(TASK_MODEL_PROPERTY_XML, 
			                        createFriendlyName(prop.getProperty()), 
			                        getAlfrescoModelType(prop.getType()),
			                        prop.isRequired()));
			        
			        // Visibility in form-config
			        formVisibilityString.append(MessageFormat.format(TASK_FORM_CONFIG_VISIBILITY, createFriendlyName(prop.getProperty())));
			        
			        // Appearance on screen in form-config
			        formAppearanceString.append(MessageFormat.format(TASK_FORM_CONFIG_APPEARANCE, 
			                createFriendlyName(prop.getProperty()),
			                prop.getProperty()));
			        
			      }
			      
			      typeString.append("</properties>");
			      
		      }
		      
		      // Add name and all form-properties to model XML
		      taskModelsString.append(MessageFormat.format(TASK_MODEL_TASK_XML, 
		              userTask.getForm().getFormKey(),
		              typeString.toString()));
		      
		      // Add task-form-config
		      formConfigString.append(MessageFormat.format(TASK_FORM_CONFIG_XML, 
		    		  userTask.getForm().getFormKey(),
		              formVisibilityString.toString(),
		              formAppearanceString.toString()));
	      }
	    }
	    
	  }
	  
	  
	  // Upload task model
		
	  Session session = getCmisSession();
	  Folder modelFolder = (Folder) session.getObjectByPath(DATA_DICTIONARY_FOLDER);
		
	  String taskModelId = UUID.randomUUID().toString();
	  String taskModelFileName = "task-model-" + taskModelId + ".xml";
	  HashMap<String, Object> properties = new HashMap<String, Object>();
	  properties.put("cmis:name", taskModelFileName);
	  properties.put("cmis:objectTypeId", "D:cm:dictionaryModel");
	  properties.put("cm:modelActive", true);
		
	  // Finally, wrap all taskdefinitions is right XML -> this is the FULL model file, including generic start-task
	  String taskModelXML = MessageFormat.format(TASK_MODEL_XML, taskModelId, taskModelsString.toString()).replace("'", "\"");
	  LOGGER.info("Deploying task model XML: " + taskModelXML);
	  ByteArrayInputStream inputStream = new ByteArrayInputStream(taskModelXML.getBytes());
	  
	  LOGGER.info("Task model file : " + taskModelFileName);
	  ContentStream contentStream = new ContentStreamImpl(taskModelFileName, null, "application/xml", inputStream);
		
	  Document document = modelFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
	 
	  
//	  // Upload form config
//	  
//	  HttpState state = new HttpState();
//	  state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));
//
//	  PostMethod postMethod = new PostMethod("http://localhost:8081/share/page/modules/module");
//
//	  try {
//
//	      // Wrap all form-configs in right XL -> this is the FULL form-config file, including generic start-task definition
//          String processName = workflow.getName().replace(" ", "_"); // process-name as defined in BPMN20.xml
//		    
//		  String formConfig = MessageFormat.format(FORM_CONFIG_XML, processName, formConfigString.toString());
//		  LOGGER.info("Deploying form config XML: " + formConfig);
//		  postMethod.setRequestEntity(new StringRequestEntity(formConfig, "application/xml", "UTF-8"));
//
//		  // postMethod.setRequestHeader("Content-type", "text/xml");
//		  HttpClient httpClient = new HttpClient();
//		  int result = httpClient.executeMethod(null, postMethod, state);
//
//		  // Display status code
//		  System.out.println("Response status code: " + result);
//
//		  // Display response
//		  System.out.println("Response body: ");
//		  System.out.println(postMethod.getResponseBodyAsString());
//	  } catch (Throwable t) {
//		  System.err.println("Error: " + t.getMessage());
//		  t.printStackTrace();
//	  } finally {
//		  postMethod.releaseConnection();
//	  }
	}


	private Object getAlfrescoModelType(String type) {
	  
	  if(type.equals("text")) {
	    return "d:text";
	  } else if(type.equals("date")) {
	    return "d:date";
  	} else if(type.equals("number")) {
  	  return "d:long";
  	}
    return null;
  }

  private Object createFriendlyName(String property) {
    return property.toLowerCase().replace(" ", "_");
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
	
	
//	public static void main(String[] args) {
//    
//	  KickstartWorkflow workflow = new KickstartWorkflow();
//	  workflow.setName("name");
//	  KickstartUserTask task = new KickstartUserTask();
//	  task.setId("1234");
//	  KickstartForm form = new KickstartForm();
//	  KickstartFormProperty prop1 = new KickstartFormProperty();
//	  prop1.setProperty("Some variable");
//	  prop1.setType("text");
//	  
//	  form.addFormProperty(prop1);
//	  
//	  task.setForm(form);
//	  
//	  workflow.addTask(task);
//	  
//	  new AlfrescoKickstartServiceImpl(null, null, null).deployTaskModel(workflow);
//	  
//  }

}