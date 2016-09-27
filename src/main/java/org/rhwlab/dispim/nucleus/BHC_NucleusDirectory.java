 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rhwlab.ace3d.SelectedNucleusFrame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;

/**
 *
 * @author gevirl
 */
public class BHC_NucleusDirectory extends Ace3DNucleusFile{
    public BHC_NucleusDirectory(File f,SynchronizedMultipleSlicePanel panel,SelectedNucleusFrame frame) {
        super(f,panel,frame);

    }
    @Override
    public void open()throws Exception {

        File directory = file.getParentFile();
        this.opening = true;
        Matcher m = p.matcher(file.getName());
        m.matches();
        String prefix = m.group(1);
        String suffix = m.group(3);
        Pattern exactP = Pattern.compile(prefix+"(\\d{3})"+suffix);
        
        fileMap = new HashMap<>();
        File[] files = directory.listFiles();
        for (File file : files){
            if (!file.isDirectory() && file.getName().endsWith("xml")){
                m = exactP.matcher(file.getName());
                if (m.matches()){
                    int time = Integer.valueOf(m.group(1));
                    BHC_NucleusFile gmmFile = new BHC_NucleusFile();
                    gmmFile.open(time, file, this);
                    fileMap.put(time, gmmFile);
                }
            }
        }
        this.opening = false;
        this.notifyListeners();
    }
    public BHC_NucleusFile getFileforTime(int time){
        return fileMap.get(time);
    }
    public void putFileForTime(int time,BHC_NucleusFile file){
        this.fileMap.put(time,file);
    }
    static public int getTime(File file){
        Matcher m = p.matcher(file.getName());
        m.matches(); 
        if (m.matches()){
            return Integer.valueOf(m.group(2));
        }
        return -1;
    }
    static Pattern p = Pattern.compile("(.+)(\\d{3})(.+xml)");
    HashMap<Integer,BHC_NucleusFile> fileMap;
}
