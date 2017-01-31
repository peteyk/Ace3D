/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.io.PrintStream;

/**
 *
 * @author gevirl
 */
public class StdCluster {
    public StdCluster(StdDataModel model){
        data = model;
        this.left = null;
        this.right = null;
        d = alpha;
        pi = 1.0;
        onePi = 0.0;
        posterior();
    } 
    
    // the likelihood of the data in this node/cluster only - given the priors
    public double marginalLikelihood(){
        return data.likelihood();
    }
    
    //the likelihood of this node and all consistent subtrees
    public double DPMLikelihood(){       
        double first = pi * this.marginalLikelihood();
        double second = 0.0;
        if (left != null && right != null){
            second = onePi * left.DPMLikelihood() * right.DPMLikelihood();
        }
        return first + second;
    }

    private void  posterior(){
        dpm = this.DPMLikelihood();
        r = this.pi * this.marginalLikelihood() / dpm;
    }

    public double getPosterior(){
        return this.r;
    }
    static public void setAlpha(double a){
        alpha = a;
    }
    public void printCluster(PrintStream stream){
        data.print(stream);
        stream.printf("dpm=%s\n", Double.toString(dpm));
        stream.printf("pi=%s\n", Double.toString(pi));
    }    
    StdCluster left;
    StdCluster right;
    StdDataModel data;    
    double d;
    double pi;
    double onePi;  // 1.0 - pi;   
    double dpm;
    double r;
    
    static double alpha;
}
