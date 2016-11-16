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
public class BHCNucleusFile {
    public BHCNucleusFile(int time,String treeFile,double thresh,BHCNucleusData[] nucs){
        this.time = time;
        this.cutThreshold = thresh;
        this.treeFile = treeFile;
        nuclei = new HashSet<>();
        for (BHCNucleusData nuc : nucs){
            this.nuclei.add(nuc);
        }
    }
    public BHCNucleusFile(Element root){
        init(root);
    }
    // open a tgmm nucleus file , adding nuclei to the Ace3dNucleusFile
    public BHCNucleusFile(File file)throws Exception {
        this.file = file;
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(file); 
        Element document = doc.getRootElement(); 
        init(document);
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
    public File getBHCTreeFile(){
        String fileName = file.getName();
        return new File(file.getParent(),fileName.replace(".xml", "BHCTree.xml"));
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
    public BHCNucleusFile cutTreeOutlier(BHCTree tree){
        BHCNucleusFile ret = null;
        
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
            ret = new BHCNucleusFile(nextEle);
            nextSize = ret.getNuclei().size();
            current = next;
        }
        return ret;
    }   
    public int getTime(){
        return time;
    }
    Set<BHCNucleusData> nuclei;
    File file;  // file where nuclei are recorded
    int time;   
    double cutThreshold;  // posterior probability generating this set of nuclei
    String treeFile;  // the BHCTree source of these nuclei
    boolean curated;  // will be true is the nuclei were human generated, then can only be changed interactively
}
