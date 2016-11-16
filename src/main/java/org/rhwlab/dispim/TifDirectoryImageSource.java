/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import ij.ImagePlus;
import ij.io.Opener;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.jdom2.Element;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;
import org.scijava.Context;
import spim.process.fusion.FusionHelper;

/**
 *
 * @author gevirl
 */
public class TifDirectoryImageSource implements ImageSource {
    public TifDirectoryImageSource(String dirName,String id,ImagedEmbryo emb){
        emb.addSource(this);
        File d = new File(dirName);
        if (!d.isDirectory()){
            this.directory = d.getParent();
        } else {
            this.directory = dirName;
        }
        this.datasetname = id;
        open();
    }
    @Override
    public boolean open(){
 //       Pattern pattern = Pattern.compile("(\\D+)(\\d+).tif");
        Pattern pattern = Pattern.compile("(\\d+)");
        File dir = new File(this.directory);
        
        File[] files = dir.listFiles();
        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;
        for (File file : files){
            String fileName = file.getName();
            if (fileName.endsWith("tif")||fileName.endsWith("tiff")){
                Matcher matcher = pattern.matcher(fileName);
                matcher.find();
//                matcher.matches();
                Integer time = new Integer(matcher.group(1));
                if (time < minTime){
                    minTime = time;
                }
                if (time> maxTime){
                    maxTime = time;
                }
                fileNames.put(time,file.getPath());
            }
        }
        opener = new ImgOpener(new Context());
        
        Iterator<DataSetDesc> iter = getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            Ace3D_Frame.setProperties(dataset,new DataSetProperties());
            Ace3D_Frame.getDataSetsDialog().addDataSet(dataset);
        }   
        
        iter = getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            TimePointImage tpi = TimePointImage.getSingleImage(dataset,getMinTime());
            DataSetProperties ps = Ace3D_Frame.getProperties(dataset);
            ps.max = 2;
            ps.min = 1;
            Ace3D_Frame.getDataSetsDialog().setProperties(dataset, ps);
        }      
    return true;        
    }

    @Override
    public TimePointImage getImage(String dataset,int time) {
        Opener opn = new Opener();
        ImagePlus ip = opn.openImage(fileNames.get(time));
        float[] mm = new float[2];
        mm[0] = (float)ip.getProcessor().getMin();
        mm[1] = (float)ip.getProcessor().getMax();
        Img img = ImageJFunctions.wrap(ip);
        return new TimePointImage(img,mm,time,dataset);
/*        
        try {
 
            List<SCIFIOImgPlus<?>> list = opener.openImgs(fileNames.get(time));
            SCIFIOImgPlus img = list.get(0);
            
            long[] dims = new long[img.numDimensions()];
            for (int d=0 ; d<dims.length ; ++d) {
                dims[d] = img.dimension(d);
            }
            float[] mm = new float[2];           
            float[] minmax = FusionHelper.minMax(img);
            
            return new TimePointImage(img.getImg(),mm,time,dataset);
        } catch (ImgIOException ex) {
            Logger.getLogger(TifDirectoryImageSource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
*/
    }

    @Override
    public int getTimes() {
        return fileNames.size();
    }
    
    public String getFile(){
        return directory;
    }
    @Override
    public Collection<DataSetDesc> getDataSets() {
        HashSet<DataSetDesc> ret = new HashSet<>();
        DataSetDescImpl ds = new DataSetDescImpl();
        ds.name = datasetname;
        ret.add(ds);
        return ret;
    }
    ImgOpener opener;
    String directory;
    String datasetname;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();
    

    @Override
    public int getMinTime() {
        return minTime;
    }

    @Override
    public int getMaxTime() {
        return maxTime;
    }
    int minTime;
    int maxTime;

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element toXML() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
