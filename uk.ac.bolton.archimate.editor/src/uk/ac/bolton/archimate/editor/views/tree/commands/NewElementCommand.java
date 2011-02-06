/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.views.tree.commands;

import org.eclipse.gef.commands.Command;

import uk.ac.bolton.archimate.editor.model.IEditorModelManager;
import uk.ac.bolton.archimate.editor.views.tree.ITreeModelView;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IFolder;


/**
 * Add Archimate Element Command
 * 
 * @author Phillip Beauvoir
 */
public class NewElementCommand extends Command {
    
    private IFolder fFolder;
    private IArchimateElement fElement;

    public NewElementCommand(IFolder folder, IArchimateElement element) {
        fFolder = folder;
        fElement = element;
        setLabel("New " + element.getName());
    }
    
    @Override
    public void execute() {
        fFolder.getElements().add(fElement);
        
        // Fire this event so the tree view can select the element
        IEditorModelManager.INSTANCE.firePropertyChange(this,
                ITreeModelView.PROPERTY_MODEL_ELEMENT_NEW, null, fElement);
    }
    
    @Override
    public void undo() {
        fFolder.getElements().remove(fElement);
        
        // Fire this event so the tree view can select a parent node
        IEditorModelManager.INSTANCE.firePropertyChange(this,
                ITreeModelView.PROPERTY_SELECTION_CHANGED, null, fFolder);
    }
}
