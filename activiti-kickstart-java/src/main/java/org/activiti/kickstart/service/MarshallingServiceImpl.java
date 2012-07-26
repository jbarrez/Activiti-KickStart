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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.activiti.kickstart.bpmn20.model.Definitions;
import org.activiti.kickstart.bpmn20.model.Documentation;
import org.activiti.kickstart.bpmn20.model.FlowElement;
import org.activiti.kickstart.bpmn20.model.FormalExpression;
import org.activiti.kickstart.bpmn20.model.Process;
import org.activiti.kickstart.bpmn20.model.activity.resource.HumanPerformer;
import org.activiti.kickstart.bpmn20.model.activity.resource.PotentialOwner;
import org.activiti.kickstart.bpmn20.model.activity.resource.ResourceAssignmentExpression;
import org.activiti.kickstart.bpmn20.model.activity.type.ScriptTask;
import org.activiti.kickstart.bpmn20.model.activity.type.ServiceTask;
import org.activiti.kickstart.bpmn20.model.activity.type.UserTask;
import org.activiti.kickstart.bpmn20.model.bpmndi.BPMNDiagram;
import org.activiti.kickstart.bpmn20.model.bpmndi.BPMNPlane;
import org.activiti.kickstart.bpmn20.model.connector.SequenceFlow;
import org.activiti.kickstart.bpmn20.model.event.EndEvent;
import org.activiti.kickstart.bpmn20.model.event.StartEvent;
import org.activiti.kickstart.bpmn20.model.extension.ExtensionElements;
import org.activiti.kickstart.bpmn20.model.extension.activiti.ActivitFieldExtensionElement;
import org.activiti.kickstart.bpmn20.model.extension.activiti.ActivitiFormProperty;
import org.activiti.kickstart.bpmn20.model.gateway.ParallelGateway;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartMailTask;
import org.activiti.kickstart.dto.KickstartScriptTask;
import org.activiti.kickstart.dto.KickstartServiceTask;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartTaskBlock;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartMailTask.Field;
import org.activiti.kickstart.util.ExpressionUtil;

/**
 * 
 * @author jbarrez
 */
public class MarshallingServiceImpl implements MarshallingService {
	
