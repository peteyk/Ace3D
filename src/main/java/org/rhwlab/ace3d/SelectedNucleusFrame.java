/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Ace3DNucleusFile;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame   {
    public SelectedNucleusFrame(Ace3D_Frame owner,ImagedEmbryo embryo){
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
        
        RadiusControlPanel radiusControl = new RadiusControlPanel(owner);
        radiusControl.setEmbryo(embryo);
        embryo.addListener(radiusControl);
        panel.add(radiusControl);
        
        MarkedNucleiPanel nucPanel = new MarkedNucleiPanel();
        embryo.addListener(nucPanel);
        panel.add(nucPanel);       
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        
        JButton child1 = new JButton("Unlink Child1");
        child1.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                Ace3DNucleusFile file = (Ace3DNucleusFile)embryo.getNucleusFile();
                String childName = npPanel.getChild1();
                Nucleus childNuc = file.getNucleus(childName);
                if (childNuc != null) {
                    file.unlink(embryo.selectedNucleus(), childNuc);
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
                    file.unlink(embryo.selectedNucleus(), childNuc);
                }
            }
        });
        
        JButton link = new JButton("Link to Marked");
        buttonPanel.add(link);
        link.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        
        content.add(panel,BorderLayout.CENTER);
        content.add(buttonPanel,BorderLayout.SOUTH);
        this.setContentPane(content);
        pack();
    }
    ImagedEmbryo embryo;  
    NucleusPropertiesPanel npPanel;
            
}
