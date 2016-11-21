/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.BHC.Node;

/**
 *
 * @author gevirl
 */
public class BHCNucleusSet {
    public BHCNucleusSet(){
        nuclei = new HashSet<>();
    }
    public BHCNucleusSet(int time,String treeFile,double thresh,BHCNucleusData[] nucs){
        this();
        this.time = time;
        this.cutThreshold = thresh;
        this.treeFile = treeFile;
        
        for (BHCNucleusData nuc : nucs){
            this.nuclei.add(nuc);
        }
    }
    public BHCNucleusSet(Element root){
        init(root);
    }

    public void init(Element document){
        nuclei = new HashSet<>();
        this.cutThreshold = Double.valueOf(document.getAttributeValue("threshold"));
        this.time = Integer.valueOf(document.getAttributeValue("time"));
        this.treeFile = document.getAttributeValue("treefile");
        List<Element> gmmList = document.getChildren("GaussianMixtureModel");
        for (Element gmm : gmmList){
            BHCNucleusData tgmmNuc = new BHCNucleusData(time,gmm);
            nuclei.add(tgmmNuc);
        }        
    }

    public double getThreshold(){
        return this.cutThreshold;
    }
    public void setThreshold(double th){
        this.cutThreshold = th;
    }

    public Set<BHCNucleusData> getNuclei(){
        return this.nuclei;
    }
    // returns null if no outliers were removed
    // otherwise returns with a resegmented time point
    public BHCNucleusSet cutTreeOutlier(BHCTree tree){
        BHCNucleusSet ret = null;
        
        double[] intensities = new double[nuclei.size()];
        int i=0;
        for (BHCNucleusData nuc : nuclei){
            intensities[i] = nuc.getAverageIntensity();
            ++i;
        }
        double intensityThresh = 5.0*StatUtils.percentile(intensities, 0.5);
        
        ArrayList<Double> volList = new ArrayList<>();
        i = 0;
        for (BHCNucleusData nuc : nuclei){
            if (intensities[i] < intensityThresh){  // not includeing the Polar bodies
                volList.add(nuc.getVolume());
            }
            ++i;
        }   
        double[] volumes  = new double[volList.size()];
        i = 0;
        for (Double vol : volList){
            volumes[i] = Math.log(vol);
            ++i;
        }
        double mean = StatUtils.mean(volumes);
        double variance = StatUtils.variance(volumes, mean);
        double sd = Math.sqrt(variance);
        NormalDistribution norm = new NormalDistribution(mean,sd);
        double[] density = new double[volumes.length];
        for (int j=0 ; j< density.length ; ++j){
            density[j] = norm.density(volumes[j]);
        }
        double lower = StatUtils.percentile(volumes,25);
        double upper = StatUtils.percentile(volumes, 75);
        double iqr =  upper - lower ;
        double volThresh = lower - 1.5*iqr;
        int outliers = 0;
        for (double vol : volumes){
            if (vol < volThresh){
                ++outliers;
            }
        }
        
        // recut to remove the outliers
        TreeSet<Double> posts = tree.allPosteriors();
        double current = cutThreshold;
        int nextSize = nuclei.size();
        while (nuclei.size()-nextSize < outliers){
            double next = posts.lower(current);
            Element nextEle = tree.cutTreeAtThreshold(next);
            ret = new BHCNucleusSet(nextEle);
            nextSize = ret.getNuclei().size();
            current = next;
        }
        return ret;
    }   
    public int getTime(){
        return time;
    }
    Set<BHCNucleusData> nuclei;
    int time;   
    double cutThreshold;  // posterior probability generating this set of nuclei
    String treeFile;  // the BHCTree source of these nuclei
    boolean curated;  // will be true is the nuclei were human generated, then can only be changed interactively
}
