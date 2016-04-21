/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import org.rhwlab.dispim.nucleus.Nucleus;
import ij.ImagePlus;
import ij.process.LUT;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.RealARGBColorConverter;

import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;

/**
 *
 * @author gevirl
 */
public class TimePointImage{


    public TimePointImage(RandomAccessibleInterval img,float[] mm,int time,long[] dims,String dataset){
        this.image = img;
        this.minmax = mm;
        this.time = time;
        this.dims = dims;
        this.dataset = dataset;
    }
    public BufferedImage getBufferedImage(int dim,long slice){
        DataSetProperties props = Ace3D_Frame.getProperties(dataset);
        RealARGBColorConverter converter = new RealARGBColorConverter.Imp0((double)props.min,(double)props.max);
        converter.setColor(new ARGBType(props.color.getRGB()));
        RandomAccessibleInterval rai = Converters.convert(image, converter,new ARGBType());
        IntervalView iv = Views.hyperSlice(rai, dim, slice);
        Cursor cursor = iv.localizingCursor();
        int x0 = (int)iv.min(0);
        int y0 = (int)iv.min(1);
        int w = (int)iv.dimension(0);
        int h = (int)iv.dimension(1);
        BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);
        while (cursor.hasNext()){
            cursor.fwd();
            ARGBType pix  = (ARGBType)cursor.get();
            int x = cursor.getIntPosition(0)-x0;
            int y = cursor.getIntPosition(1)-y0;
            System.out.printf("(%d,%d,%d,%d)\n",ARGBType.red(pix.get()),ARGBType.green(pix.get()),ARGBType.blue(pix.get()),ARGBType.alpha(pix.get()));
            try {
                ret.setRGB(x,y, pix.get());
            } catch (Exception exc){
                int iusahfuis=0;
            }
            int uisafhduisd=0;
        }

        
//        ImagePlus imagePlus = ImageJFunctions.wrapRGB(iv,String.format("(%d,%d,%d)", time,dim,slice));
//        BufferedImage bi = imagePlus.getBufferedImage();
        return ret;        
    }
/*    
    public BufferedImage getBufferedImage(int dim,long slice){
//        IntervalView iv = this.getImage(dim,slice);
        LUT lut = Ace3D_Frame.getLUT(dataset);
        RealLUTConverter converter = new RealLUTConverter();
         DataSetProperties props = Ace3D_Frame.getProperties(dataset);
        converter.setMin((double)props.min);
        converter.setMax((double)props.max);
        byte[] by = lut.getBytes();
        byte[] red = new byte[by.length/3];
        lut.getReds(red);
        byte[] green = new byte[by.length/3];
        lut.getGreens(green);
        byte[] blue = new byte[by.length/3];
        lut.getBlues(blue);
        byte[] alpha = new byte[by.length/3];
        for (int i=0 ; i<alpha.length ; ++i){
            alpha[i] = 100;
        }
        ColorTable8 ct = new ColorTable8(red,green,blue,alpha);
        converter.setLUT(ct); 
        RandomAccessibleInterval rai = Converters.convert(image, converter,new ARGBType());
        Object obj = rai.randomAccess().get();
        ARGBType t = (ARGBType)obj;
        int al = ARGBType.alpha(t.get());
        IntervalView iv = Views.hyperSlice(rai, dim, slice);
        ImagePlus imagePlus = ImageJFunctions.wrapRGB(iv,String.format("(%d,%d,%d)", time,dim,slice));
        BufferedImage bi = imagePlus.getBufferedImage();
        ColorModel model = bi.getColorModel().coerceData(bi.getRaster(), true);
        BufferedImage ret = new BufferedImage(model,bi.getRaster(),true,new Hashtable());
        return ret;
    }
*/
    ImagePlus getSlice(int dim,int slice){
        
        IntervalView iv = Views.hyperSlice(image, dim, slice);
        ImagePlus imagePlus = ImageJFunctions.wrap(iv,String.format("(%d,%d,%d)", time,dim,slice));

        return imagePlus;
    }
    public int getTime(){
        return time;
    }
    public RandomAccessibleInterval  getWholeImage(){
        LUT lut = Ace3D_Frame.getLUT(dataset);
        RealLUTConverter converter = new RealLUTConverter();
         DataSetProperties props = Ace3D_Frame.getProperties(dataset);
        converter.setMin((double)props.min);
        converter.setMax((double)props.max);
        byte[] by = lut.getBytes();
        byte[] red = new byte[by.length/3];
        lut.getReds(red);
        byte[] green = new byte[by.length/3];
        lut.getGreens(green);
        byte[] blue = new byte[by.length/3];
        lut.getBlues(blue);
        ColorTable8 ct = new ColorTable8(red,green,blue);
        converter.setLUT(ct);  
        return Converters.convert(image, converter,new ARGBType());
    }
    // get a time slice image
    public IntervalView getImage(int dim,long slice){

        return Views.hyperSlice(getWholeImage(), dim, slice);
//        return Views.hyperSlice(image, dim, slice);
    }
    public RandomAccessibleInterval getImage(){
        return this.image;
    }
    public float getMin(){
        return minmax[0];
    }
    public float getMax(){
        return minmax[1];
    }
    public long[] getDims(){
        return this.dims;
    }
    public void addNucleus(Nucleus nuc){
        nuclei.add(nuc);
    }
    public void setNuclei(Set<Nucleus> nucs){
        this.nuclei = nucs;
    }
    public Set<Nucleus> getNuclei(){
        return nuclei;
    }
    public boolean equals(Object obj){
        if (obj instanceof TimePointImage){
            TimePointImage other = (TimePointImage)obj;
            if (this.time == other.time){
                if (this.dataset.equals(other.dataset)){
                    return true;
                }
            }
        }
        return false;
    }
    public String getDataset(){
        return this.dataset;
    }
    public double[] getMinPosition(){
        double[] ret = new double[image.numDimensions()];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = image.realMin(i);
        }
        return ret;
    }
    public double[] getMaxPosition(){
        double[] ret = new double[image.numDimensions()];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = image.realMax(i);
        }
        return ret;
    }  
    public double minPosition(int d){
        return image.realMin(d);
    }
    public double maxPosition(int d){
        return image.realMax(d);
    }    
    String dataset;
    int time;
    Set<Nucleus> nuclei = new HashSet<Nucleus>();
    RandomAccessibleInterval image;
    float[] minmax;
    long[] dims;
}