	public String marshallWorkflow(KickstartWorkflow kickstartWorkflow) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Definitions.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			StringWriter writer = new StringWriter();
			marshaller.marshal(convertToBpmn(kickstartWorkflow), writer);
			return writer.toString();
		} catch (JAXBException e) {
			throw new RuntimeException("Could not marshal workflow", e);
		}
	}
	
	/**
	 * Generate the JAXB version of this adhoc workflow.
	 * 
	 * Extremely important: the flowelements are added in topological order,
	 * from left to right and top to bottom.
	 */
	public Definitions convertToBpmn(KickstartWorkflow kickstartWorkflow) {

		Definitions definitions = new Definitions();
		definitions.setTargetNamespace("adhoc");
		String processName = kickstartWorkflow.getName().replace(" ", "_");

		// Process
		org.activiti.kickstart.bpmn20.model.Process process = new org.activiti.kickstart.bpmn20.model.Process();
		process.setId("adhoc_" + processName);
		process.setName(kickstartWorkflow.getName());
		process.setExecutable(true);
		Documentation processDocumentation = new Documentation();
		processDocumentation.setId(process.getId() + "_documentation");
		processDocumentation.setText(kickstartWorkflow.getDescription());
		process.getDocumentation().add(processDocumentation);
		definitions.getRootElement().add(process);

		// BPMNDiagram
		BPMNDiagram diagram = new BPMNDiagram();
		diagram.setId(processName + "_diagram");
		definitions.getDiagram().add(diagram);
		BPMNPlane plane = new BPMNPlane();
		plane.setId(processName + "_plane");
		plane.setBpmnElement(process);
		diagram.setBPMNPlane(plane);

		// Start
		StartEvent startEvent = new StartEvent();
		startEvent.setId(KickstartWorkflow.START_NAME);
		startEvent.setInitiator("initiator");
		process.getFlowElement().add(startEvent);

		// We'll group tasks by each 'task block' that is to be executed in parallel
		List<List<FlowElement>> TaskBlockList = new ArrayList<List<FlowElement>>();
		int index = 1;
		List<KickstartTaskBlock> taskBlocks = kickstartWorkflow.getTaskBlocks();
		for (KickstartTaskBlock taskBlock : taskBlocks) {

			List<FlowElement> TaskBlock = new ArrayList<FlowElement>();
			TaskBlockList.add(TaskBlock);

			for (KickstartTask kickstartTask : taskBlock.getTasks()) {

				FlowElement generatedTask = convertToBPMN(kickstartTask);

				generatedTask.setId("task_" + index++);
				generatedTask.setName(kickstartTask.getName());

				// Description
				if (kickstartTask.getDescription() != null) {
					Documentation taskDocumentation = new Documentation(ExpressionUtil.replaceWhiteSpaces(kickstartTask.getDescription()));
					taskDocumentation.setId(generatedTask.getId() + "_documentation");
					generatedTask.getDocumentation().add(taskDocumentation);
				}
				// process.getFlowElement().add(userTask);
				TaskBlock.add(generatedTask);
			}
		}

		// Sequence flow generation
		AtomicInteger flowIndex = new AtomicInteger(1); // Hacky hacky, Integer doesnt have an increment() function ... lazy me
		AtomicInteger gatewayIndex = new AtomicInteger(1);
		List<FlowElement> lastFlowElementOfBlockStack = new ArrayList<FlowElement>();
		lastFlowElementOfBlockStack.add(startEvent);

		// All tasks blocks
		for (int i = 0; i < taskBlocks.size(); i++) {
			convertTaskBlockToBpmn20(process, flowIndex, gatewayIndex, TaskBlockList.get(i), lastFlowElementOfBlockStack);
		}

		// End
		EndEvent endEvent = new EndEvent();
		endEvent.setId(KickstartWorkflow.END_NAME);
		process.getFlowElement().add(endEvent);

		// Seq flow lastTask -> end
		createSequenceFlow(process, flowIndex, getLast(lastFlowElementOfBlockStack), endEvent);

		return definitions;
	}
	
	public FlowElement convertToBPMN(KickstartTask kickstartTask) {
		if (kickstartTask instanceof KickstartUserTask) {
			return convertToBPMN((KickstartUserTask) kickstartTask);
		} else if (kickstartTask instanceof KickstartServiceTask) {
			return convertToBPMN((KickstartServiceTask) kickstartTask);
		} else if (kickstartTask instanceof KickstartScriptTask) {
			return convertToBPMN((KickstartScriptTask) kickstartTask);
		} else if (kickstartTask instanceof KickstartMailTask) {
			return convertToBPMN((KickstartMailTask) kickstartTask);
		} else {
			throw new RuntimeException("Unknown task type: " + kickstartTask.getClass());
		}
	}
	
	public UserTask convertToBPMN(KickstartUserTask kickstartUserTask) {
		UserTask userTask = new UserTask();

		// assignee
		if (kickstartUserTask.getAssignee() != null && !"".equals(kickstartUserTask.getAssignee())) {
			HumanPerformer humanPerformer = new HumanPerformer();
			humanPerformer.setId(userTask.getId() + "_humanPerformer");
			ResourceAssignmentExpression assignmentExpression = new ResourceAssignmentExpression();
			assignmentExpression.setId(userTask.getId() + "_humanPerformer_assignmentExpression");
			FormalExpression formalExpression = new FormalExpression(kickstartUserTask.getAssignee());
			formalExpression.setId(userTask.getId() + "_humanPerformer_formalExpressions");
			assignmentExpression.setExpression(formalExpression);
			humanPerformer.setResourceAssignmentExpression(assignmentExpression);
			userTask.getActivityResource().add(humanPerformer);
		}

		// groups
		if (kickstartUserTask.getGroups() != null && !"".equals(kickstartUserTask.getGroups())) {
			PotentialOwner potentialOwner = new PotentialOwner();
			potentialOwner.setId(userTask.getId() + "_potentialOwner");
			ResourceAssignmentExpression assignmentExpression = new ResourceAssignmentExpression();
			assignmentExpression.setId(userTask.getId() + "_potentialOwner_assignmentExpression");

			StringBuilder groups = new StringBuilder();
			for (String group : kickstartUserTask.getGroups().split(",")) {
				groups.append(group + ",");
			}
			groups.deleteCharAt(groups.length() - 1);
			FormalExpression formalExpression = new FormalExpression(groups.toString());

			formalExpression.setId(userTask.getId() + "_potentialOwner_formalExpressions");
			assignmentExpression.setExpression(formalExpression);
			potentialOwner.setResourceAssignmentExpression(assignmentExpression);
			userTask.getActivityResource().add(potentialOwner);
		}

		// form
		if (kickstartUserTask.getForm() != null) {
			List<ActivitiFormProperty> formProperties = new ArrayList<ActivitiFormProperty>();
			for (KickstartFormProperty formPropertyDto : kickstartUserTask.getForm().getFormProperties()) {
				ActivitiFormProperty formProperty = new ActivitiFormProperty();
				formProperty.setId(formPropertyDto.getProperty());
				formProperty.setName(formPropertyDto.getProperty());
				formProperty.setRequired(formPropertyDto.isRequired() ? "true" : "false");

				String dtoType = formPropertyDto.getType();
				String type = "string";
				if ("number".equals(dtoType)) {
					type = "long";
				} else if ("date".equals(dtoType)) {
					type = "date";
				}
				formProperty.setType(type);

				formProperties.add(formProperty);
			}

			if (formProperties.size() > 0) {
				userTask.setExtensionElements(new ExtensionElements());
				for (ActivitiFormProperty formProperty : formProperties) {
					userTask.getExtensionElements().add(formProperty);
				}
			}
		}

		return userTask;
	}

	public ServiceTask convertToBPMN(KickstartServiceTask kickstartServiceTask) {
		ServiceTask serviceTask = new ServiceTask();
	    serviceTask.setDelegateExpression(kickstartServiceTask.getDelegateExpression());
	    serviceTask.setClassName(kickstartServiceTask.getClassName());
	    serviceTask.setExpression(kickstartServiceTask.getExpression());
	    return serviceTask;
	}

	public ScriptTask convertToBPMN(KickstartScriptTask kickstartScriptTask) {
		ScriptTask task = new ScriptTask();
		task.setScriptFormat(kickstartScriptTask.getScriptFormat());
		task.setScript(kickstartScriptTask.getScript());
		task.setResultVariableName(kickstartScriptTask.getResultVariableName());
		return task;
	}

	public ServiceTask convertToBPMN(KickstartMailTask kickstartMailTask) {
		ServiceTask serviceTask = new ServiceTask();
	    serviceTask.setType("mail");
	    ExtensionElements extensionElements = new ExtensionElements();

	    addIfFilled(extensionElements, kickstartMailTask.getTo());
	    addIfFilled(extensionElements, kickstartMailTask.getFrom());
	    addIfFilled(extensionElements, kickstartMailTask.getSubject());
	    addIfFilled(extensionElements, kickstartMailTask.getCc());
	    addIfFilled(extensionElements, kickstartMailTask.getBcc());
	    addIfFilled(extensionElements, kickstartMailTask.getHtml());
	    addIfFilled(extensionElements, kickstartMailTask.getText());

	    serviceTask.setExtensionElements(extensionElements);
	    return serviceTask;
	}

	private void addIfFilled(ExtensionElements extenstionElements, Field fieldToAdd) {
		ActivitFieldExtensionElement element = prepareExtensionElement(fieldToAdd);
		if (element != null) {
			extenstionElements.add(element);
		}
	}

	private ActivitFieldExtensionElement prepareExtensionElement(Field field) {
		if (field.getStringValue() == null && field.getExpression() == null) {
			return null;
		}

		ActivitFieldExtensionElement extensionElement = new ActivitFieldExtensionElement();
		extensionElement.setName(field.getName());
		extensionElement.setStringValue(field.getStringValue());
		extensionElement.setExpression(field.getExpression());
		return extensionElement;
	}
	
	protected void convertTaskBlockToBpmn20(Process process, AtomicInteger flowIndex, AtomicInteger gatewayIndex,
			List<FlowElement> taskBlock, List<FlowElement> lastFlowElementOfBlockStack) {

		SequenceFlow sequenceFlow = createSequenceFlow(process, flowIndex, getLast(lastFlowElementOfBlockStack), null);
		if (taskBlock.size() == 1) {
			FlowElement userTask = taskBlock.get(0);
			sequenceFlow.setTargetRef(userTask);
			lastFlowElementOfBlockStack.add(userTask);
			process.getFlowElement().add(userTask);
		} else {
			ParallelGateway fork = new ParallelGateway();
			fork.setId("parallel_gateway_fork_" + gatewayIndex.getAndIncrement());
			process.getFlowElement().add(fork);
			sequenceFlow.setTargetRef(fork);

			ParallelGateway join = new ParallelGateway();
			join.setId("parallel_gateway_join" + gatewayIndex.getAndIncrement());

			// sequence flow to each task of the task block from the parallel gateway and back to the join
			for (FlowElement taskInBlock : taskBlock) {
				createSequenceFlow(process, flowIndex, fork, taskInBlock);
				createSequenceFlow(process, flowIndex, taskInBlock, join);
				process.getFlowElement().add(taskInBlock);
			}

			process.getFlowElement().add(join);
			lastFlowElementOfBlockStack.add(join);
		}
	}
	
	// Helper methods ////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the last {@link FlowElement} from the list of elements
	 */
	protected FlowElement getLast(List<FlowElement> elements) {
		if (elements.size() > 0) {
			return elements.get(elements.size() - 1);
		}
		return null;
	}
	

	protected SequenceFlow createSequenceFlow(Process process, AtomicInteger flowIndex, FlowElement sourceRef, FlowElement targetRef) {
		SequenceFlow sequenceFlow = new SequenceFlow();
		sequenceFlow.setId("flow_" + flowIndex.getAndIncrement());
		sequenceFlow.setSourceRef(sourceRef);
		sequenceFlow.setTargetRef(targetRef);
		process.getFlowElement().add(sequenceFlow);
		return sequenceFlow;
	}

}
