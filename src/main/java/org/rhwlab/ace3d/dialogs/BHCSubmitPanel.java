/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author gevirl
 */
public class BHCSubmitPanel extends JPanel {
    public BHCSubmitPanel(String init){
        this.setLayout(new GridLayout(10,3));
        
        this.add(new JLabel("Cores to request"));
        this.add(cores);
        this.add(new JLabel("(1-16"));
        
        this.add(new JLabel("Memory per Core (Gigs)"));
        this.add(memory);
        this.add(new JLabel(""));
        
        
        this.add(new JLabel("Force Replacement"));
        this.add(force);  
        this.add(new JLabel(""));
        
        this.add(new JLabel("Series Directory"));
        seriesDir = new JTextField(init);
        this.add(seriesDir);
        JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(seriesDir.getText().trim());
                if (chooser.showOpenDialog(BHCSubmitPanel.this) == JFileChooser.APPROVE_OPTION){
                    seriesDir.setText(chooser.getSelectedFile().getPath());
                }  
            }
        });
        this.add(browse);
        
        this.add(new JLabel("Start Time"));
        this.add(startTime);
        this.add(new JLabel(""));
        
        this.add(new JLabel("End Time"));
        this.add(endTime);
        this.add(new JLabel(""));  
        
        this.add(new JLabel("Segmentation Threshold"));
        this.add(segProb);
        this.add(new JLabel("0-100"));
        
        this.add(new JLabel("Concentration parameter"));
        this.add(alpha);
        this.add(new JLabel("1000-1000000"));
        
        this.add(new JLabel("Variance"));
        this.add(variance);
        this.add(new JLabel("5-200"));
        
        this.add(new JLabel("Degrees Freedom"));
        this.add(degrees);
        this.add(new JLabel("4-20"));
    }
    public boolean isForce(){
        return force.isSelected();
    }
    public int getMemory(){
        try {
            return  Integer.valueOf(memory.getText().trim());
        } catch (Exception exc){
            return 5;
        }
    }
    public File getSeriesDirectory(){
        return new File(seriesDir.getText().trim());
    }
    public int getStartTime(){
        try {
            return  Integer.valueOf(startTime.getText().trim());
        } catch (Exception exc){
            return 1;
        }
    }
    public int getEndTime(){
        try {
            return  Integer.valueOf(endTime.getText().trim());
        } catch (Exception exc){
            return 400;
        }
    } 
    public double getAlpha(){
        try {
            return Double.valueOf(alpha.getText().trim());
        } catch (Exception exc){
            return 10000;
        }
    }
    public double getVariance(){
        try {
            return Double.valueOf(variance.getText().trim());
        } catch (Exception exc){
            return 20;
        }        
    }
    public int getDegrees(){
        try {
            return Integer.valueOf(degrees.getText().trim());
        } catch (Exception exc){
            return 10;
        }
    }
    public int getProb(){
        try {
            return Integer.valueOf(segProb.getText().trim());
        } catch (Exception exc){
            return 50;
        }
    }    
    public int getCores(){
        try {
            return  Integer.valueOf(cores.getText().trim());
        } catch (Exception exc){
            return 4;
        }
    }    
    JCheckBox force = new JCheckBox();
    JTextField cores = new JTextField("4");
    JTextField memory = new JTextField("5");
    JTextField seriesDir;
    JTextField startTime = new JTextField("1");
    JTextField endTime = new JTextField("400");
    JTextField alpha = new JTextField("10000");
    JTextField variance = new JTextField("20");
    JTextField degrees = new JTextField("10");
    JTextField segProb = new JTextField("50");
}
