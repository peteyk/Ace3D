/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.List;
import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayFieldVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.FieldVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class MicroCluster {
    public MicroCluster(double[] v,short[][] points){
        this.v = v;
        this.points = points;
    }
    // construct from an xml element
    public MicroCluster (Element ele){
        int D = 3;
        String centerStr = ele.getAttributeValue("center").trim();
        String[] centerTokens = centerStr.split(" ");
        v = new double[D];
        for (int d=0 ; d<D ; ++d){
            v[d] = Double.valueOf(centerTokens[d]);
        }
        
        int nPts = Integer.valueOf(ele.getAttributeValue("points"));
        points = new short[nPts][];
        String cont = ele.getTextNormalize();
        String[] tokens = cont.substring(1, cont.length()-1).split("\\)\\(");
        for (int i=0 ; i<nPts ; ++i){
            short[] p = new short[D];
            String[] valStrs = tokens[i].split(",");
            for (int d=0 ; d<D ; ++d){
                p[d] = Short.valueOf(valStrs[d]);
            }
            points[i] = p;
        }
        
    }
    public RealVector asRealVector(){
        return new ArrayRealVector(v);
    }
    public FieldVector asDfpVector(){
        Dfp[] dfp = new Dfp[v.length];
        for (int i=0 ; i<dfp.length ; ++i){
            dfp[i] = field.newDfp(v[i]);
        }
        return new ArrayFieldVector(dfp);
    }
    // calculate the mean of all the data points in a list of microclusters
    public static RealVector mean(List<MicroCluster> data) {
        if (data.isEmpty()){
            return null;
        }
        long n = 0;
        long[] mu = new long[data.get(0).v.length];
        for (MicroCluster micro : data){
            for (int p=0 ; p<micro.points.length ; ++p){
                for (int d=0 ; d<mu.length ; ++d){
                    mu[d] = mu[d] + micro.points[p][d];
                }
                ++n;
            }
        }
        RealVector ret = new ArrayRealVector(data.get(0).v.length);
        for (int d=0 ; d<mu.length ; ++d){
            ret.setEntry(d,(double)mu[d]/(double)n);
        }        
        return ret;
    }
    public static RealMatrix precision(List<MicroCluster> data,RealVector mu){
        RealMatrix ret = new Array2DRowRealMatrix(mu.getDimension(),mu.getDimension());
        RealVector v = new ArrayRealVector(mu.getDimension());
        long n = 0;
        for (MicroCluster micro : data){
            for (int p =0 ; p<micro.points.length ; ++p){
                for (int d=0 ; d<mu.getDimension() ; ++d){
                    v.setEntry(d, micro.points[p][d]);
                }
                RealVector del = v.subtract(mu);
                ret = ret.add(del.outerProduct(del));
                ++n;
            }
            
        }
        ret = ret.scalarMultiply(1.0/n);
        LUDecomposition lud = new LUDecomposition(ret);
        RealMatrix prec = null;
        if (lud.getSolver().isNonSingular()){
            prec = lud.getSolver().getInverse();
        }
        return prec;
    }
    // add content to an xml node
    public int addContent(Element node){
        StringBuilder builder = new StringBuilder();
        for (int j=0 ; j<v.length ; ++j){
            if (j > 0){
                builder.append(" ");
            }
            builder.append(v[j]);

        }
        node.setAttribute("center", builder.toString());
        node.addContent(pointsAsString());
        return points.length;
    }
    static public void setField(DfpField fld){
        field = fld;
    }
    public String pointsAsString(){
        StringBuilder builder = new StringBuilder();
        for (int p=0 ; p<points.length ; ++p)    {
            builder.append("(");
            short[] pnt = points[p];
            for (int i=0 ; i<pnt.length ; ++i){
                builder.append(pnt[i]);
                if (i < pnt.length-1){
                    builder.append(",");
                }
            }
            builder.append(")");
        }
        return builder.toString();
    }
    static DfpField field ;
    double[] v;  // center
    short[][] points;
}
