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
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.rhwlab.ace3d.SelectedNucleusFrame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;

/**
 *
 * @author gevirl
 */
public class Ace3DNucleusFile implements NucleusFile,javafx.beans.Observable   {

    public Ace3DNucleusFile(File file,SynchronizedMultipleSlicePanel panel,SelectedNucleusFrame frame){
        this.file =file;
        this.panel = panel;
        this.frame = frame;
    }
    @Override
    public void open() throws Exception {
        this.opening = true;
        JsonReader reader = Json.createReader(new FileReader(file));
        JsonObject obj = reader.readObject();
        JsonArray jsonNucs = obj.getJsonArray("Nuclei");
        for (int n=0 ; n<jsonNucs.size() ; ++n){
            JsonObject jsonNuc = jsonNucs.getJsonObject(n);
            Nucleus nuc = new Nucleus(jsonNuc);
            this.addNucleus(nuc,false);
        }
        JsonArray jsonRoots = obj.getJsonArray("Roots");
        for (int n=0 ; n<jsonRoots.size() ; ++n){
            JsonObject rootObj  = jsonRoots.getJsonObject(n);
            Cell root = new Cell(rootObj,null,this.byName);
            this.addRoot(root,false);
        }
        reader.close();
        this.opening = false;
        this.notifyListeners();
    }
    public void addRoot(Cell cell){
        addRoot(cell,true);
    }
    public void addRoot(Cell cell,boolean notify){
        int t = cell.firstTime();
        Set<Cell> rootSet = roots.get(t);
        if(rootSet == null){
            rootSet = new TreeSet<Cell>();
            roots.put(t,rootSet);
        }
        rootSet.add(cell);
        addCell(cell);
        if (notify)        {
            this.notifyListeners();
        }
    }
    private void addCell(Cell cell){
        cellMap.put(cell.getName(),cell);
        for (Cell child : cell.getChildren()){
            addCell(child);
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
            timeSet = new TreeSet<Nucleus>();
            byTime.put(nuc.getTime(), timeSet);
        }
        timeSet.add(nuc);
        byName.put(nuc.getName(), nuc);
        if (notify){
            this.notifyListeners();
        }
    }
 
    public void unlink(Nucleus from,Nucleus to){
        Cell fromCell = from.getCell();
        Cell toCell = to.getCell(); 
        if (fromCell == null || toCell == null) return;  // they can't be linked if one is unlinked 
        
        if (fromCell.getName().equals(toCell.getName())){
            // not a division - both nuclei in the same cell
            // split the from cell and make a new root with the to nucleus
            Cell splitCell = fromCell.split(to.getTime());
            this.addRoot(splitCell,false);
        } else {
            // unlinking a division - the division goes away and the child to keep merges with her parent
            // the child not keeping in the path is made a new root
            Nucleus keep = null;
            Cell[] children = fromCell.getChildren();
            if (children[0].getName().equals(toCell.getName())){
                keep = children[1].firstNucleus();
            } else if (children[1].getName().equals(toCell.getName())){
                keep = children[0].firstNucleus();
            }
            // keep is the child cell to keep linked
            if (keep != null){
                // unlink the children cells
                for (Cell child : fromCell.getChildren()){
                    child.setParent(null);
                    this.addRoot(child,false);                
                }
                fromCell.clearChildren();
                
                // merge the keep cell to her parent
                linkInTime(from,keep);
            }
        }
        this.notifyListeners();
    }
    public void linkInTime(Nucleus from,Nucleus to){

        
        Cell fromCell = from.getCell();
        Cell toCell = to.getCell();
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
                Set<Cell> rootSet = roots.get(toCell.firstTime());
                rootSet.remove(toCell);
                fromCell.combineWith(toCell);
            }
        }
        this.notifyListeners();
    }
    
    // create a new division by linking a nucleus to a parent nucleus that is already linked in time
    public void linkDivision(Nucleus from,Nucleus to){
        if (from.getTime() != to.getTime()-1) return; // can only link nuclei separated by one unit of time
        
        Cell fromCell = from.getCell();
        Cell toCell = to.getCell();
        if (fromCell.getName().equals(toCell.getName())) return; // can only link nuclei in different cells
        
        
        // split the from cell
        Cell splitCell = fromCell.split(to.getTime());
        fromCell.addChild(splitCell);
        fromCell.addChild(toCell);

        // to cell is no longer a root
        int time = to.getTime();
        Set<Cell> rootCells = roots.get(time);
        rootCells.remove(toCell);
        
        this.notifyListeners(); 
    }
    
   public void linkDivision(Nucleus from,Nucleus to1,Nucleus to2){

        
        Cell fromCell = from.getCell();
        if (fromCell == null){
            fromCell = new Cell(from.getName());
            fromCell.addNucleus(from);
            this.addRoot(fromCell,false);
        }
        Cell to1Cell = to1.getCell();
        if (to1Cell == null){
            to1Cell = new Cell(to1.getName());
            to1Cell.addNucleus(to1);
        }
        Cell to2Cell = to2.getCell();
        if (to2Cell == null){
            to2Cell = new Cell(to2.getName());
            to2Cell.addNucleus(to2);
        }
        
        fromCell.addChild(to1Cell);
        fromCell.addChild(to2Cell);
        int time = to1.getTime();
        Set<Cell> rootCells = roots.get(time);
        rootCells.remove(to1Cell);
        rootCells.remove(to2Cell);
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
            ret = new TreeSet<Nucleus>();
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
    @Override
    public Cell getCell(String name){
        return cellMap.get(name);
    }
    @Override
    public List<Nucleus> linkedForward(Nucleus nuc) {
        ArrayList ret = new ArrayList<>();
        Cell cell = cellMap.get(nuc.getCell().getName());
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
        Cell cell = cellMap.get(nuc.getCell().getName());
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
        if (opening){
            return;
        }
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
/*
    @Override
    public void setSelected(int time, String name) {
        Nucleus toSelect = byName.get(name);
        if (toSelect != null && toSelect.getTime()==time){
            Set<Nucleus> nucs = byTime.get(time);
            for (Nucleus nuc : nucs){
                if (nuc.getName().equals(name)){
                    setSelected(nuc);
                    return;
                }
            }
        }
    }
 */   
    @Override
    public void setSelected(Nucleus toSelect){
        this.selectedNucleus = toSelect;
        if (panel != null) {
            panel.changeTime(toSelect.getTime());
            panel.changePosition(toSelect.getCenter());
        }
        if (frame != null){
            frame.stateChanged(null);
        }
    //    this.notifyListeners();
    }
    @Override
    public Nucleus getSelected(){
        return this.selectedNucleus;
    }
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame frame;
    boolean opening = true;
    File file;
    Nucleus selectedNucleus;
    TreeMap<Integer,Set<Cell>> roots = new TreeMap<>();  // roots indexed by time
    TreeMap<String,Cell> cellMap = new TreeMap<>();  // map of the all the cells
    TreeMap<Integer,Set<Nucleus>> byTime = new TreeMap<>();  // all the nuclei present at a given time
    TreeMap<String,Nucleus> byName = new TreeMap<>();  // map of nuclei indexed by name, map indexed by time
    ArrayList<InvalidationListener> listeners = new ArrayList<>();



}
