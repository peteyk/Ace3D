/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.segmentation;

/**
 *
 * @author gevirl
 */
public class Statistics extends Thread {
    public Statistics(int[] x,double[] r){
        this.x = x;
        this.r = r;
    }
    @Override
    public void run(){
        N = 0.0;
        for (int n=0 ; n<x.length ; ++n){
            N = N + r[n];
        }
        // compute the mean 
        double sum = 0;
        for (int n=0 ; n<x.length ; ++n){
            sum = sum + r[n]*x[n];
        }
        mu = sum/N;

        // compute the SD;
        double var = 0.0;
        double mean = mu;
        for (int n=0 ; n<x.length ; ++n){
            double del = x[n] - mean;
            var = var + r[n]*del*del;
        }
        sigma = Math.sqrt(var/N);

        lnPi = Math.log(N/x.length);        
    }
    public double getMu(){
        return this.mu;
    }
    public double getN(){
        return this.N;
    }
    public double getLnPi(){
        return this.lnPi;
    }
    public double getSigma(){
        return this.sigma;
    }
    int[] x;
    double[] r;
    double mu;
    double sigma;
    double N;
    double lnPi;
    
}
