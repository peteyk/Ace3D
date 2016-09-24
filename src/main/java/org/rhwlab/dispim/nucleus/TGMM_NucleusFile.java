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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class TGMM_NucleusFile {
    // open a tgmm nucleus file , adding nuclei to the Ace3dNucleusFile
    public void open(int time,File file,Ace3DNucleusFile aceFile)throws Exception {
        this.file = file;
        SAXBuilder saxBuilder = new SAXBuilder();
        Set<TGMMNucleus> rootNucs = new HashSet<>();
        Map<Integer,TGMMNucleus> firstParents =  new HashMap<>();
        Map<Integer,TGMMNucleus> secondParents = new HashMap<>();                    

        Document doc = saxBuilder.build(file); 
        Element document = doc.getRootElement(); 
        this.cutThreshold = Double.valueOf(document.getAttributeValue("threshold"));
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
            aceFile.addNucleus(rootNuc,false);
            Cell rootCell = new Cell(rootNuc.getName());
            rootCell.addNucleus(rootNuc);
            aceFile.addRoot(rootCell,false);
        }
        for (Integer parent : secondParents.keySet()){
            TGMMNucleus first = firstParents.get(parent);
            aceFile.addNucleus(first,false);
            Cell firstCell = new Cell(first.getName());
            firstCell.addNucleus(first);
            aceFile.addRoot(firstCell,false);

            TGMMNucleus second = secondParents.get(parent);
            aceFile.addNucleus(second,false);
            Cell secondCell = new Cell(second.getName());
            secondCell.addNucleus(second);
            aceFile.addRoot(secondCell,false); 

            Nucleus parentNuc = aceFile.byName.get(first.getParentName());
            aceFile.linkDivision(parentNuc, first, second);
        }
        for (Integer parent : firstParents.keySet()){
            if (secondParents.get(parent) == null){
                TGMMNucleus nuc = firstParents.get(parent);
                aceFile.addNucleus(nuc,false);
                Nucleus parentNuc = aceFile.byName.get(nuc.getParentName());
                aceFile.linkInTime(parentNuc, nuc);
            }
        }        
    }
    public File getBHCTreeFile(){
        String fileName = file.getName();
        return new File(file.getParent(),fileName.replace(".xml", "BHCTree.xml"));
    }
    public double getThreshold(){
        return this.cutThreshold;
    }
    File file;
    int time;
    double cutThreshold;
}
