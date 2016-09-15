/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 *
 * @author gevirl
 */
// merge the base cluster with a sequential list of clusters
// the start and end of the list are specified
// it not end is given use all clusters greater than the start
public class MergeAction extends RecursiveAction {
    public MergeAction(List<Cluster> clusters,int base,int start,HashMap<Cluster,Cluster> pairs){
        this(clusters,base,start,clusters.size()-1,pairs);
    }
    public MergeAction(List<Cluster> clusters,int base,int start,int end,HashMap<Cluster,Cluster> pairs){
        this(clusters,clusters.get(base),start,end,pairs);
    }
    public MergeAction(List<Cluster> clusters,Cluster B,int start,int end,HashMap<Cluster,Cluster> pairs){
        this.clusters = clusters;
        this.B = B;
        this.start = start;
        this.end = end;
        this.pairs = pairs;
    }

    @Override
    protected void compute() {
        if (end - start < 400){
            for (int i=start ; i<=end ; ++i){
                Cluster T = clusters.get(i);
                Cluster M = new Cluster(B,T);
                if (M.data.getN() == 1){
                    int asohdfuis=0;
                }
                pairs.put(T, M);
            }            
        } else {
            // split the job up 
            int mid = (start + end)/2;
            MergeAction a = new MergeAction(clusters,B,start,mid,pairs);
            MergeAction b = new MergeAction(clusters,B,mid+1,end,pairs);
            invokeAll(a,b);
        }
    }
    List<Cluster> clusters;
    Cluster B;
    int base;
    int start;
    int end;
    HashMap<Cluster,Cluster> pairs;

}
