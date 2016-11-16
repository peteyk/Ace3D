/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.rhwlab.BHC.BHCTree;

/**
 *
 * @author gevirl
 */
abstract public class LinkedNucleusFile implements NucleusFile {
    public LinkedNucleusFile(File xml){
        this.file = xml;
    }
    public LinkedNucleusFile(BHCTreeDirectory bhc){
        this.bhcTreeDir = bhc;
    }

    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("BHC", this.bhcTreeDir.dir.getPath());
        builder.add("Times", this.timesAsJson());
        return builder;
    }    
    public JsonArrayBuilder timesAsJson(){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Integer time : byTime.navigableKeySet()){
            Set<Nucleus> roots = this.getRoots(time);
            if (roots.size()>0){
                builder.add(timeAsJson(time));
            }
        } 
        return builder;
    }
    public JsonObjectBuilder timeAsJson(int t){
 
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Time",t);

        builder.add("Nuclei", nucleiAsJson(t));
        return builder;
    }
    public JsonArrayBuilder nucleiAsJson(int t){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        Set<Nucleus> nucs = byTime.get(t);
        for (Nucleus nuc : nucs){
            if (nuc.getParent()==null){
                builder.add(nuc.asJson());
            }
        }
        return builder;
    }   

    @Override
    public Set<Nucleus> getNuclei(int time) {
        Set<Nucleus> ret = this.byTime.get(time);
        if (ret == null){
            ret = new TreeSet<>();
        }
        return ret;
    }

    @Override
    public File getFile() {
        return this.file;
    }

    @Override
    public Set<Nucleus> getRoots(int time) {
        TreeSet<Nucleus> ret = new TreeSet<>();
        Set<Nucleus> all = this.getNuclei(time);
        if (all != null){
            for (Nucleus nuc : all){
                if (nuc.getParent()==null){
                    ret.add(nuc);
                }
            }
        }
        return ret;
    }

    @Override
    public List<Nucleus> linkedForward(Nucleus nuc) {
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus child : nuc.nextNuclei()){
            ret.add(child);
        }
        return ret;
    }

    @Override
    public Nucleus linkedBack(Nucleus nuc) {
        return nuc.getParent();
    }

    @Override
    public Nucleus sister(Nucleus nuc) {
        return nuc.getSister();
    }

    public void addNucleusRecursive(Nucleus nuc){
        this.addNucleus(nuc);
        if (nuc.getChild1() != null){
            addNucleusRecursive(nuc.getChild1());
        }
        if (nuc.getChild2()!= null){
            addNucleusRecursive(nuc.getChild2());
        }
    }
    @Override
    public void addNucleus(Nucleus nuc) {
        int t= nuc.getTime();
        TreeSet<Nucleus> nucSet = byTime.get(t);
        if (nucSet == null){
            nucSet = new TreeSet<>();
            byTime.put(t, nucSet);
        }
        nucSet.add(nuc);
//        byName.put(nuc.getName(),nuc);
    }

    @Override
    public Set<Integer> getAllTimes() {
        return this.byTime.keySet();
    }
