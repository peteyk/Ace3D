/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

/**
 *
 * @author gevirl
 */
public class Ace3DNucleusFile implements NucleusFile   {
    public Ace3DNucleusFile(){
        
    }
    public Ace3DNucleusFile(File file)throws Exception {
        this.file =file;
        open();
    }
    @Override
    public void open() throws Exception {
        JsonReader reader = Json.createReader(new FileReader(file));
        JsonObject obj = reader.readObject();
        JsonArray jsonNucs = obj.getJsonArray("Nuclei");
        for (int n=0 ; n<jsonNucs.size() ; ++n){
            JsonObject jsonNuc = jsonNucs.getJsonObject(n);
            Nucleus nuc = new Nucleus(jsonNuc);
            this.addNucleus(nuc,false);
        }
        reader.close();
        this.notifyListeners();
    }
    public void addRoot(Cell cell){
        addRoot(cell,true);
    }
    public void addRoot(Cell cell,boolean notify){
        int t = cell.firstTime();
        Set<Cell> rootSet = roots.get(t);
        if(rootSet == null){
            rootSet = new HashSet<Cell>();
            roots.put(t,rootSet);
        }
        rootSet.add(cell);
        cellMap.put(cell.getName(),cell);
        if (notify)        {
            this.notifyListeners();
        }
    }
    // add a new nucleus with no cell links
    @Override
    public void addNucleus(Nucleus nuc){
        addNucleus(nuc,true);
    }    

    public void addNucleus(Nucleus nuc,boolean notify){
        Set<Nucleus> timeSet = byTime.get(nuc.getTime());
        if (timeSet == null){
            timeSet = new HashSet<Nucleus>();
            byTime.put(nuc.getTime(), timeSet);
        }
        timeSet.add(nuc);
        byName.put(nuc.getName(), nuc);
        if (notify){
            this.notifyListeners();
        }
    }
    
    // unlink a nucleus from the next time point
    public void unlinkNextTime(Nucleus unlinkNuc){
        Cell unlinkCell = this.getCell(unlinkNuc.getName());
        if (unlinkCell == null)  return;  // nucleus is not in a cell so it is unlinked
        
        // is the nucleus at the last time of a cell
        if (unlinkNuc.getTime() == unlinkCell.lastTime()){
            
            // unlink the children cells
            for (Cell child : unlinkCell.getChildren()){
                child.setParent(null);
                this.addRoot(child,false);                
            }
            unlinkCell.clearChildren();
        } else {
            // split the unlinkCell and make a new root 
            this.addRoot(unlinkCell.split(unlinkNuc.getTime()),false);
        }
        this.notifyListeners();
    }
    public void unlinkPreviousTime(Nucleus unlinkNuc){
        Cell unlinkCell = this.getCell(unlinkNuc.getName());
        if (unlinkCell == null)  return;  // nucleus is not in a cell so it is unlinked 
        
        // is the nucleus at the begining of a cell
        if (unlinkNuc.getTime() == unlinkCell.firstTime()){
            // unlink the cell from its parent and make it a new root
            unlinkCell.unlink();
            this.addRoot(unlinkCell,false);
        } else {
            // split the unlink cell and make a new root
            Cell splitCell = unlinkCell.split(unlinkNuc.getTime());
            this.addRoot(splitCell,false);
        }
        this.notifyListeners();
    }
    
    public void linkInTime(Nucleus from,Nucleus to){
        this.unlinkNextTime(from);
        this.unlinkPreviousTime(to);
        
        Cell fromCell = this.getCell(from.getName());
        Cell toCell = this.getCell(to.getName());
        if (fromCell == null){
            if (toCell == null){
                // make a new cell with the two unlinked nuclei
                Cell cell = new Cell(from.getName());
                cell.addNucleus(from);
                cell.addNucleus(to);
                this.addRoot(cell,false);
            } else {
                // put the fromNuc into the toCell
                toCell.addNucleus(from);
            }
        }else {
            if (toCell == null){
                // add the toNuc to the fromCell
                fromCell.addNucleus(to);
            } else {
                // both fromNuc and toNuc are in cells - combine the two cells
                roots.remove(toCell);
                fromCell.combineWith(toCell);
            }
        }
        this.notifyListeners();
    }
    
