/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.math.BigDecimal;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import static org.rhwlab.dispim.nucleus.Nucleus.precisionFromString;

/**
 *
 * @author gevirl
 */
public class BHC_Nucleus extends Nucleus {
    // construct the nucleus from a GaussianMixtureModel xml element
        public BHC_Nucleus(JsonObject jsonObj){
            super(jsonObj);
            this.sourceNode = jsonObj.getString("SourceNode");
            this.id = super.getName().substring(super.getName().indexOf('_')+1);
        }
    public BHC_Nucleus(int time,Element gmm){
        super(time,name(time,gmm),center(gmm),10.0);  // for now make all radii the same
        id = gmm.getAttributeValue("id");

        sourceNode = gmm.getAttributeValue("sourceNode");
        A = precisionFromString(gmm.getAttributeValue("W"));
        eigenA = new EigenDecomposition(A);
        adjustedA = A.copy();
        adjustedEigenA = new EigenDecomposition(adjustedA);
        R = new double[3];
        R[0] = R[1] = R[2] = 2.5;
        this.setAdjustment(R);
    }
    public Element asXML(){
        Element ret = super.asXML();
        ret.setAttribute("sourceNode", this.sourceNode);
        return ret;
    }

    public JsonObjectBuilder asJson(){
        JsonObjectBuilder ret = super.asJson();
        ret.add("SourceNode", this.sourceNode);
        return ret;
    }
    public void printMat(String label,RealMatrix m){
        System.out.println(label);
        for (int r=0 ; r<m.getRowDimension() ; ++r){
            for (int c=0 ; c<m.getColumnDimension() ; ++c){
                System.out.printf("%f\t",m.getEntry(r, c));
            }
            System.out.println();
        }
    }

    static long[] center(Element gmm){
        long[] ret = new long[3];
        String[] tokens = gmm.getAttributeValue("m").split(" ");
        for (int i =0 ; i<ret.length ; ++i) {
            ret[i] = (long)(Double.parseDouble(tokens[i])+.5);
        }
        return ret;
    }


    static String name(int time,String id){
        int n = Integer.valueOf(id);
        return String.format("%03d_%03d", time,n);
    }
    static String name(int time,Element gmm){
        return name(time,gmm.getAttributeValue("id"));
    }
    public String getID(){
        return id;
    }

    public String getRadiusLabel(int i){
        // find the adjusted eigenvector closest to the original unadjusted eigenvector for dimension i
        // this keeps the order of the adjusted eigenvectors the same as the original unadjusted eigenvectors
        // the eigendecomposition returns the eigenvectors sorted by eigenvalue
        // this procedure puts them back in their original order
        int adjustedI = 0;
        double maxD = 0.0;
        RealVector aV = eigenA.getEigenvector(i);
        for (int j=0 ; j<A.getColumnDimension() ; ++j){
            RealVector v = adjustedEigenA.getEigenvector(j);
            double d = Math.abs(v.dotProduct(aV));
            if (d > maxD){
                maxD = d;
                adjustedI = j;
            }
        }
        RealVector v = adjustedEigenA.getEigenvector(adjustedI);
//        double eigenVal = adjustedEigenA.getRealEigenvalue(adjustedI);
//        double r = 1.0/Math.sqrt(Ace3D_Frame.R*eigenVal);
        double r = this.getRadius(adjustedI);
        return String.format("%4.1f(%.2f,%.2f,%.2f)",r, v.getEntry(0),v.getEntry(1),v.getEntry(2));
    }
    public String getSourceNode(){
        return this.sourceNode;
    }
    String id;
    String sourceNode;
}

