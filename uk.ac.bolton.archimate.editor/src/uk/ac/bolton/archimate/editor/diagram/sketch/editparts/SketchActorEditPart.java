/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.sketch.editparts;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.LocationRequest;
import org.eclipse.gef.tools.DirectEditManager;

import uk.ac.bolton.archimate.editor.diagram.directedit.LabelCellEditorLocator;
import uk.ac.bolton.archimate.editor.diagram.directedit.LabelDirectEditManager;
import uk.ac.bolton.archimate.editor.diagram.editparts.AbstractConnectedEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.IColoredEditPart;
import uk.ac.bolton.archimate.editor.diagram.editparts.ITextEditPart;
import uk.ac.bolton.archimate.editor.diagram.figures.IDiagramModelObjectFigure;
import uk.ac.bolton.archimate.editor.diagram.figures.IEditableLabelFigure;
import uk.ac.bolton.archimate.editor.diagram.policies.PartComponentEditPolicy;
import uk.ac.bolton.archimate.editor.diagram.policies.PartDirectEditTitlePolicy;
import uk.ac.bolton.archimate.editor.diagram.sketch.figures.SketchActorFigure;
import uk.ac.bolton.archimate.editor.diagram.sketch.policies.SketchConnectionPolicy;
import uk.ac.bolton.archimate.editor.ui.ViewManager;
import uk.ac.bolton.archimate.model.ISketchModelActor;


/**
 * Sketch Actor Edit Part
 * 
 * @author Phillip Beauvoir
 */
public class SketchActorEditPart extends AbstractConnectedEditPart
implements IColoredEditPart, ITextEditPart  {
    
    private DirectEditManager fDirectEditManager;
    private ConnectionAnchor fAnchor;

    @Override
    protected IFigure createFigure() {
        SketchActorFigure figure = new SketchActorFigure((ISketchModelActor)getModel());
        return figure;
    }

    @Override
    protected void createEditPolicies() {
        // Allow parts to be connected together
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new SketchConnectionPolicy());
        
        // Add a policy to handle directly editing the Part
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new PartDirectEditTitlePolicy());

        // Add a policy to handle editing the Parts (for example, deleting a part)
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new PartComponentEditPolicy());
    }

    @Override
    protected void refreshFigure() {
        // Refresh the figure if necessary
        ((IDiagramModelObjectFigure)getFigure()).refreshVisuals();
    }

    @Override
    public void performRequest(Request request) {
        if(request.getType() == RequestConstants.REQ_OPEN) {
            // Edit the label if we clicked on it
            if(((IEditableLabelFigure)getFigure()).didClickLabel(((LocationRequest)request).getLocation().getCopy())) {
                if(fDirectEditManager == null) {
                    Label label = ((IEditableLabelFigure)getFigure()).getLabel();
                    fDirectEditManager = new LabelDirectEditManager(this, new LabelCellEditorLocator(label), label);
                }
                fDirectEditManager.show();
            }
            else {
                ViewManager.showViewPart(ViewManager.PROPERTIES_VIEW, true);
            }
        }
    }
    
    @Override
    protected ConnectionAnchor getConnectionAnchor() {
        if(fAnchor == null) {
            fAnchor = new ChopboxAnchor(getFigure());
        }
        return fAnchor;
    }

}
