/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;
import static org.rhwlab.BHC.NodeBase.beta;
import static org.rhwlab.BHC.NodeBase.maxN;
import org.rhwlab.dispim.datasource.MicroCluster;



/**
 *
 * @author gevirl
 */
public class LogNode extends StdNode {
    public LogNode(MicroCluster micro){
        super(micro);
        lnd = lnAlpha;

    }
    public LogNode(LogNode l,LogNode r){
        super(l,r);
    }
    @Override
    public void  posterior() {
        if (lnPi == null){
            lnPi = 0.0;
        }
        logPosterior();
        
    }    
    @Override
    public Node mergeWith(Node other) {
        return new LogNode(this,(LogNode)other);
    }    
    
    // the likelihood of the data in this node/cluster only - given the priors
    public double logMarginalLikelihood(List<RealVector> data) throws ArithmeticException {

        int n = data.size();
        if (n >maxN){
            return 0.0;
        }
        int D = data.get(0).getDimension();
        double rP = beta + n;
        Double lnrP = Utils.eln(rP);
        double nuP = nu + n;       
        
        RealMatrix C = new Array2DRowRealMatrix(D,D);
        RealVector X = new ArrayRealVector(D);  // a vector of zeros    
        for (RealVector v : data){
            X = X.add(v);
            RealMatrix v2 = v.outerProduct(v);
            C = C.add(v2);
        }  
        
        RealVector mP = (m.mapMultiply(beta).add(X)).mapDivide(rP);
        RealMatrix Sp = C.add(S);
        
        RealMatrix rmmP = mP.outerProduct(mP).scalarMultiply(rP);
        Sp = Sp.add(rmm).subtract(rmmP);  
        
        LUDecomposition ed = new LUDecomposition(Sp);
        if (!ed.getSolver().isNonSingular()){
            System.exit(10);
        }
        double logDetSp = Utils.eln(ed.getDeterminant());
        
        Double ret = Utils.elnPow(logPi, -n*D/2.0);
        ret = Utils.elnMult(ret, Utils.elnPow(lnBeta, D/2.0));
        ret = Utils.elnMult(ret,-Utils.elnPow(lnrP, D/2.0));
        ret = Utils.elnMult(ret, logdetSnu);
        ret = Utils.elnMult(ret, -Utils.elnPow(logDetSp, nuP/2.0));
        for (int i=1 ; i<=D ; ++i){
            ret = Utils.elnMult(ret, Gamma.logGamma((nuP+1-i)/2.0));
            ret = Utils.elnMult(ret,-Gamma.logGamma((nu+1-i)/2.0));
        }
        return ret;
    }
    public double logDPMLikelihood(int n)throws ArithmeticException {       
        LogNode stdLeft = (LogNode)left;
        LogNode stdRight = (LogNode)right;
        if (left != null && right != null){
            Double lngn = Gamma.logGamma(n);
            
            if (!Double.isFinite(lngn))            {
                System.exit(111);
            }
            Double lnagn = Utils.elnMult(lnAlpha, lngn);
            if (!Double.isFinite(lnagn))            {
                System.exit(112);
            }            
            lnd = Utils.elnsum(lnagn, Utils.elnMult(stdLeft.lnd,stdRight.lnd));
            if (lnd == 0.0){
                int aisohdfni=0;
            }
            if (!Double.isFinite(lnd))            {
                System.exit(222);
            }      
            
            lnPi = Utils.elnMult(lnagn,-lnd);
            if (lnPi == 0.0){
                int isafduis=0;
            }
            if (!Double.isFinite(lnPi))            {
                System.exit(223);
            }            
            lnonePi = Utils.eln(1.0 - Math.exp(lnPi));
            
            Double lnFirst = Utils.elnMult(lnPi,lnLike);
            Double lnSecond = Utils.elnMult(lnonePi,Utils.elnMult(stdLeft.lnDPM, stdRight.lnDPM));

            return Utils.elnsum(lnFirst, lnSecond);
        }
        return lnLike;
    }
    // calculate the posterior of a non terminal node
    public void  logPosterior()throws ArithmeticException {
        List<RealVector> data = new ArrayList<>();
        this.getDataAsRealVector(data); 
        this.lnLike = this.logMarginalLikelihood(data);
        
        lnDPM = this.logDPMLikelihood(data.size());
       
        lnR = Utils.elnMult(lnPi, lnLike);
        lnR = Utils.elnMult(lnR, -lnDPM);
        realR = Math.exp(lnR);
        if (realR == 1.0){
            int sahdfuis=0;
        }
        dfpR = field.newDfp(realR);
        
    }    
    Double lnd;
    Double lnPi = 0.0;
    Double lnonePi;
    Double lnLike;
    Double lnDPM;
    Double lnR;
    
}
