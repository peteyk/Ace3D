/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class HDF5DirectoryImageSource implements ImageSource{
    public HDF5DirectoryImageSource(File dir,String hddataset,String aceDataSet){
        this.directory = dir;
        this.HDF5dataset = hddataset;
        this.aceDataset = aceDataSet;
    }
    @Override
    public boolean open() {
        if (!directory.isDirectory()){
            return false;
        }
        Pattern p = Pattern.compile("(\\D+)(\\d{1,4})(\\D+)");
        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;
        File[] files = directory.listFiles();
        for (File file : files){
            if (file.getName().endsWith("h5")){
                Matcher matcher = p.matcher(file.getName());
                if (matcher.find() ){
                    int time = Integer.valueOf(matcher.group(2));
                    this.fileNames.put(time,file.getPath());
                    if (time < minTime){
                        minTime = time;
                    }
                    if (time > maxTime){
                        maxTime = time;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public TimePointImage getImage(String acedatatset, int time) {
        String fileName = fileNames.get(time);
        if (fileName ==  null){
            return null;
        }
        File file = new File(fileName);
        HDF5Image image = new HDF5Image(file,this.HDF5dataset);
        TimePointImage ret = new TimePointImage(image.getImage(),image.getMinMax(),time,aceDataset);
        return ret;
    }

    @Override
    public int getTimes() {
        return maxTime - minTime + 1;
    }

    @Override
    public int getMinTime() {
        return minTime;
    }

    @Override
    public int getMaxTime() {
        return maxTime;
    }

    @Override
    public String getFile() {
        return directory.getPath();
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        ArrayList<DataSetDesc> ret = new ArrayList<>();
        
        return ret;
    }

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element toXML() {
        Element ret = new Element("HDF5DirectorySource");
        ret.setAttribute("directory", directory.getPath());
        ret.setAttribute("typicalFile",fileNames.firstEntry().getValue());
        ret.setAttribute("dataset",aceDataset);
        return ret;
    }
    int minTime;
    int maxTime;
    File directory;
    String HDF5dataset;
    String aceDataset;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();    
}
