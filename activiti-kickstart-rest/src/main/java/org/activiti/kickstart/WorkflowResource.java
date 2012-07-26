package org.activiti.kickstart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class WorkflowResource extends BaseResource {

	@Post
	public String deployWorkflow(Representation representation) {

		// Convert body to internal workflow object
		KickstartWorkflow workflow = new KickstartWorkflow();

		try {
			JsonNode json = new ObjectMapper().readTree(representation.getText());

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

					KickstartUserTask workflowTask = new KickstartUserTask();
					workflowTasks.add(workflowTask);

					JsonNode taskNode = taskIterator.next();
					workflowTask.setName(taskNode.path("name").getTextValue());
					workflowTask.setDescription(taskNode.path("description").getTextValue());
					workflowTask.setStartWithPrevious(taskNode.path("startWithPrevious").getBooleanValue());
				}
			}
			
			// Actually deploy this workflow
			getKickstartService().deployWorkflow(workflow);

			// Return result
			return "Found " + workflow.getTasks().size() + " tasks";

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not deploy workflow: " + e.getMessage());
		}

	}

}
