/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.CompositeTimePointImage;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class SynchronizedMultipleSlicePanel extends JPanel implements ChangeListener,InvalidationListener  {
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
                showCurrentImage();
            }
        });
        this.add(slider,BorderLayout.SOUTH);
    }
    public void showCurrentImage(){
        timePointImage = embryo.getImage(time); 
        for (SingleSlicePanel panel : panels){
            panel.setImage(timePointImage, position);
        }  
        updateBorder();        
    }
    public void changeTime(int time){
        slider.setValue(time);
    }
    public void incrementTime(){
        if (time < slider.getMaximum()){
            Nucleus selected = this.embryo.selectedNucleus();
            if (selected != null && selected.getTime()==time){
                // track the selected nucleus forward
                List<Nucleus> next = this.embryo.nextNuclei(selected);
                if (next.size() > 0){
                    this.embryo.setSelectedNucleus(next.get(0));

                    if (next.size()>1){
                        // nucleus has divided
                        embryo.clearLabeled(time+1);
                        next.get(1).setLabeled(true);
                    }
                    this.changePosition(next.get(0).getCenter());
                }
            } else {
                slider.setValue(time+1);
            }
        }
    }
    public void decrementTime(){
        if (time > 1){
            Nucleus selected = this.embryo.selectedNucleus();
            if (selected!= null && selected.getTime()==time){
                // track the selected nucleus back in time
                Nucleus prev = this.embryo.previousNucleus(selected);
                if (prev != null){
                    embryo.setSelectedNucleus(prev);
                    this.changePosition(prev.getCenter());
                }
            }  else {          
                slider.setValue(time-1);
            }
        }
    }
    public void moveSelectedNucleus(int dim,int value){
        Nucleus selected = this.embryo.selectedNucleus();
        if (selected!= null){
            long[] center = selected.getCenter();
            center[dim] = center[dim] + (long)value;
            selected.setCenter(center);
            repaintPanels();
        }        
    }
    public void changeRadiusSelectedNucleus(int value){
        Nucleus selected = this.embryo.selectedNucleus();
        if (selected!= null){
            double v = selected.getRadius() + value;
            selected.setRadius(v);
            repaintPanels();
        }        
    }
    public void repaintPanels(){
        for (SingleSlicePanel p : panels){
            p.repaint();
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
        String l = "";
        if (embryo!= null && embryo.selectedNucleus() != null){
            l = embryo.selectedNucleus().getName();
        }
        return String.format("(%d,%d,%d)@%d %s", position[0], position[1], position[2],time,l);
    }
    public void setEmbryo(ImagedEmbryo emb){
        this.embryo = emb;
        time = emb.getTimes()/2;
        timePointImage = emb.getImage(time);
        double[] minPosition = timePointImage.getMinPosition();
        double[] maxPosition = timePointImage.getMaxPosition();        
        for (int d=0 ; d<position.length ; ++d){
                position[d] = (long)(0.5*(minPosition[d]+maxPosition[d]));
        } 
        for (int d=0 ; d<nDims ; ++d){
            SingleSlicePanel panel = panels[d];
            panel.setEmbryo(emb);
            panel.setExtent(minPosition[d],maxPosition[d]);
            panel.setImage(timePointImage, position);
        }
        slider.setMinimum(emb.getMinTime());
        slider.setMaximum(emb.getMaxTime());
        slider.setValue( (emb.getMaxTime()+emb.getMinTime())/2);
//        embryo.addListener(this);
        embryo.setPanel(this);
    }
    public long[] getPosition(){
        return this.position;
    }
    public ImagedEmbryo getEmbryo(){
        return embryo;
    }
    public CompositeTimePointImage getCuurrentImage(){
        return this.timePointImage;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        for (SingleSlicePanel p : panels){
            p.stateChanged(e);
        }
    }    

    @Override
    public void invalidated(Observable observable) {
        ChangeEvent e = new ChangeEvent(observable);
        for (SingleSlicePanel p : panels){
            p.stateChanged(e);
        }
    }
    int nDims;
    JSlider slider;
    int time;
    ImagedEmbryo embryo;
    CompositeTimePointImage timePointImage;
    TitledBorder titledBorder;
    SingleSlicePanel[] panels;
    long[] position;


}
