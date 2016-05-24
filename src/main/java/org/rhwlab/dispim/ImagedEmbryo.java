/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.ArrayList;
import org.rhwlab.dispim.nucleus.Nucleus;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.imageplus.ShortImagePlus;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 * encapsulates all the images and metadata for a dispim imaging experiment
 */
public class ImagedEmbryo implements Observable {
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
        if (nucFile != null){
            return nucFile.getSelected();    
        }
        return null;
    }
    public void setSelectedNucleus(Nucleus toSelect){
        nucFile.setSelected(toSelect);
        if (panel != null) {
            panel.changeTime(toSelect.getTime());
            panel.changePosition(toSelect.getCenter());
        }
        notifyListeners();
    }
    public void setMarked(Nucleus toMark,boolean value){
        toMark.setMarked(value);
        notifyListeners();
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
    public void calculateExpression(){
        List<String> datasets = Ace3D_Frame.datasetsSelected();
        if (!datasets.isEmpty()){
            Set<Integer> times = nucFile.getAllTimes();
            for (Integer time : times){
                this.calculateExpression(datasets.get(0), time);
            }
        }
    }
    public void calculateExpression(String dataset,int time){
        Set<Nucleus> nuclei = nucFile.getNuclei(time);
        TimePointImage tpi = source.getImage(dataset, time);
        RandomAccessibleInterval img = tpi.getImage();
        ShortImagePlus sip = (ShortImagePlus)img;
        Cursor cursor = sip.cursor();
        RandomAccess access = img.randomAccess();
        cursor.fwd();
        while(cursor.hasNext()){
            int[] position = new int[sip.numDimensions()];
            cursor.localize(position);
            for (int i=0 ; i<position.length ; ++i){
                System.out.printf("%d\t",position[i]);
            }
            System.out.println();
            cursor.fwd();
        }
                
    }

    public void notifyListeners(){
        for (InvalidationListener listener : listeners){
            listener.invalidated(this);
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners.remove(listener);
    }
    public Set<Nucleus> getMarkedNuclei(int time){
        Set<Nucleus> all = nucFile.getNuclei(time);
        TreeSet<Nucleus> ret = new TreeSet<>();
        for (Nucleus nuc : all){
            if (nuc.getMarked()){
                ret.add(nuc);
            }
        }
        return ret;
    }
    public void setPanel(SynchronizedMultipleSlicePanel panel){
        this.panel = panel;
        this.addListener(panel);
    }
    SynchronizedMultipleSlicePanel panel;
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
    NucleusFile nucFile;
    ImageSource source;    
}
