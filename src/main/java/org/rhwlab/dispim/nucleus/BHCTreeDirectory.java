/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;

/**
 *
 * @author gevirl
 */
public class BHCTreeDirectory {
    public BHCTreeDirectory(File dir){
        this.dir = dir;
        if (!dir.isDirectory()){
            dir = dir.getParentFile();
        }
        File[] files = dir.listFiles();
        Pattern p = Pattern.compile("(.+)(\\d{3})(.+xml)");
        for (File file : files){
            if (file.getName().contains("BHCTree")){
                Matcher m = p.matcher(file.getName());
                if (m.matches()){
                    int time = Integer.valueOf(m.group(2));
                    treeFiles.put(time, file);
                }
            }
        }
    }
    public Ace3DNucleusFile linkInNextTime(Ace3DNucleusFile nucFile)throws Exception {
        int nextTime = nucFile.getLastTime()+1;
        BHCTree tree = new BHCTree(treeFiles.get(nextTime).getPath());
        Element[] cuts = tree.cutTreeWithLinearFunction();
        Ace3DNucleusFile[] clones = new Ace3DNucleusFile[cuts.length];
        clones[0] = nucFile;
        for (int i=1 ; i<clones.length ; ++i){
            clones[i] = nucFile.clone();
        }
        for (int i=0 ; i<clones.length ; ++i){
            clones[i].addBHC(new BHC_NucleusFile(cuts[i]));
            clones[i].linkTimePoint(nextTime-1);
            int uihsdfuis=0;
        }
        // determine the best linkage
        int best = -1;
        int sMin = Integer.MAX_VALUE;
        for (int i=0 ; i<clones.length ; ++i){
            Set<Nucleus> dead = clones[i].getDeadNuclei();
            Set<Cell> rootCells = clones[i].getAllRoots();
            int s  = clones[i].getAllRoots().size() + clones[i].getDeadNuclei().size();
            if (s < sMin){
                best = i;
                sMin = s;
            }            
        }
        return clones[best];
    }
    static public void main(String[] args)throws Exception {
        File bhcNucFile = new File("/net/waterston/vol2/home/gevirl/rnt-1/xml/img_TL016_Simple_Segmentation.xml");
        BHC_NucleusFile nucFile = new BHC_NucleusFile(bhcNucFile);
        Ace3DNucleusFile ace3dFile = new Ace3DNucleusFile();
        ace3dFile.addBHC(nucFile);;
        BHCTreeDirectory bhcDir = new BHCTreeDirectory(new File("/net/waterston/vol2/home/gevirl/rnt-1/xml"));
//        for (int i=0 ; i<25 ; ++i){
            ace3dFile = bhcDir.linkInNextTime(ace3dFile);
 //       }
        ace3dFile.bhc = new BHC_NucleusDirectory(new File("/net/waterston/vol2/home/gevirl/rnt-1/xml"));
        ace3dFile.saveAs(new File("/net/waterston/vol2/home/gevirl/rnt-1/LinkedNuclei.json"));
        int ouahsdfuis=0;
    }
    File dir;
    TreeMap<Integer,File> treeFiles = new TreeMap<>();
}
