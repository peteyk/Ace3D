/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.rhwlab.dispim.ImagedEmbryo;



/**
 *
 * @author gevirl
 */
public class DataSetsDialog extends JFrame {
    public DataSetsDialog(Ace3D_Frame owner,ImagedEmbryo embryo,int sliderMin,int sliderMax){
        super("DataSets");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(owner);
        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());
        
        JPanel contrastPanel = new JPanel();
        contrastPanel .setLayout(new BoxLayout(contrastPanel,BoxLayout.Y_AXIS));
        for (String dataset : Ace3D_Frame.getAllDatsets()){
            contrastPanel.add(new DataSetPropertyPanel(owner,dataset,sliderMin,sliderMax));
        }
        content.add(contrastPanel,BorderLayout.CENTER);
        

        this.pack();
    }
}
