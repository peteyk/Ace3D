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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.math3.analysis.function.Logistic;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.StatUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.BHCNucleusDirectory;
import org.rhwlab.dispim.nucleus.BHCNucleusSet;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class BHCTree {
    public BHCTree(String file,int t)throws Exception {
        this.fileName = file;
        readTreeXML(file);
//        time = BHCNucleusDirectory.getTime(new File(file));
        time = t;
    }
    
    public BHCTree(double alpha,double[] s,int nu, double[] mu,List<Node> roots){
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
        
        String sStr = root.getAttributeValue("s");
        String[] tokens = sStr.substring(1).split(" ");
        s = new double[tokens.length-1];
        for (int i=0 ; i<s.length ; ++i){
            s[i] = Double.valueOf(tokens[i]);
        }
        
        nu = Integer.valueOf(root.getAttributeValue("nu"));
        
        String muStr = root.getAttributeValue("mu");
        tokens = muStr.substring(1).split(" ");
        mu = new double[tokens.length-1];
        for (int i=0 ; i<mu.length ; ++i){
            mu[i] = Double.valueOf(tokens[i]);
        }
        
        for (Element nodeEle : root.getChildren("Node")){
            LogNode std = new NucleusLogNode(nodeEle,null);  // build the node and all the children
            roots.add(std);
        }
    }    
    // save the tree as xml
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCTrees"); 
        root.setAttribute("alpha", Double.toString(alpha));
        
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i=0 ; i<s.length ; ++i){
            builder.append(s[i]);
            builder.append(" ");
        }
        builder.append(")");
        root.setAttribute("s",  builder.toString());
        
        root.setAttribute("nu", Integer.toString(nu));
        builder = new StringBuilder();
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


    
    public static void saveXML(String file,Element root)throws Exception {
        File f = new File(file);
        File outFile = new File(f.getParent(),f.getName());
        OutputStream stream = new FileOutputStream(outFile);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();         
    }
   
 /*   
    public void saveCutAtThresholdAsXML(String file,double thresh)throws Exception {
        saveXML(file,cutTreeAtThreshold(thresh));
    }  
    public BHCNucleusSet cutToNucleusFile(double threshold){
        return new BHCNucleusSet(cutTreeAtThreshold(threshold));
    }
    
    // cut this tree at the given threshold into an XML element
    // the children of the returned Element are the GaussianMixtureModel descriptions of a nucleus
    public Element cutTreeAtThreshold(double threshold){
        Element root = new Element("BHCNucleusList"); 
        if (fileName != null) root.setAttribute("treefile",fileName);
        root.setAttribute("threshold", Double.toString(threshold));
        root.setAttribute("time", Integer.toString(time));
        int id = 1;
        for (Node cl : roots){
            int used = cl.saveAsXMLByThreshold(root,threshold,id);  // save as a Gaussian Mixture Model
            if (used != -1){
                id = used + 1;
            }
        }
        return root;
    } 
*/    
    /*
    public int nodeCountAtThreshold(double thresh){
        Element el = this.cutTreeAtThreshold(thresh);
        return el.getChildren("GaussianMixtureModel").size();
    }
   */ 
    // cut the tree to N given a minimum volume and a maximum probability
    // may have to return less than N nuclei to meet volume and prob criteria
    public Nucleus[] cutToN(int n,double minVolume,double maxProb){
        int cutN = n;
        TreeSet<NucleusLogNode>  volReducedCut;
        ArrayList<Nucleus> retList = new ArrayList<>();
        while(true){
            TreeSet<NucleusLogNode> cut = cutToN(cutN);
            volReducedCut = new TreeSet<>();
            int i=1;
            retList.clear();
            for (NucleusLogNode logNode : cut){
                BHCNucleusData nucData = BHCNucleusData.factory(logNode, i, time);
                if (nucData!=null && nucData.getVolume()>=minVolume){
                    volReducedCut.add(logNode);
                    retList.add(new Nucleus(nucData));
                    ++i;
                }
            }
            double prob = Math.exp(cut.first().getLogPosterior());
            if (volReducedCut.size() < n && prob <=maxProb){
                ++cutN;
            }else {
                break;
            }
        }

        return retList.toArray(new Nucleus[0]);
    }
    
    public TreeSet<NucleusLogNode> cutToN(int n){
        TreeSet<NucleusLogNode> cut = firstTreeCut();
        while (cut.size()<n) {
            cut = nextTreeCut(cut);
        } 
        return cut;
/*        
        TreeSet<BHCNucleusData> nucSet = new TreeSet<>();
        int i = 1;
        for (NucleusLogNode node : cut){
            Element ele = node.formElementXML(i);
            if (ele !=null){
                BHCNucleusData bhcNuc = new BHCNucleusData(time,ele);
                nucSet.add(bhcNuc);
                ++i;
            }
        }        
        return new BHCNucleusSet(time,fileName,nucSet);
*/        
    }
    
    public TreeSet<NucleusLogNode> firstTreeCut(){
        TreeSet<NucleusLogNode> cut = new TreeSet<>();
        for (Node root : roots){
            cut.add((NucleusLogNode)root);
        }
        return cut;
    }
    public TreeMap<Integer,TreeSet<NucleusLogNode>> allTreeCuts(int maxNodes){
        TreeMap<Integer,TreeSet<NucleusLogNode>> ret = new TreeMap<>();
        
        TreeSet<NucleusLogNode> cut = firstTreeCut();
        ret.put(cut.size(),cut);       
        while (cut.size()<maxNodes) {
            cut = nextTreeCut(cut);
            ret.put(cut.size(), cut);
        }
        return ret;
    }
    // cuts the tree at the next level - produce one more node than previous cut
    public TreeSet<NucleusLogNode> nextTreeCut(TreeSet<NucleusLogNode> previous){
        TreeSet<NucleusLogNode> ret = new TreeSet<>();
        // find the minimum probability node that can be split
        Iterator<NucleusLogNode> iter = previous.iterator();
        while(iter.hasNext()){
            NucleusLogNode node = iter.next();
            if (node.getLeft() != null && node.getRight() != null){
                ret.addAll(previous);
                ret.remove(node);
                ret.add((NucleusLogNode)node.getLeft());
                ret.add((NucleusLogNode)node.getRight());
                break;
            }             
        }     
        return ret;
    }
       public TreeMap<Integer,Double> allPosteriorProb( int maxProbs){
        TreeMap<Integer,TreeSet<NucleusLogNode>> allCuts = allTreeCuts(maxProbs);
        TreeMap<Integer,Double> ret = new TreeMap<>();
        for (Integer i : allCuts.keySet()){
            TreeSet<NucleusLogNode> nodes = allCuts.get(i);
            double p = Math.exp(nodes.first().getLogPosterior());
            ret.put(i,p);
        }

        return ret;
    } 
   
   /*
    public TreeSet<Double> allPosteriors(){
        if (this.allPosts == null){
            this.allPosts = new TreeSet<>();
            for (Node root : roots){
                ((NodeBase)root).allPosteriors(this.allPosts);
            }
        }        
        return allPosts;
    }

    public void allPosteriorProb(TreeSet<Node> leaves,TreeMap<Double,Integer> probs,double minVolume,int maxProbs){
        
        if (leaves.isEmpty() || probs.size()==maxProbs){
            return ;
        }
        // find the leaf with the lowest probability
        Node minNode = leaves.first();
        probs.put(minNode.getLogPosterior(),leaves.size());

        
        // update the leaf set
        leaves.remove(minNode);
        if (((NodeBase)minNode).getLabel()==2){
            int sdkjfnsdiu=0;
        }
        // add the children of minNode to the input list
        if (minNode.getLeft()!=null && minNode.getRight()!=null){
            if (((NucleusLogNode)minNode.getLeft()).getVolume() >= minVolume){
                leaves.add(minNode.getLeft());
            }
            if (((NucleusLogNode)minNode.getRight()).getVolume() >= minVolume){
                leaves.add(minNode.getRight());
            }
    }
        allPosteriorProb(leaves,probs,minVolume,maxProbs);
    }
    // form a set of nuclei that meet the probability threshold and minimum volume
    public BHCNucleusSet cutAtProbability(double prob,double minVolume){
        TreeSet<Node> nodes = new TreeSet<>();
        for (Node root : this.roots){
            cutAtProbability((LogNode)root,prob,nodes);
        }
        
        TreeSet<BHCNucleusData> nucSet = new TreeSet<>();
        int i = 1;
        for (Node node : nodes){
            if (((NucleusLogNode)node).getVolume() >=minVolume){
                Element ele = ((NodeBase)node).formElementXML(i);
                BHCNucleusData bhcNuc = new BHCNucleusData(time,ele);
                nucSet.add(bhcNuc);
                ++i;
            }
        }
        return new BHCNucleusSet(time,fileName,nucSet);
    }
    // cut the tree rooted at the given node to the given probability
    public void cutAtProbability(LogNode node,double prob,TreeSet<Node> leaves){
        if (Math.exp(node.getLogPosterior()) >= prob) {
            leaves.add(node);
            return;
        }
        cutAtProbability((LogNode)node.getRight(),prob,leaves);
        cutAtProbability((LogNode)node.getLeft(),prob,leaves);
    }
    


    public void cutToN(int n,TreeSet<Node> leaves){
        if (leaves.size() == n){
            return;  // done - found n nodes
        }
        // find the minimum node that has children    
        Iterator<Node> iter = leaves.iterator();
        Node minNode = iter.next();
        while (minNode.isLeaf()){
            if (!iter.hasNext()){
                return;  // all the leaves are nodes , can't add any more nodes
            }
            minNode = iter.next();
        }
        
        // increases the leaves by one
        leaves.remove(minNode);
        leaves.add(minNode.getLeft());
        leaves.add(minNode.getRight());
        cutToN(n,leaves);
    }
    
   */ 
    
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
    /*
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
*/
    // find the node with a given label
    public Node findNode(int label){
        for (Node root : roots){
            NodeBase nodeBase = (NodeBase)root;
            Node ret = nodeBase.findNodeWithlabel(label);
            if (ret != null){
                return ret;
            }
        }
        return null;
    }
    // determine if there is a parent child relationship between two nodes given their labels
    public boolean areRelated(int parent,int child){
        NodeBase parentNode = (NodeBase)this.findNode(parent);
        Node childNode = parentNode.findNodeWithlabel(child);
        return childNode != null;
    }
