/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class NavigationTreePanel extends JPanel implements ChangeListener{
    public NavigationTreePanel(ImagedEmbryo emb){
        embryo = emb;
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        NavigationHeaderPanel headPanel = (NavigationHeaderPanel)e.getSource();
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (embryo == null){
            return;
        }
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null){
            return;
        }

    }
    ImagedEmbryo embryo;
    String root;
    int maxTime;
}
