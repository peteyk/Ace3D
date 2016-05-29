/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.ImagePlus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.CompositeTimePointImage;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class SingleSlicePanel extends JPanel implements ChangeListener {
    public SingleSlicePanel(String ttl,int d,final SynchronizedMultipleSlicePanel par){
        this.parent = par;
        this.title = ttl;
        this.dim =d;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED) );
        slider = new JSlider();
        slider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                SingleSlicePanel.this.setSlice(slider.getValue());
                 if (parent != null && timePointImage != null) parent.changePosition(dim,slice);                
            }
        });
        this.add(slider,BorderLayout.SOUTH);
        slicePanel = new JPanel(){
            
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D panelg2 = (Graphics2D)g;
                Dimension panelSize = this.getSize();
//                System.out.printf("panel(%d,%d)\n",panelSize.width,panelSize.height);

                if (timePointImage != null){                    
 //                   Graphics2D g2 = (Graphics2D) g;
                    buffered = timePointImage.getBufferedImage(dim,slice);
                    Graphics2D g2 = buffered.createGraphics();
                    if (buffered == null){
                        return ;
                    }
  
                    
                    bufH = buffered.getHeight();
                    bufW = buffered.getWidth();

                    double rw = (double)panelSize.width/(double)bufW;
                    double rh = (double)panelSize.height/(double)bufH;
                    scale = Math.max(1.0, Math.min(rw, rh));

//                    AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
//                    g2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);

                    // draw lines showing the current position
                    if (Ace3D_Frame.locationIndicated()){
                        long xPos = screenX(imagePosition);
                        long yPos = screenY(imagePosition);                        
                        g2.drawLine(0, (int)(yPos), (int)(bufW),(int)(yPos));
                        g2.drawLine((int)(xPos), 0,(int)(xPos), (int)(bufH));
//                        g2.drawLine(0, (int)(yPos), (int)(scale*bufW),(int)(yPos));
//                        g2.drawLine((int)(xPos), 0,(int)(xPos), (int)(scale*bufH));                        
                    }
                    // add an axis identifyer
                    g2.drawLine(10,5,20,5);  //horizontal
                    g2.drawString(horizontalAxis(), 25,10);
                    g2.drawLine(10,5,10,15);  //vertical
                    g2.drawString(verticalAxis(),10, 25);
                    
                    
                   // draw any nuclei
                   if (Ace3D_Frame.nucleiIndicated()){
                       drawNuclei(g2);
//                       g2.setColor(save);
                   }

                   if (Ace3D_Frame.sistersIndicated()){
                        g2.setColor(Color.GREEN);
                        drawSisters(g2);  // draw the sister indicator   
//                        g2.setColor(save);
                   }                   
                   if (Ace3D_Frame.labelNuclei()){
                        g2.setColor(Color.GREEN);
                        labelAllNuclei(g2);
//                        g2.setColor(save);
                   }
                   if (Ace3D_Frame.labelSelectedNucleus()){
                       g2.setColor(Color.RED);
                       labelSelectedNucleus(g2);
//                       g2.setColor(save);
                   }
                   g2.setColor(Color.BLUE);
                   labelMarkedNuclei(g2);
                  
                   
                    // clear the panel
                    Color save = panelg2.getColor();
                    panelg2.setColor(Color.white);
                    Dimension d = this.getSize();
                    panelg2.fillRect(0,0,d.width,d.height);                   
                    AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
                    panelg2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);   
                    panelg2.setColor(save);
                }
            }
        };

        this.add(slicePanel,BorderLayout.CENTER);
        slicePanel.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                char k = e.getKeyChar();
