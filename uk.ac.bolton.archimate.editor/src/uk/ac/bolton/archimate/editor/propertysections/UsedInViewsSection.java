/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.propertysections;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import uk.ac.bolton.archimate.editor.diagram.IDiagramEditor;
import uk.ac.bolton.archimate.editor.diagram.editparts.IArchimateEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.connections.IArchimateConnectionEditPart;
import uk.ac.bolton.archimate.editor.model.DiagramModelUtils;
import uk.ac.bolton.archimate.editor.ui.EditorManager;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IDiagramModel;


/**
 * Property Section for "Used in Views"
 * 
 * @author Phillip Beauvoir
 */
public class UsedInViewsSection extends AbstractArchimatePropertySection {
    
    private static final String HELP_ID = "uk.ac.bolton.archimate.help.usedInViewsSection";
    
    /**
     * Filter to show or reject this section depending on input value
     */
    public static class Filter implements IFilter {
        @Override
        public boolean select(Object object) {
            return object instanceof IArchimateElement || object instanceof IArchimateEditPart 
                    || object instanceof IArchimateConnectionEditPart;
        }
    }

    private IArchimateElement fArchimateElement;
    
    private TableViewer fTableViewer;
    
    @Override
    protected void createControls(Composite parent) {
        createTableControl(parent);
    }
    
    private void createTableControl(Composite parent) {
        createCLabel(parent, "Used in Views:", ITabbedLayoutConstants.STANDARD_LABEL_WIDTH, SWT.TOP);
        
        // Table
        Table table = createTable(parent);
        fTableViewer = new TableViewer(table);
        
        // Help ID
        PlatformUI.getWorkbench().getHelpSystem().setHelp(table, HELP_ID);

        fTableViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            
            public void dispose() {
            }
            
            public Object[] getElements(Object inputElement) {
                return DiagramModelUtils.findReferencedDiagramsForElement((IArchimateElement)inputElement).toArray();
            }
        });
        
        fTableViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((IDiagramModel)element).getName();
            }
            
            @Override
            public Image getImage(Object element) {
                return IArchimateImages.ImageFactory.getImage(IArchimateImages.ICON_DIAGRAM_16);
            }
        });
        
        fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if(!isAlive()) {
                    return;
                }
                Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
                if(o instanceof IDiagramModel) {
                    IDiagramModel diagramModel = (IDiagramModel)o;
                    IDiagramEditor editor = (IDiagramEditor)EditorManager.openDiagramEditor(diagramModel);
                    if(editor != null) {
                        editor.selectElements(new IArchimateElement[] { fArchimateElement });
                    }
                }
            }
        });
        
        fTableViewer.setSorter(new ViewerSorter());
    }
    
    @Override
    protected void setElement(Object element) {
        if(element instanceof IArchimateElement) {
            fArchimateElement = (IArchimateElement)element;
        }
        else if(element instanceof IAdaptable) {
            fArchimateElement = (IArchimateElement)((IAdaptable)element).getAdapter(IArchimateElement.class);
        }
        else {
            System.err.println("UsedInViewsSection wants to display for " + element);
        }
    }
    
    @Override
    public void refresh() {
        fTableViewer.setInput(fArchimateElement);
    }
    
    @Override
    protected Adapter getECoreAdapter() {
        return null;
    }

    @Override
    protected EObject getEObject() {
        return fArchimateElement;
    }
    
    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }
}
