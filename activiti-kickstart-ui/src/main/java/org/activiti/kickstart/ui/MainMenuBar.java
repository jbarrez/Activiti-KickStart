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

import java.util.HashMap;
import java.util.Map;

import org.activiti.kickstart.KickstartApplication;

import com.vaadin.terminal.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
@SuppressWarnings("serial")
public class MainMenuBar extends HorizontalLayout {

	private static final long serialVersionUID = 1L;

	public static final String MENU_ITEM_CREATE_WORKFLOW = "create-workflow";
	public static final String MENU_ITEM_EDIT_WORKFLOW = "edit-workflow";
	public static final String MENU_ITEM_SETTINGS = "settings";

	private static final String STYLE_APPLICATION_LOGO = "logo";
	private static final String STYLE_ACTIVE = "active";
	private static final String STYLE_MAIN_MENU_BUTTON = "main-menu-button";
	
	protected Map<String, Button> menuItemButtons;
	protected String currentMainNavigation;

	public MainMenuBar() {
		menuItemButtons = new HashMap<String, Button>();
		init();
	}

	/**
	 * Highlights the given main navigation in the menubar.
	 */
	public synchronized void setMainNavigation(String navigation) {
		if (currentMainNavigation != null) {
			menuItemButtons.get(currentMainNavigation).removeStyleName(STYLE_ACTIVE);
		}
		currentMainNavigation = navigation;

		Button current = menuItemButtons.get(navigation);
		if (current != null) {
			current.addStyleName(STYLE_ACTIVE);
		}
	}

	protected void init() {
		setHeight(54, UNITS_PIXELS);
		setWidth(100, UNITS_PERCENTAGE);

		setMargin(false, true, false, false);

		initTitle();
		initButtons();
	}

	protected void initTitle() {
		Label title = new Label();
		title.addStyleName(Reindeer.LABEL_H1);
		title.addStyleName(STYLE_APPLICATION_LOGO);

		addComponent(title);
		setComponentAlignment(title, Alignment.MIDDLE_LEFT);
		setExpandRatio(title, 0.5f);
	}

	protected void initButtons() {
		Button taskButton = addMenuButton("Create",
				Images.MAIN_MENU_CREATE_WORKFLOW, false, 80);
		taskButton.addListener(new CreateWorkflowButtonClickListener());
		menuItemButtons.put(MENU_ITEM_CREATE_WORKFLOW, taskButton);

		Button processButton = addMenuButton("Edit",
				Images.MAIN_MENU_EDIT_WORKFLOW, false, 80);
		processButton.addListener(new EditWorkflowButtonClickListener());
		menuItemButtons.put(MENU_ITEM_EDIT_WORKFLOW, processButton);

		Button manageButton = addMenuButton("Settings",
				Images.MAIN_MENU_SETTINGS, false, 90);
		manageButton.addListener(new SettingsClickListener());
		menuItemButtons.put(MENU_ITEM_SETTINGS, manageButton);
		
		// To center the buttons
		Label invisibleLabel = new Label();
		addComponent(invisibleLabel);
		setExpandRatio(invisibleLabel, 0.5f);
	}

	protected Button addMenuButton(String label, Resource icon, boolean active, float width) {
		Button button = new Button(label);
		button.addStyleName(STYLE_MAIN_MENU_BUTTON);
		button.addStyleName(Reindeer.BUTTON_LINK);
		button.setHeight(54, UNITS_PIXELS);
		button.setIcon(icon);
		button.setWidth(width, UNITS_PIXELS);

		addComponent(button);
		setComponentAlignment(button, Alignment.TOP_CENTER);

		return button;
	}

	// Button Listener classes
	private class CreateWorkflowButtonClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			KickstartApplication.get().getViewManager().showCreateWorkflowPage();
		}
	}

	private class EditWorkflowButtonClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			KickstartApplication.get().getViewManager().showEditWorkflowPage();
		}
	}

	private class SettingsClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			KickstartApplication.get().getViewManager().showSettingsPage();
		}
	}
}
