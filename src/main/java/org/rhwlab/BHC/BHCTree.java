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

/**
 *
 * @author gevirl
 */
public class BHCTree {
    public BHCTree(String file)throws Exception {
        this.fileName = file;
        readTreeXML(file);
        time = BHCNucleusDirectory.getTime(new File(file));
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
            LogNode std = new LogNode(nodeEle,null);  // build the node and all the children
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
    public int nodeCountAtThreshold(double thresh){
        Element el = this.cutTreeAtThreshold(thresh);
        return el.getChildren("GaussianMixtureModel").size();
    }
    
    public TreeSet<Double> allPosteriors(){
        if (this.allPosts == null){
            this.allPosts = new TreeSet<>();
            for (Node root : roots){
                ((NodeBase)root).allPosteriors(this.allPosts);
            }
        }        
        return allPosts;
    }
    public void allPosteriorProb(TreeMap<Integer,Double> probs){
        TreeSet<Node> leaves = new TreeSet<>();
        for (Node root : roots){
            leaves.add(root);
        }
        this.allPosteriorProb(leaves, probs);
    }
    public void allPosteriorProb(TreeSet<Node> leaves,TreeMap<Integer,Double> probs){
        if (leaves.isEmpty()){
            return ;
        }
        // find the leaf with the lowest probability
        Node minNode = leaves.first();
        if (minNode.getPosterior() == 1.0){
            return;
        }
        // add the minMode prob into the result
        probs.put(probs.size()+1, minNode.getPosterior());
        
        // update the leaf set
        leaves.remove(minNode);
        // add the children of minNode to the input list
        if (minNode.getLeft()!=null && minNode.getRight()!=null){
            leaves.add(minNode.getLeft());
            leaves.add(minNode.getRight());
        }
        allPosteriorProb(leaves,probs);
    }
    public BHCNucleusSet cutToN(int n){
        TreeSet<Node> leaves =  new TreeSet<>();
        leaves.addAll(roots);
        this.cutToN(n,leaves);
        Node[] nodeArray = leaves.toArray(new Node[0]);
        BHCNucleusData[] nucData= new BHCNucleusData[leaves.size()];
        
        for (int i=0 ; i<nodeArray.length ; ++i){
            NodeBase nodeBase = (NodeBase)nodeArray[i];
            Element ele = nodeBase.formElementXML(i);
            nucData[i] = new BHCNucleusData(time,ele);
            int iusdfi=0;
        }
        return new BHCNucleusSet(time,fileName,0.,nucData);
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
    public double getAlpha(){
        return alpha;
    }
    public double[] getS(){
        return s;
    }
    public int getNu(){
        return nu;
    }
    static public void main(String[] args) throws Exception {
        String f = "/net/waterston/vol2/home/gevirl/rnt-1/xml/img_TL017_Simple_SegmentationBHCTree.xml";
        BHCTree tree = new BHCTree(f);
        tree.cutTreeWithLinearFunction();

    }     
    String fileName ;
    int time;
    List<Node> roots;
    double alpha;
    double[] s;
    int nu;
    double[] mu;
    TreeSet<Double> allPosts = null;
    
            
 //   TreeMap<Double,Integer> cutCounts = new TreeMap<>();
}
