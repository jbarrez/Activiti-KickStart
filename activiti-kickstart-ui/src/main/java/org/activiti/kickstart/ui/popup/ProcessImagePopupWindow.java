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
package org.activiti.kickstart.ui.popup;

import java.io.InputStream;
import java.util.UUID;

import org.activiti.kickstart.KickstartApplication;
import org.activiti.kickstart.diagram.ProcessDiagramGenerator;
import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.service.KickstartServiceFactory;
import org.activiti.kickstart.service.MarshallingServiceImpl;
import org.activiti.kickstart.ui.ViewManager;

import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

/**
 * @author Joram Barrez
 */
public class ProcessImagePopupWindow extends Window {

	private static final long serialVersionUID = -1483000500366042513L;

	protected static final String TITLE = "Process image";

	protected ViewManager viewManager;
	protected KickstartWorkflow kickstartWorkflow;
	protected String processDefinitionId;
	protected KickstartService kickstartService;

	public ProcessImagePopupWindow(String processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
		init();
	}

	public ProcessImagePopupWindow(KickstartWorkflow adhocWorkflow) {
		this.kickstartWorkflow = adhocWorkflow;
		init();
	}

	public void init() {
		this.viewManager = KickstartApplication.get().getViewManager();
		this.kickstartService = KickstartApplication.get().getKickstartService();
		initUi();
	}

	protected void initUi() {
		setModal(true);
		setHeight("80%");
		setWidth("80%");
		center();
		setCaption(TITLE);

		StreamResource.StreamSource streamSource = null;
		if (processDefinitionId != null) {
			streamSource = new StreamSource() {

				private static final long serialVersionUID = -8875067466181823014L;

				public InputStream getStream() {
					return kickstartService.getProcessImage(processDefinitionId);
				}
			};
		} else if (kickstartWorkflow != null) {
			final ProcessDiagramGenerator converter = new ProcessDiagramGenerator(kickstartWorkflow, new MarshallingServiceImpl());
			streamSource = new StreamSource() {

				private static final long serialVersionUID = 239500411112658830L;

				public InputStream getStream() {
					return converter.execute();
				}
			};
		}

		// resource must have unique id (or cache-crap can happen)!
		StreamResource imageresource = new StreamResource(streamSource,UUID.randomUUID() + ".png", KickstartApplication.get());
		Panel panel = new Panel();
		panel.setContent(new HorizontalLayout());
		panel.setStyleName(Reindeer.PANEL_LIGHT);
		panel.setHeight("95%");
		Embedded embedded = new Embedded("", imageresource);
		embedded.setType(Embedded.TYPE_IMAGE);
		panel.addComponent(embedded);
		addComponent(panel);
	}

}
