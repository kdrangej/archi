/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.diagram.actions;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPart;

import uk.ac.bolton.archimate.editor.ArchimateEditorPlugin;
import uk.ac.bolton.archimate.editor.ui.ArchimateNames;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.model.FolderType;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IArchimateFactory;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateConnection;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateObject;
import uk.ac.bolton.archimate.model.IFolder;
import uk.ac.bolton.archimate.model.IRelationship;
import uk.ac.bolton.archimate.model.util.DerivedRelationsUtils;


/**
 * Create Derived Relation Action
 * 
 * @author Phillip Beauvoir
 */
public class CreateDerivedRelationAction extends SelectionAction {
    
    public static final String ID = "CreateDerivedRelationAction";
    public static final String TEXT = "Create Derived Relation...";

    public CreateDerivedRelationAction(IWorkbenchPart part) {
        super(part);
        setText(TEXT);
        setId(ID);
        setSelectionProvider((ISelectionProvider)part.getAdapter(GraphicalViewer.class));
        setImageDescriptor(IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_DERIVED_16));
    }

    @Override
    protected boolean calculateEnabled() {
        List<?> selection = getSelectedObjects();
        
        if(selection.size() != 2) {
            return false;
        }
        
        for(Object object : selection) {
            if(!(object instanceof EditPart)) {
                return false;
            }
            EditPart part = (EditPart)object;
            if(!(part.getModel() instanceof IDiagramModelArchimateObject)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void run() {
        List<?> selection = getSelectedObjects();
        
        EditPart editPart = (EditPart)selection.get(0);
        IDiagramModelArchimateObject diagramModelObject1 = (IDiagramModelArchimateObject)editPart.getModel();
        editPart = (EditPart)selection.get(1);
        IDiagramModelArchimateObject diagramModelObject2 = (IDiagramModelArchimateObject)editPart.getModel();
        
        ChainList chainList1 = new ChainList(diagramModelObject1, diagramModelObject2);
        ChainList chainList2 = new ChainList(diagramModelObject2, diagramModelObject1);
        
        // Already has a direct relationship
        if(chainList1.hasExistingDirectRelationship() || chainList2.hasExistingDirectRelationship()) {
            MessageDialog.openInformation(getWorkbenchPart().getSite().getShell(),
                    "Derived Relation",
                    "There is already a direct relation.");
            return;
        }
        
        // None found
        if(chainList1.getChains() == null && chainList2.getChains() == null) {
            MessageDialog.openInformation(getWorkbenchPart().getSite().getShell(),
                    "Derived Relation",
                    "No derived relation found.");
            return;
        }
        
        CreateDerivedConnectionDialog dialog = new CreateDerivedConnectionDialog(getWorkbenchPart().getSite().getShell(),
                chainList1, chainList2);
        
        if(dialog.open() == IDialogConstants.OK_ID) {
            List<IRelationship> chain = dialog.getSelectedChain();
            if(chain != null) {
                ChainList chainList = dialog.getSelectedChainList();
                EClass relationshipClass = DerivedRelationsUtils.getWeakestType(chain);
                IRelationship relation = (IRelationship)IArchimateFactory.eINSTANCE.create(relationshipClass);
                CommandStack stack = (CommandStack)getWorkbenchPart().getAdapter(CommandStack.class);
                stack.execute(new CreateDerivedConnectionCommand(chainList.srcDiagramObject, chainList.tgtDiagramObject, relation));
            }
        }
    }
    
    /**
     * Convenience class to group things together
     */
    private static class ChainList {
        IDiagramModelArchimateObject srcDiagramObject;
        IDiagramModelArchimateObject tgtDiagramObject;
        IArchimateElement srcElement;
        IArchimateElement tgtElement;
        List<List<IRelationship>> chains;
        
        ChainList(IDiagramModelArchimateObject srcDiagramObject, IDiagramModelArchimateObject tgtDiagramObject) {
            this.srcDiagramObject = srcDiagramObject;
            this.tgtDiagramObject = tgtDiagramObject;
            srcElement = srcDiagramObject.getArchimateElement();
            tgtElement = tgtDiagramObject.getArchimateElement();
        }
        
        boolean hasExistingDirectRelationship() {
            return DerivedRelationsUtils.hasDirectStructuralRelationship(srcElement, tgtElement);
        }
        
        List<List<IRelationship>> getChains() {
            if(chains == null) {
                chains = DerivedRelationsUtils.getDerivedRelationshipChains(srcElement, tgtElement);
            }
            return chains;
        }
    }
    
    /**
     * Dialog window
     */
    private static class CreateDerivedConnectionDialog extends Dialog implements ISelectionChangedListener, IDoubleClickListener {
        // For persisting dialog position and size
        private static final String DIALOG_SETTINGS_SECTION = "CreateDerivedConnectionDialog"; //$NON-NLS-1$

        private ChainList chainList1, chainList2;
        private List<IRelationship> selectedChain;
        private ChainList selectedChainList;
        
        public CreateDerivedConnectionDialog(Shell parentShell, ChainList chainList1, ChainList chainList2) {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.RESIZE);
            this.chainList1 = chainList1;
            this.chainList2 = chainList2;
        }
        
        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Create Derived Relation");
            shell.setImage(IArchimateImages.ImageFactory.getImage(IArchimateImages.ICON_DERIVED_16));
        }
        
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite)super.createDialogArea(parent);
            composite.setBackground(ColorConstants.white);
            composite.setBackgroundMode(SWT.INHERIT_DEFAULT);

            GridLayout layout = new GridLayout();
            layout.marginWidth = 10;
            composite.setLayout(layout);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 700;
            composite.setLayoutData(gd);
            
            if(chainList1.getChains() != null) {
                createTable(composite, chainList1);
            }
            if(chainList2.getChains() != null) {
                createTable(composite, chainList2);
            }
            
            return composite;
        }
        
        private void createTable(Composite parent, ChainList chainList) {
            Label rubric = new Label(parent, SWT.NULL);
            rubric.setText("From '" + chainList.srcElement.getName() + "' to '" + chainList.tgtElement.getName() + "':");
            rubric.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            Composite c = new Composite(parent, SWT.NULL);
            c.setLayout(new TableColumnLayout());
            c.setLayoutData(new GridData(GridData.FILL_BOTH));
            DerivedConnectionsTableViewer viewer = new DerivedConnectionsTableViewer(c);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            viewer.getControl().setLayoutData(gd);
            
            viewer.setInput(chainList);
            viewer.addSelectionChangedListener(this);
            viewer.addDoubleClickListener(this);
        }
        
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
        
        public List<IRelationship> getSelectedChain() {
            return selectedChain;
        }
        
        public ChainList getSelectedChainList() {
            return selectedChainList;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            IStructuredSelection selection = (IStructuredSelection)event.getSelection();
            selectedChain = (List<IRelationship>)selection.getFirstElement();
            selectedChainList = (ChainList)((TableViewer)event.getSource()).getInput();
            getButton(IDialogConstants.OK_ID).setEnabled(!selection.isEmpty());
        }

        @Override
        public void doubleClick(DoubleClickEvent event) {
            okPressed();
        }
        
        @Override
        protected IDialogSettings getDialogBoundsSettings() {
            IDialogSettings settings = ArchimateEditorPlugin.INSTANCE.getDialogSettings();
            IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
            if(section == null) {
                section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
            } 
            return section;
        }

    }
    
    /**
     * Table Viewer
     */
    private static class DerivedConnectionsTableViewer extends TableViewer {
        public DerivedConnectionsTableViewer(Composite parent) {
            super(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
            
            Table table = getTable();
            
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
            
            TableColumnLayout layout = (TableColumnLayout)getControl().getParent().getLayout();
            
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText("Chain in model");
            layout.setColumnData(column, new ColumnWeightData(80, true));
            
            column = new TableColumn(table, SWT.NONE);
            column.setText("Weakest");
            layout.setColumnData(column, new ColumnWeightData(20, true));
            
            setContentProvider(new DerivedConnectionsContentProvider());
            setLabelProvider(new DerivedConnectionsLabelProvider());
        }
        
        /**
         * Table Content Provider
         */
        private class DerivedConnectionsContentProvider implements IStructuredContentProvider {
            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public Object[] getElements(Object input) {
                if(input instanceof ChainList) {
                    return ((ChainList)input).getChains().toArray();
                }
                return null;
            }
        }

        /**
         * Table Lable Provider
         */
        private class DerivedConnectionsLabelProvider extends LabelProvider implements ITableLabelProvider {
            @Override
            public Image getColumnImage(Object element, int columnIndex) {
                return null;
            }

            @Override
            public String getColumnText(Object element, int columnIndex) {
                if(element == null) {
                    return ""; //$NON-NLS-1$
                }

                @SuppressWarnings("unchecked")
                List<IRelationship> chain = (List<IRelationship>)element;
                ChainList chainList = (ChainList)getInput();

                switch(columnIndex) {
                    // Chain
                    case 0:
                        String s = chainList.srcElement.getName();
                        s += " --> ";
                        for(int i = 1; i < chain.size(); i++) {
                            IRelationship relation = chain.get(i);
                            s += getRelationshipText(chain, relation);
                            if(DerivedRelationsUtils.isBidirectionalRelationship(relation)) {
                                s += " <-> ";
                            }
                            else {
                                s += " --> ";
                            }
                        }
                        s += chainList.tgtElement.getName();
                        
                        return s; 

                        // Weakest
                    case 1:
                        return DerivedRelationsUtils.getWeakestType(chain).getName(); 
                }

                return "";
            }
            
            private String getRelationshipText(List<IRelationship> chain, IRelationship relation) {
                if(DerivedRelationsUtils.isBidirectionalRelationship(relation)) {
                    int index = chain.indexOf(relation);
                    if(index > 0) {
                        IRelationship previous = chain.get(index - 1);
                        if(relation.getTarget() == previous.getTarget()) {
                            return relation.getTarget().getName();
                        }
                    }
                    return relation.getSource().getName();
                }
                else {
                    return relation.getSource().getName();
                }
            }
        }
    }
    
    
    /**
     * Command Stack Command
     */
    private static class CreateDerivedConnectionCommand extends Command {
        private IRelationship fRelation;
        private IDiagramModelArchimateConnection fConnection;
        private IDiagramModelArchimateObject fSource;
        private IDiagramModelArchimateObject fTarget;
        private boolean fDerivedFolderWasCreated;
        
        public CreateDerivedConnectionCommand(IDiagramModelArchimateObject source, IDiagramModelArchimateObject target,
                IRelationship relation) {
            fSource = source;
            fTarget = target;
            fRelation = relation;
            setLabel("Create Derived Relation");
        }

        @Override
        public void execute() {
            fConnection = IArchimateFactory.eINSTANCE.createDiagramModelArchimateConnection();
            fRelation.setName(ArchimateNames.getDefaultName(fRelation.eClass()));
            fConnection.setRelationship(fRelation);
            fConnection.connect(fSource, fTarget);
            addToModel();
        }
        
        @Override
        public void redo() {
            fConnection.reconnect();
            addToModel();
        }
        
        private void addToModel() {
            IFolder folder = fConnection.getDiagramModel().getArchimateModel().getFolder(FolderType.DERIVED);
            // We need to create the Derived Relations folder
            if(folder == null) {
                folder = fConnection.getDiagramModel().getArchimateModel().addDerivedRelationsFolder();
                fDerivedFolderWasCreated = true;
            }
            fConnection.addRelationshipToModel(folder);
        }
        
        @Override
        public void undo() {
            // Remove the model relationship from its model folder
            fConnection.removeRelationshipFromModel();
            
            // If the Derived Relations folder was created, remove it
            if(fDerivedFolderWasCreated) {
                fConnection.getDiagramModel().getArchimateModel().removeDerivedRelationsFolder();
            }
            
            // Disconnect last because we needed access to fConnection.getDiagramModel()
            fConnection.disconnect();
        }
        
        @Override
        public void dispose() {
            fConnection = null;
            fSource = null;
            fTarget = null;
            fRelation = null;
        }
    }
    
}
