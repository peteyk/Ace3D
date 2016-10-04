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
import org.rhwlab.dispim.nucleus.BHC_NucleusDirectory;
import org.rhwlab.dispim.nucleus.BHC_NucleusFile;

/**
 *
 * @author gevirl
 */
public class BHCTree {
    public BHCTree(String file)throws Exception {
        this.fileName = file;
        readTreeXML(file);
        time = BHC_NucleusDirectory.getTime(new File(file));
    }
    
    public BHCTree(double alpha,double s,int nu, double[] mu,List<Node> roots){
        this.roots = roots;
        this.alpha = alpha;
        this.s = s;
        this.nu = nu;
        this.mu = mu;
        this.labelNodes();
    }
    
    public void readTreeXML(String xml)throws Exception {
        roots = new ArrayList<>();
        
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();  
        
        alpha = Double.valueOf(root.getAttributeValue("alpha"));
        s = Double.valueOf(root.getAttributeValue("s"));
        nu = Integer.valueOf(root.getAttributeValue("nu"));
        String muStr = root.getAttributeValue("mu");
        String[] tokens = muStr.substring(1).split(" ");
        mu = new double[tokens.length-1];
        for (int i=0 ; i<mu.length ; ++i){
            mu[i] = Double.valueOf(tokens[i]);
        }
        
        for (Element nodeEle : root.getChildren("Node")){
            StdNode std = new StdNode(nodeEle);  // build the node and all the children
            roots.add(std);
        }
    }    
    // save the tree as xml
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCTrees"); 
        root.setAttribute("alpha", Double.toString(alpha));
        root.setAttribute("s", Double.toString(s));
        root.setAttribute("nu", Integer.toString(nu));
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int d=0 ; d<mu.length ; ++d){
            builder.append(mu[d]);
            builder.append(" ");
        }
        builder.append(")");
        root.setAttribute("mu", builder.toString());
        for (Node node : roots){
            ((NodeBase)node).saveAsTreeXML(root);
            TreeSet<Double> posts = new TreeSet<>();
            ((NodeBase)node).allPosteriors(posts);
            int aoshdfuihs=0;
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();          
    }   
    
    public void saveCutAtThresholdAsXML(String file,double thresh)throws Exception {
        saveCutAtThresholdAsXML(file,roots,fileName,thresh,time);
    }

    // save a list of clusters to an XML file
    // each cluster is saved with it's children clusters
    static void saveCutAtThresholdAsXML(String file,List<Node> clusters,String treeFile,double threshold,int time)throws Exception{
        Element root = BHC_NucleusFile.formXML(clusters,treeFile,threshold,time);
        saveXML(file,root);
    }  
    
    public static void saveXML(String file,Element root)throws Exception {
        File f = new File(file);
        File outFile = new File(f.getParent(),f.getName());
        OutputStream stream = new FileOutputStream(outFile);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();         
    }

    public Element formXML(double threshold){
        return BHC_NucleusFile.formXML(this.roots,this.fileName,threshold,time);
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
    public void labelNodes(){
        int start = 1;
        for (Node root : roots){
            int used = ((NodeBase)root).labelNode(start);
            start = used + 1;
        }
    }
    // cuts the BHC Tree to obtain the given number of nuclei/cells
    public Element cutTreeToCount(int nCells){
        int nDel = 20;
        TreeSet<Double> posteriors = this.allPosteriors();
        Double[] r = posteriors.toArray(new Double[0]);
        for (int start=0 ; start < posteriors.size()-nDel ; start = start + nDel ){
            Element e = this.formXML(r[start]);
            int n = e.getChildren("GaussianMixtureModel").size();
            if (n == nCells){
                return e;
            }
            if (n > nCells){
                // work backwards until found
                for (int i=1 ; i<=nDel ; ++i){
                    e = this.formXML(r[start-i]);
                    n = e.getChildren("GaussianMixtureModel").size();
                    if (n <= nCells){
                        return e;
                    }
                    
                }
            }            

        }
        return null;
    }
    static public void main(String[] args) throws Exception {
        String dir = "/net/waterston/vol2/home/gevirl/rnt-1/segmented";
        File directory = new File(dir);
        for (File file : directory.listFiles()){
            if (file.getName().contains("BHCTree")){
                BHCTree tree = new BHCTree(file.getPath());
                tree.labelNodes();
                tree.saveAsXML(file.getPath());
                int iuasdfusd=0;
            }
        }
    }     
    String fileName ;
    int time;
    List<Node> roots;
    double alpha;
    double s;
    int nu;
    double[] mu;
    
            
 //   TreeMap<Double,Integer> cutCounts = new TreeMap<>();
}
