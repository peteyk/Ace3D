/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;

/**
 *
 * @author gevirl
 */
public class Nuclei_Identification implements Runnable {
    public Nuclei_Identification(String segmentedTiff,boolean force){
        this.segmentedTiff = segmentedTiff;
        File file = new File(segmentedTiff);
        this.directory = file.getParentFile();
        String name = file.getName();
        this.baseName = baseName(name);
        this.force = force;
        
        // parse the time from the filename
        this.time = getTime(name);
        
    }
    static public String baseName(String fileName){
        return fileName.substring(0, fileName.toLowerCase().indexOf(".tif"));
    }
    static public int getTime(String fileName){
        int time = -1;
        Pattern p = Pattern.compile("TL(\\d{3})");
        Matcher m = p.matcher(fileName);
        boolean matched = m.find();
        if (matched){
            time = Integer.valueOf(m.group(1));
        } 
        return time;
    }

    @Override
    public void run() {

        // determine the file names
        String microClusterFileName = baseName + "Clusters.xml";
        File microClusterFile = new File(directory,microClusterFileName);
        String BHCTreeFileName = baseName+"BHCTree.xml";
        File BHCTreeFile = new File(directory,BHCTreeFileName);
        File gmmFile = new File(directory,baseName+".xml");
        
        if (!microClusterFile.exists()  || force){
            try {
                SegmentedTiffDataSource segSource = new SegmentedTiffDataSource(segmentedTiff,backgroundSegment); 
                
                this.runMicroCluster(segSource,microClusterFile);
                this.runBHC(microClusterFile,BHCTreeFile);
                this.runTreeCut(BHCTreeFile,gmmFile);
                return;
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(1);
            }
        }
        
        if (!BHCTreeFile.exists() || force){
            try {
                this.runBHC(microClusterFile,BHCTreeFile);
                this.runTreeCut(BHCTreeFile,gmmFile);
                return;                
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(2);
            }
        }
        if (!gmmFile.exists() || force){
            try {
                this.runTreeCut(BHCTreeFile, gmmFile);

            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(3);
            }
        }
    }
    private void runMicroCluster(SegmentedTiffDataSource segSource,File microClusterFile)throws Exception {
        int nVoxels = segSource.getN(nucleiSegment);
        int nClusters = clusterCount(nVoxels);
        int nPartitions = Math.max(1,(int)Math.ceil(Math.pow(nClusters/1000.0,1.0/3.0)));                
        segSource.kMeansCluster(nucleiSegment, nClusters, nPartitions).saveAsXML(microClusterFile.getPath());
    }
    private void runBHC(File microClusterFile,File BHCTreeFile)throws Exception {

        MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
        int nClusters = microDataSource.getK();
        ThreadedAlgorithm alg = new ThreadedAlgorithm();
        alg.setSource(microDataSource);

        double alpha = Math.pow(2.0*nClusters,2.0);
        alg.init(alpha);
        alg.run();
        alg.saveResultAsXML(BHCTreeFile.getPath());
    }
    private void runTreeCut(File BHCTreeFile,File gmmFile) throws Exception {
        BHCTree tree = new BHCTree(BHCTreeFile.getPath());
        TreeSet<Double> post = tree.allPosteriors();
        for (Double p : post){
            if (p >= 1.0E-10){
                tree.saveCutAtThresholdAsXML(gmmFile.getPath(),p);
                break;
            }
        }
    }
    // determine the number of microclusters to form given the number of voxels in the segmented tiff
    static int clusterCount(int nVox){
        int ret = nVox/125;
        if (ret < 500){
            ret = 500;
        } else if (ret > 7000){
            ret = 7000;
        }
        return ret;
    }
    static public void submitTimePoints(String[] tiffs,boolean force)throws Exception {
        if (tiffs.length==0) return;
        
        File directory = new File(tiffs[0]).getParentFile();
        File scriptFile = new File(directory,"SubmitTimePoints.sh");
        scriptFile.setExecutable(true, false);
        PrintStream scriptStream = new PrintStream(scriptFile);
        scriptStream.printf("cd %s\n", directory.getPath());
        
        for (String tiff : tiffs){
            String fileName = new File(tiff).getName();
            String baseName = baseName(fileName);
            int time = getTime(fileName);
/*            
            SegmentedTiffDataSource segSource = new SegmentedTiffDataSource(tiff,backgroundSegment);
            int nVoxels = segSource.getN(nucleiSegment); 
            int nMicroClusters = Math.min(7000, nVoxels/125);
            CellCounts cc = new CellCounts();
            
            int cells = cc.getCellCount(time); 
 */           
            scriptStream.printf("qsub -e %s -o %s %s.qsub\n",directory.getPath(),directory.getPath(),baseName);
            
            // write the qsub file
            PrintStream qsubStream = new PrintStream(new File(directory,baseName+".qsub"));
            qsubStream.println("#$ -S /bin/bash");
            qsubStream.println("#$ -l mfree=20G");
            qsubStream.println("#$ -l h_rt=96:0:0");
     //       qsubStream.println("#$ -l h=w014");
            qsubStream.println("#$ -pe serial 1-10");
            qsubStream.println("cd /nfs/waterston/Ace3D");
            qsubStream.println("PATH=/nfs/waterston/jdk1.8.0_102/bin:$PATH");
            qsubStream.println("JAVA_HOME=/nfs/waterston/jdk1.8.0_102");
            qsubStream.println("M2_HOME=/nfs/waterston/apache-maven-3.3.9");
            qsubStream.print("/nfs/waterston/apache-maven-3.3.9/bin/mvn \"-Dexec.args=-Xms40000m -Xmx40000m -classpath %classpath org.rhwlab.BHC.Nuclei_Identification ");
            qsubStream.printf("%s -first %d -last %d", tiff,time,time);
            if (force){
                qsubStream.print(" -force ");
            }
            qsubStream.print("\" -Dexec.executable=/nfs/waterston/jdk1.8.0_102/bin/java -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:1.2.1:exec");
            qsubStream.println();
            qsubStream.close();
        }
        scriptStream.close();
        
        // start the submission script
        ProcessBuilder pb = new ProcessBuilder("ssh","whead.gs.washington.edu",scriptFile.getPath());
        Process p = pb.start(); 
    }
    
    static public void main(String[] args)throws Exception {
        Nuclei_IdentificationCLI cli = new Nuclei_IdentificationCLI();
        cli.process(args, true);
        String[] tiffs = cli.getTiffs();
        for (String tiff : tiffs){
            System.out.println(tiff);
        }
        if (cli.getQsub()){
            submitTimePoints(tiffs,cli.getForce());
        } else {
            for (String tiff : cli.getTiffs()){
                Nuclei_Identification objectID = new Nuclei_Identification(tiff,cli.getForce());
                objectID.run();
            }
        }
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
//    SegmentedTiffDataSource segSource;
    File directory;
    String baseName;
    String segmentedTiff;
    int time=-1;
    boolean force;
    static int backgroundSegment = 1;
    static int nucleiSegment = 2;
}
