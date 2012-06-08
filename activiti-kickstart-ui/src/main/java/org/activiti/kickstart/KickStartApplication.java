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
package org.activiti.kickstart;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.kickstart.service.KickstartService;
import org.activiti.kickstart.ui.MainLayout;
import org.activiti.kickstart.ui.ViewManager;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;

/**
 * @author Joram Barrez
 */
public class KickstartApplication extends Application implements HttpServletRequestListener {

	protected static final long serialVersionUID = 6197397757268207621L;

	protected static final String TITLE = "Activiti KickStart";
	protected static final String THEME_NAME = "activiti";

	// Thread local storage of instance for each user
	protected static ThreadLocal<KickstartApplication> current = new ThreadLocal<KickstartApplication>();
	
	// services
	protected KickstartService kickstartService;

	// ui
	protected ViewManager viewManager;
	protected MainLayout mainLayout; // general layout of the app

	public void init() {
		initMainWindow();
	}

	public static KickstartApplication get() {
		return current.get();
	}

	protected void initMainWindow() {
		Window mainWindow = new Window(TITLE);
		mainWindow.setTheme(THEME_NAME);
		setMainWindow(mainWindow);

		this.mainLayout = new MainLayout();
		mainWindow.setContent(mainLayout);

		this.viewManager = new ViewManager(mainLayout);
		viewManager.showCreateWorkflowPage();
	}

	// GETTERS & SETTERS /////////////////////////////////////////////////////////////////////////

	public ViewManager getViewManager() {
		return viewManager;
	}
	
	public KickstartService getKickstartService() {
		return kickstartService;
	}

	public void setKickstartService(KickstartService kickstartService) {
		this.kickstartService = kickstartService;
	}

	// HttpServletRequestListener /////////////////////////////////////////////////////////

	public void onRequestStart(HttpServletRequest request, HttpServletResponse response) {
		// Set current application object as thread-local to make it easy accessible
		current.set(this);
	}

	public void onRequestEnd(HttpServletRequest request, HttpServletResponse response) {
		// Clean up thread-local app
		current.remove();
	}

}
