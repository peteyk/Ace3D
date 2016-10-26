/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame implements ChangeListener    {
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
        
        JButton child1 = new JButton("Unlink Child1");
        child1.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Ace3DNucleusFile file = (Ace3DNucleusFile)embryo.getNucleusFile();
                String childName = npPanel.getChild1();
                Nucleus childNuc = file.getNucleus(childName);
                if (childNuc != null) {
                    file.unlink(embryo.selectedNucleus(), true);
                }
            }
        });
        buttonPanel.add(child1);
        
        JButton child2 = new JButton("Unlink Child2");
        buttonPanel.add(child2); 
        child2.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Ace3DNucleusFile file = (Ace3DNucleusFile)embryo.getNucleusFile();
                String childName = npPanel.getChild2();
                Nucleus childNuc = file.getNucleus(childName);
                if (childNuc != null) {
                    file.unlink(embryo.selectedNucleus(),true);
                }
            }
        });
        
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
        
        content.add(panel,BorderLayout.CENTER);
        content.add(buttonPanel,BorderLayout.SOUTH);
        this.setContentPane(content);
        pack();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        npPanel.invalidated(embryo);
        radiusControl.invalidated(embryo);
    }
    ImagedEmbryo embryo;  
    NucleusPropertiesPanel npPanel;
    RadiusControlPanel radiusControl;
}