    public void linkDivision(Nucleus from,Nucleus to1,Nucleus to2){
        this.unlinkNextTime(from);
        this.unlinkPreviousTime(to1);
        this.unlinkPreviousTime(to2);
        
        Cell fromCell = this.getCell(from.getName());
        if (fromCell == null){
            fromCell = new Cell(from.getName());
            fromCell.addNucleus(from);
            this.addRoot(fromCell,false);
        }
        Cell to1Cell = this.getCell(to1.getName());
        if (to1Cell == null){
            to1Cell = new Cell(to1.getName());
            to1Cell.addNucleus(to1);
        }
        Cell to2Cell = this.getCell(to2.getName());
        if (to2Cell == null){
            to2Cell = new Cell(to2.getName());
            to2Cell.addNucleus(to2);
        }
        
        fromCell.addChild(to1Cell);
        fromCell.addChild(to2Cell);
        roots.remove(to1);
        roots.remove(to2);
        this.notifyListeners(); 
    }

    @Override
    public void saveAs(File file)throws Exception {
        this.file = file;
        this.save();
    }
    @Override
    public void save()throws Exception {
        PrintWriter writer = new PrintWriter(file);
      
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
        for (Integer t : roots.navigableKeySet()){
            Set<Cell> rootSet = roots.get(t);
            for (Cell root : rootSet){
                builder.add(root.asJson());
            }
        }
        return builder;
    }
    public Cell getCell(String name){
        return cellMap.get(name);
    }
    @Override
    public List<Nucleus> linkedForward(Nucleus nuc) {
        ArrayList ret = new ArrayList<Nucleus>();
        Cell cell = cellMap.get(nuc.cell.getName());
        if (cell != null){
            Nucleus nextNuc = cell.getNucleus(nuc.getTime()+1);
            if (nextNuc != null){
                ret.add(nextNuc);
            } else {
                for (Cell child : cell.getChildren()){
                    ret.add(child.firstNucleus());
                }
            }
        }
        return ret;
    }  
    @Override
    public Nucleus linkedBack(Nucleus nuc) {
        Nucleus ret = null;
        Cell cell = cellMap.get(nuc.cell.getName());
        if (cell != null){
            Nucleus nextNuc = cell.getNucleus(nuc.getTime()-1);
            if (nextNuc != null){
                ret = nextNuc;
            } else {
                Cell parent = cell.getParent();
                if (parent != null){
                    ret = parent.getNucleus(nuc.getTime()-1);
                }
            }
        }        
        return ret;
    }    
    // return the sister nucleus of the given nucleus
    // returns null if nucleus is not in a cell or is in a root cell
    public Nucleus sister(Nucleus nuc){
        Cell cell = nuc.getCell();
        if (cell == null){
            return null;
        }
        Cell sisterCell = cell.getSister();
        if (sisterCell == null){
            return null;
        }
        return sisterCell.getNucleus(nuc.getTime());
    }
    @Override
    public Set<Cell> getRoots(int time) {
    
        return roots.get(time);
    }    

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners.remove(listener);
    }
    
    void notifyListeners(){
        for (InvalidationListener listener : listeners){
            if (listener != null){
                listener.invalidated(this);
            }
        }
    }
    @Override
    public Set<Integer> getAllTimes(){
        return this.byTime.keySet();
    }
    @Override
    public Nucleus getNucleus(String name) {
        return byName.get(name);
    }  

    @Override
    public void setSelected(int time, String name) {
        Nucleus toSelect = byName.get(name);
        if (toSelect != null && toSelect.time==time){
            Set<Nucleus> nucs = byTime.get(time);
            for (Nucleus nuc : nucs){
                if (nuc.getName().equals(name)){
                    selectedNucleus = nuc;
                }
            }
        }
    }
    public void setSelected(Nucleus toSelect){
        this.selectedNucleus = toSelect;
    }
    @Override
    public Nucleus getSelected(){
        return this.selectedNucleus;
    }
    
    File file;
    Nucleus selectedNucleus;
    TreeMap<Integer,Set<Cell>> roots = new TreeMap<>();  // roots indexed by time
    TreeMap<String,Cell> cellMap = new TreeMap<>();  // map of the all the cells
    TreeMap<Integer,Set<Nucleus>> byTime = new TreeMap<>();  // all the nuclei present at a given time
    TreeMap<String,Nucleus> byName = new TreeMap<>();  // map of nuclei indexed by name, map indexed by time
    ArrayList<InvalidationListener> listeners = new ArrayList<>();



}
