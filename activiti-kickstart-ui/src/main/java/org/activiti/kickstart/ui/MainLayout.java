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

import org.activiti.kickstart.KickStartApplication;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.VerticalLayout;


/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
public class MainLayout extends VerticalLayout {
  
  private static final long serialVersionUID = 1L;
  
  private static final String STYLE_MAIN_WRAPPER = "main";
  private static final String STYLE_HEADER = "header";
  private static final String STYLE_MAIN_CONTENT = "main-content";
  
  protected CssLayout header;
  protected CssLayout main;
  protected CssLayout footer;
  
  protected ViewManager viewManager;
  protected MainMenuBar mainMenuBar;
  
  public MainLayout() {
    this.viewManager = KickStartApplication.get().getViewManager();
    
    setSizeFull();
    addStyleName(STYLE_MAIN_WRAPPER);
    
    initHeader();
    initMainMenuBar();
    initMain();
  }
  
  public void setMainContent(Component mainContent) {
    main.removeAllComponents();
    main.addComponent(mainContent);
  }
  
  public void setFooter(Component footerContent) {
    footer.removeAllComponents();
    footer.addComponent(footerContent);
  }
  
  public void setMainNavigation(String navigation) {
    mainMenuBar.setMainNavigation(navigation);
  }
  
  protected void initHeader() {
    header = new CssLayout();
    header.addStyleName(STYLE_HEADER);
    header.setWidth(100, UNITS_PERCENTAGE);
    addComponent(header);
  }
  
	protected void initMainMenuBar() {
		this.mainMenuBar = new MainMenuBar();
		header.addComponent(mainMenuBar);
	}

  protected void initMain() {
    main = new CssLayout();
    main.setSizeFull();
    main.addStyleName(STYLE_MAIN_CONTENT);
    addComponent(main);
    setExpandRatio(main, 1.0f);
  }

}
