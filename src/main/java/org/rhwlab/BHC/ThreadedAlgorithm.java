/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealMatrix;
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
        posteriors = new TreeSet(new NodeComparator());
/*        
        field= new DfpField(100);
        NodeBase.setDfpField(field);
        MicroCluster.setField(field);
*/        
        mu = source.getDataMean().toArray();
        NodeBase.maxN = 6000;
        StdNode.setParameters(nu,beta,mu,s);
//        DfpNode.setParameters(nu,beta,mu,s);
//        RealMatrix s0 = source.getDataVariance();
//        StdNode.setS(s0);
        
        CellCounts cc = new CellCounts();
        this.alpha = alpha;
        StdNode.setAlpha(alpha);  // for time point 75
 //       DfpNode.setAlpha(alpha);  // for time point 75
        
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
            for (Object obj : map.values()){
//                Node node = (Node)obj;
                posteriors.add((Node)obj);
 /*               
                if (this.posteriorMap.get(node.getPosterior())!=null){
                    int aiusdhfuis=0;
                }
                this.posteriorMap.put(node.getPosterior(),node);
*/                
            }            
            Node clusterI = clusters.get(i);
            pairs.put(clusterI,map);
            System.out.printf("Cluster %d paired\n",i);
        }
    }
  
    private Node maximumRCluster(){
        return this.posteriors.last();
//        return this.posteriorMap.lastEntry().getValue();
/*        
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
        */
    }  
  
    @Override
    public void run() {
        TimeProfile profile = new TimeProfile();
        while (clusters.size()>1){
            if (clusters.size()==2724){
                int asdhfuis=0;
            }
            System.out.printf("\n%d Clusters\n",clusters.size());
            profile.report(System.out, "Before maximumRCluster");
            Node T = maximumRCluster();
            profile.report(System.out, "After maximumRCluster");
            if (T == null ){
                return;
            }   
            T.print(System.out);

            Node tLeft = T.getLeft();
            Node tRight = T.getRight();
            
            // remove the children of the max r pair
            Map leftMap = (Map)pairs.remove(T.getLeft());
            if (leftMap != null){
                for (Object leftObj : leftMap.values()){
                    this.posteriors.remove(((Node)leftObj));
 //                   this.posteriorMap.remove(((Node)leftObj).getPosterior());
                }
            }
            
            
            Map rightMap = (Map)pairs.remove(T.getRight());
            if (rightMap != null){
                for (Object rightObj : rightMap.values()){
                    this.posteriors.remove(((Node)rightObj));
 //                   this.posteriorMap.remove(((Node)rightObj).getPosterior());
                }  
            }
            
            for (Object obj : pairs.values()){
                Map map = (Map)obj;
                Object nodeObj = map.remove(T.getLeft());
                if (nodeObj != null){
                    this.posteriors.remove(((Node)nodeObj));
//                    posteriorMap.remove(((Node)nodeObj).getPosterior());
                }
                nodeObj = map.remove(T.getRight());
                if (nodeObj != null){
                    this.posteriors.remove(((Node)nodeObj));
//                    posteriorMap.remove(((Node)nodeObj).getPosterior());
                }
            }
            System.out.println("Removed");
            
           
            boolean leftRemoved = clusters.remove(T.getLeft());
            boolean rightRemoved = clusters.remove(T.getRight());
            
            if (leftRemoved){
 //               T.getLeft().print(System.out);
            } else {
                int iuashdf=0;
            }
            if (rightRemoved){
 //               T.getRight().print(System.out);
            }else {
                int oiashdfio=0;
            }


            profile.report(System.out,"Starting MergeAction");
            // make new pairs with all the clusters
            Map map = Collections.synchronizedMap(new HashMap<>());
            MergeAction merge = new MergeAction(clusters,T,0,clusters.size()-1,map);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(merge);
            profile.report(System.out,"MergeAction complete");
            pairs.put(T,map);
            for (Object obj : map.values()){
/*                
                if (this.posteriorMap.get(node.getPosterior())!=null){
                    int aiusdhfuis=0;
                }     
*/
                this.posteriors.add((Node)obj);
//                this.posteriorMap.put(node.getPosterior(),node);
            }
            clusters.add(T);
        }
    }
    public void setSource(DataSource src){
        this.source = (MicroClusterDataSource)src;
    }
    public void setPrecision(double[] prec){
        this.s = prec;
    }
    public void setNu(int v){
        this.nu = v;
    }
    /*
    static public void setDfpField(DfpField fld){
        field = fld;
    }
 */   
    // saves the result as a BHC tree xml
    public BHCTree saveResultAsXML(String file)throws Exception {
        BHCTree tree = new BHCTree(alpha,s,nu,mu,clusters);
        tree.saveAsXML(file);
        return tree; 
    } 
    public BHCTree resultAsBHCTree(){
        return new BHCTree(alpha, s, nu, mu, clusters);
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
//    static DfpField field;
    MicroClusterDataSource source;
    List<Node> clusters;
//    HashMap<Cluster,HashMap<Cluster,Cluster>> pairs;
    Map pairs;
//    TreeMap<Double,Node> posteriorMap = new TreeMap<>();
    TreeSet<Node> posteriors;
    int nu = 20;
//    double s = 100;
    double[] s;
    double[] mu;
    double beta = 0.0000001; 
    double alpha = 10000000;
}
