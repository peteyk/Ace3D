/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author gevirl
 */
public class Cell {
    public Cell(String name){
        this.name = name;
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
            JsonObjectBuilder nucBuilder = Json.createObjectBuilder();
            nucBuilder.add("Time",nuc.getTime());
            nucBuilder.add("Name",nuc.getName());
            arrayBuilder.add(nucBuilder);
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
    // return the later cell
    public Cell split(int time){
        Nucleus nuc = nuclei.get(time);
        Cell ret = new Cell(nuc.getName());
        while (nuc != null){
            ret.addNucleus(nuc);
            ++time;
            nuc = nuclei.get(time);
        }
        // relink children cells
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
    // unlink this cell from its parent
    public void unlink(){
        Cell[] parentsChildren = parent.getChildren();
        parent.clearChildren();
        for (Cell child : parentsChildren){
            if (!child.getName().equals(this.getName())){
                parent.addChild(child);
            }
        }
        this.parent = null;
    }
    public void combineWith(Cell other){
        for (Nucleus nuc : other.nuclei.values()){
            this.addNucleus(nuc);
            for (Cell child : other.getChildren()){
                this.addChild(child);
            }
        }
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
            int v = nuc.getExpression();
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
            int v = nuc.getExpression();
            if (v < ret){
                ret = v;
            }
        }
        return ret;        
    }

    String name;
    Cell parent;  // the parent cell - can be null
    List<Cell> children = new ArrayList<>();  // children after division of this cell - can be empty
    TreeMap<Integer,Nucleus> nuclei =  new TreeMap<>();  // the time-linked nuclei in this cell
    
}
