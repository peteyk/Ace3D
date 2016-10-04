/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class Cell  implements Comparable {
    public Cell(String name){
        this.name = name;
        if (divisionMap == null){
            divisionMap = new TreeMap<>();
            InputStream s = this.getClass().getClassLoader().getResourceAsStream("NewRules.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(s));
            try {
                String line = reader.readLine();
                line = reader.readLine();
                while (line != null){
                    String[] tokens = line.split("\t");
                    double[] v = new double[3];
                    v[0] = Double.valueOf(tokens[4]);
                    v[1] = Double.valueOf(tokens[5]);
                    v[2] = Double.valueOf(tokens[6]);
                    Division div = new Division(tokens[2],tokens[3],v);
                    divisionMap.put(tokens[0],div);
                    line = reader.readLine();
                }
            } catch (Exception exc){
                exc.printStackTrace();
            } 
        }
    }

    public Cell(JsonObject jsonObj,Cell parent,Map<String,Nucleus> nucMap){
        this(jsonObj.getJsonString("Name").getString()); 
        this.parent = parent;
        
        JsonObject child = jsonObj.getJsonObject("Child0");
        if (child != null){
            children.add(new Cell(child,this,nucMap));
        }
        child = jsonObj.getJsonObject("Child1");
        if (child != null){
            children.add(new Cell(child,this,nucMap));
        }
        JsonArray jsonNuclei = jsonObj.getJsonArray("Nuclei");
        for (int i=0 ; i<jsonNuclei.size() ; ++i){
            String nucID = ((JsonString)jsonNuclei.get(i)).getString();
            Nucleus nuc = nucMap.get(nucID);
            if (nuc != null){
                nuclei.put(nuc.getTime(),nuc);
            }
        }
    }
    // name the children using the supplied embryo orientatation rotation matrix
    // if R == null ,compute the orientation vector with children flipped and then name children
    // if R != null name the children using the embryo orientation matrix 
    public void nameChildren(RealMatrix r){
        if (this.children.isEmpty()) return ;  // no children - no naming can be performed
        Division div = divisionMap.get(this.name);
        if (div == null) return ; // cannot name an unknown cell
        
        RealMatrix R = r;
        if (R == null){
            // flip the children
            ArrayList<Cell> temp = new ArrayList<>();
            for (int i=children.size()-1 ; i>=0 ; --i){
                temp.add(children.get(i));
            }
            children = temp;
            Vector3D A = divisionDirection();
            Vector3D B = div.getV();
            R = rotationMatrix(A,B);
            double[] op = R.operate(A.toArray());
            int uisadfuihsd=0;
        }

        Vector3D direction = divisionDirection();
        double c0 = new Vector3D(R.operate(direction.toArray())).dotProduct(div.getV());
        double c1 = new Vector3D(R.operate(direction.scalarMultiply(-1.0).toArray())).dotProduct(div.getV());
        if (c0 > c1){
            children.get(0).name = div.child1;
            children.get(1).name = div.child2;
        }
        else {
            children.get(1).name = div.child1;
            children.get(0).name = div.child2;            
        }
        children.get(0).nameChildren(R);
        children.get(1).nameChildren(R);
        
    }
    public Vector3D divisionDirection(){
        double[] p0 = children.get(0).lastNucleus().getCenter();
        Vector3D v0 = new Vector3D(p0[0],p0[1],p0[2]);
        
        double[] p1 = children.get(1).lastNucleus().getCenter();
        Vector3D v1 = new Vector3D(p1[0],p1[1],p1[2]);

        return v1.subtract(v0)        ;
    }
    //find the rotation matrix that rotates the A vector onto the B vector
    static public RealMatrix rotationMatrix(Vector3D A,Vector3D B){
        Vector3D a = A.normalize();
        Vector3D b = B.normalize();
        Vector3D v = a.crossProduct(b);
        
        double s = v.getNormSq();
        double c = a.dotProduct(b);
        
        RealMatrix vx = MatrixUtils.createRealMatrix(3, 3);
        vx.setEntry(1, 0, v.getZ());
        vx.setEntry(0, 1, -v.getZ());
        vx.setEntry(2, 0, -v.getY());
        vx.setEntry(0, 2, v.getY());
        vx.setEntry(2, 1, v.getX());
        vx.setEntry(1, 2, -v.getX());  

        RealMatrix vx2 = vx.multiply(vx);
        RealMatrix scaled = vx2.scalarMultiply((1.0-c)/s);
        
        RealMatrix ident = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix sum = vx.add(scaled);
        RealMatrix ret = ident.add(sum);

        return ret;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        if (parent != null){
            builder.add("Parent", parent.name);
        }
   
        if (children != null){
            for (int i=0 ; i<children.size() ; ++i){
                builder.add(String.format("Child%d",i), children.get(i).asJson());
            }
        }
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Nucleus nuc : nuclei.values()){
            arrayBuilder.add(nuc.getName());
        }
        builder.add("Nuclei", arrayBuilder);
        return builder;
    }
    public String getName(){
        return this.name;
    }
    public Cell getParent(){
        return this.parent;
    }
    public Cell[] getChildren(){
        return children.toArray(new Cell[0]);
    }
    public void addNucleus(Nucleus nuc){
        nuclei.put(nuc.getTime(),nuc);
        nuc.setCell(this);
    }
    public void addChild(Cell child){
        children.add(child);
        child.setParent(this);
    }
    public void setParent(Cell parent){
        this.parent = parent;
    }
    public Nucleus getNucleus(int time){
        return nuclei.get(time);
    }
    // split this cell into two at the given time
    // the nucleus at the given time begins the later cell
    // return the later cell
    public Cell split(int time){
        Nucleus nuc = nuclei.get(time);
        
        // put all the distal nuclei into the new cell
        Cell ret = new Cell(nuc.getName());
        int t= time;
        while (nuc != null){
            ret.addNucleus(nuc);
            ++t;
            nuc = nuclei.get(t);
        }
        
        // remake the proximal cell list of nuclei
        TreeMap<Integer,Nucleus> prox = new TreeMap<>();
        for (t=this.firstTime() ; t<time;++t){
            prox.put(t,nuclei.get(t));
        }
        nuclei = prox;
        
        // relink children cells to the distal cell
        for (Cell child : children){
            ret.addChild(child);
        }
        this.children.clear();
        
        return ret;
    }
    // the time of the last nucleus in the cell
    public int lastTime(){
        return nuclei.lastKey();
    }
    public void clearChildren(){
        children.clear();
    }
    public int firstTime(){
        return nuclei.firstKey();
    }
    public Nucleus firstNucleus(){
        return nuclei.firstEntry().getValue();
    }
    public Nucleus lastNucleus(){
        return nuclei.lastEntry().getValue();
    }
    // unlink this cell from its parent
    public void unlink(){
        if (parent != null){
            Cell[] parentsChildren = parent.getChildren();
            parent.clearChildren();
            for (Cell child : parentsChildren){
                if (!child.getName().equals(this.getName())){
                    parent.addChild(child);
                }
            }
        }
        this.parent = null;
    }
    public void combineWith(Cell other){
        for (Nucleus nuc : other.nuclei.values()){
            this.addNucleus(nuc);
        }
        for (Cell child : other.getChildren()){
            this.addChild(child);
        }
        other.clearChildren();
        
    }
    public Cell getSister(){
        Cell ret = null;
        if (parent != null){
            for (Cell c : parent.children){
                if (c.getName() != this.name){
                    ret = c;
                    break;
                }
            }
        }
        return ret;
    }
    // the maximum time this cell and its descents reach
    public int maxTime(){
        if (children.isEmpty()){
            return this.lastTime();
        } else {
            int ret = Integer.MIN_VALUE;
            for (Cell child : children){
                int t = child.maxTime();
                if (t >ret){
                    ret = t;
                }
            }
            return ret;
        }
    }
    // all the leaves of this cell
    public List<Cell> leaves(){
        ArrayList<Cell> ret = new ArrayList<>();
        if (!this.children.isEmpty()){
            for (Cell child : children){
                ret.addAll(child.leaves());
            }
        }
        ret.add(this);
        return ret;
    }

    // find the leaves of a cell up to a max time
    public List<Cell> leaves(int maxTime){
        ArrayList<Cell> ret = new ArrayList<>();
        if (this.lastTime() >= maxTime || this.isLeaf()){
            ret.add(this);
        }
        else if (!this.children.isEmpty()){
            for (Cell child : children){
                ret.addAll(child.leaves(maxTime));
            }
        }
        
        return ret;        
    }
    public int getMaxExpression(){
        int ret = Integer.MIN_VALUE;
        for (Cell child : children){
            int v = child.getMaxExpression();
            if (v > ret){
                ret = v;
            }
        }
        for (Nucleus nuc : nuclei.values()){
            int v = (int)nuc.getExpression();
            if (v > ret){
                ret = v;
            }
        }
        return ret;
    }
    public int getMinExpression(){
        int ret = Integer.MAX_VALUE;
        for (Cell child : children){
            int v = child.getMinExpression();
            if (v < ret){
                ret = v;
            }
        }
        for (Nucleus nuc : nuclei.values()){
            int v = (int)nuc.getExpression();
            if (v < ret){
                ret = v;
            }
        }
        return ret;        
    }
    @Override
    public int compareTo(Object o) {
        return name.compareTo(((Cell)o).name);
    }  
    public boolean isLeaf(){
        return children.isEmpty();
    }
    public void setName(String name){
        if (divisionMap.get(name) != null){
            this.name = name;
        }
    }
    String name;
    Cell parent;  // the parent cell - can be null
    List<Cell> children = new ArrayList<>();  // children after division of this cell - can be empty
    TreeMap<Integer,Nucleus> nuclei =  new TreeMap<>();  // the time-linked nuclei in this cell
    static TreeMap<String,Division> divisionMap;

    class Division{
        public Division(String d1,String d2,double[] v){
            this.child1 = d1;
            this.child2 = d2;
            this.v = v;
        }
        public Vector3D getV(){
            return new Vector3D(v);
        }
        String child1;
        String child2;
        double[] v;
    }
}
