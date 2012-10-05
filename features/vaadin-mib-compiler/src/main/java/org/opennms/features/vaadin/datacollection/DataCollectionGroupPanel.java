/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.vaadin.datacollection;

import java.io.File;
import java.io.FileWriter;

import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.features.vaadin.mibcompiler.api.Logger;
import org.opennms.netmgt.config.datacollection.DatacollectionGroup;

import com.vaadin.data.util.ObjectProperty;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;

import de.steinwedel.vaadin.MessageBox;
import de.steinwedel.vaadin.MessageBox.ButtonType;
import de.steinwedel.vaadin.MessageBox.EventListener;

/**
 * The Class DataCollectionGroupPanel.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a> 
 */
@SuppressWarnings("serial")
public abstract class DataCollectionGroupPanel extends Panel implements TabSheet.SelectedTabChangeListener {

    /** The group name. */
    private final TextField groupName;

    /** The resource types. */
    private final ResourceTypePanel resourceTypes;

    /** The groups. */
    private final GroupPanel groups;

    /** The system definitions. */
    private final SystemDefPanel systemDefs;

    /**
     * Instantiates a new data collection group panel.
     *
     * @param group the group
     * @param logger the logger
     */
    public DataCollectionGroupPanel(final DatacollectionGroup group, final Logger logger) {
        setCaption("Data Collection");
        addStyleName(Runo.PANEL_LIGHT);

        // Data Collection Group - Main Fields

        groupName = new TextField("Data Collection Group Name");
        groupName.setPropertyDataSource(new ObjectProperty<String>(group.getName()));
        groupName.setNullSettingAllowed(false);
        groupName.setImmediate(true);
        resourceTypes = new ResourceTypePanel(group, logger);
        groups = new GroupPanel(group, logger);
        systemDefs = new SystemDefPanel(group, logger);

        // Button Toolbar

        final HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.addComponent(new Button("Save Data Collection File", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                logger.info("The data collection have been saved.");
                processDataCollection(getOnmsDataCollection(), logger);
            }
        }));
        toolbar.addComponent(new Button("Cancel", new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                logger.info("Data collection processing has been canceled");
                cancel();
            }
        }));

        // Tab Panel

        final TabSheet tabs = new TabSheet();
        tabs.setStyleName(Runo.TABSHEET_SMALL);
        tabs.setSizeFull();
        tabs.addTab(resourceTypes, "Resource Types");
        tabs.addTab(groups, "MIB Groups");
        tabs.addTab(systemDefs, "System Definitions");

        // Main Layout

        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setMargin(true);
        mainLayout.addComponent(toolbar);
        mainLayout.addComponent(groupName);
        mainLayout.addComponent(tabs);
        mainLayout.setComponentAlignment(toolbar, Alignment.MIDDLE_RIGHT);
        setContent(mainLayout);
    }

    /* (non-Javadoc)
     * @see com.vaadin.ui.TabSheet.SelectedTabChangeListener#selectedTabChange(com.vaadin.ui.TabSheet.SelectedTabChangeEvent)
     */
    public void selectedTabChange(SelectedTabChangeEvent event) {
        TabSheet tabsheet = event.getTabSheet();
        Tab tab = tabsheet.getTab(tabsheet.getSelectedTab());
        if (tab != null) {
            getWindow().showNotification("Selected tab: " + tab.getCaption());
        }
    }    

    /**
     * Gets the OpenNMS data collection group.
     *
     * @return the OpenNMS data collection group
     */
    public DatacollectionGroup getOnmsDataCollection() {
        final DatacollectionGroup dto = new DatacollectionGroup();
        dto.setName((String) groupName.getValue());
        dto.getGroupCollection().addAll(groups.getGroups());
        dto.getResourceTypeCollection().addAll(resourceTypes.getResourceTypes());
        dto.getSystemDefCollection().addAll(systemDefs.getSystemDefinitions());
        return dto;
    }

    /**
     * Cancel.
     */
    public abstract void cancel();

    /**
     * Success.
     */
    public abstract void success();

    /**
     * Failure.
     */
    public abstract void failure();

    /**
     * Process data collection.
     *
     * @param dcGroup the OpenNMS Data Collection Group
     * @param logger the logger
     */
    /*
     * TODO Validations
     * 
     * - Check if there is no DCGroup with the same name
     */
    public void processDataCollection(final DatacollectionGroup dcGroup, final Logger logger) {
        final File configDir = new File(ConfigFileConstants.getHome(), "etc/datacollection/");
        final File file = new File(configDir, dcGroup.getName().replaceAll(" ", "_") + ".xml");
        if (file.exists()) {
            MessageBox mb = new MessageBox(getApplication().getMainWindow(),
                    "Are you sure?",
                    MessageBox.Icon.QUESTION,
                    "Do you really want to override the existig file?<br/>All current information will be lost.",
                    new MessageBox.ButtonConfig(MessageBox.ButtonType.YES, "Yes"),
                    new MessageBox.ButtonConfig(MessageBox.ButtonType.NO, "No"));
            mb.addStyleName(Runo.WINDOW_DIALOG);
            mb.show(new EventListener() {
                public void buttonClicked(ButtonType buttonType) {
                    if (buttonType == MessageBox.ButtonType.YES) {
                        saveFile(file, dcGroup, logger);
                    }
                }
            });
        } else {
            saveFile(file, dcGroup, logger);
        }
    }

    /**
     * Save file.
     *
     * @param file the file
     * @param dcGroup the datacollection-group
     * @param logger the logger
     */
    private void saveFile(final File file, final DatacollectionGroup dcGroup, final Logger logger) {
        try {
            FileWriter writer = new FileWriter(file);
            JaxbUtils.marshal(dcGroup, writer);
            logger.info("Saving XML data into " + file.getAbsolutePath());
            logger.warn("Remember to update datacollection-config.xml to include the group " + dcGroup.getName() + " and restart OpenNMS.");
            success();
        } catch (Exception e) {
            logger.error(e.getMessage());
            failure();
        }
    }
}