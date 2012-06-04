package org.activiti.kickstart.dto;

import static org.junit.Assert.*;

import org.activiti.kickstart.bpmn20.model.activity.type.ScriptTask;
import org.activiti.kickstart.service.MarshallingService;
import org.activiti.kickstart.service.ServiceLocator;
import org.junit.Test;


public class ScriptTaskDtoTest {

    
    @Test
    public void testCreateFlowElement() throws Exception {
//    <scriptTask id="theScriptTask" name="Execute script" scriptFormat="juel" activiti:resultVariableName="myVar">
//      <script>#{echo}</script>
//    </scriptTask>
        
        KickstartScriptTask dto = new KickstartScriptTask();
        dto.setId("theScriptTask");
        dto.setName("Execute script");
        
        dto.setScriptFormat("juel");
        dto.setResultVariableName("myVar");
        dto.setScript("#{echo}");
        
        MarshallingService marshallingService = ServiceLocator.getMarshallingService();
        ScriptTask scriptTask = marshallingService.convertToBPMN(dto);
        assertEquals("juel", scriptTask.getScriptFormat());
        assertEquals("myVar", scriptTask.getResultVariableName());
        assertEquals("#{echo}", scriptTask.getScript());
    }
}
