/*******************************************************************************
 * Copyright (c) 2011 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import uk.ac.bolton.archimate.editor.utils.PlatformUtils;


/**
 * Wraps a StyledText Control to listen for hyperlinks
 * A lot of this code is adapted from org.eclipse.ui.internal.about.AboutTextManager
 * 
 * @author Phillip Beauvoir
 */
public class StyledTextControl implements Listener, LineStyleListener {
    
    // Previous versions
    // "(http|https|ftp)://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?"    // Original
    // "(http|https|ftp)://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%~&=]*)?"   // Added ~
    // "(http|https|ftp)://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%~&=]*)?"    // Removed space
    
    private static final String regex = "(http|https|ftp)://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%~&=\\(\\)]*)?";  // added \\( and \\)
    private static Pattern pattern = Pattern.compile(regex);
    
    private StyledText fStyledText;
    
    private Cursor fHandCursor, fBusyCursor;
    private Cursor fCurrentCursor;
    
    private List<int[]> fLinkRanges;
    private List<String> fLinks;
    
    private boolean fMouseDown;
    private boolean fDragEvent;
    
    private IAction fActionSelectAll = new Action("Select All") {
        @Override
        public void run() {
            fStyledText.selectAll();
        }
    };
    
    private IAction fActionCut = new Action("Cut") {
        @Override
        public void run() {
            fStyledText.cut();
        }
    };
    
    private IAction fActionCopy = new Action("Copy") {
        @Override
        public void run() {
            fStyledText.copy();
        }
    };
    
    private IAction fActionPaste = new Action("Paste") {
        @Override
        public void run() {
            fStyledText.paste();
        }
    };
    
    private IAction fActionDelete = new Action("Delete") {
        @Override
        public void run() {
            fStyledText.invokeAction(SWT.DEL);
        }
    };

    public StyledTextControl(Composite parent, int style) {
        this(new StyledText(parent, style));
    }
    
