/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.awt.Shape;
import java.io.PrintStream;
import java.util.Arrays;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class Nucleus implements Comparable {
    public Nucleus(NucleusData data){
        this.child1 = null;
        this.child2 = null;
        this.parent = null;
        this.nucData = data;
        this.cellName = data.getName();
    }

    public Nucleus(JsonObject jsonObj){
        this(new BHCNucleusData(jsonObj));

        JsonObject child1Obj = jsonObj.getJsonObject("child1");
        if (child1Obj != null){
            this.child1 = new Nucleus(child1Obj);
            this.child1.parent = this;
        }
        JsonObject child2Obj = jsonObj.getJsonObject("child2");
        if (child2Obj != null){
            this.child2 = new Nucleus(child2Obj);
            this.child2.parent = this;
        }        
    }
    public Nucleus clone(){
        Nucleus clone = new Nucleus(this.nucData);
        clone.child1 = this.child1;
        clone.child2 = this.child2;
        clone.cellName = this.cellName;
        clone.parent = this.parent;
        clone.userNamed = this.userNamed;
        return clone;
    }
    static public String randomName(){
        return NucleusData.randomName();
    }
    static public void saveHeadings(PrintStream stream){
        stream.println("Time,Name,X,Y,Z,Radius,Child1,Child2");
    }
    public void saveNucleus(PrintStream stream){
//        stream.printf("%d,%s,%d,%d,%d,%f,%s,%s\n",time,getName(),x,y,z,radius,getChild1(),getChild2());
    }
    public int getTime(){
        return nucData.getTime();
    }
   
    public double[] getCenter(){
        return nucData.getCenter();
    }
    public void setCenter(long[] c){
        nucData.setCenter(c);;
    }
    public void setCenter(double[] c){
        nucData.setCenter(c);
    }    
    public String getName(){
        return nucData.getName();
    }

    public void setMarked(boolean s){
        nucData.setMarked(s);
    }
    public boolean getMarked(){
        return nucData.getMarked();
    }

    public double distanceSqaured(long[] p){
        double d = 0.0;
        double[] c = this.getCenter();
        for (int i=0 ; i<p.length ; ++i){
            double delta = p[i]-c[i];
            d = d + delta*delta;
        }
        return d;
    }
    public Element asXML(){
        Element ret = nucData.asXML();
        ret.setAttribute("cell", cellName);
        ret.setAttribute("usernamed", Boolean.toString(userNamed));
        
        if (this.parent != null){
            ret.setAttribute("parent", this.parent.getName());
        }
        if (this.child1 != null){
            ret.setAttribute("child1", this.child1.getName());
        }
        if (this.child2 != null){
            ret.setAttribute("child2", this.child2.getName());
        }        
        return ret;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = nucData.asJson();
        builder.add("cell", cellName);
        builder.add("username",Boolean.toString(userNamed));
        
        if (this.parent != null){
            builder.add("parent",this.parent.getName());
        }
        if (child1 != null){
            builder.add("child1",this.child1.asJson() );
        }    
        if (child2 != null){
            builder.add("child2",this.child2.asJson() );
        }         
        return builder;
    }
    public void setCell(String c){
        this.cellName = c;
    }
    public String getCell(){
 
        return this.cellName;
    }
    @Override
    public int compareTo(Object o) {
        return nucData.compareTo(((Nucleus)o).getNucleusData());
    }  
    public boolean getLabeled(){
        return nucData.getLabeled();
    }
    public void setLabeled(boolean lab){
        nucData.setLabeled(lab);
    }
    public double getExpression(){
        return nucData.getExpression();
    }
    public void setExpression(double e){
        nucData.setExpression(e);
    }
    
    public boolean isVisible(long slice,int dim){
        return nucData.isVisible(slice, dim);
    }

    public Shape getShape(long slice,int dim,int bufW,int bufH){
        return nucData.getShape(slice, dim, bufW, bufH);
    }    
    public int imageXDirection(int dim){
        if (dim==0){
            return 1;
        }
        return 0;
    }    
    public int imageYDirection(int dim){
        if (dim==2){
            return 1;
        }
        return 2;
    } 
    public Object getAdjustment(){
        return nucData.getAdjustment();
    }
    public void setAdjustment(Object o){
        nucData.setAdjustment(o);
    }       
    public RealMatrix adjustPrecision(){
        return nucData.adjustPrecision();
    } 
    public String getRadiusLabel(int i){
        return ((BHCNucleusData)nucData).getRadiusLabel(i);
    }

    public static RealMatrix precisionFromString(String s){
        double [][] ret = new double[3][3];
        String[] tokens = s.split(" ");
        for (int i=0 ; i<3 ; ++i){
            for (int j=i ; j<3 ; ++j){
                ret[i][j] = Double.valueOf(tokens[3*i+j]);
                ret[j][i] = ret[i][j];
            }
        }
        return new Array2DRowRealMatrix(ret);
    }
    // probability the given position (relative to the center)  belongs to this nucleus
    public double prob(double[] p){
        return nucData.prob(p);
    }
    public double getRadius(int d){
        return nucData.getRadius(d);
    } 
    public long[] getRadii(){
        long[] radii = new long[3];
        
        radii[0] = (long)getRadius(0);
        radii[1] = (long)getRadius(1);
        radii[2] = (long)getRadius(2);
        return radii;
    }
    // return the direction vectors of the ellipsoid axes sorted by length of radii
    public RealVector[] getAxes(){
        return nucData.getAxes();
    }
    public String getFrobenius(){
        return nucData.getFrobenius();
    }
    public double[][] getEigenVectors(){
        return nucData.getEigenVectors();
    }
    public double[][] getEigenVectorsT(){
        return nucData.getEigenVectorsT();
    }   
    
    // determine if this nucleus is dividing
    public boolean isDividing(){
        return child1!=null && child2!=null;
    }
    
    // return the next nuclei this nucleus is linked to
    // return Nucleus[0] if not linked
    // return Nulcues[1] if linked in time
    // return Nucleus[2] if dividing
    public Nucleus[] nextNuclei(){
        if (isDividing()){
            Nucleus[] ret = new Nucleus[2];
            ret[0] = child1;
            ret[1] = child2;
            return ret;  // dividing
        }else if (child1==null && child2==null){
            Nucleus[] ret = new Nucleus[0];
            return ret;  // leaf nucleus
        } else {
            Nucleus[] ret = new Nucleus[1];
            ret[0] = child1;
            return ret;  // linked in time
        }

    }
    // measure the distance to another nucleus
    public double distance(Nucleus other){
        return nucData.distance(other.nucData);
    }
    public double shapeDistance(Nucleus other){
        return nucData.shapeDistance(other.nucData);
    }
    private void reportMatrix(PrintStream stream,String label,RealMatrix m){
        stream.printf("%s: ",label);
        for (int r=0 ; r<m.getRowDimension() ; ++r){
            for (int c=0 ; c<m.getColumnDimension() ; ++c){
                stream.printf("%f ",m.getEntry(r, c));
            }
            stream.print(" : ");
        }
        stream.println();
    }
    private RealMatrix reverseHandedness(RealMatrix m){
        RealMatrix ret = m.copy();
        for (int c=0 ; c<m.getColumnDimension();++c){
            
            ret.setEntry(0, c, -m.getEntry(0, c));
        }
        return ret;
    }
   
    public void report(PrintStream stream){
         
        if (cellName != null){
            stream.printf("Nucleus:%s time=%d  cell=%s\n",nucData.getName(),nucData.getTime(),cellName);
        } else {
            stream.printf("Nucleus:%s time=%d  no cell\n",nucData.getName(),nucData.getTime());
        }
        
    }
    // return the root cell leading to this nucleus
    public String getRoot(){
        if (this.parent==null){
            return this.cellName;
        }
        return parent.getRoot();
    }
    // return the sister nucleus if parent just divided
    public Nucleus getSisterNucleus(){
        return this.getSister();
    }
    public double[] eccentricity(){
        double[] r = new double[3];
        for (int i=0 ; i<3 ; ++i){
            r[i] = this.getRadius(i);
        }
        Arrays.sort(r);
        
        double[] e = new double[3];
        e[0] = ecc(r[0],r[1]);
        e[1] = ecc(r[0],r[2]);
        e[2] = ecc(r[1],r[2]);

        return e;
    }
    private double ecc(double axis1,double axis2){
        double f = axis1/axis2;
        return Math.sqrt((1.0- f*f));        
    }
    
    // the time since this nucleus last divided
    public int timeSinceDivsion(){
        if (this.parent == null){
            return -1;
        }
        if (this.parent.child2!= null){
            return 1;
        }
        int t = this.parent.timeSinceDivsion();
        if (t != -1){
            ++t;
        }
        return t;
    }
    public NucleusData getNucleusData(){
        return nucData;
    }
    public Nucleus getSister(){
        if (parent == null){
            return null;  // must have a parent to have a sister
        }
        if (parent.child2 == null){
            return null;  // must have a dividing parent
        }
        if (parent.child1.equals(this)){
            return parent.child2;
        }
        return parent.child1;
    }
    public Nucleus getParent(){
        return this.parent;
    }
    // link this nucleus to a daughter
    // if this nucleus is dividing, then this cannot be done and false is returned
    // if this nucleus is already linked in time then it will result in a division
    public boolean linkTo(Nucleus daughter){
        if (child2 != null){
            return false;
        }
        if (child1 != null){
            child2 = daughter;
        } else {
            this.child1 = daughter;
        }
        daughter.parent = this;
        return true;
    }
    @Override
    public String toString(){
        return nucData.toString();
    }

    public void setDaughters(Nucleus c1,Nucleus c2){
        this.child1 = c1;
        this.child2 = c2;
    }
    public Nucleus getChild1(){
        return this.child1;
    }
    public Nucleus getChild2(){
        return this.child2;
    }    
    private Nucleus child1;
    private Nucleus child2;
    private Nucleus parent; 
    private String cellName;  // the cell to which this nucleus belongs - initially cellname = nucname until nuc is linked to a parent
    private boolean userNamed = false;  // made true if the user names the cell for this nucleus
    final private NucleusData nucData;

}
