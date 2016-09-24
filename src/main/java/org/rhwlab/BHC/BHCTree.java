/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.TGMM_NucleusDirectory;

/**
 *
 * @author gevirl
 */
public class BHCTree {
    public BHCTree(String file)throws Exception {
        this.fileName = file;
        roots = readTreeXML(file);
        time = TGMM_NucleusDirectory.getTime(new File(file));
    }
    
    
    
    public void saveCutAtThresholdAsXML(String file,double thresh)throws Exception {
        saveClusterListAsXML(file,roots,thresh);
    }
    
    static List<Node> readTreeXML(String xml)throws Exception {
        ArrayList<Node> ret = new ArrayList<>();
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();      
        for (Element nodeEle : root.getChildren("Node")){
            StdNode std = new StdNode(nodeEle);  // build the node and all the children
            ret.add(std);
        }
        return ret;
    }
    public void saveClustersAsXML(String file,double threshold)throws Exception {
        saveClusterListAsXML(file,roots,threshold);
    }
    public Element formXML(double threshold){
        return formXML(this.roots,threshold);
    }
    // save a list of clusters to an XML file
    // each cluster is saved with it's children clusters
    static void saveClusterListAsXML(String file,List<Node> clusters,double threshold)throws Exception{
        Element root = formXML(clusters,threshold);

        File f = new File(file);
        File outFile = new File(f.getParent(),f.getName());
        OutputStream stream = new FileOutputStream(outFile);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();        
    }  
    
    static public Element formXML(List<Node> clusters,double threshold){
        Element root = new Element("BHCClusterList"); 
        root.setAttribute("threshold", Double.toString(threshold));
        int id = 1;
        for (Node cl : clusters){
            int used = cl.saveAsXMLByThreshold(root,threshold,id);  // save as a Gaussian Mixture Model
            if (used != -1){
                id = used + 1;
            }
        }
        return root;
    }
    
    public TreeSet<Double> allPosteriors(){
        TreeSet<Double> ret = new TreeSet<>();
        for (Node root : roots){
            ((NodeBase)root).allPosteriors(ret);
        }
        return ret;
    }
    public int getTime(){
        return time;
    }
    public String getBaseName(){
        File f = new File(fileName);
        String name = f.getName().substring(0,f.getName().indexOf("BHCTree"));
        return new File(f.getParentFile(),name).getPath();
    }
    String fileName ;
    int time;
    List<Node> roots;
    TreeMap<Double,Integer> cutCounts = new TreeMap<>();
}
