/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import java.io.PrintStream;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author gevirl
 */
public class SuperVoxelGaussianMixture implements Runnable{
    

    public void init(int K){
        this.K = K;
        Dln2 = X.getD()*Math.log(2.0);
        Dln2Pi2 = X.getD()*Math.log(2.0*Math.PI)/2.0;
        
        r = new Array2DRowRealMatrix(X.getN(),K);
        if (mu0 == null){
            mu0 = new ArrayRealVector(X.getD(),0.0);
        }
        // initialize the means 
        
        m = new RealVector[K];
        N = new double[K];
        double Nk = (double)X.getN()/(double)K;
        for (int k=0 ; k<K ; ++k){
            m[k] = X.get(k).centroid;
            N[k] = Nk;
        }
        
        if (W0 == null){
            W0 = MatrixUtils.createRealIdentityMatrix(X.getD());
            W0 = W0.scalarMultiply(.01);
        }
        CholeskyDecomposition de = new CholeskyDecomposition(W0);
        W0inverse = de.getSolver().getInverse();
        W0det = de.getDeterminant();
        W = new RealMatrix[K]; 
        detW = new double[K];
        for (int k=0 ; k<K ; ++k){
            W[k] = W0.copy();
            detW[k] = de.getDeterminant();
        }
        
        alpha = new double[K];
        beta = new double[K];
        lnLambdaTilde = new double[K];
        nu = new double[K];
        xBar = new RealVector[K];
        for (int k=0 ; k<K ; ++k){
            xBar[k] = new ArrayRealVector(X.getD());
            nu[k] = nu0 + N[k];
        }
        lnPi = new double[K]; 
        lnPi();
/*        
        for (k=0 ; k<K ; ++k){
            lnPi[k] = Math.log(N[k]/X.getN());
        }
*/        
        S = new RealMatrix[K];
        for (int k=0 ; k<K ; ++k){
            S[k] = new Array2DRowRealMatrix(X.getD(),X.getD());
        }

        lnRho = new double[K];
        a0 = new double[K];
        for (int k=0 ; k<K ; ++k){
            a0[k] = alpha0;
        }
        C0 = lnC(a0);
        alphaBetaNuLambda();
    }
    
    @Override
    public void run() {
        
        for (int i=0 ; i<this.maxIterations ; ++i){
            System.out.println(i);
            Estep();
//            System.out.println("E step completed");
//            this.reportR();
            Mstep();
//            System.out.println("M step completed");
            
            this.reportN();
            this.reportM();
//            this.reportW();
            this.reportAlpha();
            this.reportBeta();
            this.reportDetW();
            this.reportNu();
            this.reportPi();
            this.reportLnPi();
            this.reportxBar();
//            this.reportS();
//            this.reportR();
            
 //           this.reportN();
 //           this.reportM();
 //           System.out.printf("%d lower bound = %f\n",i, lowerBound());
        }
        report();
    }
    public void report(){
        
    }
    
    // calculate the responsibility array
    public void responsibities(){

        // calculate the r matrix        
        for (int n=0 ; n<X.getN() ; ++n){
            SuperVoxel sv = X.get(n);
            for (int k=0 ; k<K ; ++k){
                RealVector[] voxels = sv.getVoxels();
                double E = 0.0;
                for (RealVector vox : voxels){
                    RealVector diff = vox.subtract(m[k]);
                    E = E + nu[k]*W[k].operate(diff).dotProduct(diff);
                }
                lnRho[k] = X.getD()/beta[k] + lnPi[k] + 0.5*lnLambdaTilde[k] - Dln2Pi2 - 0.5*E;
            }
            double[] rs = normalizeLogP(lnRho);
            for (int k=0 ; k<K ; ++k){
                r.setEntry(n, k, rs[k]);
            }
        }
    }
    public void lnPi(){
        for (int k=0 ; k<K ; ++k){
            lnPi[k] = Math.log(N[k]/X.getT());
        }        
    }
    // compute statisitics from resposibilities and the data
    public void statistics(){
        // calculate N
        for (int k=0 ; k<K ; ++k){
            N[k] = 0.0;
            for (int n=0 ; n<X.getN() ; ++n){
                SuperVoxel sv =X.get(n);
                N[k] = N[k] + sv.getVoxels().length*r.getEntry(n, k);
            }
            N[k] = N[k] + .000001;
        }

        
        // calculate xBar
        for (int k=0 ; k<K ; ++k){
            xBar[k].set(0.0);
            for (int n=0 ; n<X.getN() ; ++n){
                SuperVoxel sv = X.get(n);
                double rnk = r.getEntry(n, k);
                RealVector x = sv.getCenter().mapMultiply(rnk*sv.getVoxels().length);
                xBar[k] = xBar[k].add(x);
            }
            xBar[k].mapDivideToSelf(N[k]);
        } 
        
        // calculate the S
        for (int k=0 ; k<K ; ++k){
            for (int row=0 ; row<X.getD() ; ++row){
                for (int col=0 ; col<X.getD() ; ++col){
                    S[k].setEntry(row, col, 0.0);
                }
            }
            for (int n=0 ; n<X.getN() ; ++n){
                SuperVoxel sv = X.get(n);
                for (RealVector vox : sv.getVoxels()){
                    RealVector del = vox.subtract(xBar[k]);                
                    S[k] = S[k].add(del.outerProduct(del).scalarMultiply(r.getEntry(n, k))); 
                }
            }
            S[k] = S[k].scalarMultiply(1.0/N[k]);
        }
    }
    
