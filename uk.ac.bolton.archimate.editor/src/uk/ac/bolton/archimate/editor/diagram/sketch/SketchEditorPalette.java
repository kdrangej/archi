/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.sketch;

import java.util.Hashtable;
import java.util.Map.Entry;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PaletteToolbar;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.tools.MarqueeSelectionTool;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import uk.ac.bolton.archimate.editor.diagram.DiagramConstants;
import uk.ac.bolton.archimate.editor.diagram.DiagramModelFactory;
import uk.ac.bolton.archimate.editor.diagram.tools.FormatPainterToolEntry;
import uk.ac.bolton.archimate.editor.diagram.tools.PanningSelectionExtendedTool;
import uk.ac.bolton.archimate.editor.ui.ColorFactory;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.editor.ui.ImageFactory;
import uk.ac.bolton.archimate.model.IArchimatePackage;


/**
 * PaletteRoot for Sketch
 * 
 * @author Phillip Beauvoir
 */
public class SketchEditorPalette extends PaletteRoot {
    
    private FormatPainterToolEntry formatPainterEntry;
    
    private Hashtable<Color, StickyImageDescriptor> fImageTable = new Hashtable<Color, StickyImageDescriptor>();
    
    public SketchEditorPalette() {
        createControlsGroup();
        add(new PaletteSeparator(""));
        
        createElementsGroup();
        add(new PaletteSeparator(""));
        
        createStickiesGroup();
        add(new PaletteSeparator(""));
        
        createConnectionsGroup();
        add(new PaletteSeparator(""));
    }

    /**
     * Create a Group of Controls
     */
    private PaletteContainer createControlsGroup() {
        PaletteContainer group = new PaletteToolbar("Tools");
        add(group);
        
        // The selection tool
        ToolEntry tool = new PanningSelectionToolEntry();
        tool.setToolClass(PanningSelectionExtendedTool.class);
        group.add(tool);

        // Use selection tool as default entry
        setDefaultEntry(tool);

        // Marquee selection tool to select nodes and connections
        MarqueeToolEntry marquee = new MarqueeToolEntry();
        marquee.setToolProperty(MarqueeSelectionTool.PROPERTY_MARQUEE_BEHAVIOR, 
                new Integer(MarqueeSelectionTool.BEHAVIOR_NODES_AND_CONNECTIONS));
        group.add(marquee);
        
        // Marquee selection tool to select connections only
        marquee = new MarqueeToolEntry();
        marquee.setToolProperty(MarqueeSelectionTool.PROPERTY_MARQUEE_BEHAVIOR, 
                new Integer(MarqueeSelectionTool.BEHAVIOR_CONNECTIONS_TOUCHED));
        group.add(marquee);
        
        // Format Painter
        formatPainterEntry = new FormatPainterToolEntry();
        group.add(formatPainterEntry);

        return group;
    }

    private PaletteContainer createElementsGroup() {
        PaletteContainer group = new PaletteGroup("Elements");
        add(group);
        
        group.add(createCombinedTemplateCreationEntry(IArchimatePackage.eINSTANCE.getSketchModelActor(), "Actor", null));
        
        // Group
        PaletteEntry groupEntry = new CombinedTemplateCreationEntry(
                "Group",
                "Grouping Element",
                new DiagramModelFactory(IArchimatePackage.eINSTANCE.getDiagramModelGroup()),
                IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_GROUP_16),
                IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_GROUP_16));
        group.add(groupEntry);
    
        return group;
    }
    
    private PaletteContainer createStickiesGroup() {
        PaletteContainer group = new PaletteToolbar("Stickies");
        add(group);
        
        // Sticky Notes
        group.add(createStickyEntry(ColorFactory.COLOR_BUSINESS));
        group.add(createStickyEntry(ColorFactory.COLOR_APPLICATION));
        group.add(createStickyEntry(ColorFactory.COLOR_TECHNOLOGY));
        group.add(createStickyEntry(ColorConstants.orange));
        group.add(createStickyEntry(ColorConstants.yellow));
        group.add(createStickyEntry(ColorConstants.lightGreen));
        group.add(createStickyEntry(ColorConstants.lightBlue));
        group.add(createStickyEntry(ColorConstants.white));
        
        return group;
    }
    
    private PaletteContainer createConnectionsGroup() {
        PaletteContainer group = new PaletteGroup("Connections");
        add(group);
        
        ConnectionCreationToolEntry entry = createConnectionCreationToolEntry(
                IArchimatePackage.eINSTANCE.getDiagramModelConnection(),
                DiagramConstants.CONNECTION_LINE,
                "Line Connection",
                null);
        group.add(entry);
        
        entry = createConnectionCreationToolEntry(
                IArchimatePackage.eINSTANCE.getDiagramModelConnection(),
                DiagramConstants.CONNECTION_ARROW,
                "Arrow Connection",
                null);
        group.add(entry);
        
        entry = createConnectionCreationToolEntry(
                IArchimatePackage.eINSTANCE.getDiagramModelConnection(),
                DiagramConstants.CONNECTION_DASHED_ARROW,
                "Dashed Connection",
                null);
        group.add(entry);
        
        return group;
    }
    
    private ConnectionCreationToolEntry createConnectionCreationToolEntry(EClass eClass, String type, String name, String description) {
        ConnectionCreationToolEntry entry = new ConnectionCreationToolEntry(
                name,
                description,
                new SketchModelFactory(eClass, type),
                ImageFactory.getImageDescriptor(eClass, type),
                ImageFactory.getImageDescriptor(eClass, type));
        
        // Ensure Tool gets deselected
        entry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, true);
        return entry;
    }

    private CombinedTemplateCreationEntry createCombinedTemplateCreationEntry(EClass eClass, String name, String description) {
        return new CombinedTemplateCreationEntry(
                name,
                description,
                new SketchModelFactory(eClass),
                ImageFactory.getImageDescriptor(eClass),
                ImageFactory.getImageDescriptor(eClass));
    }

    private PaletteEntry createStickyEntry(Color color) {
        return new CombinedTemplateCreationEntry(
                "Sticky",
                null,
                new SketchModelFactory(IArchimatePackage.eINSTANCE.getSketchModelSticky(), color),
                getStickyImageDescriptor(color),
                getStickyImageDescriptor(color));
    }
    
    private ImageDescriptor getStickyImageDescriptor(Color color) {
        StickyImageDescriptor id = fImageTable.get(color);
        if(id == null) {
            id = new StickyImageDescriptor(color);
            fImageTable.put(color, id);
        }
        return id;
    }
    
    public void dispose() {
        for(Entry<Color, StickyImageDescriptor> entry : fImageTable.entrySet()) {
            entry.getValue().image.dispose();
        }
        
        formatPainterEntry.dispose();
    }
    
    class StickyImageDescriptor extends ImageDescriptor {
        Image image;
        
        StickyImageDescriptor(Color color) {
            image = new Image(Display.getDefault(), 16, 14);
            GC gc = new GC(image);
            SWTGraphics graphics = new SWTGraphics(gc);
            graphics.setBackgroundColor(color);
            graphics.fillRectangle(0, 0, 15, 13);
            graphics.drawRectangle(0, 0, 15, 13);
            gc.dispose();
            graphics.dispose();
        }
        
        @Override
        public ImageData getImageData() {
            return image.getImageData();
        }
        
        public void dispose() {
            if(image != null && !image.isDisposed()) {
                image.dispose();
            }
        }
    }
}
