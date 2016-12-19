/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rhwlab.command.CommandLine;

/**
 *
 * @author gevirl
 */
public class Nuclei_IdentificationCLI extends CommandLine {

    @Override
    public void init() {
    }

    @Override
    public String post() {
        return null;
    }

    @Override
    public String noOption(String s) {
        this.segTiff = s;
        File file = new File(s);
        if (file.exists()){
            return null;
        }
        return String.format("File: %s cannot be found", s);
    }
    
    public String lineageTiff(String s){
        this.lineTiff = s;
        File file = new File(s);
        if (file.exists()){
            return null;
        }
        return String.format("File: %s cannot be found", s);        
    }

    @Override
    public void usage() {
        System.out.println("Nuclei_Identification [Options] segmentedTiff");
        System.out.println("Options:");
        System.out.println("\t-first TIME");
        System.out.println("\t-last TIME");
        System.out.println("\t-force\tforce recalculation for the time point(s)");
        System.out.println("\t-qsub\trun on the whead cluster");
    }
    public String segTiff(String s){
        return noOption(s);
    }
    public String dir(String s){
        File dir = new File(s);
        if (dir.isDirectory()){
            this.directory = dir.getPath();
            return null;
        }
        return s;
    }
    public String S(String s){
        try {
            S = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -S %s", s);
        }
        return null;   
    }
    public String alpha(String s){
        try {
            alpha = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -alpha %s", s);
        }
        return null;        
    }
    public String segThresh(String s){
        try {
            segThresh = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -segThresh %s", s);
        }
        return null;        
    }    
    public String first(String s){
        try {
            firstTime = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -first %s", s);
        }
        return null;
    }
    public String last(String s){
        try {
            lastTime = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -last %s", s);
        }
        return null;
    }   
    public void force(){
        this.force = true;
    }
    public void qsub(){
        this.qsub = true;
    }
    public String memory(String s){
        this.memory = s;
        return null;
    }
    public void study(){
        this.study = true;
    }
    public Integer getFirstTime(){
        return this.firstTime;
    }
    public Integer getLastTime(){
        return this.lastTime;
    }
    public boolean getForce(){
        return this.force;
    }
    public boolean getQsub(){
        return this.qsub;
    }
    public boolean getStudy(){
        return this.study;
    }
    public String getDirectory(){
        return this.directory;
    }
    public String getMemory(){
        return memory;
    }
    public Double getAlpha(){
        return alpha;
    }
    public Double getS(){
        return this.S;
    }
    public Double getSegThresh(){
        return segThresh;
    }
    public TreeMap<Integer,String[]> getTiffs(){

 
        TreeMap<Integer,String> segMap = getFiles(this.segTiff);
        TreeMap<Integer,String> lineMap = getFiles(this.lineTiff);
        TreeMap<Integer,String[]> timeMap = new TreeMap<>();
        for (int time : lineMap.keySet()){
            String seg = segMap.get(time);
            if (seg != null){
                String[] names = new String[2];
                names[0] = lineMap.get(time);
                names[1] = segMap.get(time);
                timeMap.put(time,names);
            }
        }
        return timeMap;
    }
    public TreeMap<Integer,String> getFiles(String typical){
        TreeMap ret = new TreeMap<>();
        
        File tiff = new File(typical);

        File dir = tiff.getParentFile();
        File[] files = dir.listFiles();
        
        Pattern p = p = Pattern.compile("(TP)(\\d{1,4})(_.+)");      
        Matcher m = p.matcher(tiff.getName());
        if (!m.matches()){
            p = Pattern.compile("(.+)(\\d{3})(.+)");
            m = p.matcher(tiff.getName());
        }
        String prefix = m.group(1);
        String suffix = m.group(3);
        
        
        p = Pattern.compile(prefix+"(\\d{1,4})"+suffix);
        for (File file : files){
            m = p.matcher(file.getName());
            if (m.matches()){
                int time = Integer.valueOf(m.group(1));
                if (firstTime != null && time < firstTime){
                    continue;
                }
                if (lastTime != null && time > lastTime){
                    continue;
                }
                ret.put(time, file.getPath());
            }
        }
       
        return ret;
    }
    String memory;
    String directory;
    String lineTiff;
    String segTiff;
    Integer firstTime;
    Integer lastTime;
    Double alpha;
    Double S;
    Double segThresh;
    boolean force=false;
    boolean qsub = false;
    boolean study = false;
}
