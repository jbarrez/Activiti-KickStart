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

import org.activiti.engine.ProcessEngines;
import org.activiti.kickstart.service.alfresco.AlfrescoKickstartServiceImpl;


/**
 * @author Joram Barrez
 */
public class KickstartServiceFactory {

	public KickstartService createActivitiStandaloneKickStartService() {
		KickstartServiceImpl kickstartService = new KickstartServiceImpl();
		
		kickstartService.setRepositoryService(ProcessEngines.getDefaultProcessEngine().getRepositoryService());
		
		TransformationServiceImpl transformationService = new TransformationServiceImpl();
		transformationService.setRepositoryService(ProcessEngines.getDefaultProcessEngine().getRepositoryService());
		transformationService.setHistoryService(ProcessEngines.getDefaultProcessEngine().getHistoryService());

		FormTransformationServiceImpl formTransformationService = new FormTransformationServiceImpl();
		transformationService.setFormTransformationService(formTransformationService);
		kickstartService.setTransformationService(transformationService);
		
		MarshallingService marshallingService = new MarshallingServiceImpl();
		kickstartService.setMarshallingService(marshallingService);
		
		return kickstartService;
	}
	
	public KickstartService createAlfrescoKickstartService(String cmisUser, String cmisPassword, String cmisAtompubUrl) {
		AlfrescoKickstartServiceImpl kickstartService = new AlfrescoKickstartServiceImpl(cmisUser, cmisPassword, cmisAtompubUrl);
		
		return kickstartService;
	}

}
