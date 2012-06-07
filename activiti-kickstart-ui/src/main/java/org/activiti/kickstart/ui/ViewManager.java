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
package org.activiti.kickstart.ui;

import java.io.Serializable;

import org.activiti.kickstart.dto.KickstartWorkflow;
import org.activiti.kickstart.ui.panel.KickstartWorkflowPanel;
import org.activiti.kickstart.ui.panel.SelectWorkflowPanel;

import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

/**
 * Based on
 * http://dev.vaadin.com/browser/incubator/gasdiary/src/org/vaadin/gasdiary/ui/ViewManager.java
 * 
 * @author Joram Barrez
 */
public class ViewManager implements Serializable {

	private static final long serialVersionUID = 4097162454884471228L;

	public static final String EDIT_ADHOC_WORKFLOW = "editAdhocWorkflow";
	public static final String PROCESS_SUCESSFULLY_DEPLOYED = "processSuccessfullyDeployed";
	public static final String SELECT_ADHOC_WORKFLOW = "selectAdhocWorkflow";

	protected MainLayout mainLayout;

	public ViewManager(MainLayout mainLayout) {
		this.mainLayout = mainLayout;
	}

	public void showCreateWorkflowPage() {
		mainLayout.setMainContent(new KickstartWorkflowPanel());
		mainLayout.setMainNavigation(MainMenuBar.MENU_ITEM_CREATE_WORKFLOW);
	}

	public void showEditWorkflowPage() {
		mainLayout.setMainContent(new SelectWorkflowPanel());
		mainLayout.setMainNavigation(MainMenuBar.MENU_ITEM_EDIT_WORKFLOW);
	}
	
	public void showEditWorkflowPage(KickstartWorkflow kickstartWorkflow) {
		mainLayout.setMainContent(new KickstartWorkflowPanel(kickstartWorkflow));
		mainLayout.setMainNavigation(MainMenuBar.MENU_ITEM_SETTINGS);
	}

	public void showSettingsPage() {

	}
	
	public void showComponent(Component component) {
		mainLayout.setMainContent(component);
	}

	public void showPopupWindow(Window window) {
		mainLayout.getWindow().addWindow(window);
	}

}
