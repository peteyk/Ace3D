/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.imglib2.RandomAccessibleInterval;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TimePointImage;

/**
 *
 * @author gevirl
 */
public class SynchronizedMultipleSlicePanel extends JPanel {
    public SynchronizedMultipleSlicePanel(int n){
        this.nDims = n;
        position = new long[n];
        panels = new SingleSlicePanel[n]; 
        JPanel slicePanel = new JPanel();
        slicePanel.setLayout(new BoxLayout(slicePanel,BoxLayout.X_AXIS));
        for (int d=0 ; d<n ; ++d){
            panels[d] = new SingleSlicePanel("",d,this);
            slicePanel.add(panels[d]);
        }
        this.setLayout(new BorderLayout());
        this.add(slicePanel,BorderLayout.CENTER);
        titledBorder = BorderFactory.createTitledBorder(positionString());
        this.setBorder(titledBorder);  
        
        slider = new JSlider();
        slider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                
                time = slider.getValue();
                TimePointImage timePointImage = embryo.getImage(time); 
                for (SingleSlicePanel panel : panels){
                    panel.setImage(timePointImage, position);
                }  
                updateBorder();
            }
        });
        this.add(slider,BorderLayout.SOUTH);

    }
    public void changeTime(int time){
        slider.setValue(time);
    }
    public void incrementTime(){
        if (time < slider.getMaximum()){
            slider.setValue(time+1);
        }
    }
    public void decrementTime(){
        if (time > 1){
            slider.setValue(time-1);
        }
    }
    public void changePosition(long[] pos){
        for (int d=0 ; d<pos.length ; ++d){
            position[d] = pos[d];
        }
        updatePanelPositions();
    }
    public void changePosition(int dim,long pos){
        position[dim] = pos;
        updatePanelPositions();
    }
    public void updatePanelPositions(){
        for (int d=0 ; d<panels.length ; ++d){
            panels[d].setPosition(position);
        }
        updateBorder();
 
    }
    private void updateBorder(){
        titledBorder = BorderFactory.createTitledBorder(positionString());
        this.setBorder(titledBorder);        
    }
    private String positionString(){
        return String.format("(%d,%d,%d)@%d", position[0], position[1], position[2],time);
    }
    public void setEmbryo(ImagedEmbryo emb){
        this.embryo = emb;
//        time = emb.getTimes()/2;
        time = 210;
        
        TimePointImage timePointImage = emb.getImage(time);
        double[] xformDims = timePointImage.getDims();
        for (int d=0 ; d<position.length ; ++d){
            position[d] = (long)xformDims[d]/2;
        } 
        for (int d=0 ; d<nDims ; ++d){
            SingleSlicePanel panel = panels[d];
            panel.setImage(timePointImage, position);
            panel.setExtent(1,(int)xformDims[d]);
        }
        slider.setMinimum(0);
        slider.setMaximum(emb.getTimes());
        slider.setValue( 210);
    }
    public long[] getPosition(){
        return this.position;
    }
    int nDims;
    JSlider slider;
    int time;
    ImagedEmbryo embryo;
    TitledBorder titledBorder;
    SingleSlicePanel[] panels;
    long[] position;
}