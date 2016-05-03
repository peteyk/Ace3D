/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author gevirl
 */
public class NavigationHeaderPanel extends JPanel {
    public NavigationHeaderPanel(){
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.add(new JLabel("Nuclei at the Start Time"));
        
    }
    JList nucleiList;
    JTextField startTime;
    JTextField endTime;
}
