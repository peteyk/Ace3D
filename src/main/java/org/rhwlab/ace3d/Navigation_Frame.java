/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.util.Set;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import org.rhwlab.dispim.ImagedEmbryo;
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
        
        treePanel = new NavigationTreePanel(embryo);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        
        headPanel = new NavigationHeaderPanel();
        headPanel.setTreePanel(treePanel);
        treePanel.setHeadPanel(headPanel);
        
        this.add(headPanel,BorderLayout.NORTH);        
        rootsRoot = new DefaultMutableTreeNode("Cell Tree",true);
        rootsTree = new JTree(rootsRoot);
        rootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);    
        rootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                ChangeEvent event = new ChangeEvent(rootsTree);
                treePanel.stateChanged(event);
 /*               
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootsTree.getLastSelectedPathComponent();
                if (node == null){
                    return;
                }
                Nucleus nuc = (Nucleus)node.getUserObject();
                emb.setSelectedNucleus(nuc);
                panel.changeTime(nuc.getTime());
                panel.changePosition(nuc.getCenter());                
                
                if (nuc.getParent()==null){
                    headPanel.setRoot(nuc);
                }
*/
            }
        });
        JScrollPane rootsScroll = new JScrollPane(rootsTree);
 
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
        

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,rootsScroll,treeScroll);
        this.add(split,BorderLayout.CENTER);

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
        
        rootsRoot = new DefaultMutableTreeNode("Roots",true);
        TreeMap<Integer,Set<Nucleus>> rootMap = embryo.getRootNuclei();
        for (Integer t : rootMap.keySet()){
            for (Nucleus root : rootMap.get(t)){
                addFirstNucToNode(root,rootsRoot);
            }
            
           
        }
        rootsTree.setModel(new DefaultTreeModel(rootsRoot));
        nucsTree.setModel(new DefaultTreeModel(nucsRoot));
        treePanel.stateChanged(new ChangeEvent(rootsTree));
        this.invalidate();
        
    }
    private void addFirstNucToNode(Nucleus firstNuc,DefaultMutableTreeNode node){
        DefaultMutableTreeNode cellNode = new DefaultMutableTreeNode(firstNuc);
        node.add(cellNode);
        Nucleus lastNuc = firstNuc.lastNucleusOfCell();
        if (lastNuc.isDividing()){
            Nucleus[] next = lastNuc.nextNuclei();
            addFirstNucToNode(next[0],cellNode);
            addFirstNucToNode(next[1],cellNode);
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
