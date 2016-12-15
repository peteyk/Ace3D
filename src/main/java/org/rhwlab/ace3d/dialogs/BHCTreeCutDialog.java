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
import java.util.TreeMap;
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
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.BHCNucleusSet;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class BHCTreeCutDialog extends JDialog {
    public BHCTreeCutDialog(Ace3D_Frame owner,ImagedEmbryo embryo){
        super(owner,false);
        this.embryo = embryo;
        this.nucleusFile = embryo.getNucleusFile();
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
            LinkedNucleusFile f = (LinkedNucleusFile)this.nucleusFile;
            f.clearInterCuratedRegion(t, false);
            BHCNucleusSet bhc = tree.cutToN(((Posterior)sel).n);
            nucleusFile.addNuclei(bhc,true);
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
    public void setBHCTree(BHCTree tree){
        Posterior selected = null;
        this.tree = tree;
        DefaultListModel model = new DefaultListModel();
        TreeMap<Integer,Double> postMap = new TreeMap<>(); 
        if (tree == null) {
            return;
        }
        this.tree.allPosteriorProb(postMap);
//        Set<Double> posts = tree.allPosteriors();
        for (Integer m : postMap.keySet()){
            Double post = postMap.get(m);
            Posterior posterior = new Posterior(post,m);
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
    ImagedEmbryo embryo;
    NucleusFile nucleusFile;
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
                return String.format("%e %f",r,Math.log(r));
            }
            return String.format("(%d) %e %f",n,r,Math.log(r));
        }
        
        double r;
        int n;
    }
}
