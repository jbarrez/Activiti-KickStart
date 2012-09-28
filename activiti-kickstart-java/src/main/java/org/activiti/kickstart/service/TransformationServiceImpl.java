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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.kickstart.bpmn20.model.BaseElement;
import org.activiti.kickstart.bpmn20.model.Definitions;
import org.activiti.kickstart.bpmn20.model.FlowElement;
import org.activiti.kickstart.bpmn20.model.activity.Task;
import org.activiti.kickstart.bpmn20.model.activity.resource.ActivityResource;
import org.activiti.kickstart.bpmn20.model.activity.resource.HumanPerformer;
import org.activiti.kickstart.bpmn20.model.activity.resource.PotentialOwner;
import org.activiti.kickstart.bpmn20.model.activity.type.ScriptTask;
import org.activiti.kickstart.bpmn20.model.activity.type.ServiceTask;
import org.activiti.kickstart.bpmn20.model.activity.type.UserTask;
import org.activiti.kickstart.bpmn20.model.connector.SequenceFlow;
import org.activiti.kickstart.bpmn20.model.extension.AbstractExtensionElement;
import org.activiti.kickstart.bpmn20.model.extension.activiti.ActivitFieldExtensionElement;
import org.activiti.kickstart.bpmn20.model.extension.activiti.ActivitiFormProperty;
import org.activiti.kickstart.bpmn20.model.gateway.ParallelGateway;
import org.activiti.kickstart.dto.KickstartForm;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartMailTask;
import org.activiti.kickstart.dto.KickstartScriptTask;
import org.activiti.kickstart.dto.KickstartServiceTask;
import org.activiti.kickstart.dto.KickstartTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.dto.KickstartWorkflowInfo;

/**
 * 
 * @author jbarrez
 */
public class TransformationServiceImpl implements TransformationService {
	
	protected RepositoryService repositoryService;
	protected HistoryService historyService;
	protected FormTransformationService formTransformationService;

	public FormTransformationService getFormTransformationService() {
		return formTransformationService;
	}

