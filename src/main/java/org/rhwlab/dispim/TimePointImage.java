/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import org.rhwlab.dispim.nucleus.Nucleus;
import ij.ImagePlus;
import java.util.HashSet;
import java.util.Set;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

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
    ImagePlus getSlice(int dim,int slice){
        
        IntervalView iv = Views.hyperSlice(image, dim, slice);
        ImagePlus imagePlus = ImageJFunctions.wrap(iv,String.format("(%d,%d,%d)", time,dim,slice));

        return imagePlus;
    }
    public int getTime(){
        return time;
    }
    public IntervalView getImage(int dim,long slice){
        return Views.hyperSlice(image, dim, slice);
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
