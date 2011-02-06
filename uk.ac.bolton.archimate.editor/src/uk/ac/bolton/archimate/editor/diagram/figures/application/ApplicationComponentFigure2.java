/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.figures.application;

import org.eclipse.swt.graphics.Image;

import uk.ac.bolton.archimate.editor.diagram.figures.AbstractRectangleFigure;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateObject;

/**
 * Figure for an Application Component
 * 
 * @author Phillip Beauvoir
 */
public class ApplicationComponentFigure2 extends AbstractRectangleFigure {
    
    public ApplicationComponentFigure2(IDiagramModelArchimateObject diagramModelObject) {
        super(diagramModelObject);
    }


    @Override
    protected Image getImage() {
        return IArchimateImages.ImageFactory.getImage(IArchimateImages.ICON_APPLICATION_COMPONENT_16);
    }
}