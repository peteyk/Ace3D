/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.process.LUT;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.CellImage;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class NavigationTreePanel extends JPanel implements ChangeListener{
    public NavigationTreePanel(ImagedEmbryo emb){
        lut = LUT.createLutFromColor(Color.GREEN);
        embryo = emb;
        lut.min = 0;
        lut.max = 255;
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        NavigationHeaderPanel headPanel = (NavigationHeaderPanel)e.getSource();
        String rootCellName = headPanel.getRoot();
        rootCell = embryo.getNucleusFile().getCell(rootCellName);
        this.repaint();
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
        if (rootCell == null){
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        CellImage cellImage = new CellImage();
        BufferedImage buffered = cellImage.getImage(rootCell,lut);
        int h = buffered.getHeight();
        int w = buffered.getWidth();
        Dimension d = this.getSize();
        double scale = Math.min((double)d.width/(double)w, (double)d.height/(double)h);
        
        // clear the panel
        Color save = g2.getColor();
        g2.setColor(Color.white);
 
        g2.fillRect(0,0,d.width,d.height);
        g2.setColor(save);
        
        AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
        g2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);      
    }
    LUT lut;
    ImagedEmbryo embryo;
    Cell rootCell;
    int maxTime;
}
