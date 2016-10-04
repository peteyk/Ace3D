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
 
    // completely unlink a nucleus from any children (in time or due to division)
    public void unlink(Nucleus nuc){
        // is the nucleus dividing ?
        if (nuc.isDividing()){
            
        }else {
            
        }
    }
    public void unlink(Nucleus from,Nucleus to){
        Cell fromCell = from.getCell();
        Cell toCell = to.getCell(); 
        if (fromCell == null || toCell == null) return;  // they can't be linked if one is not in a cell
        
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
            String cellName = nuc.getCell().getName();
            cellMap.remove(cellName);
            String nucName = nuc.getName();
            byName.remove(nucName);
            byTime.remove(time);
            roots.remove(time);
        }
        this.notifyListeners(); 
    }   
    public void linkTimePoint(int fromTime,double shapeThreshold,double areaThreshold){
        Nucleus[] fromNucs = byTime.get(fromTime).toArray(new Nucleus[0]);
        Integer[] fromNN = new Integer[fromNucs.length];
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
                shape[r][c] = fromNucs[r].shapeDistance(toNucs[c]);
                dist[r][c] = fromNucs[r].distance(toNucs[c]);
            }
        }
        
        while (fromRemaining>0 && toRemaining >0){
        
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
            
            // find the clearly linkable nuclei by seeing if the nerestneighbors are exclusive and mutual
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
                            toNucs[toIndex] = null;
                            --fromRemaining;
                            --toRemaining;
                        }
                    }
                }
            }
            if (toRemaining > fromRemaining){
                int divisions = this.linkDivisions(fromNucs, toNucs, toRemaining-fromRemaining,areaThreshold);
                toRemaining = toRemaining - 2*divisions;
                fromRemaining = fromRemaining -divisions;
            }
        }
    }
    // try to link a given number of divisions - must meet area threshold criteria
    // return the number of divisions linked
    public int linkDivisions(Nucleus[] fromNucs,Nucleus[] toNucs,int nDivisions,double areaThreshold){
        int ret = 0;
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
                                double area = HeronFormula(a,b,c);
                                Integer[] division = new Integer[3];
                                division[0] = k;
                                division[1] = i;
                                division[2] = j;
                                map.put(area,division);
                            }
                        }
                    }
                }
            }
        }
        
        // link the divsions with minimal area
        for (Double area : map.navigableKeySet()){
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
            } else {
                break;
            }
        }
        return ret;
    }

  /*  
    // try to form all the nuclei linkages from the given time
    // it is assumed that everything is unlinked in this method
    public void linkTimePointsGraph(int fromTime){
        Set<Nucleus> fromNucs = byTime.get(fromTime);
        Set<Nucleus> toNucs = byTime.get(fromTime+1);
        
        // make a complete neighbor graph 
        // the edge represents the distance between each pair of nuclei
        // all the pairwise distances are computed
        SimpleWeightedGraph simpleGraph = new SimpleWeightedGraph(DefaultWeightedEdge.class);
        for (Nucleus from : fromNucs){
            simpleGraph.addVertex(from);
        }
        for (Nucleus to : toNucs){
            simpleGraph.addVertex(to);
        }
        for (Nucleus from : fromNucs){
            for (Nucleus to : toNucs){
                DefaultWeightedEdge edge = (DefaultWeightedEdge)simpleGraph.addEdge(from, to);
                simpleGraph.setEdgeWeight(edge, from.distance(to));
            }
        }
        
        // make a nearest neighbor graph where the directed edge goes to the nearest neighbor in the other time point
        DefaultDirectedWeightedGraph nnGraph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);
        for (Nucleus from : fromNucs){
            Nucleus nn = nearestNucleus(from,simpleGraph);
            nnGraph.addVertex(from);
            nnGraph.addVertex(nn);
            nnGraph.addEdge(from, nn);
        }
        for (Nucleus to : toNucs){
            Nucleus nn = nearestNucleus(to,simpleGraph);
            nnGraph.addVertex(to);
            nnGraph.addVertex(nn);
            nnGraph.addEdge(to, nn);
        }

        // find linkable nuclei
        for (Nucleus from : fromNucs){
            Set edges = nnGraph.outgoingEdgesOf(from);
            if (edges.size() != 1){
                System.err.printf("Error: more than one nearest neighbor for %s\n",from.getName());
                System.exit(1);
            }
            Object outEdge = edges.iterator().next();
            Nucleus to = (Nucleus)nnGraph.getEdgeTarget(outEdge);
            int n = nnGraph.outDegreeOf(to);
            if (n == 1){
                
            }
        }
        HashMap<Nucleus,TreeMap<Double,Nucleus>> fromMap = new HashMap<>();
        HashMap<Nucleus,TreeMap<Double,Nucleus>> toMap = new HashMap<>();
        

    }
    private Nucleus nearestNucleus(Nucleus from,SimpleWeightedGraph g){
        Set edges = g.outgoingEdgesOf(from);
        Iterator iter = edges.iterator();
        double minD = Double.MAX_VALUE;
        Nucleus nn = null;
        while (iter.hasNext()){
            Object edge = iter.next();
            double d = g.getEdgeWeight(edge);
            if (d < minD){
                minD = d;
                nn = (Nucleus)g.getEdgeTarget(edge);
            }
        }
        return nn;
    }
*/
    // stable calculation of the area of a triangle given the length of the sides
    static double HeronFormula(double a,double b,double c){
        double[] x = new double[3];
        x[0] = a;
        x[1] = b;
        x[2] = c;
        Arrays.sort(x);
        double s = (x[0]+(x[1]+x[2])) * (x[2]-(x[0]-x[1])) * (x[2]+(x[0]-x[1])) * (x[0]+(x[1]-x[2])) ;
        return 0.25*Math.sqrt(s);
    }
    
    SynchronizedMultipleSlicePanel panel;
    SelectedNucleusFrame frame;
    boolean opening = true;
    File file;
    Nucleus selectedNucleus;
    TreeMap<Integer,Set<Cell>> roots = new TreeMap<>();  // root cells indexed by time
    TreeMap<String,Cell> cellMap = new TreeMap<>();  // map of the all the cells by name
    TreeMap<Integer,Set<Nucleus>> byTime = new TreeMap<>();  // all the nuclei present at a given time
    TreeMap<String,Nucleus> byName = new TreeMap<>();  // map of nuclei indexed by name, map indexed by time
    ArrayList<InvalidationListener> listeners = new ArrayList<>();



}
