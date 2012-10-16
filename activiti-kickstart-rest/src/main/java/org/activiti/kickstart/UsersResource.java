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
public class UsersResource extends BaseResource {
  
  @Get
  public JsonNode retrieveUser() {

    // We're simply proxying the Alfresco rest API, so this code is quite unfancy ...
    AlfrescoKickstartServiceImpl kickstartService = (AlfrescoKickstartServiceImpl) getKickstartService();
    
    HttpState state = new HttpState();
    state.setCredentials(new AuthScope(null, AuthScope.ANY_PORT), 
            new UsernamePasswordCredentials(kickstartService.getCmisUser(), kickstartService.getCmisPassword()));

    String url = "http://localhost:8080/alfresco/service/api/people";
    String filter = (String) getRequest().getAttributes().get("filter");
    if (filter != null) {
      url = url + "?filter=" + filter;
    }
    GetMethod getMethod = new GetMethod(url);

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