/*
    @Override
    public Nucleus getNucleus(String name) {
        return byName.get(name);
    }
*/
    @Override
    public void setSelected(Nucleus nuc) {
        this.selectedNucleus.setSelectedNucleus(nuc);
    }

    @Override
    public Nucleus getSelected() {
        return (Nucleus)this.selectedNucleus.getValue();
    }

    @Override
    public Cell getCell(String name) {
       
       return null;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        this.listeners.remove(listener);
    }
    
    @Override
    public void addNuclei(BHCNucleusFile bhcToAdd,boolean curated){
        curatedMap.put(bhcToAdd.getTime(), curated);
        for (NucleusData nuc : bhcToAdd.getNuclei()){
            Nucleus linkedNuc = new Nucleus(nuc);
//            byName.put(nuc.getName(), linkedNuc);
            
            TreeSet<Nucleus> nucsAtTime = byTime.get(nuc.getTime());
            if (nucsAtTime == null){
                nucsAtTime = new TreeSet<>();
                byTime.put(nuc.getTime(),nucsAtTime);
            }
            nucsAtTime.add(linkedNuc);
        }
        notifyListeners();
    }
    public void notifyListeners(){
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
    public BHCTreeDirectory getTreeDirectory(){
        return this.bhcTreeDir;
    }  

    @Override
    public void addSelectionOberver(ChangeListener obs){
        selectedNucleus.addListener(obs);
    }
    
    // auto link all nuclei at a given time point to the next time point
    // if the next time point is not curated, it will be segmented to the optimal level
    // if it is curated, then the segmentation of the next time is not changed
    public void autoLink(int time)throws Exception {
        TreeSet<Nucleus> src = this.byTime.get(time);
        if (src == null){
            return;  // no nuclei at the given time
        }
        int n = src.size();  // number of nuclei at from time
        if (n == 0){
            return;  // no nuclei at the given time
        }        
        
        BHCTree nextTree = bhcTreeDir.getTree(time+1);
        if (nextTree == null){
            return ; // no tree built for the to time
        }
        TreeMap<Integer,Double> probMap = new TreeMap<>();
        nextTree.allPosteriorProb(probMap);
        int nextN = n;
        while (probMap.get(nextN) < threshold){
            ++nextN;  // skipping cuts that have a probability less than the threshold
        }
        
        Linkage current = formLinkage(time,nextN,nextTree);
        do {
            ++nextN;
            Linkage next = formLinkage(time,nextN,nextTree);
            if (next.compareTo(current)>0){
                break;
            }
            current = next;
        } while (true);

        // put the two new sets(from and to) of nuclei into this file
        TreeSet<Nucleus> newFrom = current.getFromNuclei();
        byTime.put(time, newFrom);
        // fix the links from the parent of the from nuclei
        for (Nucleus nuc : newFrom){
            Nucleus parent = nuc.getParent();
            if (parent != null){
                if (parent.getChild1().getNucleusData() == nuc.getNucleusData()){
                    parent.setDaughters(nuc, parent.getChild2());
                } else {
                    parent.setDaughters(parent.getChild1(),nuc);
                }
                
            }
        }
        byTime.put(time+1,current.getToNuclei());
        this.curatedMap.put(time+1, false);
        this.notifyListeners();
    }
    
    // form a linkage of all nuclei between time points
    // input the time and the number of nuclei to link to
    // also input the BHC tree of course
    private Linkage formLinkage(int time,int n,BHCTree nextTree){
        // clone the from nuclei
        Nucleus[] fromNucs = cloneTime(time);
        
        // build the to nuclei from the tree with n nuclei
        BHCNucleusFile nextNucFile = nextTree.cutToN(n);
        Set<BHCNucleusData> nucData = nextNucFile.getNuclei();
        Nucleus[] toNucs = new Nucleus[nucData.size()];
        int i=0;
        for (BHCNucleusData nuc : nucData){
            toNucs[i] = new Nucleus(nuc);
            ++i;
        }
        
        return new Linkage(fromNucs, toNucs);        
    }
    private Nucleus[] cloneTime(int t){
        TreeSet<Nucleus> src = byTime.get(t);
        Nucleus[] ret = new Nucleus[src.size()];
        int i=0;
        for (Nucleus nuc : src){
            ret[i] = nuc.clone();
            ++i;
        }
        return ret;
    }
    
    File file;
    TreeMap<Integer,TreeSet<Nucleus>> byTime=new TreeMap<>();
    TreeMap<Integer,Boolean> curatedMap = new TreeMap<>();
 //   TreeMap<String,Nucleus> byName=new TreeMap<>();
    
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
    SelectedNucleus selectedNucleus = new SelectedNucleus();
    BHCTreeDirectory bhcTreeDir;
    
    boolean opening = false;
    static double threshold= 1.0E-14;
}
