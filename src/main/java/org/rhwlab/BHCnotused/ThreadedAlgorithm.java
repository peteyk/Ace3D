/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealVector;
import org.rhwlab.dispim.datasource.DataSource;
import org.rhwlab.variationalbayesian.MicroClusterDataSource;

/**
 *
 * @author gevirl
 */
public class ThreadedAlgorithm implements Runnable {
    public ThreadedAlgorithm(){
    }
    public void init(int seg)throws Exception {
        clusters = new ArrayList<>();
        // build the initial clusters with one data point in each cluster
        for (int n=0 ; n<source.getK() ; ++n){
            RealVector v = source.getCenter(n);
            Dfp[] z = new Dfp[v.getDimension()];
            for (int i=0 ; i<z.length ; ++i){
                z[i] = field.newDfp(v.getEntry(i));
            }
            LabeledFieldVector fv = new LabeledFieldVector(z,n);
            Cluster cluster= new Cluster(new GaussianGIWPrior(fv));           
            clusters.add(cluster);
        }
        
        // make all possible pairings of initial clusters
        pairs = new HashMap<>();
        for (int i=0 ; i<clusters.size()-1 ; ++i){
            HashMap<Cluster,Cluster> map = new HashMap<>();
            MergeAction merge = new MergeAction(clusters,i,i+1,map);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(merge);
            Cluster clusterI = clusters.get(i);
            pairs.put(clusterI,map);
            System.out.printf("Cluster %d paired\n",i);
        }
    }
    
    private Cluster maximumRCluster(){
        Cluster ret = null;
        Dfp maxR = field.getZero();
        for (HashMap<Cluster,Cluster> map : pairs.values()){
            for (Cluster cl : map.values()){
                Dfp r = cl.getPosterior();
                if (r.greaterThan(maxR)){
                    maxR = cl.r;
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
            Cluster T = maximumRCluster();
            
            if (T == null){
                return;
            }

T.printCluster(System.out);
            
//            System.out.printf("size=%d,dpm=%e,like=%e,pi=%f,pi*like=%e,r=%.20f,d=%s,Gam=%s,f=%s,d2=%s\n", T.data.getN(),T.dpm,T.data.likelihood(),T.pi,T.pi*T.data.likelihood(),T.r,T.d,T.gammaN,T.f,T.d2);
            // remove the children of the max r pair
            pairs.remove(T.left);
            pairs.remove(T.right);
            for (HashMap<Cluster,Cluster> map : pairs.values()){
                map.remove(T.left);
                map.remove(T.right);
            }
            clusters.remove(T.left);
            clusters.remove(T.right);
            
            // make new pairs with all the clusters
            HashMap<Cluster,Cluster> map = new HashMap<>();
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
        Cluster.saveClusterListAsXML(file, clusters,0.5);
    }    
    static DfpField field;
    MicroClusterDataSource source;
    List<Cluster> clusters;
    HashMap<Cluster,HashMap<Cluster,Cluster>> pairs;
}
