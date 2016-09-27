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
import java.util.TreeSet;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.datasource.MicroCluster;

/**
 *
 * @author gevirl
 */
abstract public class NodeBase implements Node {
    
    @Override
    public void getDataAsMicroCluster(List<MicroCluster> list) {
        if (this instanceof StdNode){
            MicroCluster mc = ((StdNode)this).micro;
            if (mc != null){
                list.add(mc);
                return;
            }
            left.getDataAsMicroCluster(list);
            right.getDataAsMicroCluster(list);
        }else {
            left.getDataAsMicroCluster(list);
            right.getDataAsMicroCluster(list);            
        }
    } 

    // save a list of root nodes into an xml file
    static public void saveAsTreeListXML(String file, List<Node> nodes)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCTrees"); 
        for (Node node : nodes){
            ((NodeBase)node).saveAsTreeXML(root);
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();         
    }
    
    // save this node and all its children into an xml element
    public int saveAsTreeXML(Element root){
        int nodeCount = 1;
        Element nodeEle = new Element("Node");
        nodeEle.setAttribute("label", Integer.toString(label));
        nodeEle.setAttribute("posterior",Double.toString(realR));
        if (left != null){
            nodeCount = ((NodeBase)left).saveAsTreeXML(nodeEle);
            nodeCount = nodeCount + ((NodeBase)right).saveAsTreeXML(nodeEle);
            nodeEle.setAttribute("count",Integer.toString(nodeCount));
        } else {
            int count = ((StdNode)this).addContent(nodeEle);
            nodeEle.setAttribute("points",Integer.toString(count));
        }
        root.addContent(nodeEle);
        return nodeCount;
    }    
    // saves the node as GMM and returns the last used id
    // cuts the tree at the given threshold
    // saves all nodes below this node as GMM based on given threshold
    public  int saveAsXMLByThreshold(Element root,double threshold,int id){
        if (this.getPosterior()>=threshold){
            int used = saveAsXML(root,id);
            return used;
        }
        int idUsed = left.saveAsXMLByThreshold(root, threshold,id);
        if (idUsed == -1){
            idUsed = right.saveAsXMLByThreshold(root, threshold,id);
        }else {
            idUsed = right.saveAsXMLByThreshold(root, threshold,idUsed+1);    
        }
        if (idUsed == -1){
            return id;
        }
        return idUsed;
    }    

    // save just this node to an xml element
    // return -1 if not saved
    public int saveAsXML(Element root,int id) {
        
        List<MicroCluster> micros = new ArrayList<>();
        this.getDataAsMicroCluster(micros);
        RealVector mu = MicroCluster.mean(micros);
        RealMatrix W = MicroCluster.precision(micros, mu);
        if (W != null){
            Element clusterEle = new Element("GaussianMixtureModel");
            clusterEle.setAttribute("id", String.format("%d", id));
            clusterEle.setAttribute("parent", "-1");
            clusterEle.setAttribute("count", String.format("%d",micros.size()));
            clusterEle.setAttribute("sourceNode", String.format("%d", label));
            StringBuilder builder = new StringBuilder();
            for (int j=0 ; j<mu.getDimension() ; ++j){
                if (j > 0){
                    builder.append(" ");
                }
                builder.append(mu.getEntry(j));

            }
            clusterEle.setAttribute("posterior",Double.toString(realR));
            clusterEle.setAttribute("m", builder.toString());
            builder = new StringBuilder();
            for (int row=0 ; row<W.getRowDimension() ; ++row){
                for (int col=0 ; col<W.getColumnDimension() ; ++col){
                    if (row>0 || col>0){
                        builder.append(" ");
                    }
                    builder.append(W.getEntry(row, col));
                }
            }
            clusterEle.setAttribute("W", builder.toString());
            root.addContent(clusterEle);
            return id;
        }else {
            MicroCluster.precision(micros, mu);
        }
        return -1;
    }
    @Override
    public Node getLeft() {
        return this.left;
    }

    @Override
    public Node getRight() {
        return this.right;
    }
    static public void setDfpField(DfpField fld){
        field = fld;
    }
    public int getN(){

        if (N == null){
            ArrayList list = new ArrayList<>();
            this.getDataAsMicroCluster(list);
            this.N = list.size();
        }
        return N;
    }
    @Override
    public int compareTo(Object o) {
        NodeBase other = (NodeBase)o;
        int ret = Double.compare(this.realR,other.realR);
        if (ret == 0){
            ret = Integer.compare(this.hashCode(), other.hashCode());
        }
        return ret;
    }

    public  void allPosteriors(TreeSet<Double> posts){
        posts.add(realR);
        
        if (left != null){
            ((NodeBase)left).allPosteriors(posts);
        }
        if (right != null){
            ((NodeBase)right).allPosteriors(posts);
        }        
    }
    // label this node and all its children, given a starting label
    // return the highest label used
    public int labelNode(int startingWith){
        if (left == null && right == null){
            label = startingWith;
            return startingWith;
        }
        int used = ((NodeBase)left).labelNode(startingWith);
        this.label = used + 1;
        int ret = ((NodeBase)right).labelNode(this.label + 1);
        return ret;
    }
   
    Integer N; // number of microclusters in assigned to this node  
    Node left;
    Node right;
    int label;

    Dfp r;   // posterior of the merged hypothesis
    double realR;
    
    static DfpField field = new DfpField(20);  // 20 decimal digits  
    public static int maxN;
    
    static double nu;
    static RealVector m;
    static RealMatrix S; 
    static double beta;
    static double lnBeta;
    static RealMatrix rmm;
    static Double logdetSnu;
    static Double lnAlpha;
    
    static Double logPi =  Utils.eln(Math.PI);
}
