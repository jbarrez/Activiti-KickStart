package org.activiti.kickstart.service;


import java.io.InputStream;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.test.ActivitiTestCase;
import org.activiti.kickstart.diagram.ProcessDiagramGenerator;
import org.activiti.kickstart.dto.KickstartForm;
import org.activiti.kickstart.dto.KickstartFormProperty;
import org.activiti.kickstart.dto.KickstartMailTask;
import org.activiti.kickstart.dto.KickstartScriptTask;
import org.activiti.kickstart.dto.KickstartServiceTask;
import org.activiti.kickstart.dto.KickstartUserTask;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.junit.Test;


public class KickstartServiceImplTest extends ActivitiTestCase {
	
	protected Bpmn20MarshallingService marshallingService = new MarshallingServiceImpl();

    @Test
    public void testSimpleUserTask() throws Exception {
        KickstartWorkflow dto = createSimpleUserTaskWorkflow();

        String bpmn = marshallingService.marshallWorkflow(dto); 
//        System.out.println(bpmn);

        assertTrue(bpmn.indexOf("userTask ") > - 1);
        assertFalse(bpmn.indexOf("serviceTask ") > - 1);
    }


    @Test
    public void testSimpleUserTaskImage() throws Exception {
        KickstartWorkflow dto = createSimpleUserTaskWorkflow();
        ProcessDiagramGenerator generator = new ProcessDiagramGenerator(dto, marshallingService);

        InputStream in = generator.execute();
        assertNotNull(in);
//        createFile(in, "c:/temp/user.png");
    }


    private KickstartWorkflow createSimpleUserTaskWorkflow() {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One User Task Workflow");
        dto.setDescription("Simple workflow definition containing one user task");

        KickstartUserTask taskDto = new KickstartUserTask();
        taskDto.setName("My First User task");
        taskDto.setDescription("Desc first User task");

        KickstartForm form = new KickstartForm();
        KickstartFormProperty property = new KickstartFormProperty();
        property.setProperty("name");
        property.setType("text");
        form.addFormProperty(property);
        taskDto.setForm(form);
        dto.addTask(taskDto);
        return dto;
    }

    @Test
    public void testSimpleServiceTask() throws Exception {
        KickstartWorkflow dto = createSimpleServiceTaskWorkflow();

        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);

