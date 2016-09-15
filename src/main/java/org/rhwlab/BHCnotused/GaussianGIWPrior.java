/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayFieldVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.FieldLUDecomposition;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;
import org.rhwlab.dispim.datasource.ClusteredDataSource;
import org.rhwlab.variationalbayesian.MicroClusterDataSource;

/**
 *
 * @author gevirl
 */
// Gaussian data model with GaussianInverseWishart prior
public class GaussianGIWPrior implements DataModel{
    // model with one data item
    // the vector is labeled with a microcluster identifier so that all the voxels can be retrieved for the microcluster
    GaussianGIWPrior(LabeledFieldVector v){
        data = new HashSet<>();
        data.add(v);
        init();
    }
    // model by combining two other models
    public GaussianGIWPrior(GaussianGIWPrior m1,GaussianGIWPrior m2){
        data = new HashSet<>();
        data.addAll(m1.data);
        data.addAll(m2.data);
        init();
    }
    public void init(){
        int n = data.size();
        int d = m.getDimension();
        Dfp rP = r.add(n);
//        System.out.printf("rP=%s\n",rP.toString());
        double nuP = nu + n;
//        System.out.printf("nuP=%e\n", nuP);
        FieldMatrix C = new Array2DRowFieldMatrix(field,d,d);
        for (int row=0 ; row<C.getRowDimension() ; ++row){
            for (int col=0 ; col<C.getColumnDimension() ; ++col){
                C.setEntry(row, col, field.getZero());
            }
        }
        FieldVector X = new ArrayFieldVector(field,d);  // a vector of zeros
        
        for (FieldVector v : data){
            X = X.add(v);
            FieldMatrix v2 = v.outerProduct(v);
            C = C.add(v2);
        }
        FieldVector mP = (m.mapMultiply(r).add(X)).mapDivide(r.add(n));
        FieldMatrix Sp = C.add(S);
        
        FieldMatrix rmmP = mP.outerProduct(mP).scalarMultiply(rP);
        Sp = Sp.add(rmm).subtract(rmmP);
        
        FieldLUDecomposition ed = new FieldLUDecomposition(Sp);
        Dfp det = (Dfp)ed.getDeterminant();
        
        Dfp detSp = det.pow(field.newDfp(nuP/2.0));
        
        Dfp gamma = field.getOne();
        
        Dfp gammaP = field.getOne();
        
        for (int i=1 ; i<=d ; ++i){
            gamma = gamma.multiply(Gamma.gamma((nu+1-i)/2.0));
            gammaP = gammaP.multiply(Gamma.gamma((nuP+1-i)/2.0));
        }

        Dfp t1 = field.getPi().pow( -n*d/2.0);
        Dfp t2 = r.divide(rP).pow(d/2.0);       
        Dfp t3 = detS.divide(detSp);
        
        Dfp t4 = gammaP.divide(gamma);
        Dfp t34 = t3.multiply(t4);
/*        
        System.out.printf("detSp=%s\n", detSp.toString());
        System.out.printf("det=%s\n", det.toString());
        System.out.printf("gamma=%s\n", gamma.toString());
        System.out.printf("gammaP=%s\n", gammaP.toString());        
        System.out.printf("t1=%s\n", t1.toString());  
        System.out.printf("t2=%s\n", t2.toString());
        System.out.printf("t3=%s\n", t3.toString());
        System.out.printf("t4=%s\n", t4.toString());
*/
        likelihood = t2.multiply(t34).multiply(t1);
        double realLike = likelihood.getReal();
 //       System.out.printf("Likelihood=%e\n", realLike);
         int uhfd=0;    
    }

    @Override
    public Dfp likelihood() {
        return this.likelihood;
    }

    @Override
    public DataModel mergeWith(DataModel other) {
        return new GaussianGIWPrior(this,(GaussianGIWPrior)other);
    }

