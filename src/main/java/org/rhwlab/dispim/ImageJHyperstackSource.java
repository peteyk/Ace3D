/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.Collection;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

/**
 *
 * @author gevirl
 */
public class ImageJHyperstackSource implements ImageSource {

    @Override
    public boolean open() {
        imagePlus = IJ.getImage();
        int ndims = imagePlus.getNDimensions();
        dims = imagePlus.getDimensions();
        return true;
    }

    @Override
    public TimePointImage getImage(String datatset, int time) {
        ImageStack stack = new ImageStack(dims[0],dims[1],dims[3]);
        for (int slice =1 ; slice<=dims[3] ; ++slice){
            imagePlus.setPosition(1, slice, time);
            ImageProcessor process = imagePlus.getProcessor();
            Object obj = imagePlus.getProcessor().getPixels();
            if (obj == null){
                int ajishdf=0;
            }
            stack.setPixels(obj, slice);
        }
        float[] minmax = new float[2];
        long[] dims= new long[3];
        ImagePlus ip = new ImagePlus("",stack);
        img = ImageJFunctions.wrap(ip);
        return new TimePointImage(img,minmax,time,dims,"Hyperstack");

    }

    @Override
    public int getTimes() {
        return imagePlus.getNFrames();
        
    }

    @Override
    public int getMinTime() {
        return 1;
    }

    @Override
    public int getMaxTime() {
        return imagePlus.getNFrames();
    }

    @Override
    public String getFile() {
        return "";
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        DataSetDescImpl desc = new DataSetDescImpl();
        desc.name = "Hyperstack";
        ArrayList<DataSetDesc> ret = new ArrayList<>();
        ret.add(desc);
        return ret;
    }
    int[] dims;
    ImagePlus imagePlus;
    Img img;
}
