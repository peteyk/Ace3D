/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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
        dims = imagePlus.getDimensions();
        return true;
    }

    @Override
    public TimePointImage getImage(String dataset, int time) {
        int stackTime = time - minTime + 1;
        int channel = Integer.valueOf(dataset.substring(7));
        ImageStack stack = new ImageStack(dims[0],dims[1],dims[3]);
        for (int slice =1 ; slice<=dims[3] ; ++slice){
            imagePlus.setPosition(channel, slice, stackTime);
            stack.setPixels(imagePlus.getProcessor().getPixels(), slice);
        }
        float[] minmax = new float[2];
        img = ImageJFunctions.wrap(new ImagePlus("",stack));
        return new TimePointImage(img,minmax,stackTime,dataset);
    }

    @Override
    public int getTimes() {
        int n =imagePlus.getNFrames();
        return n;
    }

    @Override
    public void setFirstTime(int minTime){
        this.minTime = minTime;
        TimePointImage.resetCache();
    }
    @Override
    public int getMinTime() {
        return this.minTime;
    }

    @Override
    public int getMaxTime() {
        return this.minTime + imagePlus.getNFrames() - 1;
    }

    @Override
    public String getFile() {
        return "";
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        ArrayList<DataSetDesc> ret = new ArrayList<>();
        for (int d=0 ; d<dims[2] ; ++d){
            DataSetDescImpl desc = new DataSetDescImpl();
            desc.name = String.format("Channel%d",d+1);
            ret.add(desc);
        }
        return ret;
    }
    int[] dims;
    long[] longDims;
    ImagePlus imagePlus;
    Img img;
    int minTime=1;
}
