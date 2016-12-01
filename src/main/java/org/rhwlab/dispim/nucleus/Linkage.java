/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.rhwlab.BHC.BHCTree;
import static org.rhwlab.dispim.nucleus.LinkedNucleusFile.threshold;

/**
 *
 * @author gevirl
 */
public class Linkage implements Comparable {
    
    public Linkage(Nucleus[] fromN,Nucleus[] toN){
        this.from = fromN;
        this.to = toN;
    }

    // automatic segmentation and linkage to next time point
    static Nucleus[] autoLinkage(Nucleus[] nucsToLink,int fromTime,BHCTreeDirectory bhcTreeDir)throws Exception {
        
        Nucleus[] currentFrom = cloneNuclei(nucsToLink);
        if (currentFrom.length == 0)return null;
        
        BHCTree nextTree = bhcTreeDir.getTree(fromTime+1);
        if (nextTree == null){
            return null; // no tree built for the to time
        }
        TreeMap<Integer,Double> probMap = new TreeMap<>();
        nextTree.allPosteriorProb(probMap);    
        int nextN = currentFrom.length;
        
        // find a probability at which to cur the nextTree
        double prob = probMap.get(nextN);
        while (prob < threshold){
            ++nextN;  // skipping cuts that have a probability less than the threshold
            prob = probMap.get(nextN);
        }  
        
        Nucleus[] currentTo = cutTreeToNuclei(nextTree,nextN);
        double nextProb = probMap.get(nextN);        
        Linkage current = new Linkage(currentFrom,currentTo);
        current.formLinkage();
        
        if (nextProb < .95){
            do {
                ++nextN;
                nextProb = probMap.get(nextN);
                Nucleus[] nextFrom = cloneNuclei(nucsToLink);
                Nucleus[] nextTo = cutTreeToNuclei(nextTree,nextN);
                Linkage next = new Linkage(nextFrom,nextTo);
                next.formLinkage();
                
                if (next.compareTo(current)>0 || next.getToNuclei().size() < nextN){
                    break;  // the next linkage got worse - stop 
                }
                current = next;
            } while (true && nextProb <.95 );
        }
        
        // fix the child links in the original from nuclei to point to the final to nuclei
        for (int i=0 ; i<nucsToLink.length ; ++i){
            nucsToLink[i].setDaughters(current.from[i].getChild1(),current.from[i].getChild2());
        }
        for (int i=0 ; i<current.to.length ; ++i){
            
        }
        
        return current.getTo();
    }
    static Nucleus[] cloneNuclei(Nucleus[] nucs){
        Nucleus[] ret = new Nucleus[nucs.length];
        for (int i=0 ; i<nucs.length ; ++i){
            ret[i] = nucs[i].clone();
        }
        return ret;
    }
    static private Nucleus[] cutTreeToNuclei(BHCTree nextTree,int n){
        BHCNucleusSet nextNucSet = nextTree.cutToN(n);
        Set<BHCNucleusData> nucData = nextNucSet.getNuclei();
        Nucleus[] toNucs = new Nucleus[nucData.size()];
        int i=0;
        for (BHCNucleusData nuc : nucData){
            toNucs[i] = new Nucleus(nuc);
            ++i;
        }    
        return toNucs;
    }
     public void formLinkage(){
        // link the polar bodies first
        this.linkPolarBodies();
        
        List<Nucleus> fromList = noChildren(from);
        List<Nucleus> toList = noParents(to);
        
        // link the divisions
        if (fromList.size() < toList.size()){
            HashMap<Nucleus,Division> best = Division.bestDivisions(fromList, toList);
            if (!best.isEmpty()){
                // link the best divisions, if any
                for (Division div : best.values()){
                    div.parent.linkTo(div.child1);
                    div.parent.linkTo(div.child2);
                }
                fromList = noChildren(from);
                toList = noParents(to);  
            }
        }
        
        // compute all pairwise distance between nuclei in the two adjacent time points
        Nucleus[] fromNucs = fromList.toArray(new Nucleus[0]);
        Nucleus[] toNucs = toList.toArray(new Nucleus[0]);
        double[][] dist = new double[fromNucs.length][];
        for (int r=0 ; r<dist.length ; ++r){
            dist[r] = new double[toNucs.length];
            for (int c=0 ; c<toNucs.length ; ++c){
                dist[r][c] = fromNucs[r].distance(toNucs[c]);  // this distance is weighted by intensity and volume
            }
        }
        
        // use Hungarian Algorithm to assign linking
        HungarianAlgorithm hungarian = new HungarianAlgorithm(dist);
        int[] linkage = hungarian.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                double d = fromNucs[i].distance(toNucs[linkage[i]]);
                if (d < distThresh){
                    fromNucs[i].linkTo(toNucs[linkage[i]]);

                    // if the from nuc is in a named cell , put child nuc in same cell
                    String cellname = fromNucs[i].getCellName();
                    toNucs[linkage[i]].setCellName(cellname,fromNucs[i].isUsernamed());
                }
            }
        }        
    }
    // make a list of nuclei that have no children
    static public  List<Nucleus> noChildren(Nucleus[] nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (nuc.getChild1()==null){
                ret.add(nuc);
            }
        }
        return ret;
    }
    static public List<Nucleus> noParents(Nucleus[] nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (nuc.getParent()==null){
                ret.add(nuc);
            }
        }
        return ret;        
    }

    @Override
    public int compareTo(Object o) {
        Linkage other = (Linkage)o;
        int ret = Integer.compare(this.getBirths().size(),other.getBirths().size());
        if (ret==0){
            ret = Integer.compare(this.getDeaths().size(), other.getDeaths().size());
        }
        return ret;
    }
    
    // get the roots  in the to nuclei
    public Set<Nucleus> getBirths(){
        TreeSet<Nucleus> ret = new TreeSet<>();
        for (Nucleus nuc : to){
            if (nuc.getParent() == null){
                ret.add(nuc);
            }
        }
        return ret;
    }
    public Set<Nucleus> getDeaths(){
        TreeSet<Nucleus> ret = new TreeSet<>();
        for (Nucleus nuc : from){
            if (nuc.nextNuclei().length == 0){
                ret.add(nuc);
            }
        }
        return ret;        
    }
    public TreeMap<String,Nucleus> getToNuclei(){
        TreeMap ret = new TreeMap<>();
        for (Nucleus nuc : to){
            ret.put(nuc.getName(), nuc);
        }
        return ret;
    }
    public TreeMap<String,Nucleus> getFromNuclei(){
        TreeMap ret = new TreeMap<>();
        for (Nucleus nuc : from){
            ret.put(nuc.getName(), nuc);
        }
        return ret;
    }
    public void linkPolarBodies(){
        // are any of the from nuclei labeled as polar
        ArrayList<Nucleus> polarFromList = new ArrayList<>();
        for (Nucleus nuc : from){
            if (nuc.getCellName().toLowerCase().startsWith("polar")){
                polarFromList.add(nuc);
            }
        }
        if (polarFromList.isEmpty()){
            return;
        }

        // find the same number of nuclei in the to list that are polar
        // sort the to nuclei by intensity density
        Arrays.sort(to, new Comparator(){
            @Override
            public int compare(Object o1, Object o2) {
                BHCNucleusData d1 = (BHCNucleusData)((Nucleus)o1).getNucleusData();
                BHCNucleusData d2 = (BHCNucleusData)((Nucleus)o2).getNucleusData();
                return -Double.compare(d1.intensityDensity,d2.intensityDensity);
            }
        });
        // compute distance array for polar bodies
        double d[][] = new double[polarFromList.size()][];
        for (int i=0 ; i<polarFromList.size() ; ++i){
            d[i] = new double[polarFromList.size()];
            for (int j=0 ; j<polarFromList.size() ; ++j){
                d[i][j] = polarFromList.get(i).distance(to[j]);
            }
        }
        
        // use Hungarian Algorithm to find best connection (overkill, but general in number of polar bodies)
        HungarianAlgorithm hung = new HungarianAlgorithm(d);
        int[] linkage = hung.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                polarFromList.get(i).linkTo(to[linkage[i]]);
                
                // if the from nuc is in a named cell , put child nuc in same cell
                String cellname = polarFromList.get(i).getCellName();
                to[linkage[i]].setCellName(cellname,polarFromList.get(i).isUsernamed());
            }
        }
    }
    public Nucleus[] getFrom(){
        return from;
    }
    public Nucleus[] getTo(){
        return to;
    }
    Nucleus[] from;
    Nucleus[] to;   
    
    static double distThresh = 75;
}
