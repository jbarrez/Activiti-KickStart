package org.activiti.kickstart;

import org.restlet.data.Status;
import org.restlet.resource.Get;

/**
 * @author jbarrez
 */
public class WorkflowMetaDataResource extends BaseResource {
  
  @Get
  public String getWorkflowImage() {
    String workflowId = (String) getRequest().getAttributes().get("workflowId");
    String metaDataKey = (String) getRequest().getAttributes().get("metaDataKey");

    if (workflowId == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      return null;
    }
    
    if (metaDataKey == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      return null;
    }
    
   return getKickstartService().getWorkflowMetaData(workflowId, metaDataKey);
  }

}
