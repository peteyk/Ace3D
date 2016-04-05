/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.starrynite;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 *
 * @author gevirl
 */
// encapsulates all the nuclei for a given time point in a series
// this is a set of nuclei
public class TimePointNuclei {
    public TimePointNuclei(int time,SeriesNuclei series){
        this.time = time;
        this.series = series;
        nuclei = new ArrayList<TimePointNucleus>();
    }
    
    public boolean readFile(File nucFile) {
        try {
            FileInputStream stream = new FileInputStream(nucFile);
            readStream(stream);
        } catch( Exception exc) {
            return false;
        }
        return true;
    }

    public void readStream(InputStream stream) throws Exception{
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        while (line != null){
            String[] tokens = line.split(",");
            
            String[] trimmed = new String[tokens.length];
            for (int i=0 ; i<tokens.length ; ++i){
                trimmed[i] = tokens[i].trim();
            }
            TimePointNucleus nuc = new TimePointNucleus(trimmed,this);
            nuclei.add(nuc);
            line = reader.readLine();
        }
        
    }
    public SeriesNuclei getSeries(){
        return series;
    }
    public TimePointNucleus getNucleus(int index){
        if (index == 0){
            int ouashdfuis=0;
        }
        return nuclei.get(index-1);
    }
    public ArrayList<TimePointNucleus> getNuclei() {
        return nuclei;
    }
    public int getCount(){
        return nuclei.size();
    }
    SeriesNuclei series;  //  this TimePointNuclei knows what series it is in
    ArrayList<TimePointNucleus> nuclei;
    int time;
}