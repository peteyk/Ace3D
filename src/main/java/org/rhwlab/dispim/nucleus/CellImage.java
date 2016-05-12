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
        int endTime = cell.maxTime();
        int h = (int)(timeScale*(endTime - cell.firstTime() +1));
        int w = (int)(cellWidth*(cell.leaves().size()+1));
        BufferedImage image = new BufferedImage((int)w,(int)h,BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        BasicStroke stroke = new BasicStroke(strokeWidth);
        g2.setStroke(stroke);
        drawCell(0,w,0,cell);
        return image;
    }
    int drawCell(int xLeft,int width,int yStart,Cell cell){
        int xStart = xLeft+width/2;
        int c0 = 0;
        int c1 = 0;
        int childY0  = yStart + (int)(timeScale*(cell.lastTime()-cell.firstTime()));
        if (!cell.children.isEmpty()){
            //draw the children
            int n1 = cell.children.get(1).leaves().size();
            int n0 = cell.children.get(0).leaves().size();
            double f = (double)n0/(double)(n0+n1);
            int w0 = (int)(f*(double)width);
            int w1 = width - w0;
            c0 = drawCell(xLeft,w0,childY0,cell.children.get(0));
            c1 = drawCell(xLeft+w0,w1,childY0,cell.children.get(1));
            xStart = (c0+c1)/2;
        }
        // drawing the veritical line
        int y0 = yStart;
        int yend = 0;
        
        Iterator<Integer> iter = cell.nuclei.navigableKeySet().iterator();
        int t0 = iter.next();
        Nucleus nuc = cell.getNucleus(t0);
        while(iter.hasNext()){
            int time = iter.next();
            int y1 = (int)((time-t0)*timeScale)+yStart;
            int exp = nuc.getExpression();
            int rgb = lut.getRGB(exp);
            Color c = new Color(rgb);
            g2.setColor(c);
            g2.drawLine(xStart, y0,xStart , y1);
            y0 = y1;
            nuc = cell.getNucleus(time);
            yend = y1;
        }
        if (!cell.children.isEmpty()){
            // drawing the horizontal line
            g2.drawLine(c0, childY0, c1, childY0);
//            System.out.printf("Horiz %s c0=%d,c1=%d,yend=%d\n", cell.getName(),c0,c1,yend);
        }
//        System.out.printf("Vertical %s x=%d,y0=%d,y1=%d\n", cell.getName(),xStart,yStart,yend);
        return xStart;
    }
    LUT lut;
    Graphics2D g2;
    static int strokeWidth=3;
    static double timeScale = 10.0;
    static double cellWidth = 30;
}