    public void Mstep(){
        lnPi();
        alphaBetaNuLambda();
        // compute the means of the components
        for (int k=0 ; k<K ; ++k){
            RealVector v1 = xBar[k].mapMultiply(N[k]);
            RealVector v2 = mu0.mapMultiply(beta0);
            RealVector v3 = v2.add(v1);
            m[k] = v3.mapDivide(beta[k]);
        }
        
        // compute the precision matrices
        for (int k=0 ; k<K ; ++k){
            RealVector del = xBar[k].subtract(mu0);
            RealMatrix del2 = del.outerProduct(del);
            double f = beta0*N[k]/(beta0+N[k]);
            RealMatrix mat = del2.scalarMultiply(f);
            RealMatrix NS = S[k].scalarMultiply(N[k]);
            RealMatrix Winverse = W0inverse.add(NS).add(mat);
            LUDecomposition cd = new LUDecomposition(Winverse);
            W[k] = cd.getSolver().getInverse();
            detW[k] = 1.0/cd.getDeterminant();
            int auiosdfu=0;
        }
    }
    public void alphaBetaNuLambda(){
        // calculate the alpha , beta and nu vectors
        double totalAlpha = 0.0;
        for (int k=0 ; k<K ; ++k){
            alpha[k] = alpha0 + N[k];
            totalAlpha = totalAlpha + alpha[k];
            beta[k] = beta0 + N[k];
            nu[k] = nu0 +N[k] + 1.0;
        }
      
        // calculate lambda tilde vector
        for (int k=0 ; k<K ; ++k){
            double sum = 0.0;
            for (int d=1 ; d<=X.getD() ; ++d){
                double v = (nu[k]+1.0-d)/2.0;
                sum = sum + Gamma.digamma(v);
            }
            lnLambdaTilde[k] = sum + Dln2 + Math.log(detW[k]);
        }        
    }
    public void Estep(){
        responsibities();
        statistics();
    }
    public void setSource(SuperVoxelDataSource source){
        this.X = source;
    }
    public void setMu0(Object obj){
        if (obj instanceof RealVector){
            mu0 = (RealVector)obj;
        } else if (obj instanceof double[]){
            mu0 = new ArrayRealVector((double[])obj);
        }
    }
    public void setBeta0(double b){
        this.beta0 = b;
    }
    public void setAlpha0(double a){
        this.alpha0 = a;
    }
    public void setNu0(double nu){
        this.nu0 = nu;
    }
    
