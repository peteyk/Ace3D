/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class RadiusControlPanel extends JPanel implements InvalidationListener {
    public RadiusControlPanel(Ace3D_Frame frame){
        this.frame = frame;
        this.setLayout(new BorderLayout());
        add(new JLabel("Radius Control"),BorderLayout.NORTH);
        
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new BoxLayout(slidersPanel,BoxLayout.X_AXIS));
        add(slidersPanel,BorderLayout.CENTER);
        
        sliders = new JSlider[3];
        labels = new JLabel[3];
        for (int i=0 ; i<sliders.length ; ++i){
            JPanel sliderPanel = new JPanel();
            sliderPanel.setLayout(new BoxLayout(sliderPanel,BoxLayout.Y_AXIS));
            labels[i] = new JLabel("radius");
            sliderPanel.add(labels[i]);
            
            sliders[i] = new JSlider(1,sliderMax,100);
            sliders[i].setOrientation(JSlider.VERTICAL);
            sliderPanel.add(sliders[i]);
            sliderPanel.add(Box.createHorizontalStrut(5));
            sliders[i].addChangeListener(new ChangeListener(){
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider)e.getSource();
                    if (!slider.getValueIsAdjusting()){
                        if (embryo != null){
                            Nucleus nuc = embryo.selectedNucleus();
                            if (nuc != null){
                                double[] r = new double[sliders.length];
                                for (int i=0 ; i<r.length ; ++i){
                                    r[i] = sliders[i].getValue()/100.0;
                                }
                                nuc.setAdjustment(r);
                                frame.stateChanged(e);
                            }
                        }
                    }
                }
            }); 
            slidersPanel.add(sliderPanel);
        }
    }
    public void setEmbryo(ImagedEmbryo emb){
        this.embryo = emb;
    }
    public void setValues(double[] vs){
        for (int i=0 ; i<vs.length ; ++i){
            sliders[i].setValue((int)(100.0*vs[i]));
        }
    }

    @Override
    public void invalidated(Observable observable) {
        if (embryo != null){
            Nucleus nuc = embryo.selectedNucleus();
            if (nuc != null){
                Object o = nuc.getAdjustment();
                if (o instanceof double[]){
                    setValues((double[])o);
                }
                labels[0].setText(nuc.getRadiusLabel(0));
                labels[1].setText(nuc.getRadiusLabel(1));
                labels[2].setText(nuc.getRadiusLabel(2));
            }
        }
    }
    
    static int sliderMax = 300;
    Ace3D_Frame frame;
    ImagedEmbryo embryo;
    JSlider[] sliders;
    JLabel[] labels;


}
