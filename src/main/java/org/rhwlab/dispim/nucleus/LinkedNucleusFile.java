/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;

/**
 *
 * @author gevirl
 */
public class LinkedNucleusFile implements NucleusFile {
    public LinkedNucleusFile(){
        
    }
    public LinkedNucleusFile(File xml){
        this();
        this.file = xml;
    }

    @Override
    public void fromXML(Element nucleiEle) {
        TreeMap<Integer,Element> timeEleMap = new TreeMap<>();
        for (Element timeEle : nucleiEle.getChildren("Time")){
            int t = Integer.valueOf(timeEle.getAttributeValue("time"));
            timeEleMap.put(t, timeEle);
        }
        for (int t : timeEleMap.descendingKeySet()){
            TreeMap<String,Nucleus> nucMap = new TreeMap<>();
            Element timeEle = timeEleMap.get(t);
            this.curatedMap.put(t,Boolean.valueOf(timeEle.getAttributeValue("curated")));
            for (Element nucEle : timeEle.getChildren("Nucleus")){
                BHCNucleusData bhcNuc = new BHCNucleusData(nucEle);             
                Nucleus nuc = new Nucleus(bhcNuc);
                nuc.setCellName(nucEle.getAttributeValue("cell"), Boolean.valueOf(nucEle.getAttributeValue("usernamed")));
                nucMap.put(nuc.getName(), nuc);
                
                // link children
                String c1 = nucEle.getAttributeValue("child1");
                if (c1 != null){
                    TreeMap<String,Nucleus> nextNucs = byTime.get(t+1);
                    Nucleus child1Nuc = nextNucs.get(c1);
                    Nucleus child2Nuc = null;
                    String c2 = nucEle.getAttributeValue("child2");
                    if (c2 != null){
                        child2Nuc = nextNucs.get(c2);
                    }
                    nuc.setDaughters(child1Nuc, child2Nuc);
                }
            }
            this.byTime.put(t, nucMap);
        }
    }
    
