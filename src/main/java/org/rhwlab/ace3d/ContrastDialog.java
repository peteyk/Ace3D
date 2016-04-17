/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author gevirl
 */
public class ContrastDialog extends JDialog {
    public ContrastDialog(JFrame owner,DataSetProperties props,String title,int sliderMin,int sliderMax){
        super(owner,title,false);
        this.frame = (Ace3D_Frame)owner;
        this.props = props;
        this.setSize(300,125);
        this.setLocationRelativeTo(owner);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(4,1));
        this.add(centerPanel,BorderLayout.CENTER);
        
        JPanel minPanel = new JPanel();
        minPanel.setLayout(new BoxLayout(minPanel,BoxLayout.X_AXIS));
        minPanel.add(new JLabel("Minimum"));
        minField = new JTextField(Integer.toString(sliderMin));
        minPanel.add(minField);
        
        minSlider = new JSlider(sliderMin,sliderMax,sliderMin);
        minSlider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider)e.getSource();
                int v = slider.getValue();
                if (v > maxSlider.getValue()){
                    minSlider.setValue(maxSlider.getValue()-1);
                }
                if (!slider.getValueIsAdjusting()){
                    minField.setText(Integer.toString(v));
                    notifyContrastChanged();
                }
                
            }
        });
        centerPanel.add(minPanel);
        centerPanel.add(minSlider);
        
        JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new BoxLayout(maxPanel,BoxLayout.X_AXIS));
        maxPanel.add(new JLabel("Maximum"));
        maxField = new JTextField(Integer.toString(sliderMax));
        maxPanel.add(maxField);
        maxSlider = new JSlider(sliderMin,sliderMax,sliderMax);
        maxSlider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider)e.getSource();
                int v = slider.getValue();
                if (v < minSlider.getValue()){
                    maxSlider.setValue(minSlider.getValue()+1);
                }
                if (!slider.getValueIsAdjusting()){
                    maxField.setText(Integer.toString(v));
                    notifyContrastChanged();
                }                
            }
        });
        centerPanel.add(maxPanel);
        centerPanel.add(maxSlider);        
        setAllEnabled(false);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        JButton okButton = new JButton("Ok");
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);        
        buttonPanel.add(Box.createHorizontalGlue());
        
//        this.add(buttonPanel,BorderLayout.SOUTH);
        
        autoButton = new JRadioButton("Auto");
        autoButton.setSelected(true);
        autoButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoButton.isSelected()){
                    setAllEnabled(false);
                } else {
                    setAllEnabled(true);
                }
                notifyContrastChanged();
            }
        });
        this.add(autoButton,BorderLayout.NORTH);
    }
    private void setAllEnabled(boolean value){
        minSlider.setEnabled(value);
        maxSlider.setEnabled(value);
        minField.setEnabled(value);
        maxField.setEnabled(value);        
    }
    private void ok(){
        
    }
    private void cancel(){
        
    }
    public boolean getAuto(){
        return auto;
    }
    public int getMin(){
        return min;
    }
    public int getMax(){
        return max;
    }
    public void notifyContrastChanged(){
        props.autoContrast = autoButton.isSelected();
        props.min = (float)minSlider.getValue();
        props.max = (float)maxSlider.getValue();
        frame.refreshImage();
    }
    boolean auto;
    int min;
    int max;
    
    Ace3D_Frame frame;
    DataSetProperties props;
    JRadioButton autoButton;
    JSlider minSlider;
    JSlider maxSlider;
    JTextField minField;
    JTextField maxField;
}
