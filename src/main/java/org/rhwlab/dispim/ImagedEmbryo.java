/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import org.rhwlab.dispim.nucleus.Nucleus;
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
    public CompositeTimePointImage getImage(int time){
        return new CompositeTimePointImage(time);
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
    public Nucleus selectedNucleus(){
        return nucFile.getSelected();
    }
    public void setSelectedNUcleus(Nucleus toSelect){
        nucFile.setSelected(toSelect);
    }
    public List<Nucleus> nextNuclei(Nucleus source){
        return nucFile.linkedForward(source);
    }
    public Nucleus previousNucleus(Nucleus source){
        return nucFile.linkedBack(source);
    }
/*    
    public void clearSelected(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            nuc.setSelected(false);
        }        
    }
*/
    public void clearLabeled(int time){
        Set<Nucleus> nucs = nucFile.getNuclei(time);
        for (Nucleus nuc : nucs){
            nuc.setLabeled(false);
        }        
    }    
    public NucleusFile getNucleusFile(){
        return nucFile;
    }

    public Set<Nucleus> getNuclei(int time){
        return nucFile.getNuclei(time);
    }
    public void addNucleus(Nucleus nuc){
        nucFile.addNucleus(nuc);
    }
    
    NucleusFile nucFile;
    ImageSource source;
}
