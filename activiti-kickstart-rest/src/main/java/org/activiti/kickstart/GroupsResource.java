package org.activiti.kickstart;

import org.activiti.kickstart.service.alfresco.AlfrescoKickstartServiceImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.resource.Get;


/**
 * @author jbarrez
 */
public class GroupsResource extends BaseResource {
  
  private static final String RETRIEVE_GROUPS_URL = "http://localhost:8080/alfresco/service/api/groups?zone=APP.DEFAULT";
  
  @Get
  public JsonNode retrieveGroups() {
    
    // We're simply proxying the Alfresco rest API, so this code is quite unfancy ...
    
    AlfrescoKickstartServiceImpl kickstartService = (AlfrescoKickstartServiceImpl) getKickstartService();
    
    HttpState state = new HttpState();
    state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), 
            new UsernamePasswordCredentials(kickstartService.getCmisUser(), kickstartService.getCmisPassword()));

    GetMethod getMethod = new GetMethod(RETRIEVE_GROUPS_URL);

    try {
      HttpClient httpClient = new HttpClient();
      httpClient.executeMethod(null, getMethod, state);
      String responseJson = getMethod.getResponseBodyAsString();
      
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(responseJson);
      return json;
      
    } catch (Throwable t) {
      System.err.println("Error: " + t.getMessage());
      t.printStackTrace();
      return null;
    } finally {
      getMethod.releaseConnection();
    }
  }

}
