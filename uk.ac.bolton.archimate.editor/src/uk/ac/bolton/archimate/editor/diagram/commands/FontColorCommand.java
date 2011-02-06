/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.commands;

import uk.ac.bolton.archimate.editor.model.commands.EObjectFeatureCommand;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IFontAttribute;


/**
 * Font Color Command
 *
 * @author Phillip Beauvoir
 */
public class FontColorCommand extends EObjectFeatureCommand {
    
    public FontColorCommand(IFontAttribute object, String rgb) {
        super("Change font", object, IArchimatePackage.Literals.FONT_ATTRIBUTE__FONT_COLOR, rgb);
    }
    
    @Override
    public boolean canExecute() {
        return (fNewValue != null) ? !fNewValue.equals(fOldValue)
                : (fOldValue != null) ? !fOldValue.equals(fNewValue)
                : false;
    }

}