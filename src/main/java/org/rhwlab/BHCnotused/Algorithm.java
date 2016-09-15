/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.RealVector;
import org.rhwlab.dispim.datasource.DataSource;
import org.rhwlab.dispim.datasource.ClusteredDataSource;

/**
 *
 * @author gevirl
 */
public class Algorithm implements Runnable {
    public void init(int seg){
        clusters = new ArrayList<>();
        for (int n=0 ; n<source.getClusterCount() ; ++n){
            RealVector v = source.getCenter(n);
            Dfp[] z = new Dfp[v.getDimension()];
            for (int i=0 ; i<z.length ; ++i){
                z[i] = field.newDfp(v.getEntry(i));
            }
            LabeledFieldVector fv = new LabeledFieldVector(z,n);
            Cluster cluster= new Cluster(new GaussianGIWPrior(fv));           
            clusters.add(cluster);
        }
        pairs = new HashMap<>();
        for (int i=0 ; i<clusters.size()-1 ; ++i){
            Cluster clusterI = clusters.get(i);
            HashMap<Cluster,Cluster> map = new HashMap<>();
            for (int j=i+1 ; j<clusters.size() ; ++j){
                Cluster clusterJ = clusters.get(j);
                Cluster T = new Cluster(clusterI,clusterJ);
                map.put(clusterJ,T);
            }
            pairs.put(clusterI,map);
        }
    }
    private Cluster maximumRCluster(){
        Cluster ret = null;
        Dfp maxR = field.getZero();
        for (HashMap<Cluster,Cluster> map : pairs.values()){
            for (Cluster cl : map.values()){
                Dfp r = cl.getPosterior();
                double realR = r.getReal();
                if (r.greaterThan(maxR)){
                    maxR = cl.r;
                    ret = cl;
                }
            }
        }
System.out.printf("Posterior= %s\n",maxR.toString());
        return ret;
    }

    @Override
    public void run(){
        while (clusters.size()>1){
System.out.printf("\n%d Clusters\n",clusters.size());
            Cluster T = maximumRCluster();
            
            if (T == null){
                return;
            }
            if (T ==null || T.left == null || T.right == null){
                int auishdfius=0;
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
            for (Cluster cl : clusters){
                Cluster Tcl = new Cluster(T,cl);
                map.put(cl,Tcl );
            }
            pairs.put(T,map);
            clusters.add(T);
        }
        int asdsadf=0;
    }
/*
    public void run2() {
        while (clusters.size()>1){
            // find the pair of clusters with the highest posterior
            double rMax = 0.0;
            Cluster save = null;
            for (int i=0 ; i<clusters.size()-1 ; ++i){
                for (int j=i+1 ; j<clusters.size() ; ++j){
                    Cluster T = new Cluster(clusters.get(i),clusters.get(j));
                    double r = T.getPosterior();
                    if (r > rMax){
                        rMax = r;
                        save = T;
                    }
                }
            }
            clusters.remove(save.right);
            clusters.remove(save.left);
            clusters.add(save);
        }
    }
*/
    public void setSource(DataSource src){
        this.source = (ClusteredDataSource)src;
    }
    static public void setDfpField(DfpField fld){
        field = fld;
    }
    public void saveResultAsXML(String file)throws Exception {
        Cluster.saveClusterListAsXML(file, clusters,0.5);
    }
    static DfpField field;
    ClusteredDataSource source;
    List<Cluster> clusters;
    HashMap<Cluster,HashMap<Cluster,Cluster>> pairs;
}
