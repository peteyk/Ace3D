/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import org.rhwlab.dispim.datasource.ClusteredDataSource;
import org.rhwlab.dispim.datasource.Voxel;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.datasource.VoxelDataSource;

/**
 *
 * @author gevirl
 */
public class GaussianMixture implements Runnable{
    
    public double[] dirichlet(double alpha,int K){
        GammaDistribution dist = new GammaDistribution(alpha,1.0);
        double[] ret = new double[K];
        double total = 0.0;
        for (int i=0 ; i<K ; ++i){
            ret[i] = dist.sample();
            total = total + ret[i];
        }
        for (int i=0 ; i<K ; ++i){
            ret[i] = ret[i]/total;
        }
        return ret;
    }
    public void init(int K){
        this.K = K;
        this.N = new double[K];
        this.m = new RealVector[K];
        inModel = new boolean[K];
        for (int k=0 ; k<K ; ++k){
            inModel[k] = true;
        }
        Dln2 = X.getD()*Math.log(2.0);
        Dln2Pi2 = X.getD()*Math.log(2.0*Math.PI)/2.0;
        
        r = new Array2DRowRealMatrix(X.getN(),K);
        if (X instanceof ClusteredDataSource){
            ClusteredDataSource clSource = (ClusteredDataSource)X;
            for (int n=0 ; n<X.getN() ; ++n){
                int cluster = clSource.getCluster(n);
                for (int k=0 ; k<K ; ++k){
                    if (k == cluster){
                        r.setEntry(n, k, 1.0);
                    } else {
                        r.setEntry(n, k, 0.0 );
                    }
                }
            }            
        }else {
            for (int n=0 ; n<X.getN() ; ++n){
                double[] dir = dirichlet(0.001,K);
                for (int k=0 ; k<K ; ++k){
                    r.setEntry(n, k, dir[k]);
                }
            }
        }
        calculateN();
        
        if (mu0 == null){
            mu0 = new ArrayRealVector(X.getD(),0.0);
        }
        
        
        if (W0 == null){
            W0 = MatrixUtils.createRealIdentityMatrix(X.getD());
            W0 = W0.scalarMultiply(.01);
        }
        CholeskyDecomposition de = new CholeskyDecomposition(W0);
        W0inverse = de.getSolver().getInverse();
        W0det = de.getDeterminant();
        W = new RealMatrix[K]; 
        Winverse = new RealMatrix[K]; 
        detW = new double[K];
        detWinv = new double[K];
/*        
        for (int k=0 ; k<K ; ++k){
            W[k] = W0.copy();
            W[k].scalarMultiply(0.0000000001);
            detW[k] = de.getDeterminant();
        }
 */       
        alpha = new double[K];
        beta = new double[K];
        lnLambdaTilde = new double[K];
        nu = new double[K];
        xBar = new RealVector[K];
        for (int k=0 ; k<K ; ++k){
            xBar[k] = new ArrayRealVector(X.getD());
//            nu[k] = nu0 + N[k];
        }
        lnPi = new double[K];        
        for (int k=0 ; k<K ; ++k){
//            lnPi[k] = Math.log(N[k]/X.getN());
        }
        
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
//        alphaBetaNu();
//        lambdaTilde();
    }
    
