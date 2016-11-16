/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.util.Set;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class Navigation_Frame extends JFrame implements PlugIn,InvalidationListener {
    public Navigation_Frame(ImagedEmbryo emb,SynchronizedMultipleSlicePanel p){
        super();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setTitle("Navigation Trees");
        this.embryo = emb;
        this.panel = p;
        this.getContentPane().setLayout(new BorderLayout());
        
        rootsRoot = new DefaultMutableTreeNode("Cell Tree",true);
        rootsTree = new JTree(rootsRoot);
        rootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        rootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootsTree.getLastSelectedPathComponent();
                if (node == null){
                    return;
                }
                Cell cell = (Cell)node.getUserObject();
                Nucleus nuc = cell.firstNucleus();
                emb.setSelectedNucleus(nuc);
                panel.changeTime(nuc.getTime());
                panel.changePosition(nuc.getCenter());                
                
                if (nuc.getParent()==null){
                    headPanel.setRoot(nuc.getCell());
                }
            }
        });
        JScrollPane rootsScroll = new JScrollPane(rootsTree);
        this.getContentPane().add(rootsScroll,BorderLayout.WEST);
 
        nucsRoot = new DefaultMutableTreeNode("Nuclei by Time",true);
        nucsTree = new JTree(nucsRoot);
        nucsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
        nucsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)nucsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    embryo.setSelectedNucleus(nuc);
                    panel.changeTime(nuc.getTime());
                    panel.changePosition(nuc.getCenter());
                }
            }
        });
        JScrollPane nucsScroll = new JScrollPane(nucsTree);
        this.getContentPane().add(nucsScroll,BorderLayout.EAST);  
        
        treePanel = new NavigationTreePanel(embryo);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        this.add(treeScroll,BorderLayout.CENTER);
        
        headPanel = new NavigationHeaderPanel(treePanel);
        this.add(headPanel,BorderLayout.NORTH);
        pack();
        

    }

    @Override
    public void run(String arg) {
        this.setSize(800,400);
        this.setVisible(true);
    }


    @Override
    public void invalidated(Observable observable) {
        nucsRoot = new DefaultMutableTreeNode("All Nuclei",true);
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null) { 
            return;
        }
        Set<Integer> times = nucFile.getAllTimes();
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(String.format("Time:%d (%d)",time,nucs.size()));
            nucsRoot.add(timeNode);
            for (Nucleus nuc : nucs){
                DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                timeNode.add(nucNode);                
            }
        }
        
        rootsRoot = new DefaultMutableTreeNode("Root Cells",true);
        for (Cell root : embryo.getRootCells()){
            addRootToNode(root,rootsRoot);
           
        }
        rootsTree.setModel(new DefaultTreeModel(rootsRoot));
        nucsTree.setModel(new DefaultTreeModel(nucsRoot));
        treePanel.stateChanged(new ChangeEvent(headPanel));
        this.invalidate();
        
    }
    private void addRootToNode(Cell root,DefaultMutableTreeNode node){
        DefaultMutableTreeNode cellNode = new DefaultMutableTreeNode(root);
        node.add(cellNode);
        for (Cell child : root.getChildren()){
            addRootToNode(child,cellNode);
        }
        
    }
    ImagedEmbryo embryo;
    SynchronizedMultipleSlicePanel panel;
    NavigationHeaderPanel headPanel;
    DefaultMutableTreeNode rootsRoot;
    DefaultMutableTreeNode nucsRoot;
    NavigationTreePanel treePanel;
    JTree rootsTree;
    JTree nucsTree;
}
