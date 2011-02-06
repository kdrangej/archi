/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import uk.ac.bolton.archimate.editor.diagram.IDiagramModelEditor;
import uk.ac.bolton.archimate.editor.views.IModelSelectionView;
import uk.ac.bolton.archimate.model.IArchimateModel;

/**
 * Global Action for current selection in either the Tree Model View or an Editor.<br>
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelSelectionAction extends Action implements IWorkbenchAction, IPartListener {
    
    /**
     * The workbench window this action is registered with.
     */
    protected IWorkbenchWindow workbenchWindow;
    
    /**
     * Active View
     */
    protected IModelSelectionView activeModelView;
    
    /**
     * Active Editor
     */
    protected IEditorPart activeEditor;
    
    /*
     * Listen to Tree Selections to update state
     */
    protected ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
            updateState();
        }
    };

    protected AbstractModelSelectionAction(String text, IWorkbenchWindow window) {
        super(text);
        workbenchWindow = window;
        workbenchWindow.getPartService().addPartListener(this);
        setEnabled(false);
    }
    
    // ------------------ Part Listener --------------------------

    public void partOpened(IWorkbenchPart part) {
        if(part instanceof IModelSelectionView) {
            ((IModelSelectionView)part).getSelectionProvider().addSelectionChangedListener(selectionListener);
        }
    }

    public void partClosed(IWorkbenchPart part) {
        if(part instanceof IModelSelectionView) {
            ((IModelSelectionView)part).getSelectionProvider().removeSelectionChangedListener(selectionListener);
            activeModelView = null;
            updateState();
        }
        else if(part instanceof IDiagramModelEditor) {
            if(workbenchWindow.getActivePage() != null) {
                activeEditor = workbenchWindow.getActivePage().getActiveEditor();
                updateState();
            }
        }
    }

    public void partActivated(IWorkbenchPart part) {
        if(part instanceof IModelSelectionView) {
            activeModelView = (IModelSelectionView)part;
            activeEditor = null;
        }
        else if(part instanceof IDiagramModelEditor) {
            activeEditor = (IEditorPart)part;
        }
        updateState();
    }
    
    public void partDeactivated(IWorkbenchPart part) {
    }
    
    public void partBroughtToTop(IWorkbenchPart part) {
    }
    
    // -----------------------------------------------------------

    /**
     * Update the State
     */
    protected abstract void updateState();
    
    /**
     * @return The Active Archimate Model
     */
    protected IArchimateModel getActiveArchimateModel() {
        IArchimateModel model = null;
        
        // Active Editor first
        if(activeEditor != null) {
            model = (IArchimateModel)activeEditor.getAdapter(IArchimateModel.class);
        }
        // Then Active View
        else if(activeModelView != null) {
            model = (IArchimateModel)activeModelView.getAdapter(IArchimateModel.class);
        }
        
        return model;
    }

    public void dispose() {
        workbenchWindow.getPartService().removePartListener(this);
    }
}