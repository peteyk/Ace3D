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
import org.rhwlab.BHC.Node;

/**
 *
 * @author gevirl
 */
public class BHC_NucleusFile {
    public BHC_NucleusFile(Element root){
        init(root);
    }
    // open a tgmm nucleus file , adding nuclei to the Ace3dNucleusFile
    public BHC_NucleusFile(File file)throws Exception {
        this.file = file;
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(file); 
        Element document = doc.getRootElement(); 
        init(document);
    }
    public void init(Element document){
        rootNucs = new HashSet<>();
        this.cutThreshold = Double.valueOf(document.getAttributeValue("threshold"));
        this.time = Integer.valueOf(document.getAttributeValue("time"));
        this.treeFile = document.getAttributeValue("treefile");
        List<Element> gmmList = document.getChildren("GaussianMixtureModel");
        for (Element gmm : gmmList){
            BHC_Nucleus tgmmNuc = new BHC_Nucleus(time,gmm);
            rootNucs.add(tgmmNuc);
        }        
    }
    public File getBHCTreeFile(){
        String fileName = file.getName();
        return new File(file.getParent(),fileName.replace(".xml", "BHCTree.xml"));
    }
    public double getThreshold(){
        return this.cutThreshold;
    }
    public void setThreshold(double th){
        this.cutThreshold = th;
    }

    public Set<BHC_Nucleus> getNuclei(){
        return this.rootNucs;
    }
    Set<BHC_Nucleus> rootNucs;
    File file;
    int time;
    double cutThreshold;
    String treeFile;
}
