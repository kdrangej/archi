/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.sketch.editparts;

import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import uk.ac.bolton.archimate.editor.diagram.directedit.MultiLineCellEditor;
import uk.ac.bolton.archimate.editor.diagram.editparts.AbstractConnectedEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.IColoredEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.ITextAlignedEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.SnapEditPartAdapter;
import uk.ac.bolton.archimate.editor.diagram.figures.IContainerFigure;
import uk.ac.bolton.archimate.editor.diagram.figures.IDiagramModelObjectFigure;
import uk.ac.bolton.archimate.editor.diagram.policies.ContainerHighlightEditPolicy;
import uk.ac.bolton.archimate.editor.diagram.policies.DiagramLayoutPolicy;
import uk.ac.bolton.archimate.editor.diagram.policies.GroupContainerComponentEditPolicy;
import uk.ac.bolton.archimate.editor.diagram.policies.BasicContainerEditPolicy;
import uk.ac.bolton.archimate.editor.diagram.sketch.figures.StickyFigure;
import uk.ac.bolton.archimate.editor.diagram.sketch.policies.SketchConnectionPolicy;
import uk.ac.bolton.archimate.editor.model.commands.EObjectFeatureCommand;
import uk.ac.bolton.archimate.editor.utils.StringUtils;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModelContainer;
import uk.ac.bolton.archimate.model.IFontAttribute;
import uk.ac.bolton.archimate.model.ISketchModelSticky;
import uk.ac.bolton.archimate.model.ITextContent;


/**
 * Sticky Edit Part
 * 
 * @author Phillip Beauvoir
 */
public class StickyEditPart extends AbstractConnectedEditPart
implements IColoredEditPart, ITextAlignedEditPart  {
    
    private DirectEditManager fDirectManager;
    private ConnectionAnchor fAnchor;

    @Override
    protected List<?> getModelChildren() {
        return ((IDiagramModelContainer)getModel()).getChildren();
    }

    @Override
    protected IFigure createFigure() {
        StickyFigure figure = new StickyFigure((ISketchModelSticky)getModel());
        return figure;
    }

    @Override
    protected void createEditPolicies() {
        // Allow parts to be joined together
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new SketchConnectionPolicy());
        
        // Add a policy to handle directly editing the Part
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new StickyDirectEditTitlePolicy());

        // Add a policy to handle editing the Parts (for example, deleting a part)
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new GroupContainerComponentEditPolicy());
        
        // Install a custom layout policy that handles dragging things around and creating new objects
        installEditPolicy(EditPolicy.LAYOUT_ROLE, new DiagramLayoutPolicy());
        
        // Orphaning
        installEditPolicy(EditPolicy.CONTAINER_ROLE, new BasicContainerEditPolicy());
        
        // Snap to Geometry feedback
        installEditPolicy("Snap Feedback", new SnapFeedbackPolicy()); //$NON-NLS-1$

        // Selection Feedback
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new ContainerHighlightEditPolicy());
    }

    @Override
    public IFigure getContentPane() {
        return ((IContainerFigure)getFigure()).getContentPane();
    }
    
    @Override
    protected void refreshFigure() {
        // Refresh the figure if necessary
        ((IDiagramModelObjectFigure)getFigure()).refreshVisuals();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if(adapter == SnapToHelper.class) {
            return new SnapEditPartAdapter(this).getSnapToHelper();
        }
        
        return super.getAdapter(adapter);
    }
    
    @Override
    public void performRequest(Request req) {
        if(req.getType() == RequestConstants.REQ_OPEN) {
            performDirectEdit();
        }
    }
    
    private void performDirectEdit() {
        if(fDirectManager == null) {
            fDirectManager = new StickyDirectEditManager();
        }
        fDirectManager.show();
    }

    @Override
    protected ConnectionAnchor getConnectionAnchor() {
        if(fAnchor == null) {
            fAnchor = new ChopboxAnchor(getFigure());
        }
        return fAnchor;
    }

    /**
     * DirectEditManager
     */
    private class StickyDirectEditManager extends DirectEditManager {
        public StickyDirectEditManager() {
            super(StickyEditPart.this, MultiLineCellEditor.class, new StickyCellEditorLocator());
        }

        @Override
        protected CellEditor createCellEditorOn(Composite composite) {
            ISketchModelSticky sticky = (ISketchModelSticky)getModel();
            int alignment = sticky.getTextAlignment();
            if(alignment == IFontAttribute.TEXT_ALIGNMENT_CENTER) {
                alignment = SWT.CENTER;
            }
            else if(alignment == IFontAttribute.TEXT_ALIGNMENT_RIGHT) {
                alignment = SWT.RIGHT;
            }
            else {
                alignment = SWT.LEFT;
            }
            return new MultiLineCellEditor(composite, alignment);
        }

        @Override
        protected void initCellEditor() {
            String value = ((ITextContent)getModel()).getContent();
            getCellEditor().setValue(StringUtils.safeString(value));
            
            Text text = (Text)getCellEditor().getControl();
            text.selectAll();
            
            StickyFigure figure = (StickyFigure)getFigure();
            text.setFont(figure.getFont());
            text.setForeground(figure.getTextControl().getForegroundColor());
        }
    }
    
    /**
     * CellEditorLocator
     */
    private class StickyCellEditorLocator implements CellEditorLocator {
        public void relocate(CellEditor celleditor) {
            IFigure figure = getFigure();
            Text text = (Text)celleditor.getControl();
            Rectangle rect = figure.getBounds().getCopy();
            figure.translateToAbsolute(rect);
            text.setBounds(rect.x + 5, rect.y + 5, rect.width, rect.height);
        }
    }

    /**
     * DirectEditPolicy
     */
    private class StickyDirectEditTitlePolicy extends DirectEditPolicy {
        @Override
        protected Command getDirectEditCommand(DirectEditRequest request) {
            String content = (String)request.getCellEditor().getValue();
            return new EObjectFeatureCommand("Content", (EObject)getModel(),
                    IArchimatePackage.Literals.TEXT_CONTENT__CONTENT, content);
        }

        @Override
        protected void showCurrentEditValue(DirectEditRequest request) {
        }
    }
}
