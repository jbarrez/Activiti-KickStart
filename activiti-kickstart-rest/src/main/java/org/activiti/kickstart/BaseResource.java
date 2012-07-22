package org.activiti.kickstart;

import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.KickstartServiceFactory;
import org.restlet.resource.ServerResource;

public class BaseResource extends ServerResource {
	
	protected static KickstartService cachedKickstartService;
	
	protected KickstartService getKickstartService() {
		if (cachedKickstartService == null) {
			synchronized (BaseResource.class) {
				if (cachedKickstartService == null) {
					String cmisUser = getContextParameter("cmisUser");
					String cmisPassword = getContextParameter("cmisPassword");
					String cmisAtompubUrl = getContextParameter("cmisAtompubUrl");
					
					KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
					cachedKickstartService = kickstartServiceFactory.createAlfrescoKickstartService(cmisUser, cmisPassword, cmisAtompubUrl); 
				}
			}
		}
		return cachedKickstartService;
	}
	
	protected String getContextParameter(String parameterName) {
		String parameterValue = getContext().getParameters().getFirstValue(parameterName);
		if (parameterValue == null) {
			throw new RuntimeException("Mandatory parameter " + parameterName + " not found");
		}
		return parameterValue;
	}

}
