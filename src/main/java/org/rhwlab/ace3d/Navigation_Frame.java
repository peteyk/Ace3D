/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.plugin.PlugIn;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JFrame;

/**
 *
 * @author gevirl
 */
public class Navigation_Frame extends JFrame implements PlugIn,Observer {

    @Override
    public void run(String arg) {
        this.setSize(1800,600);
        this.setVisible(true);
    }

    @Override
    public void update(Observable o, Object arg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
