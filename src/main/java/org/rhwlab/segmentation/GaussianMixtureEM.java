/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.segmentation;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.rhwlab.BHC.Utils;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;
import org.rhwlab.dispim.datasource.TiffDataSource;
import org.rhwlab.dispim.datasource.Voxel;
/**
 *
 * @author gevirl
 */
public class GaussianMixtureEM implements Runnable {
    public void setSource(TiffDataSource source){
        this.source = source;
    }
    public void setMaxI(int i){
        this.maxI = i;
    }
    public void init(){
        N = new double[K];
        mu = new double[K];
        sigma = new double[K];
        lnpi = new double[K];
        normal = new NormalDistribution[K];
        
        // initialize all the voxels to be likely background segment
        r = new double[source.getN()][];
        for (int n=0 ; n<source.getN() ; ++n){
            int intensity = source.get(n).getIntensity();
            r[n] = new double[K];
            for (int k=0 ; k<K ; ++k){
                r[n][k] = 0.0;
            }
            if (intensity > thresh){
                r[n][1] = 1.0;
            } else {
                r[n][0] = 1.0;
            }
        }
        
        M_Step();
    }
    @Override
    public void run() {
        int iter = 0;
        while (iter < maxI){
            E_Step();
            M_Step();
            ++iter;
            int asdfuios=0;
            
        }
    }    
    private void M_Step(){
        for (int k=0 ; k<K ; ++k){
            N[k] = 0.0;
            for (int n=0 ; n<source.getN() ; ++n){
                N[k] = N[k] + r[n][k];
            }
            // compute the mean 
            double sum = 0;
            for (int n=0 ; n<source.getN() ; ++n){
                sum = sum + r[n][k]*source.get(n).getIntensity();
            }
            mu[k] = sum/N[k];
            
            // compute the SD;
            double var = 0.0;
            double mean = mu[k];
            for (int n=0 ; n<source.getN() ; ++n){
                double del = source.get(n).getIntensity() - mean;
                var = var + r[n][k]*del*del;
            }
            sigma[k] = Math.sqrt(var/N[k]);
            
            lnpi[k] = Math.log(N[k]/source.getN());
            try {
                normal[k] = new NormalDistribution(mu[k],sigma[k]);
            } catch (Exception exc){
                int ashdf=0;
            }
        }
    }
    private void E_Step(){
        for (int n=0 ; n<source.getN() ; ++n){
            double x =source.get(n).getIntensity();
            r[n][0] = Utils.elnMult(lnpi[0], normal[0].logDensity(x));
            double sum =  r[n][0];
            for (int k=1 ; k<K ; ++k){
                double v=  Utils.elnMult(lnpi[k], normal[k].logDensity(x));
                sum = Utils.elnsum(sum, v);
                r[n][k] = v;
            }
            for (int k=0 ; k<K ; ++k){
                r[n][k] = Math.exp(r[n][k] - sum);
                if (!Double.isFinite(r[n][k])){
                    int asf=0;
                }
            }
            int uisahdfuisd=0;
        }
    }
    public SegmentedTiffDataSource asSegmentedTiff(){
        SegmentedTiffDataSource ret = new SegmentedTiffDataSource(source);
        for (int n=0 ; n<source.getN() ; ++n){
            double p = r[n][0];
            int seg = 0;
            for (int k=1 ;k<K ; ++k){
                if (r[n][k] > p){
                    seg = k;
                    p = r[n][k];
                }
            }
            ret.addVoxelToSegment(n, seg);
        }
        return ret;
    }
    public static void main(String[] args) throws Exception {
        TiffDataSource source = new TiffDataSource("/net/waterston/vol2/home/gevirl/rnt-1/lineaging/img_TL016.tif");
        GaussianMixtureEM model = new GaussianMixtureEM();
        model.setSource(source);
        model.init();
        model.run();
        SegmentedTiffDataSource result = model.asSegmentedTiff();
        result.saveAsTiff("/net/waterston/vol2/home/gevirl/rnt-1/lineaging/img_TL016_segmented.tif");
        int iausdfhuis=0;
    }
    int thresh = 50;
    int maxI=20;  // maximum interations
    int K=2;  // number of segments
    TiffDataSource source;
    double[] N;  // number of voxels in each segment;
    double r[][];  // responsibilities (N,K)
    double[] mu;
    double[] sigma;
    double[] lnpi;
    NormalDistribution[] normal;


}
