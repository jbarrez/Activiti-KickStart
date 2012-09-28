package org.activiti.kickstart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.activiti.kickstart.dto.KickstartForm;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;
import org.activiti.kickstart.service.MetaDataKeys;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class WorkflowResource extends BaseResource {

  private static final Logger LOGGER = Logger.getLogger(WorkflowResource.class.getName());
  
  @Get
  public KickstartWorkflowInfo findWorkflowInfo() {
    String workflowId = (String) getRequest().getAttributes().get("workflowId");
    if (workflowId == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      return null;
    }
    
    return getKickstartService().findWorkflowInformation(workflowId, true);
  }
  
  @Delete
  public void deleteWorkflow() {
    String workflowId = (String) getRequest().getAttributes().get("workflowId");
    if (workflowId == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
    }
    
    getKickstartService().deleteWorkflow(workflowId);
  }

  @Post
  public ObjectNode deployWorkflow(Representation representation) {

    // Convert body to internal workflow object
    KickstartWorkflow workflow = new KickstartWorkflow();

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
      String jsonText = representation.getText();
      JsonNode json = mapper.readTree(jsonText);

      LOGGER.info("Received json:");
      LOGGER.info(mapper.writeValueAsString(json));

      // Workflow name
      String name = json.path("name").getTextValue();
      if (name == null) {
        throw new RuntimeException("Missing parameter [name] in json body");
      }
      workflow.setName(name);

      // Workflow description
      String description = json.path("description").getTextValue();
      workflow.setDescription(description);

      // Workflow tasks
      JsonNode taskArray = json.path("tasks");
      if (taskArray != null && taskArray.isArray()) {

        List<KickstartTask> workflowTasks = new ArrayList<KickstartTask>();
        workflow.setTasks(workflowTasks);

        Iterator<JsonNode> taskIterator = taskArray.iterator();
        while (taskIterator.hasNext()) {

          JsonNode taskNode = taskIterator.next();

          KickstartUserTask workflowTask = new KickstartUserTask();
          workflowTasks.add(workflowTask);

          // Task details
          workflowTask.setName(taskNode.path("name").getTextValue());
          workflowTask.setDescription(taskNode.path("description").getTextValue());
          workflowTask.setStartWithPrevious(taskNode.path("startWithPrevious").getBooleanValue());

          // Task form
          JsonNode formArray = taskNode.path("form");
          if (formArray != null && formArray.isArray()) {

            KickstartForm kickstartForm = new KickstartForm();
            workflowTask.setForm(kickstartForm);

            Iterator<JsonNode> formIterator = formArray.iterator();
            while (formIterator.hasNext()) {
              KickstartFormProperty formProperty = new KickstartFormProperty();
              kickstartForm.addFormProperty(formProperty);

              JsonNode formEntry = formIterator.next();
              formProperty.setProperty(formEntry.path("name").getTextValue());
              formProperty.setType(formEntry.path("type").getTextValue());
              formProperty.setRequired(formEntry.path("isRequired").getTextValue().equals("true"));
            }

          }
        }
      }

      // Actually deploy this workflow
      String workflowId = getKickstartService().deployWorkflow(workflow, 
              Collections.singletonMap(MetaDataKeys.WORKFLOW_JSON_SOURCE, jsonText));
      ObjectNode idNode = new ObjectMapper().createObjectNode();
      idNode.put("id", workflowId);
      return idNode;

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Could not deploy workflow: " + e.getMessage());
    }

  }

}
