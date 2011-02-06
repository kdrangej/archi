/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import uk.ac.bolton.archimate.editor.model.IEditorModelManager;
import uk.ac.bolton.archimate.editor.ui.EditorManager;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.model.IArchimateModel;

/**
 * New ArchiMate Model Action
 * 
 * @author Phillip Beauvoir
 */
public class NewArchimateModelAction
extends Action
implements IWorkbenchAction
{
    
    public NewArchimateModelAction() {
        setImageDescriptor(IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_NEW_FILE_16));
        setText("&Empty Model");
        setToolTipText("New Empty Model");
        setId("uk.ac.bolton.archimate.editor.action.newModel");
        setActionDefinitionId(getId()); // register key binding
    }
    
    @Override
    public void run() {
        // Create new Model
        IArchimateModel model = IEditorModelManager.INSTANCE.createNewModel();
        
        // Open Diagram Editor
        EditorManager.openDiagramEditor(model.getDefaultDiagramModel());
    }

    public void dispose() {
    } 
}