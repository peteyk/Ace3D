/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import org.rhwlab.dispim.nucleus.Nucleus;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
    public TimePointImage getImage(String dataset,int time){
        for (TimePointImage image : timePointCache){
            if (image.getTime()==time && image.getDataset().equals(dataset)){
                Set<Nucleus> nuclei = nucFile.getNuclei(time);
                image.setNuclei(nuclei);
                return image;
            }
        }
        TimePointImage image = source.getImage(dataset,time);
        Set<Nucleus> nuclei = nucFile.getNuclei(time);
        image.setNuclei(nuclei);
        if (timePointCache.size()==cacheSize){
            timePointCache.removeLast();
        } 
        timePointCache.addFirst(image);
        return image;
    }
    public int getTimes(){
        return source.getTimes();
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
    static int cacheSize = 20;
    NucleusFile nucFile;
    ImageSource source;
    LinkedList<TimePointImage> timePointCache = new LinkedList<TimePointImage>();
}