    @Override
    public void run() {
//        this.reportN();
  //      this.reportPi();
        double[] nPrev = new double[K];
        for (int k=0 ; k<K ; ++k){
            nPrev[k] = N[k];
        }
        
        for (int i=0 ; i<this.maxIterations ; ++i){
            System.out.println(i);
            this.statistics();
            this.reportN();
/*            
            this.reportVector(System.out,"xBar",xBar);
            this.reportMatrix(System.out,"S[0]",S[0]);
            LUDecomposition cd = new LUDecomposition(S[0]);
            RealMatrix Si  = cd.getSolver().getInverse();  
            System.out.printf("det S inv = %e\n", 1.0/cd.getDeterminant());            
            this.reportMatrix(System.out,"Sinverse",Si);
*/            
            this.lnPi();
            this.Mstep();
                       
//            this.reportMatrix(System.out,"W[0]", W[0]);
//            this.reportMatrix(System.out,"W[0]*nu", W[0].scalarMultiply(nu[0]));
            this.alphaBetaNu();
//            this.reportNu();
            this.responsibities();
/*
            this.reportN();
            this.reportM();
 //          this.reportW();
            this.reportAlpha();
            this.reportBeta();
            this.reportDetW();
            
            this.reportPi();
            this.reportLnPi();
            this.reportxBar();
//            this.reportS();
//            this.reportR();
            
 //           this.reportN();
 //           this.reportM();
 */           
 //           System.out.printf("%d lower bound = %f\n",i, lowerBound());
            double total = 0.0;
            for (int k=0 ; k<K ; ++k){
                total =total + Math.abs(nPrev[k]-N[k]);
            }
            System.out.printf("N change = %f\n",total);
            for (int k=0 ; k<K ; ++k){
                nPrev[k] = N[k];
            }
            if (total <100 && total > 0){
                break;
            }
        }
        report();
    }
    public void Estep(){
        responsibities();
        statistics();
    }
    