    public StyledTextControl(StyledText styledText) {
        fStyledText = styledText;
        fStyledText.setLeftMargin(PlatformUtils.isWindows() ? 4 : 2);
        fStyledText.setKeyBinding(ST.SELECT_ALL, ST.SELECT_ALL);
        
        fHandCursor = new Cursor(styledText.getDisplay(), SWT.CURSOR_HAND);
        fBusyCursor = new Cursor(styledText.getDisplay(), SWT.CURSOR_WAIT);
        
        fStyledText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                fHandCursor.dispose();
                fBusyCursor.dispose();
                
                fStyledText.removeListener(SWT.MouseDown, StyledTextControl.this);
                fStyledText.removeListener(SWT.MouseUp, StyledTextControl.this);
                fStyledText.removeListener(SWT.MouseMove, StyledTextControl.this);
                fStyledText.getDisplay().removeFilter(SWT.KeyDown, StyledTextControl.this);
                fStyledText.getDisplay().removeFilter(SWT.KeyUp, StyledTextControl.this);
                
                fStyledText.removeLineStyleListener(StyledTextControl.this);
                
                fHandCursor = null;
                fBusyCursor = null;
                fCurrentCursor = null;
                fLinks = null;
            }
        });
        
        fStyledText.addListener(SWT.MouseDown, this);
        fStyledText.addListener(SWT.MouseUp, this);
        fStyledText.addListener(SWT.MouseMove, this);
        fStyledText.getDisplay().addFilter(SWT.KeyDown, this);
        fStyledText.getDisplay().addFilter(SWT.KeyUp, this);
        
        fStyledText.addLineStyleListener(this);
        
        hookContextMenu();
    }
    
    @Override
    public void lineGetStyle(LineStyleEvent event) {
        // Do this on any text change because it will be needed for mouse over
        scanLinks(fStyledText.getText());
        
        int lineLength = event.lineText.length();
        if(lineLength < 8) {
            return; // optimise
        }
        
        int lineOffset = event.lineOffset;
        
        List<StyleRange> list = new ArrayList<StyleRange>();
        
        for(int[] linkRange : fLinkRanges) {
            int start = linkRange[0];
            int length = linkRange[1];
            if(start >= lineOffset && (start + length) <= (lineOffset + lineLength)) {
                StyleRange sr = new StyleRange(start, length, ColorConstants.blue, null);
                sr.underline = true;
                list.add(sr);
            }
        }
        
        if(!list.isEmpty()) {
            event.styles = list.toArray(new StyleRange[list.size()]);
        }
    }
    
    /**
     * Hook into a right-click menu
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu1"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        
        Menu menu = menuMgr.createContextMenu(fStyledText);
        fStyledText.setMenu(menu);
    }
    
    /**
     * Fill context menu when user right-clicks
     * @param manager
     */
    private void fillContextMenu(IMenuManager manager) {
        int textLength = fStyledText.getText().length();
        boolean hasText = textLength > 0;
        boolean hasSelectedText = fStyledText.getSelectionText().length() > 0;
        
        // Cut
        fActionCut.setEnabled(hasSelectedText);
        manager.add(fActionCut);
        
        // Copy
        fActionCopy.setEnabled(hasSelectedText);
        manager.add(fActionCopy);
        
        // Paste
        Clipboard cb = new Clipboard(null);
        Object content = cb.getContents(TextTransfer.getInstance());
        cb.dispose();
        fActionPaste.setEnabled(content != null);
        manager.add(fActionPaste);
        
        // Delete
        fActionDelete.setEnabled(hasSelectedText);
        manager.add(fActionDelete);
        
        // Select All
        manager.add(new Separator());
        fActionSelectAll.setEnabled(hasText);
        manager.add(fActionSelectAll);
    }
    
    public StyledText getControl() {
        return fStyledText;
    }
    
    public String setText() {
        return fStyledText.getText();
    }
    
    /**
     * Scan links using method 1
     */
    @SuppressWarnings("unused")
    private void scanLinksOld(String s) {
        fLinkRanges = new ArrayList<int[]>();
        fLinks = new ArrayList<String>();
        
        int urlSeparatorOffset = s.indexOf("://");
        while(urlSeparatorOffset >= 0) {
            // URL protocol (left to "://")
            int urlOffset = urlSeparatorOffset;
            char ch;
            do {
                urlOffset--;
                ch = ' ';
                if(urlOffset > -1) {
                    ch = s.charAt(urlOffset);
                }
            }
            while(Character.isUnicodeIdentifierStart(ch));
            urlOffset++;

            // Right to "://"
            StringTokenizer tokenizer = new StringTokenizer(s.substring(urlSeparatorOffset + 3), " \t\n\r\f<>", false);
            if(!tokenizer.hasMoreTokens()) {
                return;
            }

            int urlLength = tokenizer.nextToken().length() + 3 + urlSeparatorOffset - urlOffset;

            fLinkRanges.add(new int[]{urlOffset, urlLength});
            fLinks.add(s.substring(urlOffset, urlOffset + urlLength));

            urlSeparatorOffset = s.indexOf("://", urlOffset + urlLength + 1);
        }
    }

    /**
     * Scan links using method
     */
    private void scanLinks(String s) {
        fLinkRanges = new ArrayList<int[]>();
        fLinks = new ArrayList<String>();
        
        Matcher matcher = pattern.matcher(s);
        while(matcher.find()) {
            String group = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            
            fLinkRanges.add(new int[]{start, end - start});
            fLinks.add(group);
        }
    }
    
    /**
     * Returns true if a link is present at the given character location
     */
    private boolean isLinkAt(int offset) {
        for(int[] linkRange : fLinkRanges) {
            int start = linkRange[0];
            int length = linkRange[1];
            if(offset >= start && offset < start + length) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Returns the link at the given offset (if there is one),
     * otherwise returns null
     */
    private String getLinkAt(int offset) {
        for(int i = 0; i < fLinkRanges.size(); i++) {
            int start = fLinkRanges.get(i)[0];
            int length = fLinkRanges.get(i)[1];
            if(offset >= start && offset < start + length) {
                return fLinks.get(i);
            }
        }
        return null;
    }

    private void openLink(Shell shell, String href) {
        // format the href for an html file (file:///<filename.html>
        // required for Mac only.
        if(href.startsWith("file:")) {
            href = href.substring(5);
            while(href.startsWith("/")) {
                href = href.substring(1);
            }
            href = "file:///" + href;
        }
        
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
        try {
            IWebBrowser browser = support.getExternalBrowser();
            browser.openURL(new URL(urlEncodeForSpaces(href.toCharArray())));
        }
        catch(MalformedURLException ex) {
            ex.printStackTrace();
        }
        catch(PartInitException ex) {
            ex.printStackTrace();
        }
    }

    private String urlEncodeForSpaces(char[] input) {
        StringBuffer retu = new StringBuffer(input.length);
        for(int i = 0; i < input.length; i++) {
            if(input[i] == ' ') {
                retu.append("%20"); //$NON-NLS-1$
            }
            else {
                retu.append(input[i]);
            }
        }
        return retu.toString();
    }

    
    @Override
    public void handleEvent(Event event) {
        switch(event.type) {
            case SWT.MouseDown:
                doMouseDown(event);
                break;
            case SWT.MouseUp:
                doMouseUp(event);
                break;
            case SWT.MouseMove:
                doMouseMove(event);
                break;
            case SWT.KeyDown:
                doKeyDown(event);
                break;
            case SWT.KeyUp:
                doKeyUp(event);
                break;
        }
    }

    /**
     * Mouse Up
     */
    private void doMouseDown(Event e) {
        if(e.button != 1) {
            return;
        }
        fMouseDown = true;
    }
    
    /**
     * Mouse Down
     */
    private void doMouseUp(Event e) {
        fMouseDown = false;
        
        int offset;
        try {
            offset = fStyledText.getOffsetAtLocation(new Point(e.x, e.y));
        }
        catch(IllegalArgumentException ex) {
            fDragEvent = false; // must do this
            return;
        }
        
        boolean modKey = (e.stateMask & SWT.MOD1) != 0;
        
        if(fDragEvent) {
            // don't activate a link during a drag/mouse up operation
            fDragEvent = false;
            if(modKey && isLinkAt(offset)) {
                setCursor(fHandCursor);
            }
        }
        else if(modKey && isLinkAt(offset)) {
            fStyledText.setCursor(fBusyCursor);
            openLink(fStyledText.getShell(), getLinkAt(offset));
            setCursor(null);
        }
    }
    
    /**
     * Mouse Move
     */
    private void doMouseMove(Event e) {
        // Do not change cursor on drag events
        if(fMouseDown) {
            if(!fDragEvent) {
                setCursor(null);
            }
            fDragEvent = true;
            return;
        }

        int offset;
        try {
            offset = fStyledText.getOffsetAtLocation(new Point(e.x, e.y));
        }
        catch(IllegalArgumentException ex) {
            setCursor(null);
            return;
        }

        boolean modKey = (e.stateMask & SWT.MOD1) != 0;
        
        if(modKey && isLinkAt(offset)) {
            setCursor(fHandCursor);
        }
        else {
            setCursor(null);
        }
    }
    
    /**
     * Key down
     */
    private void doKeyDown(Event e) {
        if(e.keyCode == SWT.MOD1) {
            Point pt = fStyledText.getDisplay().getCursorLocation();
            pt = fStyledText.toControl(pt);
            int offset;
            try {
                offset = fStyledText.getOffsetAtLocation(pt);
            }
            catch(IllegalArgumentException ex) {
                return;
            }
            if(isLinkAt(offset)) {
                setCursor(fHandCursor);
            }
        }
    }

    /**
     * Key up
     */
    private void doKeyUp(Event e) {
        if(e.keyCode == SWT.MOD1) {
            setCursor(null);
        }
    }
    
    /**
     * Optimise setting cursor 1000s of times
     */
    private void setCursor(Cursor cursor) {
        if(fCurrentCursor != cursor) {
            fStyledText.setCursor(cursor);
            fCurrentCursor = cursor;
        }
    }
}
