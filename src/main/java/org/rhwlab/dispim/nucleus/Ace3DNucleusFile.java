/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.rhwlab.ace3d.SelectedNucleusFrame;
import org.rhwlab.ace3d.SynchronizedMultipleSlicePanel;

/**
 *
 * @author gevirl
 */
public class Ace3DNucleusFile implements NucleusFile,javafx.beans.Observable   {
    public Ace3DNucleusFile(SynchronizedMultipleSlicePanel panel,SelectedNucleusFrame frame){
        this.panel = panel;
        this.frame = frame;        
    }
    public Ace3DNucleusFile(File file,SynchronizedMultipleSlicePanel panel,SelectedNucleusFrame frame){
        this(panel,frame);
        this.file =file;

    }
    @Override
    public void open() throws Exception {
        this.opening = true;
        JsonReader reader = Json.createReader(new FileReader(file));
        JsonObject obj = reader.readObject();
        JsonArray jsonNucs = obj.getJsonArray("Nuclei");
        for (int n=0 ; n<jsonNucs.size() ; ++n){
            JsonObject jsonNuc = jsonNucs.getJsonObject(n);
            Nucleus nuc = new BHC_Nucleus(jsonNuc);
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
    public void addBHC(BHC_NucleusFile bhc){
        for (BHC_Nucleus rootNuc : bhc.getNuclei()){
            this.addNucleus(rootNuc,false);
            Cell rootCell = new Cell(rootNuc.getName());
            rootCell.addNucleus(rootNuc);
            this.addRoot(rootCell,false);
        }        
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
        cell.parent = null;
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

        TreeSet<Nucleus> timeSet = byTime.get(nuc.getTime());
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
 
    // unkink a cell from its children
    public void unLinkCellFromChildren(Cell cell,boolean notify){
        Cell[] children = cell.getChildren();
        for (Cell child : children){
            this.addRoot(child,false);
        }
        cell.clearChildren();
        if (notify){
            this.notifyListeners();
        }
    }
    // unlink all the nuclei in a gven time
    public void unlinkTime(int t){
        System.out.print("Before unlinking\n");
        report(System.out,t);
        System.out.printf("Unlinking time: %d\n", t);
        TreeSet<Nucleus> nucs = this.getNuclei(t);
        for (Nucleus nuc : nucs.descendingSet()){
            this.unlink(nuc,false);
            System.out.printf("After unlinking nucleus: %s\n",nuc.getName());
            this.report(System.out, t);
            System.out.printf("Roots at time %d\n",t+1);
            for (Cell r : this.roots.get(t+1)){
                r.report(System.out);
            }
        }
        this.notifyListeners();
    }

    // completely unlink a nucleus from any children (in time or due to division)
    public void unlink(Nucleus nuc,boolean notify){
        Nucleus[] children = nuc.nextNuclei();
        if (children.length == 0){
            return ;  // nothing to unlink
        }
        
        if (children.length==1){
            // not a division - both nuclei in the same cell
            // split the cell and make a new root with the next nucleus
            Cell cell = nuc.getCell();
            Cell splitCell = cell.split(children[0].getTime());
            this.addRoot(splitCell,false);
        } else {
            // unlinking a division - 
            Cell cell = nuc.getCell();
            for (Cell child : cell.getChildren()){
                this.addRoot(child, notify);
            }
            cell.clearChildren();
            
        }
        if (notify){
            this.notifyListeners();
        }
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
    public TreeSet<Nucleus> getNuclei(int time){
        TreeSet<Nucleus> ret = (TreeSet)byTime.get(time);
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
    public void removeNucleiAtTime(int time){
        // make sure all nuclei are unlinked BHC nuclei
        Set<Nucleus> nuclei = this.byTime.get(time);
        for (Nucleus nuc : nuclei){
            if (!(nuc instanceof BHC_Nucleus)){
                System.err.println("cannot remove non BHC nuclei");
                System.exit(1);
            }
        }
        for (Nucleus nuc : nuclei){
            this.unlink(nuc, false);
            String cellName = nuc.getCell().getName();
            cellMap.remove(cellName);
            String nucName = nuc.getName();
            byName.remove(nucName);
            byTime.remove(time);
            roots.remove(time);
        }
        this.notifyListeners(); 
    }   

    
    public void linkTimePoint(int fromTime){
System.out.printf("Linking time: %d\n", fromTime);
        Nucleus[] fromNucs = byTime.get(fromTime).toArray(new Nucleus[0]);
        Integer[] fromNN = new Integer[fromNucs.length];
        Set<Nucleus> toNucsSet = byTime.get(fromTime+1);
        if (toNucsSet == null || toNucsSet.isEmpty()){
            return;
        }
        Nucleus[] toNucs = byTime.get(fromTime+1).toArray(new Nucleus[0]);
        Integer[] toNN = new Integer[toNucs.length];

        double[][] dist = new double[fromNucs.length][];
        double[][] shape = new double[fromNucs.length][];
        int fromRemaining = fromNucs.length;
        int toRemaining = toNucs.length;
        
        // compute all pairwise distance and shape difference between nuclei in the two adjacent time points
        for (int r=0 ; r<dist.length ; ++r){
            dist[r] = new double[toNucs.length];
            shape[r] = new double[toNucs.length];
            for (int c=0 ; c<toNucs.length ; ++c){
                if (r == 5 && c==3){
                    int sfduisd=0;
                }
                shape[r][c] = fromNucs[r].shapeDistance(toNucs[c]);
                dist[r][c] = fromNucs[r].distance(toNucs[c]);
            }
        }
        boolean changed = true;
        while (fromRemaining>0 && toRemaining >0 && changed){
            changed = false;
            
            // find the nearest neighbor for each nucleus in both time points
            for (int r=0 ; r<fromNucs.length ; ++r){
                fromNN[r] = null;
                if (fromNucs[r]!=null){
                    
                    double minD = Double.MAX_VALUE;
                    for (int c=0 ; c<toNucs.length ; ++c){
                        if (toNucs[c]!=null){
                            if (minD > dist[r][c] && shape[r][c]<=shapeThreshold){
                                minD = dist[r][c];
                                fromNN[r] = c;
                            }
                        }
                    }
                }
            }
            for (int c=0 ; c<toNucs.length ; ++c){
                toNN[c] = null;
                if (toNucs[c]!=null){
                    toNN[c] = null;
                    double minD = Double.MAX_VALUE;
                    for (int r=0 ; r<fromNucs.length ; ++r){
                        if (fromNucs[r]!=null){
                            if (minD > dist[r][c] && shape[r][c]<=shapeThreshold){
                                minD = dist[r][c];
                                toNN[c] = r;
                            }
                        }
                    }
                }
            } 
 
            
            // link the closely associated nuclei
            for (int r=0 ; r<fromNucs.length ; ++r){
                if (fromNucs[r] != null) {
                    Integer toIndex = fromNN[r];
                    // is this nearest neighbor unique - no other from Nucleus points to it
                    for (int i=0 ; i<fromNN.length ;++i){
                        if (fromNN[i] != null){
                            if (i != r){
                                if (fromNN[i] == toIndex){
                                    // not unique - cannot be linked
                                    toIndex = -1;
                                    break;
                                }
                            }
                        }
                    }
                    if (toIndex !=-1  && toNN[toIndex]==r){
                        // make sure the from Nucleus is not a nearest neighbor of any other to Nucleus
                        boolean linkable = true;
                        for (int i=0 ; i<toNN.length ; ++i){
                            if (toNN[i] != null){
                                if (i != toIndex){
                                    if (toNN[i] == r){
                                        // not unique
                                        linkable = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (linkable){
                            this.linkInTime(fromNucs[r], toNucs[toIndex]);
                            fromNucs[r] = null;
                            fromNN[r] = null;
                            toNucs[toIndex] = null;
                            toNN[toIndex]=null;
                            --fromRemaining;
                            --toRemaining;
                            changed = true;
                        }
                    }
                }
            }

            if (toRemaining <= fromRemaining){
                // see if there is a non-exclusive close association
                for (int r=0 ; r<fromNucs.length ; ++r){
                    if (fromNucs[r] != null) {
                        Integer toIndex = fromNN[r];
                        if (toIndex != -1 && toNN[toIndex]!=null && toNN[toIndex] == r){
                            // can be linked
                            this.linkInTime(fromNucs[r], toNucs[toIndex]);
                            fromNucs[r] = null;
                            fromNN[r] = null;
                            toNucs[toIndex] = null;
                            toNN[toIndex]=null;
                            --fromRemaining;
                            --toRemaining;
                            changed = true;
                        }
                    }
                }
                
            }
            
            // resolve the divisions
            if (toRemaining > fromRemaining){
                int nd = Math.min(fromRemaining,toRemaining-fromRemaining);
                int divisions = this.linkDivisions(fromNucs, toNucs,nd ,areaThreshold);
                toRemaining = toRemaining - 2*divisions;
                fromRemaining = fromRemaining -divisions;
                if (divisions>0 ){
                    changed = true;
                }
            }             
        }
    }
    // try to link a given number of divisions - must meet area threshold criteria
    // return the number of divisions linked
    public int linkDivisions(Nucleus[] fromNucs,Nucleus[] toNucs,int nDivisions,double areaThreshold){
        int ret = 0;
        // try to form nDivisions
        for (int n=0 ;n<nDivisions ; ++n){
        
            // compute the area of triangles
            TreeMap<Double,Integer[]> map = new TreeMap<>();
            for (int k=0 ; k<fromNucs.length ; ++k){
                Nucleus from = fromNucs[k];
                if (from != null){
                    for (int i=0 ; i<toNucs.length ; ++i){
                        if (toNucs[i] != null){
                            for (int j=i+1 ; j<toNucs.length ; ++j){
                                if (toNucs[j] != null){
                                    double a = from.distance(toNucs[i]);
                                    double b = from.distance(toNucs[j]);
                                    double c = toNucs[i].distance(toNucs[j]);
//System.out.printf("from:%d to:%d to%d a=%f b=%f c=%f \n",k,i,j,a,b,c);                                    
//                                    double area = HeronFormula(a,b,c);
                        
                                    Integer[] division = new Integer[3];
                                    division[0] = k;
                                    division[1] = i;
                                    division[2] = j;
                                    map.put(a+b+c,division);
                                }
                            }
                        }
                    }
                }
            }
        
            // link the divsions with minimal area
            Double area = map.firstKey();
            if (area <=areaThreshold){
                Integer[] division = map.get(area);
                this.linkDivision(fromNucs[division[0]], toNucs[division[1]], toNucs[division[2]]);
                fromNucs[division[0]] = null;
                toNucs[division[1]] = null;
                toNucs[division[2]] = null;
                ++ret;
                if (ret == nDivisions ){
                    return ret;
                }
            }
        }
        return ret;
    }

    // stable calculation of the area of a triangle given the length of the sides
    static double HeronFormula(double a,double b,double c){
        double s = (a + b + c)/2.0;
        double sa = s-a;
        double sb = s-b;
        double sc = s-c;
        double a2 = s*sa*sb*sc;
        double area = Math.sqrt(a2);
        
        System.out.printf("s=%f s-a=%f s-b=%f s-c=%f a2=%f area=%f\n\n",s,sa,sb,sc,a2,area);
        return area;
/*        
        double[] x = new double[3];
        x[0] = a;
        x[1] = b;
        x[2] = c;
        Arrays.sort(x);
        double s = (x[0]+(x[1]+x[2])) * (x[2]-(x[0]-x[1])) * (x[2]+(x[0]-x[1])) * (x[0]+(x[1]-x[2])) ;
        return 0.25*Math.sqrt(s);
*/
    }
    //report all connections at a given time 
    public void report(PrintStream stream,int time){
        stream.printf("Reporting Time:%d\n",time);
        // report the roots
        Set<Cell> rootCells = roots.get(time);
        if (rootCells.isEmpty()){
            stream.println("No root cells");
        }else {
            for (Cell root : roots.get(time)){
                root.report(stream);
            }
        }
        for (Nucleus nuc : byTime.get(time)){
            nuc.report(stream);
        }
    }
    // rename the cell containing the selected nucleus
    public void renameCell(String newName){
        Cell selectedCell = selectedNucleus.getCell();
        String oldName = selectedCell.name;
        selectedCell.setName(newName);
        cellMap.put(newName, selectedCell);
        cellMap.remove(oldName);
    }
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame frame;
    boolean opening = true;
    File file;
    Nucleus selectedNucleus;
    TreeMap<Integer,Set<Cell>> roots = new TreeMap<>();  // root cells indexed by time
    TreeMap<String,Cell> cellMap = new TreeMap<>();  // map of the all the cells by name
    TreeMap<Integer,TreeSet<Nucleus>> byTime = new TreeMap<>();  // all the nuclei present at a given time
    TreeMap<String,Nucleus> byName = new TreeMap<>();  // map of nuclei indexed by name, map indexed by time
    ArrayList<InvalidationListener> listeners = new ArrayList<>();

    double shapeThreshold=25;
    double areaThreshold=200;

}
