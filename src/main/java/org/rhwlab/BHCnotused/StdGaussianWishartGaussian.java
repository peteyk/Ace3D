/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author gevirl
 */
public class StdGaussianWishartGaussian implements StdDataModel {
    // model with one data item
    StdGaussianWishartGaussian(RealVector v){
        data = new HashSet<>();
        data.add(v);
        init();
    }
    // model by combining two other models
    public StdGaussianWishartGaussian(StdGaussianWishartGaussian m1,StdGaussianWishartGaussian m2){
        data = new HashSet<>();
        data.addAll(m1.data);
        data.addAll(m2.data);
        init();
    }
    public void init(){
        int n = data.size();
        int d = m.getDimension();
        double rP = r + n;
        double nuP = nu + n;       
        
        RealMatrix C = new Array2DRowRealMatrix(d,d);
        RealVector X = new ArrayRealVector(d);  // a vector of zeros    
        for (RealVector v : data){
            X = X.add(v);
            RealMatrix v2 = v.outerProduct(v);
            C = C.add(v2);
        }  
        
        RealVector mP = (m.mapMultiply(r).add(X)).mapDivide(rP);
        RealMatrix Sp = C.add(S);
        
        RealMatrix rmmP = mP.outerProduct(mP).scalarMultiply(rP);
        Sp = Sp.add(rmm).subtract(rmmP);  
        
        LUDecomposition ed = new LUDecomposition(Sp);
        double detSp = Math.pow(ed.getDeterminant(),nuP/2.0);   
        
        double gamma = 1.0;
        double gammaP = 1.0;
        for (int i=1 ; i<=d ; ++i){
            gamma = gamma * Gamma.gamma((nu+1-i)/2.0);
            gammaP = gammaP * Gamma.gamma((nuP+1-i)/2.0);
        }  
        
        double t1 = Math.pow(Math.PI, -n*d/2.0);
        double t2 = Math.pow(r/rP,d/2.0);       
        double t3 = detS/detSp;
        
        double t4 = gammaP/gamma;
        likelihood = t1*t2*t3*t4; 
    }
    @Override
    public double likelihood() {
        return likelihood;
    }

    @Override
    public StdDataModel mergeWith(StdDataModel other) {
        return new StdGaussianWishartGaussian(this,(StdGaussianWishartGaussian)other);
    }

    @Override
    public int getN() {
        return data.size();
    }

    @Override
    public void print(PrintStream stream) {
        stream.printf("Size=%d\n",data.size());
        boolean first = true;
        for (RealVector v : data){
            if (!first){
                stream.print(",");
            }
            printVector(stream,v);
            first = false;
        }
        stream.println();
        stream.printf("Likelihood: %s\n", Double.toString(likelihood));
    }
    
    private void printVector(PrintStream stream,RealVector v){
        boolean first = true;
        stream.print("(");
        for (int i=0 ; i<v.getDimension() ; ++i){
            if (!first){
                stream.printf(",%d",(int)v.getEntry(i));
            } else {
                stream.printf("%d",(int)v.getEntry(i));
            }
            first = false;
        }
        stream.print(")");
    }    
    static void setParameters(double n,double beta,double[] mu,double s){
        StdGaussianWishartGaussian.nu = n;
        StdGaussianWishartGaussian.S = new Array2DRowRealMatrix(mu.length,mu.length);
        for (int i=0 ; i<mu.length ; ++i){
            S.setEntry(i, i, s);
        }
        StdGaussianWishartGaussian.r = beta;
        StdGaussianWishartGaussian.m = new ArrayRealVector(mu);
        
        LUDecomposition ed = new LUDecomposition(S);
        detS = Math.pow(ed.getDeterminant(),nu/2.0);
        rmm = m.outerProduct(m).scalarMultiply(r);
    }    
    Set<RealVector> data;
    double likelihood;
    
    static double nu;
    static double r;
    static RealVector m;
    static RealMatrix S;  
    static double detS;
    static RealMatrix rmm;
    
}
