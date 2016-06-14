/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.scijava.Context;
import spim.process.fusion.FusionHelper;

/**
 *
 * @author gevirl
 */
public class TifDirectoryImageSource implements ImageSource {
    public TifDirectoryImageSource(String dirName){
        this.directory = dirName;

    }
    @Override
    public boolean open(){
        Pattern pattern = Pattern.compile("(\\D+)(\\d+).tif");
        File dir = new File(this.directory);
        File[] files = dir.listFiles();
        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;
        for (File file : files){
            String fileName = file.getName();
            if (fileName.endsWith("tif")){
                Matcher matcher = pattern.matcher(fileName);
                matcher.matches();
                Integer time = new Integer(matcher.group(2));
                if (time < minTime){
                    minTime = time;
                }
                if (time> maxTime){
                    maxTime = time;
                }
                datasetname = matcher.group(1);
                fileNames.put(time,file.getPath());
            }
        }
        opener = new ImgOpener(new Context());
    return true;        
    }

    @Override
    public TimePointImage getImage(String dataset,int time) {
        
        try {
 
            List<SCIFIOImgPlus<?>> list = opener.openImgs(fileNames.get(time));
            SCIFIOImgPlus img = list.get(0);
            
            long[] dims = new long[img.numDimensions()];
            for (int d=0 ; d<dims.length ; ++d) {
                dims[d] = img.dimension(d);
            }
            float[] mm = new float[2];           
            float[] minmax = FusionHelper.minMax(img);
            
            return new TimePointImage(img.getImg(),mm,time,dataset);
        } catch (ImgIOException ex) {
            Logger.getLogger(TifDirectoryImageSource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public int getTimes() {
        return fileNames.size();
    }
    
    public String getFile(){
        return directory;
    }
    @Override
    public Collection<DataSetDesc> getDataSets() {
        HashSet<DataSetDesc> ret = new HashSet<>();
        DataSetDescImpl ds = new DataSetDescImpl();
        ds.name = datasetname;
        ret.add(ds);
        return ret;
    }
    ImgOpener opener;
    String directory;
    String datasetname;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();
    
    static public void main(String[] args){
        TifDirectoryImageSource source = new TifDirectoryImageSource("/net/waterston/vol9/diSPIM/20151118_nhr-25_XIL0141/CroppedReslicedBGSubtract488");
        
    }

    @Override
    public int getMinTime() {
        return minTime;
    }

    @Override
    public int getMaxTime() {
        return maxTime;
    }
    int minTime;
    int maxTime;


}
