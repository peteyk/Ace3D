/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.rhwlab.ace3d.SelectedNucleusFrame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;

/**
 *
 * @author gevirl
 */
public class TGMM_NucleusFile extends Ace3DNucleusFile{
    public TGMM_NucleusFile(File f,SynchronizedMultipleSlicePanel panel,SelectedNucleusFrame frame) {
        super(f,panel,frame);

    }
    @Override
    public void open()throws Exception {
        if (!file.isDirectory()){
            this.file = file.getParentFile();
        }        
        this.opening = true;
        Pattern p = Pattern.compile(".+(\\d{4}).xml");
        SAXBuilder saxBuilder = new SAXBuilder();

        File[] files = file.listFiles();

        for (File file : files){
            if (!file.isDirectory() && file.getName().endsWith("xml")){
                Matcher m = p.matcher(file.getName());
                if (m.matches()){
                    Set<TGMMNucleus> rootNucs = new HashSet<>();
                    Map<Integer,TGMMNucleus> firstParents =  new HashMap<>();
                    Map<Integer,TGMMNucleus> secondParents = new HashMap<>();                    
                    int time = Integer.valueOf(m.group(1));

                    Document doc = saxBuilder.build(file); 
                    Element document = doc.getRootElement();  
                    List<Element> gmmList = document.getChildren("GaussianMixtureModel");
                    for (Element gmm : gmmList){
                        int parent = Integer.valueOf(gmm.getAttributeValue("parent"));
                        TGMMNucleus tgmmNuc = new TGMMNucleus(time,gmm);
                        if (parent == -1){
                            // a new root
                            rootNucs.add(tgmmNuc);
                        } else {
                            TGMMNucleus first = firstParents.get(parent);
                            if (first == null){
                                firstParents.put(parent,tgmmNuc);
                            } else {
                                TGMMNucleus second = secondParents.get(parent);
                                if (second != null){
                                    System.err.println("Error in TGNN_NucleusFile");
                                }
                                secondParents.put(parent,tgmmNuc);
                            }
                        }
                    }
                    for (TGMMNucleus rootNuc : rootNucs){
                        this.addNucleus(rootNuc,false);
                        Cell rootCell = new Cell(rootNuc.getName());
                        rootCell.addNucleus(rootNuc);
                        this.addRoot(rootCell,false);
                    }
                    for (Integer parent : secondParents.keySet()){
                        TGMMNucleus first = firstParents.get(parent);
                        this.addNucleus(first,false);
                        Cell firstCell = new Cell(first.getName());
                        firstCell.addNucleus(first);
                        this.addRoot(firstCell,false);
                        
                        TGMMNucleus second = secondParents.get(parent);
                        this.addNucleus(second,false);
                        Cell secondCell = new Cell(second.getName());
                        secondCell.addNucleus(second);
                        this.addRoot(secondCell,false); 
                        
                        Nucleus parentNuc = this.byName.get(first.getParent());
                        this.linkDivision(parentNuc, first, second);
                    }
                    for (Integer parent : firstParents.keySet()){
                        if (secondParents.get(parent) == null){
                            TGMMNucleus nuc = firstParents.get(parent);
                            this.addNucleus(nuc,false);
                            Nucleus parentNuc = this.byName.get(nuc.getParent());
                            this.linkInTime(parentNuc, nuc);
                        }
                    }
                }
            }
        }
        this.opening = false;
        this.notifyListeners();
    }
}