        assertTrue(bpmn.indexOf("serviceTask ") > - 1);
        assertTrue(bpmn.indexOf(":expression=\"#{my.favorite.expression}\"") > - 1);
        assertTrue(bpmn.indexOf(":class=\"de.test.MyClass\"") > - 1);
        assertTrue(bpmn.indexOf(":delegateExpression=\"#{myDelegateExpression}\"") > - 1);
    }

    @Test
    public void testSimpleServiceTaskImage() throws Exception {
        KickstartWorkflow dto = createSimpleServiceTaskWorkflow();
        ProcessDiagramGenerator generator = new ProcessDiagramGenerator(dto, marshallingService);

        InputStream in = generator.execute();
        assertNotNull(in);
//        createFile(in, "c:/temp/service.png");
    }

    private KickstartWorkflow createSimpleServiceTaskWorkflow() {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Service Task Workflow");
        dto.setDescription("Simple workflow definition containing one service task");

        KickstartServiceTask task = new KickstartServiceTask();
        task.setName("My First Service task");
        task.setDescription("Desc first Service task");
        task.setDelegateExpression("#{myDelegateExpression}");
        task.setClassName("de.test.MyClass");
        task.setExpression("#{my.favorite.expression}");
        dto.addTask(task);
        return dto;
    }

    

    @Test
    public void testSimpleEmailTask() throws Exception {
        KickstartWorkflow dto = createSimpleEmailTaskWorkflow();
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);
        
        assertTrue(bpmn.indexOf("serviceTask ") > - 1);
        assertTrue(bpmn.indexOf(":type=\"mail\"") > - 1);
        assertTrue(bpmn.indexOf("name=\"to\"") > - 1);
        assertTrue(bpmn.indexOf(":string>test@mycompany.com</") > - 1);
    }
    
    @Test
    public void testSimpleEmailTaskWithAllFields() throws Exception {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Mail Task Workflow");
        dto.setDescription("Simple workflow definition containing one mail task");

        KickstartMailTask task = new KickstartMailTask();
        task.setId("myId");
        task.setName("My First Mail task");
        task.setDescription("Desc first Mail task");
        
        task.getTo().setStringValue("you@yourcompany.com");
        task.getTo().setExpression("${to_email}");
        
        task.getFrom().setStringValue("me@mycompany.com");
        task.getFrom().setExpression("${from_email}");
        
        task.getSubject().setStringValue("my subject string");
        task.getSubject().setExpression("${my.subject.expression}");
        
        task.getCc().setStringValue("my cc string");
        task.getCc().setExpression("${my.cc.expression}");
        
        task.getBcc().setStringValue("my bcc string");
        task.getBcc().setExpression("${my.bcc.expression}");
        
        task.getHtml().setStringValue("my html string");
        task.getHtml().setExpression("${my.html.expression}");
        
        task.getText().setStringValue("my text string");
        task.getText().setExpression("${my.text.expression}");
        
        
        dto.addTask(task);
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);
        
        assertTrue(bpmn.indexOf("serviceTask ") > - 1);
        assertTrue(bpmn.indexOf(":type=\"mail\"") > - 1);
        
        assertTrue(bpmn.indexOf("name=\"to\"") > - 1);
        assertTrue(containsStringValue(bpmn, "you@yourcompany.com"));
        assertTrue(containsExpression(bpmn, "${to_email}"));
        
        assertTrue(bpmn.indexOf("name=\"from\"") > - 1);
        assertTrue(containsStringValue(bpmn, "me@mycompany.com"));
        assertTrue(containsExpression(bpmn, "${from_email}"));
        
        assertTrue(bpmn.indexOf("name=\"subject\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my subject string"));
        assertTrue(containsExpression(bpmn, "${my.subject.expression}"));
        
        assertTrue(bpmn.indexOf("name=\"cc\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my cc string"));
        assertTrue(containsExpression(bpmn, "${my.cc.expression}"));
        
        assertTrue(bpmn.indexOf("name=\"bcc\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my bcc string"));
        assertTrue(containsExpression(bpmn, "${my.bcc.expression}"));
        
        assertTrue(bpmn.indexOf("name=\"html\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my html string"));
        assertTrue(containsExpression(bpmn, "${my.html.expression}"));
        
        assertTrue(bpmn.indexOf("name=\"text\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my text string"));
        assertTrue(containsExpression(bpmn, "${my.text.expression}"));
    }
    
    private boolean containsStringValue(String completeString, String strToCheck) {
      return completeString.indexOf(":string>" + strToCheck +"</") > - 1;
    }
    
    private boolean containsExpression(String completeString, String strToCheck) {
      return completeString.indexOf(":expression>" + strToCheck +"</") > - 1;
    }
    
    
    @Test
    public void testEmailTaskWithSomeFieldsOnly() throws Exception {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Mail Task Workflow");
        dto.setDescription("Simple workflow definition containing one mail task");

        KickstartMailTask task = new KickstartMailTask();
        task.setId("myId");
        task.setName("My First Mail task");
        task.setDescription("Desc first Mail task");
        
        task.getTo().setStringValue("you@yourcompany.com");
        
        task.getSubject().setStringValue("my subject string");
        task.getHtml().setExpression("${my.html.expression}");
        
        task.getText().setStringValue("my text string");
        
        
        dto.addTask(task);
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);
        
        assertTrue(bpmn.indexOf("serviceTask ") > - 1);
        assertTrue(bpmn.indexOf(":type=\"mail\"") > - 1);
        
        assertTrue(bpmn.indexOf("name=\"to\"") > - 1);
        assertTrue(containsStringValue(bpmn, "you@yourcompany.com"));
        
        assertEquals(-1, bpmn.indexOf("name=\"from\""));
        
        assertTrue(bpmn.indexOf("name=\"subject\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my subject string"));
        
        assertEquals(-1, bpmn.indexOf("name=\"cc\""));
        assertEquals(-1, bpmn.indexOf("name=\"bcc\""));
        
        assertTrue(bpmn.indexOf("name=\"html\"") > - 1);
        assertTrue(containsExpression(bpmn, "${my.html.expression}"));
        
        assertTrue(bpmn.indexOf("name=\"text\"") > - 1);
        assertTrue(containsStringValue(bpmn, "my text string"));
    }
    
    @Test
    public void testSimpleEmailTaskImage() throws Exception {
        KickstartWorkflow dto = createSimpleEmailTaskWorkflow();
        ProcessDiagramGenerator generator = new ProcessDiagramGenerator(dto, marshallingService);
        
        InputStream in = generator.execute();
        assertNotNull(in);
    }

    private KickstartWorkflow createSimpleEmailTaskWorkflow() {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Mail Task Workflow");
        dto.setDescription("Simple workflow definition containing one mail task");

        KickstartMailTask task = new KickstartMailTask();
        task.setId("myId");
        task.setName("My First Mail task");
        task.setDescription("Desc first Mail task");
        task.getTo().setStringValue("test@mycompany.com");
        task.getText().setStringValue("My text content");
        
        dto.addTask(task);
        return dto;
    }
    
    
    @Test
    public void testDeployAndFindUserTaskWorkflow() throws Exception {
        KickstartWorkflow dto = createSimpleUserTaskWorkflow();
        
        KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
        KickstartService service = kickstartServiceFactory.createActivitiStandaloneKickStartService();
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        System.out.println(bpmn);


        String deploymentId = service.deployWorkflow(dto);
        String pid = getDeployedProcessDefinitionId(deploymentId);
        KickstartWorkflow deployedDto = service.findWorkflowById(pid);

        assertEquals(dto.getName(), deployedDto.getName());
        assertEquals(dto.getDescription(), deployedDto.getDescription());

        assertTrue("Task should be a UserTask", deployedDto.getTasks().get(0) instanceof KickstartUserTask);
        KickstartUserTask task = (KickstartUserTask) deployedDto.getTasks().get(0);
        assertEquals("task_1", task.getId());
        assertEquals("My First User task", task.getName());
        assertEquals("Desc first User task", task.getDescription());
    }

    @Test
    public void testDeployAndFindServiceTaskWorkflow() throws Exception {
        KickstartWorkflow dto = createSimpleServiceTaskWorkflow();
        
        KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
        KickstartService service = kickstartServiceFactory.createActivitiStandaloneKickStartService();

        String deploymentId = service.deployWorkflow(dto);
        String pid = getDeployedProcessDefinitionId(deploymentId);
        KickstartWorkflow deployedDto = service.findWorkflowById(pid);

        assertEquals(dto.getName(), deployedDto.getName());
        assertEquals(dto.getDescription(), deployedDto.getDescription());

        assertTrue("Task should be a ServiceTask", deployedDto.getTasks().get(0) instanceof KickstartServiceTask);
        KickstartServiceTask task = (KickstartServiceTask) deployedDto.getTasks().get(0);
        assertEquals("task_1", task.getId());
        assertEquals("#{myDelegateExpression}", task.getDelegateExpression());
        assertEquals("#{my.favorite.expression}", task.getExpression());
        assertEquals("de.test.MyClass", task.getClassName());
    }
    
    @Test
    public void testDeployAndFindEmailTaskWorkflowWithSomeElements() throws Exception {
        KickstartWorkflow dto = createSimpleEmailTaskWorkflow();
        
        KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
        KickstartService service = kickstartServiceFactory.createActivitiStandaloneKickStartService();

        String deploymentId = service.deployWorkflow(dto);
        String pid = getDeployedProcessDefinitionId(deploymentId);
        KickstartWorkflow deployedDto = service.findWorkflowById(pid);

        assertEquals(dto.getName(), deployedDto.getName());
        assertEquals(dto.getDescription(), deployedDto.getDescription());

        assertTrue("Task should be a MailTaskDto", deployedDto.getTasks().get(0) instanceof KickstartMailTask);
        KickstartMailTask task = (KickstartMailTask) deployedDto.getTasks().get(0);
        assertEquals("task_1", task.getId());
        assertEquals("test@mycompany.com", task.getTo().getStringValue());
        assertEquals("My text content", task.getText().getStringValue());
        
        assertEquals(null, task.getFrom().getStringValue());
        assertEquals(null, task.getFrom().getExpression());
        assertEquals(null, task.getSubject().getStringValue());
        assertEquals(null, task.getSubject().getExpression());
        assertEquals(null, task.getCc().getStringValue());
        assertEquals(null, task.getCc().getExpression());
        assertEquals(null, task.getBcc().getStringValue());
        assertEquals(null, task.getBcc().getExpression());
        assertEquals(null, task.getHtml().getStringValue());
        assertEquals(null, task.getHtml().getExpression());
    }
    
    
    @Test
    public void testDeployAndFindEmailTaskWorkflowWithAllElements() throws Exception {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Mail Task Workflow");
        dto.setDescription("Simple workflow definition containing one mail task");
        
        
        KickstartMailTask taskDto = new KickstartMailTask();
        taskDto.setName("My Email Task");
        taskDto.setDescription("my email task description");
        taskDto.getTo().setStringValue("you@yourcompany.com");
        taskDto.getTo().setExpression("${to_email}");
        
        taskDto.getFrom().setStringValue("me@mycompany.com");
        taskDto.getFrom().setExpression("${from_email}");
        
        taskDto.getSubject().setStringValue("my subject string");
        taskDto.getSubject().setExpression("${my.subject.expression}");
        
        taskDto.getCc().setStringValue("my cc string");
        taskDto.getCc().setExpression("${my.cc.expression}");
        
        taskDto.getBcc().setStringValue("my bcc string");
        taskDto.getBcc().setExpression("${my.bcc.expression}");
        
        taskDto.getHtml().setStringValue("my html string");
        taskDto.getHtml().setExpression("${my.html.expression}");
        
        taskDto.getText().setStringValue("my text string");
        taskDto.getText().setExpression("${my.text.expression}");
        
        dto.addTask(taskDto);
        
        KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
        KickstartService service = kickstartServiceFactory.createActivitiStandaloneKickStartService();

        String deploymentId = service.deployWorkflow(dto);
        String pid = getDeployedProcessDefinitionId(deploymentId);
        KickstartWorkflow deployedDto = service.findWorkflowById(pid);

        assertEquals(dto.getName(), deployedDto.getName());
        assertEquals(dto.getDescription(), deployedDto.getDescription());

        assertTrue("Task should be a MailTaskDto", deployedDto.getTasks().get(0) instanceof KickstartMailTask);
        KickstartMailTask task = (KickstartMailTask) deployedDto.getTasks().get(0);
        assertEquals("task_1", task.getId());
        assertEquals("you@yourcompany.com", task.getTo().getStringValue());
        assertEquals("${to_email}", task.getTo().getExpression());

        assertEquals("me@mycompany.com", task.getFrom().getStringValue());
        assertEquals("${from_email}", task.getFrom().getExpression());
        
        assertEquals("my subject string", task.getSubject().getStringValue());
        assertEquals("${my.subject.expression}", task.getSubject().getExpression());
        
        assertEquals("my cc string", task.getCc().getStringValue());
        assertEquals("${my.cc.expression}", task.getCc().getExpression());
        
        assertEquals("my bcc string", task.getBcc().getStringValue());
        assertEquals("${my.bcc.expression}", task.getBcc().getExpression());
        
        assertEquals("my html string", task.getHtml().getStringValue());
        assertEquals("${my.html.expression}", task.getHtml().getExpression());
        
        assertEquals("my text string", task.getText().getStringValue());
        assertEquals("${my.text.expression}", task.getText().getExpression());
    }

    private String getDeployedProcessDefinitionId(final String deploymentId) {
        ProcessDefinition process = processEngine.getRepositoryService()
            .createProcessDefinitionQuery()
            .deploymentId(deploymentId)
            .singleResult();
        return process.getId();
    }
    
    
    @Test
    public void testTwoParallelServiceTasks() throws Exception {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Service Task Workflow");
        dto.setDescription("Simple workflow definition containing one service task");

        KickstartServiceTask task1 = new KickstartServiceTask();
        task1.setId("myFirstId");
        task1.setName("My First Service task");
        task1.setDescription("Desc first Service task");
        task1.setDelegateExpression("#{myFirstDelegateExpression}");
        task1.setClassName("de.test.MyFirstClass");
        task1.setExpression("#{my.favorite.first.expression}");
        dto.addTask(task1);
        
        KickstartServiceTask task2 = new KickstartServiceTask();
        task2.setId("mySecondId");
        task2.setName("My Second Service task");
        task2.setDescription("Desc Second Service task");
        task2.setDelegateExpression("#{mySecondDelegateExpression}");
        task2.setClassName("de.test.MySecondClass");
        task2.setExpression("#{my.favorite.second.expression}");
        task2.setStartWithPrevious(true);
        dto.addTask(task2);
        
        ProcessDiagramGenerator generator = new ProcessDiagramGenerator(dto, marshallingService);
        
        InputStream in = generator.execute();
        assertNotNull(in);
        
//        createFile(in, "c:/temp/parallel.png");
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);
        
        assertTrue(bpmn.indexOf("serviceTask ") > - 1);
        assertTrue(bpmn.indexOf(":delegateExpression=\"#{myFirstDelegateExpression}\"") > - 1);
        assertTrue(bpmn.indexOf(":delegateExpression=\"#{mySecondDelegateExpression}\"") > - 1);
        
    }
    
    
    @Test
    public void testSimpleScriptTask() throws Exception {
//    <scriptTask id="theScriptTask" name="Execute script" scriptFormat="juel" activiti:resultVariableName="myVar">
//      <script>#{echo}</script>
//    </scriptTask>
        
        KickstartWorkflow dto = createSimpleScriptTask();
        
        String bpmn = marshallingService.marshallWorkflow(dto);
        //System.out.println(bpmn);

        assertTrue(bpmn.indexOf("scriptTask ") > - 1);
        assertTrue(bpmn.indexOf("scriptFormat=\"juel\"") > - 1);
        assertTrue(bpmn.indexOf(":resultVariableName=\"myVar\"") > - 1);
//        assertTrue(bpmn.indexOf("<script>#{echo}</script>") > - 1);
    }
    
    @Test
    public void testSimpleScriptTaskImage() throws Exception {
        KickstartWorkflow dto = createSimpleScriptTask();
        ProcessDiagramGenerator generator = new ProcessDiagramGenerator(dto, marshallingService);

        InputStream in = generator.execute();
        assertNotNull(in);
//        createFile(in, "c:/temp/script.png");
    }
    
    private KickstartWorkflow createSimpleScriptTask() {
        KickstartWorkflow dto = new KickstartWorkflow();
        dto.setName("One Script Task Workflow");
        dto.setDescription("Simple workflow definition containing one script task");
        
        KickstartScriptTask scriptDto = new KickstartScriptTask();
        scriptDto.setId("theScriptTask");
        scriptDto.setName("Execute script");
        
        scriptDto.setScriptFormat("juel");
        scriptDto.setResultVariableName("myVar");
        scriptDto.setScript("#{echo}");
        
        dto.addTask(scriptDto);
        return dto;
    }
    
    @Test
    public void testDeployAndFindScriptTaskWorkflow() throws Exception {
        KickstartWorkflow dto = createSimpleScriptTask();
        
        KickstartServiceFactory kickstartServiceFactory = new KickstartServiceFactory();
        KickstartService service = kickstartServiceFactory.createActivitiStandaloneKickStartService();

        String deploymentId = service.deployWorkflow(dto);
        String pid = getDeployedProcessDefinitionId(deploymentId);
        KickstartWorkflow deployedDto = service.findWorkflowById(pid);

        assertEquals(dto.getName(), deployedDto.getName());
        assertEquals(dto.getDescription(), deployedDto.getDescription());

        assertTrue("Task should be a ScriptTask", deployedDto.getTasks().get(0) instanceof KickstartScriptTask);
        KickstartScriptTask task = (KickstartScriptTask) deployedDto.getTasks().get(0);
        assertEquals("task_1", task.getId());
        assertEquals("juel", task.getScriptFormat());
        assertEquals("myVar", task.getResultVariableName());
        assertEquals("#{echo}", task.getScript());
    }
    
//    private void createFile(final InputStream imageStream, final String fileName) throws Exception {
//        File f = new File(fileName);
//        if (f.exists()) {
//            f.delete();
//        }
//        OutputStream out = new FileOutputStream(f);
//        byte[] buf = new byte[1024];
//        int len;
//        while ((len = imageStream.read(buf)) > 0) {
//            out.write(buf, 0, len);
//        }
//        out.close();
//        imageStream.close();
//    }
}