//                int mask = KeyEvent.SHIFT_DOWN_MASK;
//                int modifier = e.getModifiersEx();
 //               if ((modifier&mask) == mask){
                    long pos[];
                    switch (k) {
                    case 'z':
                        parent.changeRadiusSelectedNucleus(-1);
                        break;
                    case 'x':
                        parent.changeRadiusSelectedNucleus(1);
                        break;
                    case 'A':
                        parent.moveSelectedNucleus(imageXDirection(), -1);
                        break;
                    case 'D':
                        parent.moveSelectedNucleus(imageXDirection(), 1);
                        break;
                    case 'S':
                        parent.moveSelectedNucleus(imageYDirection(), 1);
                        break;
                    case 'W':
                        parent.moveSelectedNucleus(imageYDirection(), -1);
                        break;
                    case 'w':
                        pos = parent.getPosition();
                        pos[dim] = pos[dim] + 1;
                        setPosition(pos);
                        break;  
                    case 's':
                        pos = parent.getPosition();
                        pos[dim] = pos[dim] - 1;
                        setPosition(pos);
                        break; 
                    case 'a':
                        parent.decrementTime();
                        break;
                    case 'd':
                        parent.incrementTime();
                        break;
                    }
                
            }
        });
        this.addMouseWheelListener(new MouseAdapter(){
            @Override
            public void mouseWheelMoved(MouseWheelEvent e){
                slider.setValue(slider.getValue()+e.getWheelRotation());
            }            
        });
        this.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){
                slicePanel.requestFocusInWindow();
            }
            @Override
            public void mouseClicked(MouseEvent e){
                
                long [] pos = imageCoordinates(e.getX(),e.getY());
                if (e.getButton() == MouseEvent.BUTTON1){
                    int mask = MouseEvent.SHIFT_DOWN_MASK;
                    if (( e.getModifiersEx()&mask) == mask){
                        // making a new nucleus  - shift left button
                        long[] parentPos = parent.getPosition();
                        long[] center = new long[parentPos.length];
                        double radius = 0.0;
                        for (int d=0 ; d<pos.length ; ++d){
                            center[d] = parentPos[d];     //(image coordinates)
                            long l = pos[d]-parentPos[d];
                            radius = radius + l*l;  // (image coordinates)
                        }
                        radius = Math.sqrt(radius);
                        embryo.addNucleus(new Nucleus(timePointImage.getTime(),center,radius));
                        parent.repaint();                        
                    }
                    else if (parent != null ){
                        parent.changePosition(pos);  // left button moves to location
                    }                 
                } 
                else if (e.getButton() == MouseEvent.BUTTON3){
                    // finding the closest nucleus
                    Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
                    double min = Double.MAX_VALUE;
                    Nucleus closest = null;
                    for (Nucleus nuc : nucs){
                        double d = nuc.distanceSqaured(pos);
                        if (d < min){
                            closest = nuc;
                            min = d;
                        }
                    }                    
                    int mask = MouseEvent.SHIFT_DOWN_MASK;
                    if (( e.getModifiersEx()&mask) == mask){   // right shift toggles the marked status of the nucleus
                        boolean mark = closest.getMarked();
                        if (mark){
                            embryo.setMarked(closest, false);
                        } else {
                            embryo.setMarked(closest, true);
                        }
                    }
                    else {  // right mouse button selects a nucleus
                        embryo.setSelectedNucleus(closest);
                       
                    }
                     parent.repaint();
                }
            }
        });        
    }

    public int getDimension(){
        return dim;
    }
    // set the position (image coordinates)
    public void setPosition(long[] pos){

        imagePosition = pos;
        setSlice(pos[dim]);
        slider.setValue((int)pos[dim]);
        this.repaint();
    }
