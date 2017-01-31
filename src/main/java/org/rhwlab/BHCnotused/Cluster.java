/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.special.Gamma;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;


/**
 *
 * @author gevirl
 */
public class Cluster {
    public Cluster(DataModel model){
        data = model;
        this.left = null;
        this.right = null;
        d = field.newDfp(alpha);
        pi = field.getOne();
        dpm = this.DPMLikelihood();
        posterior();
    }
    public Cluster(Cluster lef,Cluster rig){
        data = lef.data.mergeWith(rig.data);
        Dfp G = field.newDfp(Gamma.logGamma(data.getN())).exp();
        d = alpha.multiply(G.add(lef.d.multiply(rig.d)));
        pi = alpha.multiply(G.divide(d));

        this.left = lef;
        this.right = rig;
        dpm = this.DPMLikelihood();
        posterior();

        int iodrti=0;
    }
    

    
    //the likelihood of this node and all consistent subtrees
    public Dfp DPMLikelihood(){       
        Dfp first = pi.multiply(data.likelihood());
        Dfp second = field.getZero();
        if (left != null && right != null){
            Dfp onePi = field.getOne().subtract(pi);
            second = onePi.multiply(left.dpm.multiply(right.dpm));
        }
        Dfp ret = first.add(second);
        return ret;
    }

    private Dfp posterior(){
        r = this.pi.multiply(data.likelihood().divide(dpm));
        return r;
    }

    public Dfp getPosterior(){
        return this.r;
    }
    static public void setAlpha(double a){
        alpha = field.newDfp(a);
    }
    public void printCluster(PrintStream stream){
        data.print(stream);
        stream.printf("dpm=%s\n", this.dpm.toString());
        stream.printf("pi=%s\n", this.pi.toString());
    }
    static public void setDfpField(DfpField fld){
        field = fld;
    } 
    
    // save a list of clusters to an XML file
    // each cluster is saved with it's children clusters
    static void saveClusterListAsXML(String file,List<Cluster> clusters,double threshold)throws Exception{
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCClusterList"); 
        int id = 1;
        for (Cluster cl : clusters){
            id = cl.saveAsXML(root,threshold,id) + 1;
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();        
    }
    public  int saveAsXML(Element root,double threshold,int id)throws Exception {
        if (this.getPosterior().getReal()>=threshold){
            saveAsXML(root,id);
            return id;
        }
        if (this.left != null && this.right != null){
            int idleft = left.saveAsXML(root, threshold,id);
            int idright = right.saveAsXML(root, threshold,idleft+1);
            return idright;
        }
        return id-1;
        
    }
    // save this cluster and children clusters to the xml element
    public void saveAsXML(Element root,int id)throws Exception {
        Element clusterEle = new Element("GaussianMixtureModel");
        clusterEle.setAttribute("id", String.format("%d", id));
        clusterEle.setAttribute("parent", "-1");
        
        StringBuilder builder = new StringBuilder();
        for (int j=0 ; j<data.getMean().getDimension() ; ++j){
            if (j > 0){
                builder.append(" ");
            }
            builder.append(data.getMean().getEntry(j));

        }
        clusterEle.setAttribute("m", builder.toString());
        
        RealMatrix W = data.getPrecision();
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
        
        clusterEle.setAttribute("pi", pi.toString());
        clusterEle.setAttribute("likelihood",data.likelihood().toString());
        clusterEle.setAttribute("DPM",dpm.toString());
        clusterEle.setAttribute("posterior", r.toString());
        
        clusterEle.setAttribute("d", d.toString());

        clusterEle.addContent(data.asString());

        root.addContent(clusterEle);
    }
    Cluster left;
    Cluster right;
    DataModel data;
    Dfp d;
    Dfp pi;
    Dfp r;   // posterior of the merged hypothesis
    Dfp dpm;  //likelihood of this cluster/subtree - all tree consistent groupings of data considered
    
    static Dfp alpha;
    static DfpField field = new DfpField(20);  // 20 decimal digits
}