    // calculate the responsibility array
    public void responsibities(){

        // calculate the r matrix        
        for (int n=0 ; n<X.getN() ; ++n){
            RealVector x = X.get(n).coords;
            double intensity = X.get(n).adjustedIntensity;
                for (int k=0 ; k<K ; ++k){
                    if (inModel[k]){
                        RealVector diff = x.subtract(m[k]);
                        double E = X.getD()/beta[k] + nu[k]*W[k].operate(diff).dotProduct(diff);
                        lnRho[k] =  lnPi[k] +intensity*(0.5*lnLambdaTilde[k] - Dln2Pi2 - 0.5*E);
                    }
                }
                double[] rs = normalizeLogP(lnRho);
                for (int k=0 ; k<K ; ++k){
                    r.setEntry(n, k, rs[k]);
                }

            int sahdf=0;
        }
    }
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
                p[i] = 0.0;  // underflow occured
            }
            sum = sum + p[i];
        }
        
        // normalize sum to 1
        for (int i=0 ; i<p.length ; ++i){
            p[i] = p[i]/sum;
        }
        return p;
    }
    // given a vector of log values, compute the log of the sum of the values
    double lnSumP(double[] lnP){
        
        // find the maximum
        double maxLnP = lnP[0];
        for (int k=1 ; k<K ; ++k){
            if (lnP[k] > maxLnP){
                maxLnP = lnP[k];
            }
        }
        
        // subtract the maximum and exponentiate
        // sum is guaranted to be >= 1.0;
        double sum = 0.0;
        for (int k=0 ; k<K ; ++k){
            sum = sum + Math.exp(lnP[k] - maxLnP);
        }
        if (!Double.isFinite(sum)){
            return maxLnP;
        }
        return maxLnP + Math.log(sum);
    }
    public void lnPi(){
        double l = Math.log(X.getN());
        for (int k=0 ; k<K ; ++k){
            double pi = 0.0;
            for (int n=0 ; n<X.getN() ; ++n){
                double adj = X.get(n).adjustedIntensity;
                pi = pi + adj*r.getEntry(n,k);
            }
            double lnp = Math.log(pi);
            if (Double.isFinite(lnp)){
                lnPi[k] = Math.log(pi)-l;
            } else {    
                lnPi[k] = -10000;
//                inModel[k] = false;
            }
            
        }
/*        
        double aHat = 0.0;
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                aHat = aHat + alpha[k];
            }
        }
        double psi = Gamma.digamma(aHat);
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                lnPi[k] = Gamma.digamma(alpha[k])-psi;
                 if (!Double.isFinite(lnPi[k])){
                    inModel[k] = false;
                } 
            }
        } 
*/
    }
    public void calculateN(){
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                N[k] = 0.0;
                for (int n=0 ; n<X.getN() ; ++n){
                    Voxel vox = X.get(n);
                    double intensity = vox.getAdjusted();
//                        N[k] = N[k] + (intensity-background)*r.getEntry(n, k);
                            N[k] = N[k] + intensity*r.getEntry(n, k);
                    }
                N[k] = N[k] + .000000001;
            }
        }       
    }
    // compute statisitics from resposibilities and the data
     public void statistics(){
        // calculate N
        calculateN();

        
        // calculate xBar
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                xBar[k].set(0.0);
                for (int n=0 ; n<X.getN() ; ++n){
                    Voxel vox = X.get(n);
                    double intensity = vox.getAdjusted(); 
                    RealVector x = vox.coords;
//                       double rnk = (intensity-background)*r.getEntry(n, k);
                        double rnk = intensity*r.getEntry(n, k);
                        xBar[k] = xBar[k].add(x.mapMultiply(rnk));
                }
                xBar[k].mapDivideToSelf(N[k]);
            }
        } 
        
        // calculate the S
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                if (k == 31 ){
                    int iusahdfis=90;
                }
                for (int row=0 ; row<X.getD() ; ++row){
                    for (int col=0 ; col<X.getD() ; ++col){
                        S[k].setEntry(row, col, 0.0);
                    }
                }
                for (int n=0 ; n<X.getN() ; ++n){
                    Voxel vox = X.get(n);
                        RealVector x  = vox.coords;
                        RealVector del = x.subtract(xBar[k]); 
                        RealMatrix mat = del.outerProduct(del);
                        double rnk = r.getEntry(n, k);
                        if (rnk > 0){
                            int iuhsafuid=0;
                        }
//                        S[k] = S[k].add(mat.scalarMultiply((intensity-background)*rnk)); 
                        S[k] = S[k].add(mat.scalarMultiply(vox.getAdjusted()*rnk));
                }
                S[k] = S[k].scalarMultiply(1.0/N[k]);
            }
        }
        int uisahdfius=0;
    }
    
    public void Mstep(){
        alphaBetaNu();
        // compute the means of the components
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){
                RealVector v1 = xBar[k].mapMultiply(N[k]);
                RealVector v2 = mu0.mapMultiply(beta0);
                RealVector v3 = v2.add(v1);
                m[k] = v3.mapDivide(beta[k]);
            }
        }
        
        // compute the precision matrices
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){            
                RealVector del = xBar[k].subtract(mu0);
                RealMatrix del2 = del.outerProduct(del);
                double f = beta0*N[k]/(beta0+N[k]);
                RealMatrix mat = del2.scalarMultiply(f);
                RealMatrix NS = S[k].scalarMultiply(N[k]);
                Winverse[k] = W0inverse.add(NS).add(mat);
                LUDecomposition cd = new LUDecomposition(Winverse[k]);
                W[k] = cd.getSolver().getInverse();
                detWinv[k] = cd.getDeterminant();
                detW[k] = 1.0/cd.getDeterminant();
            }
        }
        lambdaTilde();
    }
    public void alphaBetaNu(){
        // calculate the alpha , beta and nu vectors
        double totalAlpha = 0.0;
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){ 
                alpha[k] = alpha0 + N[k];
                totalAlpha = totalAlpha + alpha[k];
                beta[k] = beta0 + N[k];
                nu[k] = nu0 +N[k] + 1.0;
            }
        }
      
       
    }
    public void lambdaTilde(){
        // calculate lambda tilde vector
        for (int k=0 ; k<K ; ++k){
            if (inModel[k]){ 
                double sum = 0.0;
                for (int d=1 ; d<=X.getD() ; ++d){
                    double v = (nu[k]+1.0-d)/2.0;
                    sum = sum + Gamma.digamma(v);
                }
                lnLambdaTilde[k] = sum + Dln2 + Math.log(detW[k]);
            }
        }         
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
        double lnb = lnB(W0det,nu0,X.getD());
        double ret = 0.5*sum + K*(lnb) + sum2*(nu0-X.getD()-1)/2 - 0.5*sum1;
        return ret;
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
 //           sum = sum + (alpha[k]-1.0)*(Gamma.digamma(alpha[k])-Gamma.digamma(aHat));
            sum = sum + (alpha[k]-1.0)*lnPi[k];
        }
        double lnc = lnC(alpha);
        return sum + lnc;
    }
    public double L7(){
        double sum = 0.0;
        for (int k=0 ; k<K ; ++k){
            double s1 = 0.5*lnLambdaTilde[k];
            double s2 = 0.5*X.getD()*Math.log(beta[k]/(2*Math.PI));
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
    public void saveAsXML(String file)throws Exception {
        FileOutputStream stream = new FileOutputStream(file,false);
        Element root = new Element("document");
        for (int k=0 ; k<K ; ++k){
            if (N[k] > 1){
                Element gmm = new Element("GaussianMixtureModel");
                gmm.setAttribute("id", String.format("%d", k));
                gmm.setAttribute("parent", "-1");
                gmm.setAttribute("N", String.format("%f", N[k]));
                gmm.setAttribute("detW", String.format("%e",detW[k]));
                gmm.setAttribute("lnLamdbaTilde", String.format("%f",this.lnLambdaTilde[k]));
                gmm.setAttribute("nu",String.format("%f",nu[k]));    
                gmm.setAttribute("lnPi",String.format("%f",lnPi[k])); 
                StringBuilder builder = new StringBuilder();
                for (int d=0 ; d<X.getD() ; ++d){
                    if (d > 0){
                        builder.append(" ");
                    }
                    builder.append(m[k].getEntry(d));

                }
                gmm.setAttribute("m", builder.toString());

 //               double c = Math.pow(Math.exp(this.lnLambdaTilde[k])/this.detW[k],1.0/X.getD());
                builder = new StringBuilder();
                for (int row=0 ; row<X.getD() ; ++row){
                    for (int col=0 ; col<X.getD() ; ++col){
                        if (row>0 || col>0){
                            builder.append(" ");
                        }
                        builder.append(W[k].getEntry(row, col));
                    }
                }
                gmm.setAttribute("W", builder.toString());
                
                builder = new StringBuilder();
                for (int row=0 ; row<X.getD() ; ++row){
                    for (int col=0 ; col<X.getD() ; ++col){
                        if (row>0 || col>0){
                            builder.append(" ");
                        }
                        builder.append(W[k].getEntry(row, col));
                    }
                }
                gmm.setAttribute("W", builder.toString());
                
                LUDecomposition cd = new LUDecomposition(S[k]);
                RealMatrix Sinv = cd.getSolver().getInverse();                
                builder = new StringBuilder();
                for (int row=0 ; row<X.getD() ; ++row){
                    for (int col=0 ; col<X.getD() ; ++col){
                        if (row>0 || col>0){
                            builder.append(" ");
                        }
                        builder.append(Sinv.getEntry(row, col));
                    }
                }
                gmm.setAttribute("Sinv", builder.toString());                
                root.addContent(gmm);

            }
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);        
        
    }
   public void report(){
        
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
        reportEVar(System.out,"|W|",detW);
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
    public void reportEVar(PrintStream str,String varName,double[] var){
        str.printf("%s",varName);
        for (int i=0 ; i<var.length ; ++i){
            str.printf(",%e",var[i]);
        }
        str.println();
    }
    public void reportVar(PrintStream str,String varName,double[] var){
        double sum = 0.0;
        str.printf("%s",varName);
        for (int i=0 ; i<var.length ; ++i){
            str.printf(",%f",var[i]);
            sum = sum + var[i];
        }
        str.printf( "\t%f",sum);
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
                    str.printf(" %e",mat.getEntry(r, c));
                }
                str.println();
            }
    }       
    public void setSource(VoxelDataSource source){
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
    public void setW0(RealMatrix w0){
        this.W0 = w0;
    } 
    public void setIterationMax(int mx){
        this.maxIterations = mx;
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
    double nu0 =  20;
    
    VoxelDataSource X;
    
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
   
    boolean[] inModel;
    RealMatrix Winverse[];
    double[] detWinv;
}
