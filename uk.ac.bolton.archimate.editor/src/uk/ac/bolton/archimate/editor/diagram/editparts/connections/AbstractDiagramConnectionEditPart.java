/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.editparts.connections;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Bendpoint;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ManhattanConnectionRouter;
import org.eclipse.draw2d.RelativeBendpoint;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.LocationRequest;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.util.IPropertyChangeListener;

import uk.ac.bolton.archimate.editor.diagram.commands.DiagramCommandFactory;
import uk.ac.bolton.archimate.editor.diagram.directedit.LabelCellEditorLocator;
import uk.ac.bolton.archimate.editor.diagram.directedit.LabelDirectEditManager;
import uk.ac.bolton.archimate.editor.diagram.figures.connections.IDiagramConnectionFigure;
import uk.ac.bolton.archimate.editor.diagram.policies.ManualBendpointEditPolicy;
import uk.ac.bolton.archimate.editor.model.commands.EObjectFeatureCommand;
import uk.ac.bolton.archimate.editor.preferences.IPreferenceConstants;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.editor.ui.ViewManager;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModelBendpoint;
import uk.ac.bolton.archimate.model.IDiagramModelConnection;


/**
 * Abstract Diagram Connection EditPart
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractDiagramConnectionEditPart extends AbstractConnectionEditPart
implements IDiagramConnectionEditPart {

    private DirectEditManager fDirectEditManager;
    
    protected Adapter fConnectionAdapter = new AdapterImpl() {
        @Override
        public void notifyChanged(Notification msg) {
            switch(msg.getEventType()) {
                case Notification.ADD:
                case Notification.ADD_MANY:
                case Notification.REMOVE:
                case Notification.REMOVE_MANY:
                case Notification.MOVE:
                case Notification.SET:
                    refreshVisuals();
                    break;

                default:
                    break;
            }
        }
    };
    
    /**
     * Listen to default font change in Prefs
     */
    private IPropertyChangeListener prefsListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
            if(IPreferenceConstants.DEFAULT_VIEW_FONT.equals(event.getProperty())) {
                refreshVisuals();
            }
        }
    };
    
    /**
     * Figure Listener 
     */
    private PropertyChangeListener figureListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // Connection Router change
            String property = evt.getPropertyName();
            if(Connection.PROPERTY_CONNECTION_ROUTER.equals(property)){
                refreshBendpoints();
                refreshBendpointEditPolicy();
            }
        }
    };
    
    @Override
    public void activate() {
        if(!isActive()) {
            super.activate();
            
            // Listen to changes in Diagram Model Object
            ((IDiagramModelConnection)getModel()).eAdapters().add(fConnectionAdapter);
            
            // Listen to Prefs changes to set default Font
            Preferences.STORE.addPropertyChangeListener(prefsListener);
        }
    }
    
    @Override
    public void deactivate() {
        if(isActive()) {
            super.deactivate();
            
            // Remove Listener to changes in Diagram Model Object
            ((IDiagramModelConnection)getModel()).eAdapters().remove(fConnectionAdapter);
            
            Preferences.STORE.removePropertyChangeListener(prefsListener);
            
            // Dispose of figure
            ((IDiagramConnectionFigure)getFigure()).dispose();
        }
    }
    
    @Override
    public void activateFigure(){
        super.activateFigure();
        // Once the figure has been added to the ConnectionLayer, start listening for its router to change.
        getFigure().addPropertyChangeListener(Connection.PROPERTY_CONNECTION_ROUTER, figureListener);
    }
    
    @Override
    public void deactivateFigure(){
        // Remove listener
        getFigure().removePropertyChangeListener(Connection.PROPERTY_CONNECTION_ROUTER, figureListener);
        super.deactivateFigure();
    }

    @Override
    public void performRequest(Request request) {
        if(request.getType() == RequestConstants.REQ_OPEN) {
            // Edit relationship label if we clicked on it
            if(((IDiagramConnectionFigure)getFigure()).didClickConnectionLabel(((LocationRequest)request).getLocation().getCopy())) {
                Label label = ((IDiagramConnectionFigure)getFigure()).getConnectionLabel();
                if(fDirectEditManager == null) {
                    fDirectEditManager = new LabelDirectEditManager(this, new LabelCellEditorLocator(label), label);
                }
                fDirectEditManager.show();
            }
            else {
                handleEditRequest(request); 
            }
        }
    }
    
    @Override
    protected void createEditPolicies() {
        // Selection handle edit policy. 
        // Makes the connection show a feedback, when selected by the user.
        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new ConnectionEndpointEditPolicy());
        
        // Allows the removal of the connection model element
        installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionEditPolicy() {
            @Override
            protected Command getDeleteCommand(GroupRequest request) {
                return DiagramCommandFactory.createDeleteDiagramConnectionCommand((IDiagramModelConnection)getModel());
            }
        });
        
        // Add a policy to handle directly editing the connection label
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new ConnectionDirectEditTextPolicy());
        
        // Add a policy for manual bendpoints
        refreshBendpointEditPolicy();
    }

    protected void handleEditRequest(Request request) {
        // Show Properties View
        ViewManager.showViewPart(ViewManager.PROPERTIES_VIEW, true);
    }

    @Override
    protected void refreshVisuals() {
        // This will need to be updated when user changes source or target end-connection
        // as well as on Property change.
        ((IDiagramConnectionFigure)getFigure()).refreshVisuals();
        
        refreshBendpoints();
    }
    
    /**
     * Updates the bendpoints, based on the model
     */
    protected void refreshBendpoints() {
        if(getConnectionFigure().getConnectionRouter() instanceof ManhattanConnectionRouter) {
            return;
        }
        
        List<Bendpoint> figureConstraint = new ArrayList<Bendpoint>();
        
        EList<IDiagramModelBendpoint> bendpoints = ((IDiagramModelConnection)getModel()).getBendpoints();
        for(int i = 0; i < bendpoints.size(); i++) {
            IDiagramModelBendpoint bendpoint = bendpoints.get(i);
            
            RelativeBendpoint rbp = new RelativeBendpoint(getConnectionFigure());
            Dimension dim1 = new Dimension(bendpoint.getStartX(), bendpoint.getStartY());
            Dimension dim2 = new Dimension(bendpoint.getEndX(), bendpoint.getEndY());
            rbp.setRelativeDimensions(dim1, dim2);
            rbp.setWeight((i + 1) / ((float)bendpoints.size() + 1));
            
            figureConstraint.add(rbp);
        }
        
        getConnectionFigure().setRoutingConstraint(figureConstraint);
    }
    
    protected void refreshBendpointEditPolicy(){
        // Doesn't work for Manhattan Router
        if(getConnectionFigure().getConnectionRouter() instanceof ManhattanConnectionRouter) {
            installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, null);
        }
        else {
            installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, new ManualBendpointEditPolicy());
        }
    }
        
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if(adapter == IDiagramModelConnection.class) {
            return getModel();
        }
        return super.getAdapter(adapter);
    }
    
    /**
     * Direct Edit Policy
     */
    private class ConnectionDirectEditTextPolicy extends DirectEditPolicy {

        @Override
        protected Command getDirectEditCommand(DirectEditRequest request) {
            String name = (String)request.getCellEditor().getValue();
            IDiagramModelConnection connection = (IDiagramModelConnection)getModel();
            return new EObjectFeatureCommand("Connection text", connection, IArchimatePackage.Literals.DIAGRAM_MODEL_CONNECTION__TEXT, name);
        }

        @Override
        protected void showCurrentEditValue(DirectEditRequest request) {
//          String value = (String)request.getCellEditor().getValue();
//          
//          if(getHostFigure() instanceof IArchimateConnectionFigure) {
//              ((IArchimateConnectionFigure)getHostFigure()).setConnectionText(value);
//          }
        }
    }

}
