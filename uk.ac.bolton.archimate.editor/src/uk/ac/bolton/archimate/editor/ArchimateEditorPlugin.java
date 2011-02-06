/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import uk.ac.bolton.archimate.editor.preferences.Preferences;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArchimateEditorPlugin extends AbstractUIPlugin {

    /**
     * ID of the plug-in
     */
    public static final String PLUGIN_ID = "uk.ac.bolton.archimate.editor"; //$NON-NLS-1$

    /**
     * The File location of this plugin folder
     */
    private static File fPluginFolder;

    /**
     * The shared instance
     */
    public static ArchimateEditorPlugin INSTANCE;

    /**
     * The constructor.
     */
    public ArchimateEditorPlugin() {
        INSTANCE = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        // super must be *last*
        super.stop(context);
    }
    
    /**
     * @return The Platform specific launcher, if any, to launch application from file in OS
     */
    public IPlatformLauncher getPlatformLauncher() {
        Bundle bundle = getBundle();
        try {
            Class<?> clazz = bundle.loadClass("uk.ac.bolton.archimate.editor.PlatformLauncher"); //$NON-NLS-1$
            if(IPlatformLauncher.class.isAssignableFrom(clazz)) {
                return ((IPlatformLauncher) clazz.newInstance());
            }
        } catch(Exception e) {
        }
        
        return null;
    }
    
    /**
     * @return The User data folder
     */ 
    public File getUserDataFolder() {
        String path = Preferences.getUserDataFolder();
        return new File(path);
    }
    
    /**
     * @return The assets folder
     */
    public File getAssetsFolder() {
        return new File(getPluginFolder(), "assets"); //$NON-NLS-1$
    }
    
    /**
     * @return The templates folder
     */
    public File getTemplatesFolder() {
        return new File(getPluginFolder(), "templates"); //$NON-NLS-1$
    }
    
    /**
     * @return The Workspace folder
     */
    public File getWorkspaceFolder() {
        /*
         * Get Data Folder.  Try for one set by a user system property first, otherwise
         * use the workbench instance data location
         */
        String strFolder = System.getProperty("uk.ac.bolton.archimate.editor.workspaceFolder"); //$NON-NLS-1$
        if(strFolder != null) {
            return new File(strFolder);
        }
        
        Location instanceLoc = Platform.getInstanceLocation();
        if(instanceLoc == null) {
            Logger.logWarning("Instance Location is null. Using user.home"); //$NON-NLS-1$
            return new File(System.getProperty("user.home")); //$NON-NLS-1$
        }
        else {
            URL url = instanceLoc.getURL();
            if(url != null) {
                return new File(url.getPath());
            }
            else {
                return new File(System.getProperty("user.home")); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * @return The File Location of this plugin
     */
    public File getPluginFolder() {
        if(fPluginFolder == null) {
            URL url = getBundle().getEntry("/"); //$NON-NLS-1$
            try {
                url = FileLocator.resolve(url);
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
            fPluginFolder = new File(url.getPath());
        }
        
        return fPluginFolder;
    }
    
    /**
     * @param key A string key beginning with "%"
     * @return A Resource String from the plugin.properties file
     */
    public String getResourceString(String key) {
        return Platform.getResourceString(Platform.getBundle(PLUGIN_ID), key);
    }
    
    /**
     * @return The plugin id
     */
    public String getId(){
        return getBundle().getSymbolicName();
    }
}
