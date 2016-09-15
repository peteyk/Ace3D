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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
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
    public int saveAsTreeXML(Element root){
        int nodeCount = 1;
        Element nodeEle = new Element("Node");
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
    public  int saveAsXML(Element root,double threshold,int id)throws Exception {
        if (this.getPosterior()>=threshold){
            int used = saveAsXML(root,id);
            return used;
        }
        int idleft = left.saveAsXML(root, threshold,id);
        int idright = right.saveAsXML(root, threshold,idleft+1);
        return idright;
        
    }    

    // save this node to an xml element
    // return -1 if not saved
    public int saveAsXML(Element root,int id) throws Exception {
        List<MicroCluster> micros = new ArrayList<>();
        this.getDataAsMicroCluster(micros);
        RealVector mu = MicroCluster.mean(micros);
        RealMatrix W = MicroCluster.precision(micros, mu);
        if (W != null){
            Element clusterEle = new Element("GaussianMixtureModel");
            clusterEle.setAttribute("id", String.format("%d", id));
            clusterEle.setAttribute("parent", "-1");
            clusterEle.setAttribute("count", String.format("%d",micros.size()));
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
    static public void main(String[] args) throws Exception {
//        List<Node> nodes = NodeBase.readTreeXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHCTree0350.xml");
//        NodeBase.saveClusterListAsXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHCTreeCut0350.xml", nodes, .95);
        int iuhdfui=0;
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
    Integer N;
    Node left;
    Node right;

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
