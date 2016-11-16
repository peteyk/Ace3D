/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author gevirl
 */
public class Linkage implements Comparable {
    public Linkage(Nucleus[] from,Nucleus[] to){
        this.from = from;
        this.to = to;
        TreeSet<Nucleus> fromSet = new TreeSet<>();
        for (Nucleus f : from){
            fromSet.add(f);
        }
        TreeSet<Nucleus> toSet = new TreeSet<>();
        for (Nucleus t : to){
            toSet.add(t);
        }
        
        HashMap<Nucleus,Division> best = Division.bestDivisions(from, to);
        // link the best divisions, if any
        for (Division div : best.values()){
            div.parent.linkTo(div.child1);
            div.parent.linkTo(div.child2);
            fromSet.remove(div.parent);
            toSet.remove(div.child1);
            toSet.remove(div.child2);
        }
        
        // compute all pairwise distance between nuclei in the two adjacent time points
        Nucleus[] fromNucs = fromSet.toArray(new Nucleus[0]);
        Nucleus[] toNucs = toSet.toArray(new Nucleus[0]);
        double[][] dist = new double[fromNucs.length][];
        for (int r=0 ; r<dist.length ; ++r){
            dist[r] = new double[toNucs.length];
            for (int c=0 ; c<toNucs.length ; ++c){
                dist[r][c] = fromNucs[r].distance(toNucs[c]);
            }
        }
        
        // use Hungarian Algorithm to assign linking
        HungarianAlgorithm hungarian = new HungarianAlgorithm(dist);
        int[] linkage = hungarian.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                fromNucs[i].linkTo(toNucs[linkage[i]]);
                
                // if the from nuc is in a named cell , put child nuc in same cell
                String cellname = fromNucs[i].getCell();
                if (cellname != null){
                    toNucs[i].setCell(cellname);
                }
            }
        }
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
    public TreeSet<Nucleus> getToNuclei(){
        return new TreeSet<>(Arrays.asList(to));
    }
    public TreeSet<Nucleus> getFromNuclei(){
        return new TreeSet<>(Arrays.asList(from));
    }
    Nucleus[] from;
    Nucleus[] to;    
}
