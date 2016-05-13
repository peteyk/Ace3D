/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author gevirl
 */
public class RadiusControlPanel extends JPanel {
    public RadiusControlPanel(Ace3D_Frame frame){
        this.frame = frame;
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add(new JLabel("Radius Control"));
        slider = new JSlider(1,100,(int)Ace3D_Frame.R);
        slider.setOrientation(JSlider.VERTICAL);
        add(slider);
        slider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!slider.getValueIsAdjusting()){
                    Ace3D_Frame.R = slider.getValue();
                    frame.stateChanged(e);
                }
            }
        });
        
    }
    Ace3D_Frame frame;
    JSlider slider;
}
