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
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.IntervalView;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class SingleSlicePanel extends JPanel {
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
                 if (parent != null )parent.changePosition(dim,slice);                
            }
        });
        this.add(slider,BorderLayout.SOUTH);
        slicePanel = new JPanel(){
            
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Dimension panelSize = this.getSize();
//                System.out.printf("panel(%d,%d)\n",panelSize.width,panelSize.height);

                if (timePointImage != null){
                    IntervalView iv = timePointImage.getImage(dim, slice);
                    imagePlus = ImageJFunctions.wrap(iv, title);
                    if (autoContrast){
                        minContrast = timePointImage.getMin();
                        maxContrast = timePointImage.getMax();
                    }
                    imagePlus.setDisplayRange(minContrast,maxContrast);
                    Graphics2D g2 = (Graphics2D) g;

                    // clear the panel
                    Color save = g2.getColor();
                    g2.setColor(Color.white);
                    Dimension d = this.getSize();
                    g2.fillRect(0,0,d.width,d.height);

                    BufferedImage buffered = imagePlus.getBufferedImage();
                    int h = buffered.getHeight();
                    int w = buffered.getWidth();

                    double rw = (double)panelSize.width/(double)w;
                    double rh = (double)panelSize.height/(double)h;
                    scale = Math.max(1.0, Math.min(rw, rh));

                    AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
                    g2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);

                    // draw lines showing the current position
                    g2.drawLine(0, (int)(scale*yPos), (int)(scale*w),(int)(scale*yPos));
                    g2.drawLine((int)(scale*xPos), 0,(int)(scale*xPos), (int)(scale*h));

                    // add an axis identifyer
                    g2.drawLine(10,5,20,5);  //horizontal
                    g2.drawString(horizontalAxis(), 25,10);
                    g2.drawLine(10,5,10,15);  //vertical
                    g2.drawString(verticalAxis(),10, 25);
                    
                    
                   // draw any nuclei
                   if (Ace3D_Frame.nucleiIndicated()){
                       drawNuclei(g2);
                       g2.setColor(save);
                   }

                   if (Ace3D_Frame.sistersIndicated()){
                        g2.setColor(Color.GREEN);
                        drawSisters(g2);  // draw the sister indicator   
                        g2.setColor(save);
                   }                   
                   if (Ace3D_Frame.labelNuclei()){
                        g2.setColor(Color.GREEN);
                        labelAllNuclei(g2);
                        g2.setColor(save);
                   }
                   if (Ace3D_Frame.labelSelectedNucleus()){
                       g2.setColor(Color.RED);
                       labelSelectedNucleus(g2);
                       g2.setColor(save);
                   }
                   g2.setColor(Color.BLUE);
                   labelMarkedNuclei(g2);
                   g2.setColor(save);
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
                long[] pos = new long[timePointImage.getImage().numDimensions()];
                pos[dim] = SingleSlicePanel.this.slice;
                switch (dim) {
                case 0:
                    pos[1] = (int)(e.getX()/scale);
                    pos[2] = (int)(e.getY()/scale);
                    break;
                case 1:
                    pos[0] = (int)(e.getX()/scale);
                    pos[2] = (int)(e.getY()/scale);
                    break;            
                default:
                    pos[0] = (int)(e.getX()/scale);
                    pos[1] = (int)(e.getY()/scale);
                    break;
                }                 
                if (e.getButton() == MouseEvent.BUTTON1){
                    if (parent != null )parent.changePosition(pos);                 
                } else if (e.getButton() == MouseEvent.BUTTON3){
                    int mask = MouseEvent.SHIFT_DOWN_MASK;
                    if (( e.getModifiersEx()&mask) == mask){
                        // making a new nucleus
                        long[] parentPos = parent.getPosition();
                        long[] center = new long[parentPos.length];
                        double radius = 0.0;
                        for (int d=0 ; d<pos.length ; ++d){
                            center[d] = parentPos[d];     //(image coordinates)
                            long l = pos[d]-parentPos[d];
                            radius = radius + l*l;  // (image coordinates)
                        }
                        radius = Math.sqrt(radius);
                        timePointImage.addNucleus(new Nucleus(timePointImage.getTime(),center,radius));
                        parent.repaint();
                    } else {
                        // selecting the closest nucleus
                        Set<Nucleus> nucs = timePointImage.getNuclei();
                        double min = Double.MAX_VALUE;
                        Nucleus closest = null;
                        for (Nucleus nuc : nucs){
                            nuc.setSelected(false);
                            double d = nuc.distanceSqaured(pos);
                            if (d < min){
                                closest = nuc;
                                min = d;
                            }
                        }
                        closest.setSelected(true);
                        parent.repaint();
                    }
                }
            }
        });        
    }

    public int getDimension(){
        return dim;
    }
    // set the position (image coordinates)
    public void setPosition(long[] pos){
        switch (dim) {
            case 0:
                xPos = pos[1];
                yPos = pos[2];
                break;
            case 1:
                xPos = pos[0];
                yPos = pos[2];
                break;            
            default:
                xPos = pos[0];
                yPos = pos[1];
                break;
        }        
        setSlice(pos[dim]);
        slider.setValue((int)pos[dim]);
        this.repaint();
    }
    // return the screen x coordinate given image coordinates
    public int screenX(long[] p){
        if (dim==0){
            return (int)(p[1]*scale);
        } else {
            return (int)(p[0]*scale);
        }
    }
    public int imageXDirection(){
        if (dim==0){
            return 1;
        }
        return 0;
    }

    public int screenY(long[] p){
        if (dim==2){
            return (int)(p[1]*scale);
        } else {
            return (int)(p[2]*scale);
        }
    } 
    public int imageYDirection(){
        if (dim==2){
            return 1;
        }
        return 2;
    }    
    private boolean visible(Nucleus nuc){
        long[] center = nuc.getCenter();  // image corrdinates
        double r = nuc.getRadius();   // image corrdinates
        double delta = Math.abs(slice-center[dim]);   // image corrdinates
        return delta <= r;       
    }
    final private void setSlice(long p){
        slice = p;

    }
    public long getSlice(){
        return this.slice;
    }
    
    final public void setImage(TimePointImage tpi,long[] pos){
        this.timePointImage = tpi;
        this.setPosition(pos);
        this.repaint();
    }
    public void setExtent(int min,int max) {
        slider.setMaximum(max);
        slider.setMinimum(min);
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
       Set<Nucleus> nucs = timePointImage.getNuclei();
       for (Nucleus nuc : nucs){
            long[] center = nuc.getCenter();  // image corrdinates
            double r = nuc.getRadius();   // image corrdinates
            double delta = Math.abs(slice-center[dim]);   // image corrdinates
            if (delta <= r){
                double diam = 2.0*scale*Math.sqrt(r*r-delta*delta);  //screen coordinates
                double rad = 0.5*diam;   // screen coordinates
                Ellipse2D.Double ellipse;
                switch (dim) {
                    case 0:
                        ellipse = new Ellipse2D.Double(scale*center[1]-rad,scale*center[2]-rad,diam,diam);
                        break;
                    case 1:
                        ellipse = new Ellipse2D.Double(scale*center[0]-rad,scale*center[2]-rad,diam,diam);
                        break;            
                    default:
                        ellipse = new Ellipse2D.Double(scale*center[0]-rad,scale*center[1]-rad,diam,diam);
                        break;
                }
                if (nuc.getSelected()){
                    g2.setColor(Color.RED);
                }else {
                    g2.setColor(Color.GREEN);
                }
                g2.draw(ellipse);
            }
       }        
    }
    private void drawSisters(Graphics2D g2){
        NucleusFile nucFile = parent.getEmbryo().getNucleusFile();
        
        TreeMap<Nucleus,Nucleus> sisterPairs = new TreeMap<>();
        Set<Nucleus> nucs = timePointImage.getNuclei();
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
        Set<Nucleus> nucs = timePointImage.getNuclei();
        for (Nucleus nuc : nucs){
            if (nuc.getLabeled()){
                labelNucleus(g2,nuc);
            }
        }
        
    }
    private void labelSelectedNucleus(Graphics2D g2){
        Set<Nucleus> nucs = timePointImage.getNuclei();
        for (Nucleus nuc : nucs){
            if (nuc.getSelected()){
                labelNucleus(g2,nuc);
            }
        }        
    }
    // label all visible nuclei 
    private void labelAllNuclei(Graphics2D g2){
        Set<Nucleus> nucs = timePointImage.getNuclei();
        for (Nucleus nuc : nucs){
            if(visible(nuc)){
                labelNucleus(g2,nuc);
            }
        }        
    }
    private void labelNucleus(Graphics g2,Nucleus nuc){
        g2.drawString(nuc.getName(),screenX(nuc.getCenter()),screenY(nuc.getCenter()));
    }
    public void changeContrast(boolean auto,float min,float max){
        this.autoContrast = auto;
        this.maxContrast = max;
        this.minContrast = min;
        this.repaint();
    }
    SynchronizedMultipleSlicePanel parent;
    JPanel slicePanel;
    double scale=1.0;
    long slice;
    long xPos=128;
    long yPos=128;
    int dim;
    JSlider slider;
    final String title;
    TimePointImage timePointImage;
    ImagePlus imagePlus;
    boolean autoContrast=true;
    float minContrast;
    float maxContrast;
    
}
