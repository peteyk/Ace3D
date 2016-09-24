/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.util.ArrayList;
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

    @Override
    public void usage() {
        System.out.println("Nuclei_Identification [Options] segmentedTiff");
        System.out.println("Options:");
        System.out.println("\t-first TIME");
        System.out.println("\t-last TIME");
        System.out.println("\t-force\tforce recalculation for the time point(s)");
        System.out.println("\t-qsub\trun on the whead cluster");
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
    public String[] getTiffs(){
        ArrayList<String> list = new ArrayList<>();
        File tiff = new File(segTiff);

        File dir = tiff.getParentFile();
        File[] files = dir.listFiles();
        
        Pattern p = Pattern.compile("(.+)\\d{3}(.+)");
        Matcher m = p.matcher(tiff.getName());
        if (!m.matches()){
            return null;
        }
        String prefix = m.group(1);
        String suffix = m.group(2);
        
        
        p = Pattern.compile(prefix+"(\\d{3})"+suffix);
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
                list.add(file.getPath());
            }
        }
       
        return list.toArray(new String[0]);
    }
    
    String segTiff;
    Integer firstTime;
    Integer lastTime;
    boolean force=false;
    boolean qsub = false;
}
