/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.nucleus.BHC_NucleusFile;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class BHCTreeCutDialog extends JDialog {
    public BHCTreeCutDialog(Ace3D_Frame owner,NucleusFile nucleusFile){
        super(owner,true);
        this.nucleusFile = (Ace3DNucleusFile)nucleusFile;
        this.setTitle("Cut the BHC Tree");
        this.setSize(300, 500);
        this.getContentPane().setLayout(new BorderLayout());
        this.setLocationRelativeTo(owner);
        
        jList = new JList();
        jList.addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()){
                        countClusters();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(jList);
        this.getContentPane().add(scroll, BorderLayout.CENTER);
        
        JPanel button = new JPanel();
        JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                BHCTreeCutDialog.this.ok();
                BHCTreeCutDialog.this.setVisible(false);
                result = true;
            }
        });        
        button.add(ok);
        JButton cancel = new JButton("Cancel");
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
            nucleusFile.unlinkTime(t);
            nucleusFile.unlinkTime(t-1);
            
            nucleusFile.removeNucleiAtTime(t);
            thresh = ((Posterior)sel).r;
            try {
                BHC_NucleusFile bhc = tree.cutToNucleusFile(thresh);
                nucleusFile.addBHC(bhc);
            } catch (Exception exc){
                exc.printStackTrace();
            }
            nucleusFile.linkTimePoint(t-1);
            nucleusFile.linkTimePoint(t);
            nucleusFile.notifyListeners();
        }
    }
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
    public void setBHCTree(BHCTree tree,double th){
        thresh = th;
        Posterior selected = null;
        this.tree = tree;
        DefaultListModel model = new DefaultListModel();
        Set<Double> posts = tree.allPosteriors();
        for (Double post : posts){
            Posterior posterior = new Posterior(post,-1);
            model.addElement(posterior);
            if (post == thresh){
                selected = posterior;
            }            
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
    double thresh;
    Ace3DNucleusFile nucleusFile;
    BHCTree tree;
    JList jList;
    
    class Posterior {
        public Posterior(double r,int n){
            this.r = r;
            this.n = n;
        }
        
        @Override
        public String toString(){
            if (n ==-1){
                return String.format("%e",r);
            }
            return String.format("(%d) %e",n,r);
        }
        
        double r;
        int n;
    }
}
