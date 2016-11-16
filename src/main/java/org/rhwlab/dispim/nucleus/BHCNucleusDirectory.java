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

/**
 *
 * @author gevirl
 */
// the container directory of all the BHCNucleusFiles
public class BHCNucleusDirectory {
    public BHCNucleusDirectory(File file) {
        this.typicalFile = file;
    }
/*
    public void openInto(Ace3DNucleusFile nucFile)throws Exception {
        nucFile.opening = true;
        buildMap();
        for (BHCNucleusFile gmmFile : fileMap.values()){
            nucFile.addBHC(gmmFile);
        }
        nucFile.opening = false;
        nucFile.notifyListeners();
    }
*/
    private void buildMap()throws Exception {
        File directory = typicalFile.getParentFile();
        Matcher m = p.matcher(typicalFile.getName());
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
                    BHCNucleusFile gmmFile = new BHCNucleusFile(file);
                    
                    fileMap.put(time, gmmFile);
                }
            }
        }        
    }
    public BHCNucleusFile getFileforTime(int time)throws Exception {
        if (fileMap== null){
            buildMap();
        }
        return fileMap.get(time);
    }
    public void putFileForTime(int time,BHCNucleusFile file){
        this.fileMap.put(time,file);
    }
    
    // extract the time from the BHCNucleusFile name
    static public int getTime(File file){
        Matcher m = p.matcher(file.getName());
        m.matches(); 
        if (m.matches()){
            return Integer.valueOf(m.group(2));
        }
        return -1;
    }
    public File getTypicalFile(){
        return typicalFile;
    }

    File typicalFile;
    static Pattern p = Pattern.compile("(.+)(\\d{3})(.+xml)");
    HashMap<Integer,BHCNucleusFile> fileMap;  // the BHCNucleusFile's indexed by time
}
