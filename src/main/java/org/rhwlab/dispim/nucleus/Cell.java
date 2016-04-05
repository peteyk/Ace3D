/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author gevirl
 */
public class Cell {
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        if (parent != null){
            builder.add("Parent", parent.name);
        }
   
        if (children != null){
            for (int i=0 ; i<children.length ; ++i){
                builder.add(String.format("Child%d",i), children[i].asJson());
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
        return children;
    }
    String name;
    Cell parent;  // the parent cell - can be null
    Cell[] children;  // children after division of this cell - can be null
    TreeMap<Integer,Nucleus> nuclei;  // the time-linked nuclei in this cell
    
}
