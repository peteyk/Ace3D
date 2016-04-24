/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.RandomAccessibleIntervalCursor;
import net.imglib2.view.Views;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;

/**
 *
 * @author gevirl
 */
public class CompositeTimePointImage  {
    public CompositeTimePointImage(List<TimePointImage> images){
        this.images = images;
    }
    

    public BufferedImage getBufferedImage(int dim,long slice){
        TimePointImage tpi = images.get(0);
        IntervalView iv = Views.hyperSlice(tpi.image, dim, slice);
        
//        RandomAccessibleIntervalCursor cursor = (RandomAccessibleIntervalCursor)iv.localizingCursor();
        Cursor cursor = iv.localizingCursor();
        cursor.fwd();
        int x0 = (int)iv.min(0);  // images min x position
        int y0 = (int)iv.min(1);  // images min y position
        int w = (int)iv.dimension(0);
        int h = (int)iv.dimension(1);
        BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);   
        
        // get the properties of each dataset in the composite image
        RandomAccess[] access = new RandomAccess[images.size()];
        double[] Rfactor = new double[images.size()];
        double[] Gfactor = new double[images.size()];
        double[] Bfactor = new double[images.size()];
        double[] pixMin = new double[images.size()];
        for (int i=0 ; i<images.size() ; ++i){
            DataSetProperties ps = Ace3D_Frame.getProperties(images.get(i).getDataset());
            double red = ps.color.getRed();
            double green = ps.color.getGreen();
            double blue = ps.color.getBlue();
            double alpha = ps.color.getAlpha();
            double range = ps.max-ps.min;
            if (ps.autoContrast){
                range = tpi.getMax()-tpi.getMin();
            }
            pixMin[i] = ps.min;
            double f = 255.0/(alpha*range*images.size());
            Rfactor[i] = red*f;
            Gfactor[i] = green*f;
            Bfactor[i] = blue*f;
            
            access[i] = Views.hyperSlice(images.get(i).getImage(), dim, slice).randomAccess();
        }
        

        int black = new Color(0,0,0,0).getRGB();
//        RealARGBColorConverter converter = new RealARGBColorConverter.Imp0((double)props.min,(double)props.max);
//        converter.setColor(new ARGBType(props.color.getRGB()));
//        RandomAccessibleInterval rai = Converters.convert(image, converter,new ARGBType());



        while (cursor.hasNext()){
            UnsignedShortType pix = (UnsignedShortType)cursor.get();
            int[] cursorPos = new int[2];
            cursor.localize(cursorPos);        
            double v = Math.max(0.0, pix.getRealDouble() - pixMin[0]);
            double r = Rfactor[0]*v;
            double g = Gfactor[0]*v;
            double b = Bfactor[0]*v;
            int x = cursorPos[0]-x0;   // BufferedImage x position
            int y = cursorPos[1]-y0;    // BufferedImage y position
            
            // fuse over all the images the current cursor location
            for (int i=1 ; i<images.size() ; ++i){
                access[i].setPosition(cursorPos);
                pix = (UnsignedShortType)access[i].get();
                v = Math.max(0.0,pix.getRealDouble() - pixMin[i]);
                r = r + Rfactor[i]*v;
                g = g + Gfactor[i]*v;
                b = b + Bfactor[i]*v;
            }
            Color c = new Color(Math.min(255,(int)r),Math.min(255,(int)g),Math.min(255,(int)b),255);
            ret.setRGB(x,y,c.getRGB());

            cursor.fwd();
        }

        
//        ImagePlus imagePlus = ImageJFunctions.wrapRGB(iv,String.format("(%d,%d,%d)", time,dim,slice));
//        BufferedImage bi = imagePlus.getBufferedImage();
        return ret;  
    }
    public int getTime(){
        return images.get(0).getTime();
    }
    public double minPosition(int d){
        return images.get(0).minPosition(d);
    }
    public double maxPosition(int d){
        return images.get(0).maxPosition(d);
    } 
    public int numDimensions(){
        return images.get(0).image.numDimensions();
    }
    public double[] getMinPosition(){
        return images.get(0).getMinPosition();
    }
    public double[] getMaxPosition(){
        return images.get(0).getMaxPosition();
    }
    List<TimePointImage> images;
}
