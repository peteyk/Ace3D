/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.BHC.NucleusLogNode;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.BHCNucleusSet;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class BHCTreeCutDialog extends JDialog {
    public BHCTreeCutDialog(Ace3D_Frame owner,ImagedEmbryo embryo){
        super(owner,false);
        this.owner = owner;
        this.nucleusFile = embryo.getNucleusFile();
        this.setTitle("Cut the BHC Tree");
        this.setSize(300, 500);
        this.getContentPane().setLayout(new BorderLayout());
        this.setLocationRelativeTo(owner);
        
        JPanel volumePanel = new JPanel();
        volumePanel.setLayout(new FlowLayout());
        volumePanel.add(new JLabel("Minimum Volume: "));
        volumePanel.add(volumeField);
        volumeField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    minVolume = Double.valueOf(volumeField.getText().trim());
                    ok();
                }catch (Exception exc){
                    
                }
            }
        });
        volumePanel.add(new JLabel("  List Size: "));
        volumePanel.add(maxItemsField);
        this.getContentPane().add(volumePanel,BorderLayout.NORTH);
        
        jList = new JList();
        jList.addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()){
                        ok();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(jList);
        this.getContentPane().add(scroll, BorderLayout.CENTER);
        
        JPanel button = new JPanel();
       
        JButton cancel = new JButton("Done");
        cancel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                BHCTreeCutDialog.this.setVisible(false);
                result = false;
            }
        });
        button.add(cancel);
        this.getContentPane().add(button,BorderLayout.SOUTH);
    }
    private void ok(){
        Object sel = jList.getSelectedValue();
        
        if (sel != null){
            int t = tree.getTime();
            LinkedNucleusFile f = (LinkedNucleusFile)this.nucleusFile;
            f.clearInterCuratedRegion(t, false);
            Set<BHCNucleusData> nucData =  BHCNucleusData.factory(tree.cutToN(((CutDescriptor)sel).getNodeCount()), minVolume, t);
            BHCNucleusSet nucSet = new BHCNucleusSet(t,tree.getFileName(),nucData);
            nucleusFile.addNuclei(nucSet,true);
            owner.stateChanged(null);
/*            
            nucleusFile.unlinkTime(t);
            nucleusFile.unlinkTime(t-1);
            
            nucleusFile.removeNucleiAtTime(t);
            thresh = ((Posterior)sel).r;
            try {
                BHCNucleusFile bhc = tree.cutToNucleusFile(thresh);
                nucleusFile.addBHC(bhc);
            } catch (Exception exc){
                exc.printStackTrace();
            }
//            nucleusFile.linkTimePoint(t-1);
//            nucleusFile.linkTimePoint(t);
            nucleusFile.notifyListeners();
*/
        }
    }
/*    
    public void countClusters(){
        Posterior selected = (Posterior)jList.getSelectedValue();
        if (selected == null){
            return;
        }
        if (selected.n == -1){
            Element ele = tree.cutTreeAtThreshold(selected.r);
            selected.n = ele.getChildren("GaussianMixtureModel").size();            
        }
        jList.repaint();
    }
*/
    public void setBHCTree(BHCTree tree){
        
        this.tree = tree;
        buildListModel();

    }
    private void buildListModel(){
        CutDescriptor sel = (CutDescriptor)jList.getSelectedValue(); 
        Integer n = null;
        if (sel != null){
            n = sel.getNodeCount();
        }
        
        DefaultListModel model = new DefaultListModel();
        if (tree == null) {
            return;
        }
        
        CutDescriptor selected = null;
        CutDescriptor cutDesc = new CutDescriptor(tree.firstTreeCut());
        while (cutDesc.getNodeCount() <= maxItems){
            model.addElement(cutDesc);
            if (n != null && cutDesc.getNodeCount()==n){
                selected = cutDesc;
            }
            TreeSet<NucleusLogNode> current = cutDesc.getCut();
            TreeSet<NucleusLogNode> next = tree.nextTreeCut(current);
            cutDesc = new CutDescriptor(next);
        }

        jList.setModel(model);
        jList.setSelectedValue(selected,true);    
        
    }
    public boolean isOK(){
        return this.result;
    }
    public double getThresh(){
        return thresh;
    }
    boolean result = false;
    double thresh = Math.log(.9);
    Ace3D_Frame owner;
    NucleusFile nucleusFile;
    BHCTree tree;
    JList jList;
    JTextField volumeField = new JTextField("1000");;
    JTextField maxItemsField  = new JTextField("60");;
    int maxItems=60;
    double minVolume=1000.0;
    
    class CutDescriptor {
        public CutDescriptor(TreeSet<NucleusLogNode> cut){
            this.cut = cut;
        }
        public int getNodeCount(){
            return cut.size();
        }
        public TreeSet<NucleusLogNode> getCut(){
            return cut;
        }
        public String toString(){
            NucleusLogNode first = cut.first();
            double lnP = first.getLogPosterior();
            return String.format("(%d) %e %f",getNodeCount(),Math.exp(lnP),lnP);
        }
        TreeSet<NucleusLogNode> cut;
    }
/*    
    class Posterior {
        public Posterior(double r,int n){
            this.r = r;
            this.n = n;
        }
        
        @Override
        public String toString(){
            if (n ==-1){
                return String.format("%e %f",Math.exp(r),r);
            }
            return String.format("(%d) %e %f",n,Math.exp(r),r);
        }
        
        double r;
        int n;
    }
*/
}
