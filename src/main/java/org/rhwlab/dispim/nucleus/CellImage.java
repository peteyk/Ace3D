/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import ij.process.LUT;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;

/**
 *
 * @author gevirl
 */
public class CellImage {
    public BufferedImage getImage(Cell cell,LUT lut){
        this.lut = lut;
        lut.min = cell.getMinExpression();
        lut.max = cell.getMaxExpression();
        double h = timeScale*(cell.maxTime() - cell.firstTime() +1);
        double w = cellWidth*cell.leaves().size();
        BufferedImage image = new BufferedImage((int)w,(int)h,BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        BasicStroke stroke = new BasicStroke(3);
        g2.setStroke(stroke);
        drawCell((int)w,0,cell);
        return image;
    }
    void drawCell(int width,int yStart,Cell cell){
        int wl = width/2;

        int childY0  = yStart + (int)timeScale*(cell.lastTime()-cell.firstTime()+1);
        if (!cell.children.isEmpty()){
            //draw the children
            int n1 = cell.children.get(1).leaves().size();
            int n0 = cell.children.get(0).leaves().size();
            double f = (double)n0/(double)(n0+n1);
            wl = (int)f*width;
            int wr = width - wl;
            drawCell(wl,childY0,cell.children.get(0));
            drawCell(wr,childY0,cell.children.get(1));
        }
        // drawing the veritical line
        int y0 = 0;
        int yend = 0;
        Iterator<Integer> iter = cell.nuclei.navigableKeySet().iterator();
        int t0 = iter.next();
        Nucleus nuc = cell.getNucleus(t0);
        while(iter.hasNext()){
            int time = iter.next();
            int y1 = (int)((time-t0)*timeScale);
            Color c = new Color(lut.getRGB(nuc.getExpression()));
            g2.setColor(c);
//            g2.drawLine(x, y0, x, y1);
            y0 = y1;
            t0 = time;
            nuc = cell.getNucleus(time);
            yend = y1;
        }
        // drawing the horizontal line
        
        // draw the children
        for (Cell child : cell.getChildren()){
 //           drawCellAt(x,yend,cell);
        }
    }
    LUT lut;
    Graphics2D g2;
    static double timeScale = 1.0;
    static double cellWidth = 30;
}
