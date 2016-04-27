/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.ArrayList;
import org.rhwlab.dispim.nucleus.Nucleus;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.imglib2.view.Views;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 * encapsulates all the images and metadata for a dispim imaging experiment
 */
public class ImagedEmbryo {
    public ImagedEmbryo(ImageSource src){
        this.source=src;
    }
    public CompositeTimePointImage getImage(List<String> datasets,int time){
        ArrayList<TimePointImage> list = new ArrayList<>();
        for (String dataset : datasets){
            TimePointImage tpi = getSingleImage(dataset,time);
            list.add(tpi);
        }
        return new CompositeTimePointImage(list);
    }
    public TimePointImage getSingleImage(String dataset,int time){
        for (TimePointImage image : timePointCache){
            if (image.getTime()==time && image.getDataset().equals(dataset)){
                return image;
            }
        }
        TimePointImage image = source.getImage(dataset,time);
        if (timePointCache.size()==cacheSize){
            timePointCache.removeLast();
        } 
        timePointCache.addFirst(image);
        return image;
    }
    public int getTimes(){
        return source.getTimes();
    }
    public int getMinTime(){
        return source.getMinTime();
    }
    public int getMaxTime(){
        return source.getMaxTime();
    }
    public void setNucleusFile(NucleusFile file){
        nucFile = file;
    }
    public Nucleus selectedNucleus(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            if (nuc.getSelected()){
                return nuc;
            }
        }
        return null;
    }
    public List<Nucleus> nextNuclei(Nucleus source){
        return nucFile.linkedForward(source);
    }
    public Nucleus previousNucleus(Nucleus source){
        return nucFile.linkedBack(source);
    }
    public void clearSelected(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            nuc.setSelected(false);
        }        
    }
    public void clearLabeled(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            nuc.setLabeled(false);
        }        
    }    
    public NucleusFile getNucleusFile(){
        return nucFile;
    }
    public long[] getMinCoordinate(){
        
        TimePointImage tpi = timePointCache.get(0);
        long[] ret = new long[tpi.image.numDimensions()];
        tpi.image.min(ret);
        return ret;
    }
    public Set<Nucleus> getNuclei(int time){
        return nucFile.getNuclei(time);
    }
    public void addNucleus(Nucleus nuc){
        nucFile.addNucleus(nuc);
    }

    
    static int cacheSize = 100;
    NucleusFile nucFile;
    ImageSource source;
    LinkedList<TimePointImage> timePointCache = new LinkedList<TimePointImage>();
}
