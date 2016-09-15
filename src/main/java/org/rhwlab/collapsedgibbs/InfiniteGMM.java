/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.collapsedgibbs;

import java.util.List;
import org.rhwlab.dispim.datasource.ClusteredDataSource;
import org.rhwlab.dispim.datasource.GaussianComponent;
import org.rhwlab.dispim.datasource.Voxel;

/**
 *
 * @author gevirl
 */
public class InfiniteGMM implements Runnable {

    public void setSource(ClusteredDataSource s){
        this.X = s;
    }

    @Override
    public void run() {
        for (int t=0 ;  t<iterations ; ++t){
            for (int n=0 ; n<X.getN() ; ++n){
                // remove the nth data point from it's cluster and recalucate statistics on that cluster
                X.getGaussian(n).removePoint(n);
                
                double[] p = new double[X.getClusterCount()];
                List<GaussianComponent> gaussians = X.getAllGaussians();
                for (int k=0 ; k<X.getClusterCount() ; ++k) {
                    GaussianComponent gauss = gaussians.get(k);
                    double pFirst = gauss.getN()/(X.getN()+alpha-1.0);
                }
            }
        }
    }
    double alpha;
    ClusteredDataSource X;
    int iterations;
    
}
