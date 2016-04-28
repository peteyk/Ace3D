/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.HashMap;
import java.util.LinkedList;
import net.imglib2.RandomAccessibleInterval;

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

    public int getTime(){
        return time;
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

    @Override
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
    static public TimePointImage getSingleImage(String dataset,int time){
        HashMap<Integer,TimePointImage> map = imageIndex.get(dataset);
        if (map != null){
            TimePointImage ret = map.get(time);
            if (ret != null){
                return ret;
            }   
        } else {
            map = new HashMap<>();
            imageIndex.put(dataset,map);
        }
        TimePointImage ret = addToCache(dataset,time);
        map.put(time, ret);
        return ret;

    }
    static private TimePointImage addToCache(String dataset,int time){
        TimePointImage tpi = source.getImage(dataset,time);
        if (timePointCache.size()==cacheSize){
            
            // cache is full, remove the last
            TimePointImage last = timePointCache.removeLast();
            HashMap<Integer,TimePointImage> map  = imageIndex.get(dataset);
            map.remove(last.time);  // remove it from the index too
        }  
        timePointCache.addFirst(tpi);
        return tpi;
    }
    static public void setSource(ImageSource s){
        source = s;
    }

    String dataset;
    int time;
    private RandomAccessibleInterval image;
    float[] minmax;
    long[] dims;
    
    static int cacheSize = 20;
    static ImageSource source;
    static LinkedList<TimePointImage> timePointCache = new LinkedList<>();    
    static HashMap<String,HashMap<Integer,TimePointImage>> imageIndex = new HashMap<>();
}
