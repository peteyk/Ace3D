/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.beans.value.ObservableValue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.rhwlab.dispim.ImagedEmbryo;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame implements javafx.beans.value.ChangeListener   {
    public SelectedNucleusFrame(Ace3D_Frame owner,ImagedEmbryo emb){
        this.embryo = emb;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(owner);        
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
        
        MarkedNucleiPanel nucPanel = new MarkedNucleiPanel();
        embryo.addListener(nucPanel);
        panel.add(nucPanel);       
        
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
        JButton unlink = new JButton("Unlink Selected");
        unlink.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().unlinkNucleus(embryo.selectedNucleus(),true);
                }
            }
        });
        buttonPanel.add(unlink);
        
        JButton remove = new JButton("Remove Selected");
        buttonPanel.add(remove); 
        remove.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (embryo.selectedNucleus() != null){
                    embryo.getNucleusFile().removeNucleus(embryo.selectedNucleus());
                }
            }
        });
/*       
        JButton link = new JButton("Link to Marked");
        buttonPanel.add(link);
        link.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] marked = nucPanel.getMarkedNuclei();
                if (marked.length==1){
                    Ace3DNucleusFile file = (Ace3DNucleusFile)embryo.getNucleusFile();
                    Nucleus markedNuc = file.getNucleus(marked[0]);
                    String childName = npPanel.getChild1();
                    Nucleus childNuc = file.getNucleus(childName);
                    if (childNuc == null) {
                        file.linkInTime(embryo.selectedNucleus(), markedNuc);
                    } 
                    childName = npPanel.getChild2();
                    childNuc = file.getNucleus(childName);  
                    if (childNuc == null){
                        
//                        file.linkDivision(embryo.selectedNucleus(), markedNuc);
                    }
                }
            }
        });
  */      
        content.add(panel,BorderLayout.CENTER);
        content.add(buttonPanel,BorderLayout.SOUTH);
        this.setContentPane(content);
        pack();
    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        npPanel.invalidated(embryo);
        radiusControl.invalidated(embryo);
    }

    ImagedEmbryo embryo;  
    NucleusPropertiesPanel npPanel;
    RadiusControlPanel radiusControl;
}
