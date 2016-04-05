/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import org.rhwlab.dispim.nucleus.Nucleus;
import java.util.LinkedList;
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
    public TimePointImage getImage(int time){
        for (TimePointImage image : timePointCache){
            if (image.getTime()==time){
                Set<Nucleus> nuclei = nucFile.getNuclei(time);
                image.setNuclei(nuclei);
                return image;
            }
        }
        TimePointImage image = source.getImage(time);
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
    static int cacheSize = 10;
    NucleusFile nucFile;
    ImageSource source;
    LinkedList<TimePointImage> timePointCache = new LinkedList<TimePointImage>();
}
