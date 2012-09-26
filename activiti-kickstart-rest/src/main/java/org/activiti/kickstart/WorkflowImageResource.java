package org.activiti.kickstart;

import java.io.InputStream;
import java.util.List;

import org.activiti.engine.ActivitiException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

/**
 * @author jbarrez
 */
public class WorkflowImageResource extends BaseResource {
  
  @Get
  public InputRepresentation getWorkflowImage() {
    String workflowId = (String) getRequest().getAttributes().get("workflowId");

    if (workflowId == null) {
      throw new ActivitiException("No workflowId provided in URL");
    }
    
    
    InputStream imageStream = getKickstartService().getProcessImage(workflowId);
    if (imageStream == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      return null;
    } else {
      return new InputRepresentation(imageStream, MediaType.IMAGE_PNG);
    }
  }
  
  @Post
  public void setWorkflowImage(Representation entity) {
    String workflowId = (String) getRequest().getAttributes().get("workflowId");
    
    try {
      RestletFileUpload upload = new RestletFileUpload(new DiskFileItemFactory());
      List<FileItem> items = upload.parseRepresentation(entity);
      
      FileItem uploadItem = null;
      for (FileItem fileItem : items) {
        if(fileItem.getName() != null) {
          uploadItem = fileItem;
        }
      }
      
      getKickstartService().setProcessImage(workflowId, uploadItem.getInputStream());
      
    } catch(Exception e) {
      throw new ActivitiException("Unable to upload workflow image: " + e.getMessage());
    }
  }

}
