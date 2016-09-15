/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.datasource.DataSource;
import org.rhwlab.dispim.datasource.MicroCluster;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;

/**
 *
 * @author gevirl
 */
public class ThreadedAlgorithm implements Runnable {
    public ThreadedAlgorithm(){
    }
    public void init(double alpha)throws Exception {
        field= new DfpField(100);
        NodeBase.setDfpField(field);
        MicroCluster.setField(field);
        
        mu = source.getDataMean().toArray();
        NodeBase.maxN = 6000;
        StdNode.setParameters(nu,beta,mu,s);
        DfpNode.setParameters(nu,beta,mu,s);

        CellCounts cc = new CellCounts();
        this.alpha = alpha;
        StdNode.setAlpha(alpha);  // for time point 75
        DfpNode.setAlpha(alpha);  // for time point 75
        
        clusters = new ArrayList<>();
        // build the initial clusters with one data point in each standard cluster
        for (int n=0 ; n<source.getK() ; ++n){
            MicroCluster micro = (MicroCluster)source.get(n);
//           Node cluster= new StdNode(micro);    
            Node cluster= new LogNode(micro); 
            clusters.add(cluster);
        }
        
        // make all possible pairings of initial clusters
        pairs = Collections.synchronizedMap(new HashMap<>());
        for (int i=0 ; i<clusters.size()-1 ; ++i){
            Map map = Collections.synchronizedMap(new HashMap<>());
            MergeAction merge = new MergeAction(clusters,i,i+1,map);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(merge);
            Node clusterI = clusters.get(i);
            pairs.put(clusterI,map);
            System.out.printf("Cluster %d paired\n",i);
        }
    }
  
    private Node maximumRCluster(){
        Node ret = null;
        Dfp  maxR = field.getZero();
        maxR = field.newDfp(-1.0);
        for (Object obj : pairs.values()){
            Map map = (Map)obj;
            for (Object clObj : map.values()){
                Node cl = (Node)clObj;

                Dfp dfpR = cl.getPosteriorDfp();
                if (maxR.lessThan(dfpR)){
                    maxR = dfpR;
                    ret = cl;
                }
            }
        }
        return ret;
    }  
  
    @Override
    public void run() {
        while (clusters.size()>1){
            
            System.out.printf("\n%d Clusters\n",clusters.size());
            Node T = maximumRCluster();

            if (T == null ){
                return;
            }   

            T.print(System.out);

            // remove the children of the max r pair
            pairs.remove(T.getLeft());
            pairs.remove(T.getRight());
            for (Object obj : pairs.values()){
                Map map = (Map)obj;
                map.remove(T.getLeft());
                map.remove(T.getRight());
            }
            clusters.remove(T.getLeft());
            clusters.remove(T.getRight());

            
            // make new pairs with all the clusters
            Map map = Collections.synchronizedMap(new HashMap<>());
            MergeAction merge = new MergeAction(clusters,T,0,clusters.size()-1,map);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(merge);
            pairs.put(T,map);
            clusters.add(T);
        }
    }
    public void setSource(DataSource src){
        this.source = (MicroClusterDataSource)src;
    }
    static public void setDfpField(DfpField fld){
        field = fld;
    }
    public void saveResultAsXML(String file)throws Exception {
//        NodeBase.saveClusterListAsXML(file, clusters,0.99);
         
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
        for (Node node : clusters){
            ((NodeBase)node).saveAsTreeXML(root);
            TreeSet<Double> posts = new TreeSet<>();
            ((NodeBase)node).allPosteriors(posts);
            int aoshdfuihs=0;
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();          
    } 
    static public void main(String[] args) throws Exception {
        System.out.println("GaussianGIWPrior");
//        SegmentedTiffDataSource source = new SegmentedTiffDataSource("/nfs/waterston/pete/Segmentation/Cherryimg75.tif",
//                "/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.tiff",1);  // background is seg=1
//        source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentationMulti0350.xml");
        MicroClusterDataSource source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentation0350Multi.xml");
/*        
        Dfp[] mu = new Dfp[mean.getDimension()];
        for (int i=0 ;  i<mu.length ; ++i){
            mu[i] = field.newDfp(mean.getEntry(i));
        }
*/
//    double alpha = 10000000000.;
//        Cluster.setAlpha(600);  // for time point 350
        ThreadedAlgorithm alg = new ThreadedAlgorithm();
        alg.setSource(source);
        alg.init(10000000);
        alg.run();
        alg.saveResultAsXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHCTree0350.xml");
//        NodeBase.saveClusterListAsXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHC0350.xml", alg.clusters,0.999);
        int hfuis=0;
    }    
    static DfpField field;
    MicroClusterDataSource source;
    List<Node> clusters;
//    HashMap<Cluster,HashMap<Cluster,Cluster>> pairs;
    Map pairs;
    
    int nu = 4;
    double s = 100;
    double[] mu;
    double beta = 0.0000001; 
    double alpha = 10000000;
}
