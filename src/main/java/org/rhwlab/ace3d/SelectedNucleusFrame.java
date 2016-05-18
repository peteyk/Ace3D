/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JFrame;
import org.rhwlab.dispim.ImagedEmbryo;

/**
 *
 * @author gevirl
 */
public class SelectedNucleusFrame extends JFrame implements InvalidationListener  {
    public SelectedNucleusFrame(Ace3D_Frame owner,ImagedEmbryo embryo){
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(owner);        
        this.setTitle("Selected Nucleus");
        this.setLayout(new BorderLayout());
        
        NucleusPropertiesPanel npPanel = new NucleusPropertiesPanel();
        embryo.addListener(npPanel);
        this.add(npPanel,BorderLayout.CENTER);
        
        RadiusControlPanel radiusControl = new RadiusControlPanel(owner);
        radiusControl.setEmbryo(embryo);
        embryo.addListener(radiusControl);
        this.add(radiusControl,BorderLayout.EAST);
        pack();
    }


    @Override
    public void invalidated(Observable observable) {
        
    }
    
    ImagedEmbryo embryo;  

            
}
