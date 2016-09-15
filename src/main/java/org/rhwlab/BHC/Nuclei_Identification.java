/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rhwlab.dispim.datasource.ClusteredDataSource;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;

/**
 *
 * @author gevirl
 */
public class Nuclei_Identification implements Runnable {
    public Nuclei_Identification(String segmentedTiffFile,boolean force){
        this.segmentedTiff = segmentedTiffFile;
        this.force = force;
        tiffFile = new File(segmentedTiffFile);
        // parse the time from the filename
        Pattern p = Pattern.compile("TL(\\d{3})");
        Matcher m = p.matcher(tiffFile.getName());
        boolean matched = m.find();
        if (matched){
            time = Integer.valueOf(m.group(1));
        }
    }
    @Override
    public void run() {
        int nClusters=0;;
        SegmentedTiffDataSource segSource = new SegmentedTiffDataSource(segmentedTiff,backgroundSegment);
        int nVoxels = segSource.getN(nucleiSegment);        
        // have the micro clusters been formed?
        String name = tiffFile.getName();
        String baseName = name.substring(0, name.toLowerCase().indexOf(".tif"));
        String microClusterFileName = baseName + "Clusters.xml";
        File microClusterFile = new File(tiffFile.getParent(),microClusterFileName);
        if (!microClusterFile.exists()  || force){

            
            try {
                CellCounts cc = new CellCounts();
                int cells = cc.getCellCount(time);
                nClusters = 50*cells;
                int nPartitions = Math.max(1,(int)Math.pow(nClusters/1000.0,1.0/3.0));                
                clusterDataSource = segSource.kMeansCluster(nucleiSegment, nClusters, nPartitions);
                clusterDataSource.saveAsXML(microClusterFile.getPath());
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(1);
            }
        }
        
        String BHCTreeFileName = baseName+"BHCTree.xml";
        File BHCTreeFile = new File(tiffFile.getParent(),BHCTreeFileName);
        if (!BHCTreeFile.exists() || force){
            try {
                MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
                nClusters = microDataSource.getK();
                ThreadedAlgorithm alg = new ThreadedAlgorithm();
                alg.setSource(microDataSource);
                CellCounts cc = new CellCounts();
                double f = 2.0*(cc.getCellCount(time));
                double alpha = 2.0*nClusters;
                alg.init(alpha);
                alg.run();
                alg.saveResultAsXML(BHCTreeFile.getPath());
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(2);
            }
        }
        
        File gmmFile = new File(tiffFile.getParent(),baseName+".xml");
        try {
            List<Node> list = BHCTree.readTreeXML(BHCTreeFile.getPath());
            BHCTree.saveClusterListAsXML(gmmFile.getPath(), list, .0001);
        } catch (Exception exc){
            exc.printStackTrace();
            System.exit(3);
        }
    }
    
    static public void main(String[] args)throws Exception {
        
                Nuclei_Identification objectID = new Nuclei_Identification("/net/waterston/vol2/home/gevirl/rnt-1/segmented/img_TL017_Simple_Segmentation.tiff",true);
                objectID.run();
/*                
        File dir = new File("/net/waterston/vol2/home/gevirl/rnt-1/segmented");
        for (File file : dir.listFiles()){
            System.out.println(file.getPath());
            if (file.getName().equals("img_TL016_Simple_Segmentation.tiff")){
                Nuclei_Identification objectID = new Nuclei_Identification(file.getPath(),true);
                objectID.run();
            }
        }
*/
        int iuasdfisd=0;
    }
    String segmentedTiff;
    File tiffFile;
    int time=-1;
    boolean force;
    ClusteredDataSource clusterDataSource;
    int backgroundSegment = 1;
    int nucleiSegment = 2;
}
