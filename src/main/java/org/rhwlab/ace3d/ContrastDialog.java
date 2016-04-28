/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.Container;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;



/**
 *
 * @author gevirl
 */
public class ContrastDialog extends JFrame {
    public ContrastDialog(Ace3D_Frame owner,String title,int sliderMin,int sliderMax){
        super(title);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(owner);
        Container content = this.getContentPane();
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        for (String dataset : Ace3D_Frame.getAllDatsets()){
            content.add(new ContrastColorPanel(owner,dataset,sliderMin,sliderMax));
        }
        this.pack();
    }
}
