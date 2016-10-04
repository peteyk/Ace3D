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
    
    // open a tgmm nucleus file , adding nuclei to the Ace3dNucleusFile
    public void open(int time,File file)throws Exception {
        this.file = file;
        SAXBuilder saxBuilder = new SAXBuilder();
        rootNucs = new HashSet<>();

        Document doc = saxBuilder.build(file); 
        Element document = doc.getRootElement(); 
        this.cutThreshold = Double.valueOf(document.getAttributeValue("threshold"));
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
    static public Element formXML(List<Node> clusters,String treeFile,double threshold,int time){
        Element root = new Element("BHCNucleusList"); 
        root.setAttribute("treefile", treeFile);
        root.setAttribute("threshold", Double.toString(threshold));
        root.setAttribute("time", Integer.toString(time));
        int id = 1;
        for (Node cl : clusters){
            int used = cl.saveAsXMLByThreshold(root,threshold,id);  // save as a Gaussian Mixture Model
            if (used != -1){
                id = used + 1;
            }
        }
        return root;
    } 
    public Set<BHC_Nucleus> getNuclei(){
        return this.rootNucs;
    }
    Set<BHC_Nucleus> rootNucs;
    File file;
    int time;
    double cutThreshold;
}
