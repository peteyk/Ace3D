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
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public class CellImage {
    public BufferedImage getImage(Cell cell,int maxTime,LUT lut,boolean nodes,boolean leaves,double timeScale,double cellWidth){
        
        this.lut = lut;
        this.maxTime = maxTime;
        this.labelNodes = nodes;
        this.labelLeaves = leaves;
        this.timeScale = timeScale;
        this.cellWidth = cellWidth;
        int h = (int)(timeScale*(maxTime - cell.firstTime() +1));
        int w = (int)(cellWidth*(cell.leaves(maxTime).size()+1));
        BufferedImage image = new BufferedImage((int)w+20,(int)h,BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        BasicStroke stroke = new BasicStroke(strokeWidth);
        g2.setStroke(stroke);
        locations.clear();
        drawCell(0,w,0,cell);

        return image;
    }
    // draw a cell and its descendents into a portion of the bufferedimage
    // xLeft,width, and yStart determine the portion of the bufferimage to use
    // returns the x position where the cell was drawn
    private double drawCell(int xLeft,int width,double yStart,Cell cell){
        double xStart = xLeft+width/2; // cell is drawn in middle if no children are drawn
        double c0 = 0;
        double c1 = 0;
        boolean hasChildren = false;
        // ?? draw children
        if (this.maxTime >= cell.lastTime()){
            
            double childY0  = yStart + (timeScale*(1+cell.lastTime()-cell.firstTime()));
            if (!cell.children.isEmpty()){
                //draw the children
                int n1 = cell.children.get(1).leaves(maxTime).size();
                int n0 = cell.children.get(0).leaves(maxTime).size();
                double f = (double)n0/(double)(n0+n1);
                int w0 = (int)(f*(double)width);
                int w1 = width - w0;
                c0 = drawCell(xLeft   ,w0,childY0,cell.children.get(0));
                c1 = drawCell(xLeft+w0,w1,childY0,cell.children.get(1));
                xStart = (c0+c1)/2;  // draw the parent in the middle of the children
                hasChildren = true;
            }            
        }

        // drawing the vertical line
        double y0 = yStart;
        double x0 = xStart;
        double yend = 0;
        int endt = Math.min(cell.lastTime(), this.maxTime);
        for (int t = cell.firstTime() ; t <=endt ; ++t){
            Nucleus nuc = cell.getNucleus(t);
            double y1 = y0 + timeScale;
            Line2D.Double line = new Line2D.Double(x0,y0,x0,y1);

            int exp = (int)nuc.getExpression();
            int rgb = lut.getRGB(exp);
            Color c = new Color(rgb);
            g2.setColor(c);
            g2.draw(line);
            y0 = y1;
            yend = y0;
        }
        CellLocation loc = new CellLocation(cell.getName(),xStart,yStart,yend);
        locations.add(loc);
        if(hasChildren){
             // drawing the horizontal line
            Line2D.Double horiz = new Line2D.Double(c0, yend, c1, yend);
            g2.draw(horiz);
           
  //          System.out.printf("Horiz %s c0=%f,c1=%f,yend=%f\n", cell.getName(),c0,c1,yend);
        }
 //       System.out.printf("Vertical %s x=%f,y0=%f,y1=%f\n", cell.getName(),xStart,yStart,yend);
        if (cell.isLeaf()){
            if (labelLeaves){
                labelCell(cell,x0,(yStart+yend)/2.0);
            }
        } else if (labelNodes){
            labelCell(cell,x0,(yStart+yend)/2.0);
        }
        return xStart;
    }
    private void labelCell(Cell cell, double x,double y){
        g2.setColor(Color.BLACK);
        AffineTransform save = g2.getTransform();
        AffineTransform xform = (AffineTransform)save.clone();
       
        xform.translate(5.0+x,y);
        xform.rotate(-Math.PI/4.0);
        g2.setTransform(xform);
        float zero = (float)0.0;
        g2.drawString(cell.getName(),zero,zero);
        g2.setTransform(save);
    }
    public CellLocation cellAtLocation(int x,int y){
        CellLocation ret = null;
        double dMin = Double.MAX_VALUE;
        for (CellLocation loc : locations){
            if (loc.y0<=y && y<=loc.y1){
                double d = Math.abs(loc.x-x);
                if (d <dMin){
                    dMin = d;
                    ret = loc;
                }
            }
        }
        return ret;
    }
    public class CellLocation {
        public CellLocation(String name,double x,double y0,double y1){
            this.name = name;
            this.x = x;
            this.y0 = y0;
            this.y1 = y1;
        }
        public String name;
        public double x;
        public double y0;
        public double y1;
    }
    int maxTime;
    boolean labelNodes;
    boolean labelLeaves;
    LUT lut;
    Graphics2D g2;
    static int strokeWidth=3;
    double timeScale;
    double cellWidth;
    Set<CellLocation> locations = new HashSet<>();
}
