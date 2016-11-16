/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class BHCNucleusData extends NucleusData {
    // construct the nucleus from a GaussianMixtureModel xml element
    public BHCNucleusData(JsonObject jsonObj){
            super(jsonObj);
            this.sourceNode = jsonObj.getString("SourceNode");
            this.id = super.getName().substring(super.getName().indexOf('_')+1);
            this.count = jsonObj.getInt("Count");
            this.voxels = jsonObj.getInt("Voxels");
            this.intensity = jsonObj.getJsonNumber("Intensity").longValue();
            init();
        }
    public BHCNucleusData(int time,Element gmm){
        super(time,name(time,gmm),center(gmm),10.0);  // for now make all radii the same
        id = gmm.getAttributeValue("id");
        count = Integer.valueOf(gmm.getAttributeValue("count"));
        sourceNode = gmm.getAttributeValue("sourceNode");
        intensity = Long.valueOf(gmm.getAttributeValue("intensity"));
        voxels = Integer.valueOf(gmm.getAttributeValue("voxels"));
        A = precisionFromString(gmm.getAttributeValue("W"));
        eigenA = new EigenDecomposition(A);
        adjustedA = A.copy();
        adjustedEigenA = new EigenDecomposition(adjustedA);
        R = new double[3];
        R[0] = R[1] = R[2] = 2.5;
        this.setAdjustment(R);
        init();

    }
    private void init(){
        volume = vf;
        for (int i=0 ; i<3 ; ++i){
            volume = volume*this.getRadius(i);
        }
        density = count/volume;        
    }
    public Element asXML(){
        Element ret = super.asXML();
        ret.setAttribute("sourceNode", this.sourceNode);
        ret.setAttribute("count",Integer.toString(this.count));
        return ret;
    }

    public JsonObjectBuilder asJson(){
        JsonObjectBuilder ret = super.asJson();
        ret.add("SourceNode", this.sourceNode);
        ret.add("Count",this.count);
        ret.add("Voxels",this.voxels);
        ret.add("Intensity",this.intensity);
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

    @Override
    public String toString(){
        return name(this.getTime(),this.getID());
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
    public double getVolume(){
        return volume;
    }
    public double getDensity(){
        return density;
    }
    public double getAverageIntensity(){
        return ((double)intensity)/voxels;
    }
    public double distance(BHCNucleusData other){
        double v = this.volume/other.volume;
        if (v <1.0){
            v = 1.0/v;
        }
        double ir = this.intensity/other.intensity;
        if (ir<1.0){
            ir = 1.0/ir;
        }
        return v*ir*super.distance(other);
    }
    static double vf = 4.0*Math.PI/3.0;
    String id;
    int count;
    String sourceNode;
    double volume;
    double density;
    long intensity;
    int voxels;
}

