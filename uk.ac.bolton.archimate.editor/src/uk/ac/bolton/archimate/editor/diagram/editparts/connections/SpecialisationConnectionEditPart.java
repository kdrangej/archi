/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.editparts.connections;

import org.eclipse.draw2d.IFigure;

import uk.ac.bolton.archimate.editor.diagram.figures.connections.SpecialisationConnectionFigure;
import uk.ac.bolton.archimate.model.IDiagramModelConnection;


/**
 * Specialisation Connection Edit Part
 * 
 * @author Phillip Beauvoir
 */
public class SpecialisationConnectionEditPart extends AbstractArchimateConnectionEditPart {
	
    @Override
    protected IFigure createFigure() {
		return new SpecialisationConnectionFigure((IDiagramModelConnection)getModel());
	}
	
}
