package org.activiti.kickstart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.activiti.kickstart.dto.KickstartForm;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class WorkflowResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(WorkflowResource.class.getName());
	
	@Post
	public String deployWorkflow(Representation representation) {

		// Convert body to internal workflow object
		KickstartWorkflow workflow = new KickstartWorkflow();

		try {
			JsonNode json = new ObjectMapper().readTree(representation.getText());
			LOGGER.info("Received json: " + json.toString());

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
			return getKickstartService().deployWorkflow(workflow);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not deploy workflow: " + e.getMessage());
		}

	}

}
