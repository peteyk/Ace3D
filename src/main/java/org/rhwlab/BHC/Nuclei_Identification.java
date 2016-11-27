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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.rhwlab.dispim.datasource.MicroCluster4DDataSource;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;
import org.rhwlab.dispim.datasource.SegmentedTiffIntensityDataSource;

/**
 *
 * @author gevirl
 */
public class Nuclei_Identification implements Runnable {
    public Nuclei_Identification(String dir,String lineageTiff,String segmentedTiff,boolean force,boolean study){
        this.segmentedTiff = segmentedTiff;
        this.lineageTff = lineageTiff;
        File file = new File(segmentedTiff);
        this.directory = new File(dir);
        String name = file.getName();
        this.baseName = baseName(name);
        this.force = force;
        this.study = study;
        
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

    static File[] xmlFiles(File directory,String baseName){
        File[] ret = new File[3];
        String microClusterFileName = baseName + "Clusters.xml";
        ret[0] = new File(directory,microClusterFileName);
        String BHCTreeFileName = baseName+"BHCTree.xml";
        ret[1] = new File(directory,BHCTreeFileName);
        ret[2] = new File(directory,baseName+".xml");        
        return ret;
    }
    @Override
    public void run() {

        // determine the file names
        File[] xmls = xmlFiles(directory,baseName);
        File microClusterFile = xmls[0];
        File BHCTreeFile = xmls[1];
        File gmmFile = xmls[2];
        if (study){
            try {
//                runBHCStudy(microClusterFile);
            } catch (Exception exc){
                exc.printStackTrace();
            }
            return;
        }
        if (!microClusterFile.exists()  || force){
            try {
                SegmentedTiffIntensityDataSource segSource = new SegmentedTiffIntensityDataSource(lineageTff,segmentedTiff,backgroundSegment); 
//                SegmentedTiffDataSource segSource = new SegmentedTiffDataSource(segmentedTiff,backgroundSegment); 
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
        double alpha = 1.0E3;
        MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
        ThreadedAlgorithm alg;
//        MicroCluster4DDataSource microDataSource = new MicroCluster4DDataSource(microClusterFile.getPath());
        do {
            alg = new ThreadedAlgorithm();
            alg.setTime(time);
            alg.setSource(microDataSource);
            double[] precision = new double[microDataSource.getD()];
            precision[0] = precision[1] = precision[2] = 20.0;
    //        precision[3] = 200.0;
            alg.setPrecision(precision);
            alg.setNu(10);
      //      double alpha = Math.pow(2.0*nClusters,2.0);

            alg.init(alpha);
            alg.run();
            alpha = 10.*alpha;
        } while (alg.getFinalCluster().getPosterior()!=0.0);
        
        alg.saveResultAsXML(BHCTreeFile.getPath());
    }
    private void runTreeCut(File BHCTreeFile,File gmmFile) throws Exception {
        BHCTree tree = new BHCTree(BHCTreeFile.getPath());
        tree.saveCutAtThresholdAsXML(gmmFile.getPath(),.5);
/*        
        Double[] post = tree.allPosteriors().toArray(new Double[0]);
        for (int i=0 ; i<post.length ; ++i){
            if (post[i] >= 0.5){
                tree.saveCutAtThresholdAsXML(gmmFile.getPath(),post[i-1]);
                break;
            }
        }
*/        
    }
/*    
    static public void runBHCStudy(File microClusterFile)throws Exception {
        double[] alphas = new double[8];
        alphas[0] = 1000.0;
        for (int i=1 ; i<alphas.length ; ++i){
            alphas[i] = 10.0*alphas[i-1];
        }
        double[] precisions = new double[10];
        precisions[0] = 50.0;
        for (int i=1 ; i<precisions.length ; ++i){
            precisions[i] = precisions[i-1] + 50.0;
        }
        MicroClusterDataSource microDataSource = new MicroClusterDataSource(microClusterFile.getPath());
        PrintStream stream = new PrintStream(microClusterFile.getPath()+".study");
        for (double alpha : alphas){
            for (double prec : precisions){
                ThreadedAlgorithm alg = new ThreadedAlgorithm();
                alg.setSource(microDataSource); 
                alg.setPrecision(prec);
                alg.setNu(20);
                alg.init(alpha);
                alg.run();
                BHCTree tree = alg.resultAsBHCTree();
                Double[] post = tree.allPosteriors().toArray(new Double[0]);
                for (int i=0 ; i<post.length ; ++i){
                    if (post[i] >= 0.5){
                        Element ele = tree.cutTreeAtThreshold(post[i-1]);
                        List<Element> eleList = ele.getChildren("GaussianMixtureModel");
                        stream.printf("%e\t%f\t%d\n",alpha,prec,eleList.size());
                        break;
                    }
                }
                stream.flush();
            }
        }
    }
    */
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
    static public void submitStudyTimes(File directory,TreeMap<Integer,String[]> tiffs)throws Exception {
        if (tiffs.isEmpty()) return;
        
        File scriptFile = new File(directory,"SubmitStudyTimes.sh");
       
        PrintStream scriptStream = new PrintStream(scriptFile);
        scriptStream.printf("cd %s\n", directory.getPath());
        
        for (int time : tiffs.keySet()){
            String[] names = tiffs.get(time);
            String fileName = new File(names[1]).getName();
            String baseName = baseName(fileName);
            File[] xmls = xmlFiles(directory,baseName);

           
            scriptStream.printf("qsub -e %s -o %s %s.qsub\n",directory.getPath(),directory.getPath(),baseName);
            
            // write the qsub file
            PrintStream qsubStream = new PrintStream(new File(directory,baseName+".qsub"));
            qsubStream.println("#$ -S /bin/bash");
            qsubStream.println("#$ -l mfree=40G");
            
     //       qsubStream.println("#$ -l h=w014");
            qsubStream.println("#$ -pe serial 1-10");
            qsubStream.println("cd /nfs/waterston/Ace3D");
            qsubStream.println("PATH=/nfs/waterston/jdk1.8.0_102/bin:$PATH");
            qsubStream.println("JAVA_HOME=/nfs/waterston/jdk1.8.0_102");
            qsubStream.println("M2_HOME=/nfs/waterston/apache-maven-3.3.9");
            qsubStream.print("/nfs/waterston/apache-maven-3.3.9/bin/mvn \"-Dexec.args=-Xms36000m -Xmx36000m -classpath %classpath org.rhwlab.BHC.Nuclei_Identification ");
            qsubStream.printf("-study -first %d -last %d -segTiff %s  -lineageTiff %s -dir %s  ",time,time,names[1],names[0],directory.getPath());

            qsubStream.print("\" -Dexec.executable=/nfs/waterston/jdk1.8.0_102/bin/java -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:1.2.1:exec");
            qsubStream.println();
            qsubStream.close();
        }
        scriptStream.close();
         scriptFile.setExecutable(true, false);
 
        // start the submission script
        ProcessBuilder pb = new ProcessBuilder("ssh","grid.gs.washington.edu",scriptFile.getPath());
        Process p = pb.start();        
    }
    static public void submitTimePoints(File directory,TreeMap<Integer,String[]> tiffs,boolean force,String memory)throws Exception {
        if (tiffs.isEmpty()) return;
        
        File scriptFile = new File(directory,"SubmitTimePoints.sh");
        
        PrintStream scriptStream = new PrintStream(scriptFile);
        scriptStream.printf("cd %s\n", directory.getPath());
        
        for (int time : tiffs.keySet()){
            String[] names = tiffs.get(time);
            String fileName = new File(names[1]).getName();
            String baseName = baseName(fileName);
            File[] xmls = xmlFiles(directory,baseName);
            if (xmls[0].exists() && xmls[1].exists() && xmls[2].exists()){
                continue;
            }
           
            scriptStream.printf("qsub -e %s -o %s %s.qsub\n",directory.getPath(),directory.getPath(),baseName);
            
            // write the qsub file
            PrintStream qsubStream = new PrintStream(new File(directory,baseName+".qsub"));
            qsubStream.println("#$ -S /bin/bash");
            qsubStream.printf("#$ -l mfree=%s\n",memory);
            qsubStream.println("#$ -l h_rt=96:0:0");
     //       qsubStream.println("#$ -l h=w014");
            qsubStream.println("#$ -pe serial 1-10");
            qsubStream.println("cd /nfs/waterston/Ace3D");
            qsubStream.println("PATH=/nfs/waterston/jdk1.8.0_102/bin:$PATH");
            qsubStream.println("JAVA_HOME=/nfs/waterston/jdk1.8.0_102");
            qsubStream.println("M2_HOME=/nfs/waterston/apache-maven-3.3.9");
            qsubStream.printf("/nfs/waterston/apache-maven-3.3.9/bin/mvn \"-Dexec.args=-Xms%s -Xmx%s  ",memory,memory);
            qsubStream.print(" -classpath %classpath org.rhwlab.BHC.Nuclei_Identification ");
            qsubStream.printf(" -first %d -last %d -segTiff %s  -lineageTiff %s -dir %s  ",time,time,names[1],names[0],directory.getPath());
            if (force){
                qsubStream.print(" -force ");
            }
            qsubStream.print("\" -Dexec.executable=/nfs/waterston/jdk1.8.0_102/bin/java -Dexec.classpathScope=runtime org.codehaus.mojo:exec-maven-plugin:1.2.1:exec");
            qsubStream.println();
            qsubStream.close();
        }
        scriptStream.close();
        scriptFile.setExecutable(true, false);
        // start the submission script
        ProcessBuilder pb = new ProcessBuilder("ssh","grid.gs.washington.edu",scriptFile.getPath());
        Process p = pb.start(); 
        }
    
    static public void main(String[] args)throws Exception {
        Nuclei_IdentificationCLI cli = new Nuclei_IdentificationCLI();
        cli.process(args, true);
        TreeMap<Integer,String[]> tiffs = cli.getTiffs();

        if (cli.getStudy()&& cli.getQsub()){
            submitStudyTimes(new File(cli.getDirectory()),tiffs);
        }
        else if (cli.getQsub()){
            submitTimePoints(new File(cli.getDirectory()),tiffs,cli.getForce(),cli.getMemory());
        } else {
            for (int time : tiffs.keySet()){
                String[] names = tiffs.get(time);
                Nuclei_Identification objectID = new Nuclei_Identification(cli.getDirectory(),names[0],names[1],cli.getForce(),cli.getStudy());
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
    String lineageTff;
    File directory;
    String baseName;
    String segmentedTiff;
    int time=-1;
    boolean force;
    boolean study;
    static int backgroundSegment = 1;
    static int nucleiSegment = 2;
}