    @Override
    public int getN() {
        return data.size();
    }
    static void setParameters(double n,double beta,Dfp[] mu,double s){
        GaussianGIWPrior.nu = n;
        GaussianGIWPrior.S = new Array2DRowFieldMatrix(field,mu.length,mu.length);
        for (int i=0 ; i<mu.length ; ++i){
            S.setEntry(i, i, field.newDfp(s));
        }
        GaussianGIWPrior.r = field.newDfp(beta);
        GaussianGIWPrior.m = new ArrayFieldVector(field,mu);
        
//        EigenDecomposition ed = new EigenDecomposition(S);
        FieldLUDecomposition ed = new FieldLUDecomposition(S);
        detS = ((Dfp)ed.getDeterminant());
        System.out.printf("detS=%s\n", detS.toString());
        detS = detS.pow(nu/2.0);
        System.out.printf("detSnu=%s\n",detS.toString() );
        rmm = m.outerProduct(m).scalarMultiply(field.newDfp(r));
    }
    @Override
    public void print(PrintStream stream) {
        stream.printf("Size=%d\n",data.size());
        stream.printf("%s\n",asString());
        stream.printf("Likelihood: %s\n", this.likelihood.toString());
    } 

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (FieldVector v : data){
            if (!first){
                builder.append(String.format(","));
            }
            vectorAsString(v,builder);
            first = false;
        }     
        return builder.toString();
    }    
    private void vectorAsString(FieldVector v,StringBuilder builder){
        boolean first = true;
        builder.append("(");
        for (int i=0 ; i<v.getDimension() ; ++i){
            if (!first){
                builder.append(String.format(",%d",((Dfp)v.getEntry(i)).intValue()));
            } else {
                builder.append(String.format("%d",((Dfp)v.getEntry(i)).intValue()));
            }
            first = false;
        }
        builder.append(")");
    }

    // the mean of all the voxel coordinates in all the microclusters in this gaussian
    @Override
    public RealVector getMean() {
        RealVector ret  = new ArrayRealVector(m.getDimension());
        int n = 0;
        for (LabeledFieldVector v : data){
            int label = v.getLabel();
            RealVector[] vs =source.getClusterVectors(label);
            for (int i=0 ; i<vs.length ; ++i){
                ret = ret.add(vs[i]);
                ++n;
            }
        ret = ret.mapDivide(n);
        }
        return ret;
    }
    // the precision of all the voxel coordinates in all the microclusters in this gaussian
    @Override
    public RealMatrix getPrecision() {
        RealMatrix ret = new Array2DRowRealMatrix(m.getDimension(),m.getDimension());
        int n = 0;
        for (LabeledFieldVector v : data){
            int label = v.getLabel();
            RealVector[] vs =source.getClusterVectors(label);
            for (int i=0 ; i<vs.length ; ++i){ 
                RealVector del = vs[i].subtract(getMean());
                ret = ret.add(del.outerProduct(del));
                ++n;
            }
        }
        ret = ret.scalarMultiply(1.0/n);
        LUDecomposition lud = new LUDecomposition(ret);
        RealMatrix prec = lud.getSolver().getInverse();
        return prec;
    }    
    static public void setDfpField(DfpField fld){
        field = fld;
    }

    
    static public void main(String[] args) throws Exception {
        System.out.println("GaussianGIWPrior");
        field= new DfpField(20);  // 20 decimal digits
        Cluster.setDfpField(field);
        ThreadedAlgorithm.setDfpField(field);
        GaussianGIWPrior.setDfpField(field);
        
//        SegmentedTiffDataSource source = new SegmentedTiffDataSource("/nfs/waterston/pete/Segmentation/Cherryimg75.tif",
//                "/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.tiff",1);  // background is seg=1
 //       source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentationMulti0350.xml");
        source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentation0075.save");
        RealVector mean = source.getDataMean();
        Dfp[] mu = new Dfp[mean.getDimension()];
        for (int i=0 ;  i<mu.length ; ++i){
            mu[i] = field.newDfp(mean.getEntry(i));
        }
        GaussianGIWPrior.setParameters(4.0, 0.001,mu, 200.0);
        Cluster.setAlpha(30);
        ThreadedAlgorithm alg = new ThreadedAlgorithm();
        alg.setSource(source);
        alg.init(2);
        alg.run();
        alg.saveResultAsXML("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentation0075_BHC.xml");
        int hfuis=0;
    }
    
    Set<LabeledFieldVector> data;  // the set of microclusters in this gaussian model
//    FieldVector mu;
    Dfp likelihood;
//    FieldMatrix C;
//    FieldVector X;
//    RealVector mean;
    
    static double nu;
    static Dfp r;
    static FieldVector m;
    static FieldMatrix S;  
    static Dfp detS;
    static FieldMatrix rmm;
//    static double lnPi = Math.log(Math.PI);
    static DfpField field;
    static MicroClusterDataSource source;


}