	public void setFormTransformationService(FormTransformationService formTransformationService) {
		this.formTransformationService = formTransformationService;
	}
	
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public HistoryService getHistoryService() {
		return historyService;
	}

	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}
	
	// -------------------------------------------------------------------------------
	// BPMN 2.0 --> KICKSTART INTERNAL CLASSES ---------------------------------------
	// -------------------------------------------------------------------------------

	public List<KickstartWorkflowInfo> convertToWorkflowInfoList(List<ProcessDefinition> processDefinitions, boolean includeCounts) {
		List<KickstartWorkflowInfo> infoList = new ArrayList<KickstartWorkflowInfo>();
		for (ProcessDefinition processDefinition : processDefinitions) {
			KickstartWorkflowInfo workflowInfo = new KickstartWorkflowInfo();
			workflowInfo.setId(processDefinition.getId());
			workflowInfo.setKey(processDefinition.getKey());
			workflowInfo.setName(processDefinition.getName());
			workflowInfo.setVersion(processDefinition.getVersion());
			workflowInfo.setDeploymentId(processDefinition.getDeploymentId());

			Date deploymentTime = repositoryService.createDeploymentQuery()
					.deploymentId(processDefinition.getDeploymentId())
					.singleResult().getDeploymentTime();
			workflowInfo.setCreateTime(deploymentTime);

			if (includeCounts) {
  			workflowInfo.setNrOfRuntimeInstances(historyService
  					.createHistoricProcessInstanceQuery()
  					.processDefinitionId(processDefinition.getId())
  					.unfinished().count());
  			workflowInfo.setNrOfHistoricInstances(historyService
  					.createHistoricProcessInstanceQuery()
  					.processDefinitionId(processDefinition.getId()).finished()
  					.count());
			}

			infoList.add(workflowInfo);
		}
		return infoList;
	}

	public KickstartWorkflow convertToKickstartWorkflow(Definitions definitions) {
		KickstartWorkflow adhocWorkflow = new KickstartWorkflow();

		for (BaseElement baseElement : definitions.getRootElement()) {
			if (baseElement instanceof org.activiti.kickstart.bpmn20.model.Process) {

				org.activiti.kickstart.bpmn20.model.Process process = (org.activiti.kickstart.bpmn20.model.Process) baseElement;

				// Process name and description
				adhocWorkflow.setName(process.getName());
				if (!process.getDocumentation().isEmpty()) {
					adhocWorkflow.setDescription(process.getDocumentation().get(0).getText());
				}

				// user tasks
				Map<String, Task> tasks = new HashMap<String, Task>();
				Map<String, ParallelGateway> gateways = new HashMap<String, ParallelGateway>();
				Map<String, List<SequenceFlow>> sequenceFlows = new HashMap<String, List<SequenceFlow>>();
				for (FlowElement flowElement : process.getFlowElement()) {
					if (flowElement instanceof Task) {
						tasks.put(flowElement.getId(), (Task) flowElement);
					}
					if (flowElement instanceof ParallelGateway) {
						gateways.put(flowElement.getId(),(ParallelGateway) flowElement);
					}
					if (flowElement instanceof SequenceFlow) {
						SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
						String sourceRef = sequenceFlow.getSourceRef().getId();
						if (!sequenceFlows.containsKey(sourceRef)) {
							sequenceFlows.put(sourceRef, new ArrayList<SequenceFlow>());
						}
						sequenceFlows.get(sourceRef).add(sequenceFlow);
					}
				}

				// Follow sequence flow to discover sequence of tasks
				SequenceFlow currentSequenceFlow = sequenceFlows.get(KickstartWorkflow.START_NAME).get(0); // Can be only one
				while (!currentSequenceFlow.getTargetRef().getId().equals(KickstartWorkflow.END_NAME)) {

					String targetRef = currentSequenceFlow.getTargetRef().getId();
					KickstartTask taskDto = null;
					if (tasks.containsKey(targetRef)) {

						taskDto = convertToKickstartTask((Task) currentSequenceFlow.getTargetRef());
						currentSequenceFlow = sequenceFlows.get(currentSequenceFlow.getTargetRef().getId()).get(0); // Can be only one
						adhocWorkflow.addTask(taskDto);

					} else if (gateways.containsKey(targetRef)) {

						Task task = null;
						for (int i = 0; i < sequenceFlows.get(targetRef).size(); i++) {
							SequenceFlow seqFlowOutOfGateway = sequenceFlows.get(targetRef).get(i);
							task = (Task) seqFlowOutOfGateway.getTargetRef();
							taskDto = convertToKickstartTask(task);
							if (i > 0) {
								taskDto.setStartWithPrevious(true);
							}
							adhocWorkflow.addTask(taskDto);
						}

						String parallelJoinId = sequenceFlows.get(task.getId()).get(0).getTargetRef().getId(); // any seqflow is ok
						currentSequenceFlow = sequenceFlows.get(parallelJoinId).get(0); // can be only one

					}

				} // end while

			}
		}

		return adhocWorkflow;
	}

	public KickstartTask convertToKickstartTask(final Task task) {
		KickstartTask taskDto = null;

		if (task instanceof UserTask) {
			taskDto = convertToKickstartUserTask((UserTask) task);
		} else if (task instanceof ServiceTask) {
			if (((ServiceTask) task).getType() != null && ((ServiceTask) task).getType().equals("mail")) {
				taskDto = convertToKickstartMailTask((ServiceTask) task);
			} else {
				taskDto = convertToKickstartServiceTask((ServiceTask) task);
			}
		} else if (task instanceof ScriptTask) {
			taskDto = convertToKickstartScriptTask((ScriptTask) task);
		}

		handleGeneralTaskDtoProperties(taskDto, task);

		return taskDto;
	}

	private void handleGeneralTaskDtoProperties(KickstartTask baseTaskDto, Task task) {
		// task id
		baseTaskDto.setId(task.getId());

		// task name
		baseTaskDto.setName(task.getName());

		// task description
		if (!task.getDocumentation().isEmpty()) {
			baseTaskDto.setDescription(task.getDocumentation().get(0).getText());
		}
	}

	public KickstartUserTask convertToKickstartUserTask(final UserTask userTask) {
		
		KickstartUserTask task = new KickstartUserTask();
		
		// Assignment
		for (ActivityResource activityResource : userTask.getActivityResource()) {
			if (activityResource instanceof PotentialOwner) {
				PotentialOwner potentialOwner = (PotentialOwner) activityResource;
				List<String> content = potentialOwner
						.getResourceAssignmentExpression().getExpression()
						.getContent();
				if (!content.isEmpty()) {
					task.setGroups(content.get(0));
				}
			} else if (activityResource instanceof HumanPerformer) {
				HumanPerformer humanPerformer = (HumanPerformer) activityResource;
				List<String> content = humanPerformer
						.getResourceAssignmentExpression().getExpression()
						.getContent();
				if (!content.isEmpty()) {
					task.setAssignee(content.get(0));
				}
			}
		}

		// Task form
		List<KickstartFormProperty> formPropertyDtos = new ArrayList<KickstartFormProperty>();
		if (userTask.getExtensionElements() != null) {
			for (AbstractExtensionElement extensionElement : userTask.getExtensionElements().getAllElementOfType(ActivitiFormProperty.class)) {
				ActivitiFormProperty formProperty = (ActivitiFormProperty) extensionElement;
				KickstartFormProperty formPropertyDto = new KickstartFormProperty();
				formPropertyDto.setProperty(formProperty.getName());

				String formType = formProperty.getType();
				String type = "text";
				if ("date".equals(formType)) {
					type = "date";
				} else if ("long".equals(formType)) {
					type = "number";
				}
				formPropertyDto.setType(type);
				formPropertyDto.setRequired("true".equals(formProperty.getRequired()));
				formPropertyDtos.add(formPropertyDto);
			}
		}

		if (formPropertyDtos.size() > 0) {
			KickstartForm formDto = new KickstartForm();
			formDto.setFormProperties(formPropertyDtos);
			task.setForm(formDto);
		}
		
		return task;
	}

	public KickstartServiceTask convertToKickstartServiceTask(final ServiceTask serviceTask) {
		KickstartServiceTask task = new KickstartServiceTask();
		task.setClassName(serviceTask.getClassName());
		task.setExpression(serviceTask.getExpression());
		task.setDelegateExpression(serviceTask.getDelegateExpression());
		return task;
	}

	public KickstartMailTask convertToKickstartMailTask(final ServiceTask serviceTask) {

		KickstartMailTask task = new KickstartMailTask();
		
		List<AbstractExtensionElement> extensionElements = serviceTask.getExtensionElements().getAny();
		for (AbstractExtensionElement abstractExtensionElement : extensionElements) {
			ActivitFieldExtensionElement field = (ActivitFieldExtensionElement) abstractExtensionElement;
			String fieldName = field.getName();
			if (fieldName.equals("to")) {
				task.getTo().setStringValue(field.getStringValue());
				task.getTo().setExpression(field.getExpression());
			} else if (fieldName.equals("from")) {
				task.getFrom().setStringValue(field.getStringValue());
				task.getFrom().setExpression(field.getExpression());
			} else if (fieldName.equals("subject")) {
				task.getSubject().setStringValue(field.getStringValue());
				task.getSubject().setExpression(field.getExpression());
			} else if (fieldName.equals("cc")) {
				task.getCc().setStringValue(field.getStringValue());
				task.getCc().setExpression(field.getExpression());
			} else if (fieldName.equals("bcc")) {
				task.getBcc().setStringValue(field.getStringValue());
				task.getBcc().setExpression(field.getExpression());
			} else if (fieldName.equals("html")) {
				task.getHtml().setStringValue(field.getStringValue());
				task.getHtml().setExpression(field.getExpression());
			} else if (fieldName.equals("text")) {
				task.getText().setStringValue(field.getStringValue());
				task.getText().setExpression(field.getExpression());
			}
		}
		
		return task;
	}

	public KickstartScriptTask convertToKickstartScriptTask(final ScriptTask serviceTask) {
		KickstartScriptTask task = new KickstartScriptTask();
		task.setScriptFormat(serviceTask.getScriptFormat());
		task.setResultVariableName(serviceTask.getResultVariableName());
		task.setScript(serviceTask.getScript());
		return task;
	}

}
