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
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import spim.process.fusion.FusionHelper;

/**
 *
 * @author gevirl
 */
public class TifDirectoryImageSource implements ImageSource {
    public TifDirectoryImageSource(String dirName){
        this.directory = dirName;
        Pattern pattern = Pattern.compile("(\\D+)(\\d+).tif");
        File dir = new File(dirName);
        File[] files = dir.listFiles();
        for (File file : files){
            String fileName = file.getName();
            if (fileName.endsWith("tif")){
                Matcher matcher = pattern.matcher(fileName);
                matcher.matches();
                Integer time = new Integer(matcher.group(2));
                fileNames.put(time,file.getPath());
            }
        }
    }

    @Override
    public TimePointImage getImage(int time) {
        ImgOpener opener = new ImgOpener();
        try {
 
            List<SCIFIOImgPlus<?>> list = opener.openImgs(fileNames.get(time));
            SCIFIOImgPlus img = list.get(0);
            
            double[] dims = new double[img.numDimensions()];
            for (int d=0 ; d<dims.length ; ++d) {
                dims[d] = img.dimension(d);
            }
            float[] mm = new float[2];           
            float[] minmax = FusionHelper.minMax(img);
            
            return new TimePointImage(img.getImg(),mm,time,dims);
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
    String directory;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();
    
    static public void main(String[] args){
        TifDirectoryImageSource source = new TifDirectoryImageSource("/net/waterston/vol9/diSPIM/20151118_nhr-25_XIL0141/CroppedReslicedBGSubtract488");
        source.getImage(1);
    }
    
}