    public Element toXML(){
        Element ret = new Element("Nuclei");
        ret.setAttribute("BHCTreeDirectory", this.bhcTreeDir.dir.getPath());
        for (Integer t : this.byTime.keySet()){
            ret.addContent(timeAsXML(t));
        }
        return ret;
    }
    public Element timeAsXML(int time){
        Element ret = new Element("Time");
        ret.setAttribute("time", Integer.toString(time));
        ret.setAttribute("curated", Boolean.toString(this.curatedMap.get(time)));
        for (Nucleus nuc : byTime.get(time).values()){
            ret.addContent(nuc.asXML());
        }
        return ret;
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
                builder.add(timeAsJson(time,this.curatedMap.get(time)));
            }
        } 
        return builder;
    }
    public JsonObjectBuilder timeAsJson(int t,boolean cur){
 
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Time",t);
        builder.add("Curated", cur);
        builder.add("Nuclei", nucleiAsJson(t));
        
        return builder;
    }
    public JsonArrayBuilder nucleiAsJson(int t){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Nucleus nuc : byTime.get(t).values()){
            if (nuc.getParent()==null){
                builder.add(nuc.asJson());
            }
        }
        return builder;
    }   

    @Override
    public Set<Nucleus> getNuclei(int time) {
        TreeSet ret = new TreeSet<>();
        if (this.byTime.get(time) != null){
            ret.addAll(this.byTime.get(time).values());
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
    public Set<Nucleus> getLeaves(int time) {
        TreeSet<Nucleus> ret = new TreeSet<>();
        Set<Nucleus> all = this.getNuclei(time);
        if (all != null){
            for (Nucleus nuc : all){
                if (nuc.isLeaf()){
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
        TreeMap<String,Nucleus> nucMap = byTime.get(t);
        if (nucMap == null){
            nucMap = new TreeMap<>();
            byTime.put(t, nucMap);
        }
        nucMap.put(nuc.getName(),nuc);
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
        return this.selectedNucleus.getSelected();
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
    public void addNuclei(BHCNucleusSet bhcToAdd,boolean curated){
        curatedMap.put(bhcToAdd.getTime(), curated);
        TreeMap<String,Nucleus> nucsAtTime = new TreeMap<>();
        for (NucleusData nuc : bhcToAdd.getNuclei()){
            Nucleus linkedNuc = new Nucleus(nuc);

            nucsAtTime.put(linkedNuc.getName(),linkedNuc);
        }
        byTime.put(bhcToAdd.getTime(),nucsAtTime);
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
/*    
    // auto link all nuclei at a given time point to the next time point
    // if the next time point is not curated, it will be segmented to the optimal level
    // if it is curated, then the segmentation of the next time is not changed
    public void autoLink(int time)throws Exception {

        TreeMap<String,Nucleus> src = this.byTime.get(time);
        if (src == null){
            return;  // no nuclei at the given time
        }
        int n = src.size();  // number of nuclei at from time
        if (n == 0){
            return;  // no nuclei at the given time
        }        
        int nextN = n;
        
        BHCTree nextTree = bhcTreeDir.getTree(time+1);
        if (nextTree == null){
            return ; // no tree built for the to time
        }
        TreeMap<Integer,Double> probMap = new TreeMap<>();
        nextTree.allPosteriorProb(probMap);
        
        // find a probability at which to cur the nextTree
        double prob = probMap.get(nextN);
        while (prob < threshold){
            ++nextN;  // skipping cuts that have a probability less than the threshold
            prob = probMap.get(nextN);
        }
        
        Linkage current = formLinkage(time,nextN,nextTree);
        double nextProb = probMap.get(nextN);
    
        if (nextProb < .5){
            do {
                ++nextN;
                nextProb = probMap.get(nextN);
                Linkage next = formLinkage(time,nextN,nextTree);
                if (next.compareTo(current)>0 || next.getToNuclei().size() < nextN){
                    break;  // the next linkage got worse - stop 
                }
                current = next;
            } while (true && nextProb <.9 );
        }

        // put the two new sets(from and to) of nuclei into this file
        TreeMap<String,Nucleus> newFrom = current.getFromNuclei();
        byTime.put(time, newFrom);
        
        // fix the links from the parent of the from nuclei
        for (Nucleus nuc : newFrom.values()){
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
        this.autoLinkedMap.put(time, true);
        this.notifyListeners();
    }
    
    // form a linkage of all nuclei between time points
    // input the time and the number of nuclei to link to
    // also input the BHC tree of course
    private Linkage formLinkage(int time,int n,BHCTree nextTree){
        // clone the from nuclei
        Nucleus[] fromNucs = cloneTime(time);
        
        // build the to nuclei from the tree with n nuclei
        BHCNucleusSet nextNucFile = nextTree.cutToN(n);
        Set<BHCNucleusData> nucData = nextNucFile.getNuclei();
        Nucleus[] toNucs = new Nucleus[nucData.size()];
        int i=0;
        for (BHCNucleusData nuc : nucData){
            toNucs[i] = new Nucleus(nuc);
            ++i;
        }
        
        return new Linkage(fromNucs, toNucs);        
    }
*/    
    public Nucleus[] cloneTime(int t){
        Collection<Nucleus> src = byTime.get(t).values();
        Nucleus[] ret = new Nucleus[src.size()];
        int i=0;
        for (Nucleus nuc : src){
            ret[i] = nuc.clone();
            ++i;
        }
        return ret;
    }
    @Override
    public void setFile(File f){
        this.file = f;
    }

    @Override
    public void setBHCTreeDirectory(BHCTreeDirectory bhc) {
        this.bhcTreeDir = bhc;
    } 
    // unlink a nucleus from its parent
    public void unlinkNucleus(Nucleus nuc,boolean notify){
        Nucleus parent = nuc.getParent();
        if (parent == null){
            return;
        }
        // the nucleus being unlinked must get a new cellname
        nuc.renameContainingCell(nuc.getName());
        
        if (parent.getChild1()==nuc){
            // move parents child2 into child1
            parent.setDaughters(parent.getChild2(), null);
        }else {
            // unlinking parents child2
            parent.setDaughters(parent.getChild1(), null);
        }
        if (parent.getChild1()!=null){
            // child1 now part of parents cell
            parent.getChild1().renameContainingCell(parent.getCellName());            
        }
        if (notify){
            this.notifyListeners();
        }
    }

    @Override
    public void removeNucleus(Nucleus nuc,boolean notify) {
        
        if (selectedNucleus.getSelected()!=null && selectedNucleus.getSelected().equals(nuc)){
            this.setSelected(null);
        }
        
        // unlink any child fron the nuc being deleted
        if (nuc.getChild1() != null){
            unlinkNucleus(nuc.getChild1(),false);
        }
        if (nuc.getChild2() != null){
            unlinkNucleus(nuc.getChild2(),false);
        }
        // unlink from parent
        unlinkNucleus(nuc,false);  

        TreeMap<String,Nucleus> map = byTime.get(nuc.getTime());
        map.remove(nuc.getName());
        
        curatedMap.put(nuc.getTime(), true);  // this time is now curated
        
        if (map.isEmpty()){
            byTime.remove(nuc.getTime());
            curatedMap.remove(nuc.getTime());
        }
        if (notify){
            this.notifyListeners();
        }
    }
    
    public Nucleus getMarked(){
        return selectedNucleus.getMarked();
    }
    public void setMarked(Nucleus toMark){
        selectedNucleus.setMarked(toMark);
    }
    public void autoLinkBetweenCuratedTimes(int time)throws Exception {

        // find the curated points containing the given time
        Integer fromTime = curatedMap.floorKey(time);
        if (fromTime == null) return;
        
        if (fromTime == time) ++time;
        Integer toTime = curatedMap.ceilingKey(time);
        if (toTime == null) return;
        
        // autolink between the curated points
        ArrayList<Nucleus[]> linkages = new ArrayList<>();
        Nucleus[] from = this.getNuclei(fromTime).toArray(new Nucleus[0]);
 //       Nucleus[] from = this.cloneTime(fromTime);
 //       linkages.add(from);
        for (int t=fromTime ; t<toTime ; ++t){
        if (t == 57){
            int sajhdfuisd=0;
        }            
            Nucleus[] to = Linkage.autoLinkage(from,t, bhcTreeDir);
            linkages.add(to);
            from = to;
        }
        
        // make a copy of the curated nuclei and remove them from the file
        TreeMap<String,Nucleus> curatedToNucs = byTime.get(toTime);
        TreeMap<String,Nucleus> clone = (TreeMap<String,Nucleus>)curatedToNucs.clone();
        for (Nucleus nuc : clone.values()){
            this.removeNucleus(nuc, false);
        }
         
        // put all the new linkages into this file 
        for (Nucleus[] link : linkages){
            for (Nucleus nuc : link){
                this.addNucleus(nuc);
            }
        } 
        curatedMap.put(toTime, true);
        
        //find new linked nuclei in last time point that do not match the curated nuclei and remove them and their ancestors
        Nucleus[] last = linkages.get(linkages.size()-1);    
        TreeSet<String> sourceNodesSet = new TreeSet<String>();
        for (Nucleus nuc : clone.values()){
            sourceNodesSet.add(((BHCNucleusData)nuc.getNucleusData()).getSourceNode());
        }
        for (int i=0 ; i<last.length ; ++i){
            Nucleus nuc = last[i];
            String sourceNode = ((BHCNucleusData)nuc.getNucleusData()).getSourceNode();
            if (!sourceNodesSet.contains(sourceNode)){
                // trim out the nucleus from the linkage
                removeAncestorsInCell(nuc);
            }
        }
        
        // put back any curated nuclei that did not correspond to new linked nuclei
        TreeSet<String> linkedNodeSet = new TreeSet<>();
        Set<Nucleus> toNucs = this.getNuclei(toTime);
        for (Nucleus toNuc : toNucs){
            linkedNodeSet.add(((BHCNucleusData)toNuc.getNucleusData()).getSourceNode());
        }
        for (Nucleus curatedNuc : clone.values()){
            String curatedNode = ((BHCNucleusData)curatedNuc.getNucleusData()).getSourceNode();
            if (!linkedNodeSet.contains(curatedNode)){
                this.addNucleus(curatedNuc);
            }
        }
        
        this.notifyListeners();
    }

        // remove all the ancestor nuclei in the cell containing this nucleus
    public void removeAncestorsInCell(Nucleus nuc){
        Nucleus par = nuc.getParent();
        if (par != null){
            if (!par.isDividing()){
                removeAncestorsInCell(par);
            }
        }
        removeNucleus(nuc,false);
    }

    
    File file;
    TreeMap<Integer,TreeMap<String,Nucleus>> byTime=new TreeMap<>();
    TreeMap<Integer,Boolean> curatedMap = new TreeMap<>();
//    TreeMap<Integer,Boolean> autoLinkedMap = new TreeMap<>();
    
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
    SelectedNucleus selectedNucleus = new SelectedNucleus();
    BHCTreeDirectory bhcTreeDir;
    
    boolean opening = false;
    static double threshold= 1.0E-11;




}
