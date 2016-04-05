/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.starrynite;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.*;
import java.util.*;

/**
 *
 * @author gevirl
 */
// encapsulates all the nuclei in a given series (all the time points)
// build from a Starry Night Zip file or directory of nuclei files
public class SeriesNuclei {
    public SeriesNuclei(String id){
        seriesID = id;
    }
    
    // read the nuclei from a Starry Night ZIP file
    public void readZipFile(File snZipFile) {
        ZipFile zipFile=null;
        try {
            zipFile = new ZipFile(snZipFile);
        } catch (Exception exc){
            exc.printStackTrace();
            System.out.printf("File: %s\n", snZipFile.getPath());
            return;
        }
        
        maxTime = 0;
        nucSeries = new TimePointNuclei[zipFile.size()];
        Enumeration entries = zipFile.entries();
        while(entries.hasMoreElements()){
            ZipEntry entry = (ZipEntry)entries.nextElement();
            if (!entry.isDirectory()){
                String name = entry.getName();
                if (name.startsWith("nuclei")){
                    int time = this.timeIndex(name);
                    TimePointNuclei nuclei = new TimePointNuclei(time,this);
                    try {
                        nuclei.readStream(zipFile.getInputStream(entry));
                        if (nuclei.getCount()>0) {
                            nucSeries[time-1] = nuclei;
                            if (time > maxTime) maxTime = time;
                        }
                    } catch(Exception exc){
                        exc.printStackTrace();
                        return;
                    }
                } else {
                    int kks=0;
                }
            }
        }
    }
    
    // construct the series from a directory
    public void readDirectory(File nucDir){
        if (!nucDir.isDirectory()) return; // make sure it is a directory
        
        File[] files = nucDir.listFiles();
        nucSeries = new TimePointNuclei[files.length];
        for (File file : files){
            // determine the time index from the name of the file
            int time = this.timeIndex(file.getName());   
            TimePointNuclei nuclei = new TimePointNuclei(time,this);
            nuclei.readFile(file);
            nucSeries[time-1] = nuclei;
        }
    }
    // returns all the nuclei at the given time
    public TimePointNuclei getNucleiAtTime(int time) {
        return nucSeries[time-1];
    }
    
    private int timeIndex(String fileName){
        Matcher match = digits.matcher(fileName);
        match.lookingAt();
        String intStr = match.group(1);
        int i = Integer.parseInt(intStr);
        return i;
    }
    public long countNuclei(){
        long count = 0;
        for (TimePointNuclei nuc : nucSeries){
            count = count + nuc.getCount();
        }
        return count;
    }

    public String getID(){
        return seriesID;
    }
    public int getMaxTime(){
        return maxTime;
    }
    static Pattern digits = Pattern.compile(".+(\\d{3})");
    String seriesID;
    TimePointNuclei[] nucSeries;    // this array is indexed by time, each time point is the set of nuclei found at that time
    int maxTime;
}