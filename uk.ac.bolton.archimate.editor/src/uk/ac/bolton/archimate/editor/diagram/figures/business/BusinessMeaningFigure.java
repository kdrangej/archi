/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.figures.business;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Image;

import uk.ac.bolton.archimate.editor.diagram.figures.AbstractEditableTextFlowFigure;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateObject;



/**
 * Business Meaning Figure
 * 
 * @author Phillip Beauvoir
 */
public class BusinessMeaningFigure extends AbstractEditableTextFlowFigure {

    int SHADOW_OFFSET = 2;
    
    public BusinessMeaningFigure(IDiagramModelArchimateObject diagramModelObject) {
        super(diagramModelObject);
    }
    
    @Override
    public void drawFigure(Graphics graphics) {
        Rectangle bounds = getBounds().getCopy();
        
        // The following is the most awful code to draw a cloud...
        
        // Shadow fill
        graphics.setAlpha(100);
        graphics.setBackgroundColor(ColorConstants.black);
        graphics.fillOval(bounds.x + bounds.width / 3 + 1, bounds.y + 1, bounds.width / 3 * 2, bounds.height / 3 * 2);
        graphics.fillOval(bounds.x, bounds.y + bounds.height / 3, bounds.width / 5 * 3, bounds.height / 3 * 2);
        graphics.fillOval(bounds.x + bounds.width / 3, bounds.y + bounds.height / 4, bounds.width / 5 * 3, bounds.height / 3 * 2);
    
        // Main fill
        graphics.setAlpha(255);
        graphics.setBackgroundColor(getFillColor());
        graphics.fillOval(bounds.x, bounds.y, bounds.width / 3 * 2, bounds.height / 3 * 2);
        graphics.fillOval(bounds.x + bounds.width / 3 - 1, bounds.y, bounds.width / 3 * 2, bounds.height / 3 * 2);
        graphics.fillOval(bounds.x, bounds.y + bounds.height / 3, bounds.width / 5 * 3, bounds.height / 3 * 2 - SHADOW_OFFSET);
        graphics.fillOval(bounds.x + bounds.width / 3, bounds.y + bounds.height / 4, bounds.width / 5 * 3 - SHADOW_OFFSET, bounds.height / 3 * 2 - SHADOW_OFFSET);
        
        // Outline
        graphics.setForegroundColor(ColorConstants.black);
        graphics.drawArc(bounds.x, bounds.y, bounds.width / 3 * 2, bounds.height / 3 * 2, 60, 147);
        graphics.drawArc(bounds.x + bounds.width / 3 - 1, bounds.y, bounds.width / 3 * 2 - 1, bounds.height / 3 * 2, -40, 159);
        graphics.drawArc(bounds.x, bounds.y + bounds.height / 3, bounds.width / 5 * 3 - 1, bounds.height / 3 * 2 - SHADOW_OFFSET - 1, -43, -167);
        graphics.drawArc(bounds.x + bounds.width / 3, bounds.y + bounds.height / 4, bounds.width / 5 * 3 - SHADOW_OFFSET - 1, bounds.height / 3 * 2 - SHADOW_OFFSET - 1, 0, -110);
    }
    
    @Override
    protected void drawTargetFeedback(Graphics graphics) {
        graphics.pushState();
        graphics.setForegroundColor(ColorConstants.blue);
        graphics.setLineWidth(2);
        graphics.drawRectangle(new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - SHADOW_OFFSET - 1));
        graphics.popState();
    }

    public Rectangle calculateTextControlBounds() {
        Rectangle bounds = getBounds().getCopy();
        bounds.x += 20;
        bounds.y += 10;
        bounds.width = bounds.width - 40;
        bounds.height -= 10;
        
        translateFromParent(bounds);
        
        return bounds;
    }

    @Override
    protected Image getImage() {
        return null;
    }
}
