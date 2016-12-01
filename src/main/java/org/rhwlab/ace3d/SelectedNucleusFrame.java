/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.beans.value.ObservableValue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame implements PlugIn,javafx.beans.value.ChangeListener   {
    public SelectedNucleusFrame(Ace3D_Frame owner,ImagedEmbryo emb){
        this.embryo = emb;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        this.setLocationRelativeTo(owner);  
//        this.setLocationByPlatform(true);
        this.setTitle("Selected Nucleus");
        
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
        npPanel = new NucleusPropertiesPanel();
        embryo.addListener(npPanel);
        panel.add(npPanel);
        
        radiusControl = new RadiusControlPanel(owner);
        radiusControl.setEmbryo(embryo);
        embryo.addListener(radiusControl);
        panel.add(radiusControl);
/*        
        MarkedNucleiPanel nucPanel = new MarkedNucleiPanel();
        embryo.addListener(nucPanel);
        panel.add(nucPanel);       
 */       
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        
        JButton unselectButton = new JButton("Unselect");
        unselectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                embryo.setSelectedNucleus(null);
            }
        });
        buttonPanel.add(unselectButton);
/*        
        JButton calcExp = new JButton("Calc Expression");
        calcExp.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> datasets = Ace3D_Frame.datasetsSelected();
                if (datasets.isEmpty()) return;
                Nucleus nuc = embryo.selectedNucleus();
                TimePointImage tpi = embryo.getTimePointImage(datasets.get(0), nuc.getTime());
                double[][] eigen = nuc.getEigenVectors();
                try {
                    double exp = embryo.calculateExpression(nuc,tpi,eigen);
                    embryo.setExpression(nuc, exp);
                } catch (Exception exc){
                    exc.printStackTrace();
                }
            }
        });
        buttonPanel.add(calcExp);
*/       
        unlink = new JButton("Unlink");
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().unlinkNucleus(embryo.selectedNucleus(),true);
                }
            }
        });
        buttonPanel.add(unlink);
        
        JButton remove = new JButton("Remove");
        buttonPanel.add(remove); 
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().removeNucleus(embryo.selectedNucleus(),true);
                }
            }
        });
       
        link = new JButton("Link");
        link.setEnabled(false);
        buttonPanel.add(link);
        link.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Nucleus sel = embryo.selectedNucleus();
                Nucleus mark = embryo.getMarked();
                mark.linkTo(sel);
                embryo.notifyListeners();
            }
        });
        
        content.add(panel,BorderLayout.CENTER);
        content.add(buttonPanel,BorderLayout.SOUTH);
        this.setContentPane(content);
        pack();
    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        Nucleus sel = embryo.selectedNucleus();
        if (sel != null){
            if (sel.getParent()!= null){
                unlink.setEnabled(true);
            } else {
                unlink.setEnabled(false);
                if (embryo.getMarked() != null){
                    link.setEnabled(true);
                }else {
                    link.setEnabled(false);
                }
            }
        }
        npPanel.invalidated(embryo);
        radiusControl.invalidated(embryo);
    }
    @Override
    public void run(String arg) {
  //      this.setSize(800,400);
        this.setLocationByPlatform(true);
        this.setVisible(true);
    }    

    ImagedEmbryo embryo;  
    NucleusPropertiesPanel npPanel;
    RadiusControlPanel radiusControl;
    
    JButton unlink;
    JButton link;
}