    public void reportAlpha(){
        reportVar(System.out,"Alpha",alpha);
    }
    public void reportBeta(){
        reportVar(System.out,"Beta",beta);
    }    
    public void reportLnPi(){
        
        reportVar(System.out,"lnPi",lnPi);
    }    
    public void reportPi(){
        
        reportExpVar(System.out,"Pi",lnPi);
    }       
    public void reportNu(){
        reportVar(System.out,"Nu",nu);
    } 
    public void reportN(){
        reportVar(System.out,"N",N);
    }   
    public void reportDetW(){
        reportVar(System.out,"|W|",detW);
    } 
    public void reportM(){
        reportVector(System.out,"m",m);
    }
    public void reportxBar(){
        reportVector(System.out,"xBar",xBar);
    }
    public void reportW(){
        reportMatrixArray(System.out,"W",W);
    }
    public void reportS(){
        reportMatrixArray(System.out,"S",S);
    } 
    public void reportR(){
        reportMatrix(System.out,"r",this.r);
    }
    public void reportVar(PrintStream str,String varName,double[] var){
        str.printf("%s",varName);
        for (int i=0 ; i<var.length ; ++i){
            str.printf(",%f",var[i]);
        }
        str.println();
    }
    public void reportExpVar(PrintStream str,String varName,double[] var){
        str.printf("%s",varName);
        for (int i=0 ; i<var.length ; ++i){
            str.printf(",%f",Math.exp(var[i]));
        }
        str.println();
    }    
    public void reportVector(PrintStream str,String name,RealVector[] vec){
        str.printf("%s",name);
        for (int i=0 ; i<vec.length ; ++i){
            str.print("(");
            for (int d=0 ; d< vec[i].getDimension() ; ++d){
                if (d >0){
                    str.print(",");
                }
                str.printf("%f", vec[i].getEntry(d));
            }
            str.print(")");
        }
        str.println();
    }
    public void reportMatrixArray(PrintStream str,String name,RealMatrix[] mat){
        for (int i=0 ; i<mat.length ; ++i){
            str.printf("%s %d\n",name,i);
            for (int r=0 ; r<mat[i].getRowDimension();++r){
                for (int c=0 ; c<mat[i].getColumnDimension() ; ++c){
                    str.printf(" %f",mat[i].getEntry(r, c));
                }
                str.println();
            }
        }
    }
    public void reportMatrix(PrintStream str,String name,RealMatrix mat){
            str.printf("%s\n",name);
            for (int r=0 ; r<mat.getRowDimension();++r){
                for (int c=0 ; c<mat.getColumnDimension() ; ++c){
                    str.printf(" %f",mat.getEntry(r, c));
                }
                str.println();
            }
    }    
    // given a vector of log values, return the normalized values as a probability
    static double[] normalizeLogP(double[] lnP){
        double[] p = new double[lnP.length];
        
        // find the maximum
        double maxLnP = lnP[0];
        for (int i=1 ; i<p.length ; ++i){
            if (lnP[i] > maxLnP){
                maxLnP = lnP[i];
            }
        }
        
        // subtract the maximum and exponentiate
        // sum is guaranted to be >= 1.0;
        double sum = 0.0;
        for (int i=0 ; i<p.length ; ++i){
            p[i] = Math.exp(lnP[i] - maxLnP);
            if (!Double.isFinite(p[i])){
                p[i] = 0.0;
            }
            sum = sum + p[i];
        }
        
        // normalize sum to 1
        for (int i=0 ; i<p.length ; ++i){
            p[i] = p[i]/sum;
        }
        return p;
    }
    public double lowerBound(){
        double l1 = L1();
        double l2 = L2();
        double l3 = L3();
        double l4 = L4();
        double l5 = L5();
        double l6 = L6();
        double l7 = L7();
        System.out.printf("%f\t%f\t%f\t%f\t%f\t%f\t%f\n",l1,l2,l3,l4,l5,l6,l7);
        return l1+l2+l3+l4-l5-l6-l7 ;
    }
    public double L1(){
        double c = X.getD()*Math.log(2*Math.PI);
        double L=0;
        for (int k=0 ; k<K ; ++k){
            RealVector del = xBar[k].subtract(m[k]);
            double f = nu[k]*W[k].operate(del).dotProduct(del);
            double trace = S[k].multiply(W[k]).getTrace();
            L = L + 
            N[k]*(this.lnLambdaTilde[k] - X.getD()/beta[k] - nu[k]*trace - f - c);
        }
        return 0.5*L;
    }
    public double L2(){
        double L = 0.0;
        for (int n=0 ; n<X.getN() ; ++n){
            for (int k=0 ; k<K ; ++k){
                L = L + r.getEntry(n, k)*lnPi[k];
            }
        }
        return L;
    }
    public double L3(){
        double piSum = 0.0;
        for (int k=0 ; k<K ; ++k){
            piSum = piSum + lnPi[k];
        }
        return C0 + (alpha0-1.0)*piSum;
    }
    public double L4(){
        double sum = K*X.getD()*Math.log(beta0/(2*Math.PI));
        for (int k=0 ; k<K ; ++k){
            RealVector del = m[k].subtract(mu0);
            sum = sum + lnLambdaTilde[k] - X.getD()*beta0/beta[k] 
                    - beta0*nu[k]*W[k].operate(del).dotProduct(del);
        }
        
        double sum1 = 0.0;
        for (int k=0 ; k<K ; ++k){
            sum1 = sum1 + nu[k]* W0inverse.multiply(W[k]).getTrace();
        }  
        double sum2 = 0.0;
        for (int k=0 ; k<K ; ++k){
            sum2 = sum2 + lnLambdaTilde[k];
        }          
        
        return 0.5*sum + K*(lnB(W0det,nu0,X.getD())) + sum2*(nu0-X.getD()-1)/2 - 0.5*sum1;
    }
    public double L5(){
        double sum = 0.0;
        for (int n=0 ; n<X.getN() ; ++n){
            for (int k=0 ; k<K ; ++k){
                double lnr = Math.log(r.getEntry(n, k));
                if (Double.isFinite(lnr)){
                    sum = sum + r.getEntry(n, k)*lnr;
                }
            }
        }
        return sum;
    }
    public double L6(){
        double aHat = 0.0;
        for (int k=0 ; k<alpha.length ; ++k){
            aHat = aHat + alpha[k];
        }        
        double sum = 0.0;
        for (int k=0 ; k<K ; ++k){
            sum = sum + (alpha[k]-1.0)*(Gamma.digamma(alpha[k])-Gamma.digamma(aHat));
        }
        return sum + lnC(alpha);
    }
    public double L7(){
        double sum = 0.0;
        for (int k=0 ; k<K ; ++k){
            double s1 = 0.5*lnLambdaTilde[k];
            double s2 = 0.5*X.getD()*beta[k]/(2*Math.PI);
            double s3 = - X.getD()/2.0;
            double s4 = - H(detW[k],nu[k],lnLambdaTilde[k]);
            sum = sum + s1 + s2 + s3 + s4;
            if (!Double.isFinite(sum)){
                int iusahf=0;
            }
        }
        return sum ;
    }
    public double H(double detW,double nu,double lambdaTildeExp){
        double s1 = -(lnB(detW, nu, X.getD()));
        double s2 = -lambdaTildeExp*(nu- X.getD()-1)/2;
        double s3 = nu * X.getD() / 2;
        double sum = s1 + s2 + s3;
        if (!Double.isFinite(sum)){
            int iuasfduisd=0;
        }
        return sum;
    }
    static public double lnC(double[] a){
        double aHat = 0.0;
        for (int k=0 ; k<a.length ; ++k){
            aHat = aHat + a[k];
        }
        double C = Gamma.logGamma(aHat);
        for (int k=0 ; k<a.length ; ++k){
            C = C - Gamma.logGamma(a[k]);
        } 
        return C;
    }
    static public double lnB(double detW,double nu,int D){
        double p = 0.0;
        for (int i=0 ; i<=D ; ++i){
            p = p  + Gamma.logGamma((nu+1-i)/2.0);
        }
        double t1 = -0.5*nu*Math.log(detW);
        double t2 = 0.5*nu*D*Math.log(2.0);
        double t3 = 0.25*D*(D-1.0)*Math.log(Math.PI);
        double lnB = t1 - t2 - t3 - p;
        if (!Double.isFinite(lnB)){
            int iusahdf=0;
        }
        return lnB;
    }
    

    //prior parameters
    RealVector mu0;
    double beta0 = 1.0;
    RealMatrix W0;
    RealMatrix W0inverse;
    double W0det;
    double alpha0 = .0001;
    double[] a0;
    double C0;
    double nu0 =  3;
    
    SuperVoxelDataSource X;
    
    // run parameters
    int K; // the number of gaussians to model in the mixture
    int maxIterations =250;
    
    double[] alpha;
    double[] lnPi;
    double[] beta;
    double[] nu;
    RealVector[] m;   // means for each component
    RealMatrix[] W;  // precision matrix for each component 
    double[] detW;
    RealMatrix r; // responsibilities (N,K)
    double[] N;  // sum of resposibilities (over all data points) for each component
    RealVector[] xBar;  // resposibility weighted mean of data for each component;
    double[] lnLambdaTilde;
    RealMatrix[] S;
    
    double Dln2;
    double Dln2Pi2;
    double[] lnRho;
    double KlnB;
   
}