/*    
    // return the screen x coordinate given image coordinates
    public int screenX(long[] p){
        if (dim==0){
            return (int)((p[1]-timePointImage.minPosition(1))*scale*bufW/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[0]-timePointImage.minPosition(0))*scale*bufW/(timePointImage.maxPosition(0)-timePointImage.minPosition(0)));
        }
    }

    // return the screen y coordinate given image coordinates
    public int screenY(long[] p){
        if (dim==2){
            return (int)((p[1]-timePointImage.minPosition(1))*scale*bufH/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[2]-timePointImage.minPosition(2))*scale*bufH/(timePointImage.maxPosition(2)-timePointImage.minPosition(2)));
        }
    } 
*/    
    public int screenX(long[] p){
        if (dim==0){
            return (int)((p[1]-timePointImage.minPosition(1))*bufW/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[0]-timePointImage.minPosition(0))*bufW/(timePointImage.maxPosition(0)-timePointImage.minPosition(0)));
        }
    }
    static public int screenX(long[] p,int dim,int bufW){
 //       System.out.printf("screenX: p =(%d,%d,%d),dim=%d,bufH=%d\n",p[0],p[1],p[2],dim,bufW);
        if (dim==0){
            return (int)((p[1]-TimePointImage.getMinPosition(1))*bufW/(TimePointImage.getMaxPosition(1)-TimePointImage.getMinPosition(1)));
        } else {
            return (int)((p[0]-TimePointImage.getMinPosition(0))*bufW/(TimePointImage.getMaxPosition(0)-TimePointImage.getMinPosition(0)));
        }
    }

    // return the screen y coordinate given image coordinates
    public int screenY(long[] p){
        if (dim==2){
            return (int)((p[1]-timePointImage.minPosition(1))*bufH/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[2]-timePointImage.minPosition(2))*bufH/(timePointImage.maxPosition(2)-timePointImage.minPosition(2)));
        }
    }   
    static public int screenY(long[] p,int dim,int bufH){
 //       System.out.printf("screenY: p =(%d,%d,%d),dim=%d,bufH=%d\n",p[0],p[1],p[2],dim,bufH);
        if (dim==2){
            return (int)((p[1]-TimePointImage.getMinPosition(1))*bufH/(TimePointImage.getMaxPosition(1)-TimePointImage.getMinPosition(1)));
        } else {
            return (int)((p[2]-TimePointImage.getMinPosition(2))*bufH/(TimePointImage.getMaxPosition(2)-TimePointImage.getMinPosition(2)));
        }
    }     
    public long[] imageCoordinates(int screenX,int screenY){
        long[] pos = new long[timePointImage.numDimensions()];
        pos[dim] = SingleSlicePanel.this.slice;
        double bufX = screenX/scale/bufW;
        double bufY = screenY/scale/bufH;
        int ix = imageXDirection();
        int iy = imageYDirection();
        
        pos[ix] =(long)( timePointImage.minPosition(ix) + bufX*(timePointImage.maxPosition(ix)-timePointImage.minPosition(ix)));
        pos[imageYDirection()] = (long)( timePointImage.minPosition(iy) + bufY*(timePointImage.maxPosition(iy)-timePointImage.minPosition(iy)));;
        
        return pos;
    }    
    public int imageXDirection(){
        if (dim==0){
            return 1;
        }
        return 0;
    }    
    public int imageYDirection(){
        if (dim==2){
            return 1;
        }
        return 2;
    }  
    private boolean visible(Nucleus nuc){
        return nuc.isVisible(slice, dim);
       
    }
    final private void setSlice(long p){
        slice = p;

    }
    public long getSlice(){
        return this.slice;
    }
    
    final public void setImage(CompositeTimePointImage tpi,long[] pos){
        this.timePointImage = tpi;
        this.setPosition(pos);
        this.repaint();
    }
    public void setExtent(double min,double max) {
        slider.setMaximum((int)max);
        slider.setMinimum((int)min);
        slider.setValue((int)((min+max)/2));        
    
}
    private String horizontalAxis(){
        if (dim == 0){
            return "y";
        } 
        return "x";
    }
    private String verticalAxis(){
        if (dim==2){
            return "y";
        }
        return "z";
    }
    private void drawNuclei(Graphics2D g2){
       Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
       for (Nucleus nuc : nucs){
           if (nuc == this.embryo.selectedNucleus()){
               int asdfyusgdf=0;
           }
           Shape nucShape = nuc.getShape(slice, dim, bufW, bufH);   
           if (nucShape != null){
                if (nuc == this.embryo.selectedNucleus()){
                    g2.setColor(Color.RED);
                }
                else if (nuc.getMarked()){
                    g2.setColor(Color.BLUE);
                }
                else {
                    g2.setColor(Color.GREEN);
                }
                g2.draw(nucShape);               
           }
           
           
/*           
           
            long[] center = nuc.getCenter();  // image corrdinates
            double r = nuc.getRadius();   // image corrdinates
            double delta = Math.abs(slice-center[dim]);   // image corrdinates
            if (nuc.isVisible(slice, dim)){
                double rad = Math.sqrt(r*r-delta*delta);  //image coordinates
                int ix = imageXDirection();
                int iy = imageYDirection(); 
                long[] low = new long[center.length];
                long[] high = new long[center.length];
                low[dim] = slice;
                high[dim] = slice;
                low[ix] = center[ix] - (long)rad;
                low[iy] = center[iy] - (long)rad;
                high[ix] = center[ix] + (long)rad;
                high[iy] = center[iy] + (long)rad;  
                int scrX = screenX(low);
                int scrY = screenY(low);
                int scrHighX = screenX(high);
                int scrHighY = screenY(high);
                Ellipse2D.Double ellipse = new Ellipse2D.Double(scrX,scrY,scrHighX-scrX,scrHighY-scrY);
                


                
                if (nuc == this.embryo.selectedNucleus()){
                    g2.setColor(Color.RED);
                }else {
                    g2.setColor(Color.GREEN);
                }
                g2.draw(ellipse);
               
            }
            
*/            
            
       }        
    }
    private void drawSisters(Graphics2D g2){
        NucleusFile nucFile = parent.getEmbryo().getNucleusFile();
        
        TreeMap<Nucleus,Nucleus> sisterPairs = new TreeMap<>();
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
           Nucleus sisterNuc = nucFile.sister(nuc);
           if (sisterNuc != null){
               if (sisterPairs.get(sisterNuc)==null){
                   sisterPairs.put(nuc,sisterNuc);
               }
           }
        } 
        
        for (Entry<Nucleus,Nucleus> entry : sisterPairs.entrySet()){
            Nucleus nuc1 = entry.getKey();
            Nucleus nuc2 = entry.getValue();
            if (visible(nuc1)||visible(nuc2)){
                g2.drawLine(screenX(nuc1.getCenter()),screenY(nuc1.getCenter()),screenX(nuc2.getCenter()),screenY(nuc2.getCenter()));
            }
        }
    }
    private void labelMarkedNuclei(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if (nuc.getLabeled()){
                labelNucleus(g2,nuc);
            }
        }
        
    }
    private void labelSelectedNucleus(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if (nuc == embryo.selectedNucleus()){
                labelNucleus(g2,nuc);
            }
        }        
    }
    // label all visible nuclei 
    private void labelAllNuclei(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if(visible(nuc)){
                labelNucleus(g2,nuc);
            }
        }        
    }
    private void labelNucleus(Graphics g2,Nucleus nuc){
        g2.drawString(nuc.getName(),screenX(nuc.getCenter()),screenY(nuc.getCenter()));
    }
    public void setEmbryo(ImagedEmbryo em){
        this.embryo = em;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.slicePanel.repaint();
    }
    
    SynchronizedMultipleSlicePanel parent;
    JPanel slicePanel;
    double scale=1.0;
    int bufW;
    int bufH;
    long slice;
    BufferedImage buffered;
    long bufferedSlice=Long.MAX_VALUE;
    int bufferedTime = Integer.MAX_VALUE;
    DataSetProperties bufferedProps;
    int dim;
    JSlider slider;
    final String title;
    CompositeTimePointImage timePointImage;
    ImagedEmbryo embryo;
    long[] imagePosition;

}
