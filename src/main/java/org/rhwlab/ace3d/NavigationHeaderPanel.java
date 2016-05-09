/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author gevirl
 */
public class NavigationHeaderPanel extends JPanel {
    public NavigationHeaderPanel(NavigationTreePanel p){
        this.treePanel = p;
        
        this.add(new JLabel("Root: "));
        rootField = new JTextField();
        rootField.setColumns(20);
        rootField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });
        this.add(rootField);
        
        this.add(new JLabel("Max Time:"));
        maxTime = new JTextField();
        maxTime.setColumns(10);
        maxTime.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });
        this.add(maxTime);
        
    }

    public String getRoot(){
        return rootField.getText().trim();
    }
    public int getMaxTime(){
        return Integer.valueOf(maxTime.getText().trim());
    }
    
    NavigationTreePanel treePanel;
    JTextField rootField;
    JTextField maxTime;
}
