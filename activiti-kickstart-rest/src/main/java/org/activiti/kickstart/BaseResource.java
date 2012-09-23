package org.activiti.kickstart;

import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.KickstartServiceFactory;
import org.restlet.resource.ServerResource;

public class BaseResource extends ServerResource {
	
	protected static KickstartService kickstartServiceInstance;
	
	protected KickstartService getKickstartService() {
		if (kickstartServiceInstance == null) {
			synchronized (BaseResource.class) {
				if (kickstartServiceInstance == null) {
					String cmisUser = getContextParameter("cmisUser");
					String cmisPassword = getContextParameter("cmisPassword");
					String cmisAtompubUrl = getContextParameter("cmisAtompubUrl");
					
					KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
					kickstartServiceInstance = kickstartServiceFactory.createAlfrescoKickstartService(cmisUser, cmisPassword, cmisAtompubUrl); 
				}
			}
		}
		return kickstartServiceInstance;
	}
	
	protected String getContextParameter(String parameterName) {
		String parameterValue = getContext().getParameters().getFirstValue(parameterName);
		if (parameterValue == null) {
			throw new RuntimeException("Mandatory parameter " + parameterName + " not found");
		}
		return parameterValue;
	}

}
