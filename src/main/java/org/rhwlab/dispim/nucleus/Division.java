/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class Division {
    public Division(Nucleus parent,Nucleus child1,Nucleus child2){
        this.parent = parent;
        this.child1 = child1;
        this.child2 = child2;
        dist = this.parent.distance(this.child1) + this.parent.distance(this.child2);
    }
    // determine if this is a possible division
    public boolean isPossible(){
        System.out.printf("P:%s  C1:%s   C2:%s  ",parent.getName(),child1.getName(),child2.getName());
        
        if (parent.getCellName().contains("Polar")){
            System.out.println("Polar");
            return false;  // polar bodies do not divide
        }
        
        if (dist > distThresh){
            System.out.println("Distance");
            return false;  // daughters are to far from parent
        }   
        
        double ratio = ((BHCNucleusData)child1.getNucleusData()).getVolume()/((BHCNucleusData)child2.getNucleusData()).getVolume();
        if (ratio < 1.0) {
            ratio = 1.0/ratio;
        }
        if (ratio > volumeThresh){
            System.out.println("Volume");
            return false;
        }
        
        double intensityRatio = ((BHCNucleusData)parent.getNucleusData()).getAverageIntensity()/((BHCNucleusData)child1.getNucleusData()).getAverageIntensity();
        if (intensityRatio < 1.0){
            intensityRatio = 1.0/intensityRatio;
        }
        if (intensityRatio > intensityThresh){
            System.out.println("Intensity child1");
            return false;
        }
        intensityRatio = ((BHCNucleusData)parent.getNucleusData()).getAverageIntensity()/((BHCNucleusData)child2.getNucleusData()).getAverageIntensity();
        if (intensityRatio < 1.0){
            intensityRatio = 1.0/intensityRatio;
        }
        if (intensityRatio > intensityThresh){
            System.out.println("Intensity child2");
            return false;
        }        

        int lastDiv = parent.timeSinceDivsion();
        if ( lastDiv != -1 && lastDiv < timeThresh){
            System.out.println("Time");
            return false;  // lifetime of cell is too short for another division
        }
        double[] ecc = parent.eccentricity();
        if (ecc[1] < eccThresh){
            System.out.println("Parent Eccentricity");
            return false;  // nuclei are not eccentric enough            
        }
        ecc = child1.eccentricity();
        if (ecc[1] < eccThresh){
            System.out.println("Child1 Eccentricity");
            return false;  // nuclei are not eccentric enough            
        }        
        ecc = child2.eccentricity();
        if (ecc[1] < eccThresh){
            System.out.println("Child2 Eccentricity");
            return false;  // nuclei are not eccentric enough            
        }
/*        
        RealVector[] parentAxes = parent.getAxes();
        RealVector[] child1Axes = child1.getAxes();
        RealVector[] child2Axes = child2.getAxes();
        // the children's major axes must be close
        if (!related(child1Axes[0],child2Axes[0])){
            System.out.println("Axes");
            return false;
        }
        
        // one of the  major axes of parent must be close to the major axis of both children
        if (!( (related(parentAxes[1],child1Axes[2])&&related(parentAxes[1],child2Axes[2])) ||
                (related(parentAxes[2],child1Axes[2])&&related(parentAxes[2],child2Axes[2])) )){
            return false;
        }
 */     System.out.println("Accepted");
        return true;
    }
    private boolean related(RealVector axis1,RealVector axis2){
        double cos = axis1.dotProduct(axis2);
        boolean ret =  cos >= cosThresh ;
        if (!ret){
            System.out.printf("Cosine: %f\n",cos);
            
        }
        return ret;
    }
    
    public double getDistance(){
        return dist;
    }

    // determine if a set of divisions is consistent
    // ie - can all exist at the same time
    // it is assumed that the parents are already unique in the set
    static public boolean isConsistent(Set<Division> divisions){
        // to be consistent, no two parents go to the same child
        Set<Nucleus> unique = new HashSet<>();
        for (Division div : divisions){
            unique.add(div.child1);
            unique.add(div.child2);        
        }
        return unique.size() == 2*divisions.size();
    }
    // make all possible divisions from a given nuclues
    static public Set<Division> possibleDivisions(Nucleus from,Nucleus[] to){
        HashSet<Division> ret = new HashSet<>();
        for (int i=0 ; i<to.length-1 ; ++i){
            if (to[i] != null){
                for (int j=i+1 ; j<to.length ; ++j){
                    if (to[j] != null){
                        Division div = new Division(from,to[i],to[j]);
                        if (div.isPossible()){
                            ret.add(div);
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    // determine which nuclei could be a parent in a divison
    static public List<Nucleus> possibleParents(List<Nucleus> nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (possibleParent(nuc)){
                ret.add(nuc);
            }
        }
        return ret;
    }
    static public boolean possibleParent(Nucleus nuc){
        double[] ecc = nuc.eccentricity();
        if (nuc.getTime() < 25) {
            return true;
        }
        return ecc[1]>eccThresh;
    }
    // decide on best divisions
    static public HashMap<Nucleus,Division> bestDivisions(List<Nucleus> fromList,List<Nucleus> toList){
        
        List<Nucleus> possibleParentList = possibleParents(fromList);
        
        // determine all possible divisions from each possible parent
        HashMap<Nucleus,Set<Division>> possibleFrom = new HashMap<>(); 
        for (Nucleus possibleParent : possibleParentList){
            Set<Division> possibleDivs = possibleDivisions(possibleParent,toList.toArray(new Nucleus[0]));
            if (!possibleDivs.isEmpty()){
                possibleFrom.put(possibleParent, possibleDivs);
            }
        }
        
        // determine all the divsions ending on each 'to' nucleus
        HashMap<Nucleus,Set<Division>> possibleTo = new HashMap<>();
        for (Nucleus fromNuc : possibleFrom.keySet()){
            Set<Division> divs = possibleFrom.get(fromNuc);
            for (Division div : divs){
                Nucleus to1 = div.child1;
                Nucleus to2 = div.child2;
                
                Set<Division> toDivs = possibleTo.get(to1);
                if (toDivs == null){
                    toDivs = new HashSet<>();
                    possibleTo.put(to1, toDivs);
                }
                toDivs.add(div);
                
                toDivs = possibleTo.get(to2);
                if (toDivs == null){
                    toDivs = new HashSet<>();
                    possibleTo.put(to2, toDivs);
                }
                toDivs.add(div);                
            }
        }
        // use the 'best' division for each 'to' nucleus
        HashMap<Nucleus,Division> ret = new HashMap<>();
        while (!possibleTo.isEmpty()){
            Nucleus first = possibleTo.keySet().iterator().next();
            Set<Division> divs = possibleTo.get(first);
            Division best = bestDivision(divs);
            ret.put(first,best);
            
            // remove any divisions that are no longer possible
            possibleTo.remove(best.child1);
            possibleTo.remove(best.child2);
        }
        

        return ret;
    }
    static private Division bestDivision(Set<Division> divs){
        Division ret = null;
        double minD = Double.MAX_VALUE;
        for (Division div : divs){
            double d = div.getDistance();
            if ( d < minD){
                minD = d;
                ret = div;
            }
        }
        return ret;
    }
    Nucleus parent;
    Nucleus child1;
    Nucleus child2;
    double dist;
    
    static int timeThresh = 10;
    static double eccThresh = 0.7;
    static double distThresh = 50.0;
    static double cosThresh = .8;
    static double volumeThresh = 3.0;
    static double intensityThresh = 5.0;  // ratio of average intensity 
}