/*    
    public Element[] cutTreeWithLinearFunction(){
        Double[] posteriors = this.allPosteriors().toArray(new Double[0]);
        int x0 = 0;
        int x1 = posteriors.length-1;
        boolean better = true;
        double eMin = lmsError(x0,x1,posteriors);
        while (x0 < x1 && better){
            better = false;
            // try to move x0 up
            while (x0 < x1){
                double e = lmsError(x0+1,x1,posteriors);
                if (e < eMin){
                    eMin = e;
                    ++x0;
                    better = true;
                } else{
                    break;
                }
            }   
            // try to move x1 down
            while (x0 < x1){
                double e = lmsError(x0,x1-1,posteriors);
                if (e < eMin){
                    eMin = e;
                    --x1;
                    better = true;
                } else{
                    break;
                }
            }             

        }
        Element[] ret = new Element[x1-x0+1];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = this.cutTreeAtThreshold(posteriors[x0+i]);
        }
        return ret;
    }
*/
    double lmsError(int x0,int x1,Double[] y){
        double e = 0.0;
        double xDel = (double)(x1-x0);
        for (int i=0 ; i<y.length ; ++i){
            double del;
            if (i < x0 ){
                del = y[i];
            } else if (i > x1){
                del = y[i] - 1.0;
            }  else if (xDel != 0.0 ){
                del = y[i] - ((double)(i-x0))/(xDel);
            } else {
                del = Math.min(y[i],1.0-y[i]);
            }
            e = e + del * del;
        }
        return e;        
    }
    /*
    public Element cutTreeWithLogisticFunction(){
        Double[] posteriors = this.allPosteriors().toArray(new Double[0]);
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i=0 ; i<posteriors.length ; i = i+10){
            if (i >=410 && i<=520){
            points.add(i, posteriors[i]);
 //           System.out.printf("%d  %f\n",i,posteriors[i]);
            }
        }
        double[] parameters = new double[6];
        parameters[0] = 1.0;  // k - upper bound
        parameters[1]= 475.0;  //m -  x value at maximum growth 
        parameters[2] = 1.0;  //b - growth rate
        parameters[3] = 1.0;  // q - related to Y(0)
        parameters[4] = 0.0;  //a - lower bound
        parameters[5] = 1.0;  // nu - affect near which asymptote max growth occurs
        
        Logistic.Parametric func = new Logistic.Parametric();
        for (int i=0 ; i<posteriors.length ; ++i){
            double x = (double)i;
            double value = func.value(x, parameters);
            double[] grad = func.gradient(x, parameters);
//            System.out.printf("%d  %f\n", i,value);
            for (int j=0 ; j<grad.length ; ++j){
 //               System.out.printf("\t%f\n",grad[j]);
            }
        }
        
        SimpleCurveFitter fitter = SimpleCurveFitter.create(new Logistic.Parametric(), parameters);
//        fitter.withMaxIterations(1);
        double[] params = fitter.fit(points.toList());
        return null;
//        double[] results = fitter.fit(points);
    }
*/
    public double getAlpha(){
        return alpha;
    }
    public double[] getS(){
        return s;
    }
    public int getNu(){
        return nu;
    }
    public Node trimSubTree(Node subtree,double minProb){
        return null;
    }
    static public void main(String[] args) throws Exception {
        String f = "/net/waterston/vol2/home/gevirl/rnt-1/xml/img_TL017_Simple_SegmentationBHCTree.xml";
//        BHCTree tree = new BHCTree(f);
 //       tree.cutTreeWithLinearFunction();

    } 
    public String getFileName(){
        return fileName;
    }
   String fileName;
    int time;
    List<Node> roots;
    double alpha;
    double[] s;
    int nu;
    double[] mu;
    TreeSet<Double> allPosts = null;
    
            
    public class TreeCut{
        public double posterior;
        public double volume;
        
    }
}
