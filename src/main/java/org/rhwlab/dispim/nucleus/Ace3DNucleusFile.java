/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 *
 * @author gevirl
 */
public class Ace3DNucleusFile implements NucleusFile {
    public Ace3DNucleusFile(){
        
    }
    public Ace3DNucleusFile(File file)throws Exception {
        this.file =file;
        open();
    }
    public void open() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        String[] headings = line.split(",|\t");
        line = reader.readLine();
        while (line != null){
            String[] tokens = line.split(",|\t");
            Nucleus nuc = new Nucleus(headings,tokens);
            this.putNucleusIntoMaps(nuc);
            line = reader.readLine();
        }
        reader.close();
    }
    public void putNucleusIntoMaps(Nucleus nuc){
            Set<Nucleus> timeSet = byTime.get(nuc.getTime());
            if (timeSet == null){
                timeSet = new HashSet<Nucleus>();
                byTime.put(nuc.getTime(), timeSet);
            }
            timeSet.add(nuc);
            
            Map<Integer,Nucleus> nameMap = byName.get(nuc.getName());
            if (nameMap == null){
                nameMap = new TreeMap<Integer,Nucleus>();
                byName.put(nuc.getName(), nameMap);
            }
            nameMap.put(nuc.getTime(),nuc);        
    }
    @Override
    public void saveAs(File file)throws Exception {
        this.file = file;
        this.save();
    }
    @Override
    public void save()throws Exception {
        PrintWriter writer = new PrintWriter(file);
/*        
        JsonGenerator gen = Json.createGenerator(writer);
        gen.writeStartObject();
        gen.write("Nuclei", this.nucleiAsJson().build());
        gen.writeEnd();
        gen.flush();
        gen.close();
 */       
       PrettyWriter pretty = new PrettyWriter(writer);
        pretty.writeObject(this.asJson().build(), 0);
       writer.close();
    }
    @Override
    public Set<Nucleus> getNuclei(int time){
        Set<Nucleus> ret = byTime.get(time);
        if (ret == null){
            ret = new HashSet<Nucleus>();
            byTime.put(time, ret);
        }
        return ret;
    }
    @Override
    public File getFile(){
        return file;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Nuclei", this.nucleiAsJson());
        builder.add("Roots",this.rootsAsJson());
        return builder;
    }
    public JsonArrayBuilder nucleiAsJson(){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Integer time : byTime.navigableKeySet()){
            Set<Nucleus> nucs = byTime.get(time);
            for (Nucleus nuc : nucs){
                builder.add(nuc.asJson());
            }
        }
        return builder;
    }
    
    public JsonArrayBuilder rootsAsJson(){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Cell root : roots){
            builder.add(root.asJson());
        }
        return builder;
    }
    File file;
    Set<Cell> roots = new TreeSet<Cell>();
    TreeMap<String,Cell> cellMap = new TreeMap<String,Cell>();  // map of the all the cells
    TreeMap<Integer,Set<Nucleus>> byTime = new TreeMap<Integer,Set<Nucleus>>();  // all the nuclei present at a given time
    TreeMap<String,Map<Integer,Nucleus>> byName = new TreeMap<String,Map<Integer,Nucleus>>();  // map of nuclei indexed by name, map indexed by time
}
