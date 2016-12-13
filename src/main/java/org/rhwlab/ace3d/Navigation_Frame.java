/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Set;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
        
        rootsRoot = new DefaultMutableTreeNode("Roots",true);
        rootsTree = new JTree(rootsRoot);
        rootsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);    
        rootsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                ChangeEvent event = new ChangeEvent(rootsTree);
                treePanel.stateChanged(event);
            }
        });
        JScrollPane rootsScroll = new JScrollPane(rootsTree);

       
        nucsRoot = new DefaultMutableTreeNode("All Nuclei",true);
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
                } else{
                    String timeLabel = (String)node.getUserObject();
                    int t = Integer.valueOf(timeLabel.substring(5,timeLabel.indexOf(' ')));
                    panel.changeTime(t);
                }
            }
        });
        JScrollPane nucsScroll = new JScrollPane(nucsTree);
        Dimension prefdim = nucsScroll.getPreferredSize();
        prefdim.setSize(2*prefdim.width, prefdim.height);
        nucsScroll.setPreferredSize(prefdim);
        
        deathsRoot = new DefaultMutableTreeNode("Terminal Nuclei",true);  
        deathsTree = new JTree(deathsRoot);
        deathsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); 
        deathsTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)deathsTree.getLastSelectedPathComponent();
                if (node == null)return;
                if (node.isLeaf()){
                    Nucleus nuc = (Nucleus)node.getUserObject();
                    embryo.setSelectedNucleus(nuc);
                    panel.changeTime(nuc.getTime());
                    panel.changePosition(nuc.getCenter());
                }                
            }
        });
        JScrollPane deathsScroll = new JScrollPane(deathsTree);       
        JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,deathsScroll,rootsScroll);
        prefdim = leftPane.getPreferredSize();
        prefdim.setSize(2*prefdim.width, prefdim.height);
        leftPane.setPreferredSize(prefdim);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,nucsScroll,leftPane);
        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,split,treeScroll);
        this.add(split2,BorderLayout.CENTER);

        pack();
        

    }

    @Override
    public void run(String arg) {
        this.setSize(800,400);
        this.setLocationByPlatform(true);
        this.setVisible(true);
    }


    @Override
    public void invalidated(Observable observable) {
        nucsRoot.removeAllChildren();
        rootsRoot.removeAllChildren();
        deathsRoot.removeAllChildren();
        
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null) { 
            return;
        }
        Set<Integer> times = nucFile.getAllTimes();
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            DefaultMutableTreeNode timeNode = null;
            if (nucFile.isCurated(time)){
                timeNode = new DefaultMutableTreeNode(String.format("Curated:%d (%d)",time,nucs.size()));
            }else {
                timeNode = new DefaultMutableTreeNode(String.format("Auto:%d (%d)",time,nucs.size()));
            }
            nucsRoot.add(timeNode);
            for (Nucleus nuc : nucs){
                DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                timeNode.add(nucNode);                
            }
        }
        
        
        TreeMap<Integer,Set<Nucleus>> rootMap = embryo.getRootNuclei();
        for (Integer t : rootMap.keySet()){
            for (Nucleus root : rootMap.get(t)){
                addFirstNucToNode(root,rootsRoot);
            }
        }
        
        
        for (Integer time : times){
            Set<Nucleus> nucs = nucFile.getLeaves(time);
            if (!nucs.isEmpty()){
                DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(String.format("Time:%d",time));
                deathsRoot.add(timeNode);
                for (Nucleus nuc : nucs){
                    DefaultMutableTreeNode nucNode = new DefaultMutableTreeNode(nuc);
                    timeNode.add(nucNode);                
                }
            }
        }        
        rootsTree.setModel(new DefaultTreeModel(rootsRoot));
        nucsTree.setModel(new DefaultTreeModel(nucsRoot));
        deathsTree.setModel(new DefaultTreeModel(deathsRoot));
        
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
    NavigationTreePanel treePanel;
    
    DefaultMutableTreeNode rootsRoot;
    DefaultMutableTreeNode nucsRoot;
    DefaultMutableTreeNode deathsRoot;
    
    JTree rootsTree;
    JTree nucsTree;
    JTree deathsTree;
}
