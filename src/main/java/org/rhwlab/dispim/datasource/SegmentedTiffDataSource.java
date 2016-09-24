/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;


/**
 *
 * @author gevirl
 */
public class SegmentedTiffDataSource extends TiffDataSource implements SegmentedDataSource{
    public SegmentedTiffDataSource(TiffDataSource s){
        super(s);
        init();
    }
    public SegmentedTiffDataSource(String segmentedTiff,int bck){
        super(segmentedTiff);
        init();

        for (long i=0 ; i<this.getN() ; ++i){  // process each voxel in the segmented tiff
            Voxel segVox = this.get(i);
            int seg = segVox.getIntensity();  // intensity identifies the segment
            if (seg != bck){  // do not build a segment for the background
                addVoxelToSegment(i,seg);

            }
        }
    }    
    private final void init(){
        segmentIndex = new HashMap<>();
        mins = new double[dims.length];
        maxs = new double[dims.length];    
        for (int d=0 ; d<dims.length ; ++d ){
            mins[d] = Double.MAX_VALUE;
            maxs[d] = 0.0;
        }        
    }
    public void addVoxelToSegment(long i,int seg){
        Voxel segVox = this.get(i);
        // record mins and max of coordinates
        for (int d=0 ; d<mins.length ; ++d){
            RealVector v = segVox.coords;
            double e = v.getEntry(d);
            if (e < mins[d]){
                mins[d] = e;
            }
            if (e > maxs[d]) {
                maxs[d] = e;
            }
        }                  
        // group the voxels by segment - intensity determines the segment
        List<Long> positions = segmentIndex.get(seg);

        if (positions == null){
            positions = new ArrayList<>();
            segmentIndex.put(seg, positions);
        }
        positions.add(i);        
    }

    @Override
    public void saveAsTiff(String file){
        for (Integer seg : segmentIndex.keySet()){
            for (Long i : segmentIndex.get(seg)){
                this.setIntensity(i, seg);
            }
        }
        super.saveAsTiff(file);
    }
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("Segmented");
        root.setAttribute("NumberOfPoints",Long.toString(this.getN()));
        root.setAttribute("NumberOfSegments", Integer.toString(segmentIndex.size()));
        for (Integer seg : segmentIndex.keySet()){
            List<Long> indexes = segmentIndex.get(seg);
            Element segment = new Element("Segment");
            segment.setAttribute("PointsInSegment",Integer.toString(indexes.size()));
            for (long n : indexes){
                Element pointEle = new Element("Point");
                Voxel vox = super.get(n);
                double[] point = vox.coords.toArray();
                StringBuilder builder = new StringBuilder();
                for (int d=0 ; d<point.length ; ++d){
                    if (d > 0 ){
                        builder.append(" ");
                    }
                    builder.append((int)point[d]);
                } 
                pointEle.addContent(builder.toString());
                segment.addContent(pointEle);
            }            
            root.addContent(segment);
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();
        
    }
    @Override
    public ClusteredDataSource kMeansCluster(int segment, int nClusters, int nPartitions) throws Exception {
        
        double[] dels = new double[dims.length];
        for (int d=0 ; d<dims.length ; ++d){
            dels[d] = (maxs[d]-mins[d])/nPartitions;
        }        
        // partition the voxels into separate lists
        int mx = (int)Math.pow(nPartitions,getD());
        ArrayList<Voxel>[] lists = new ArrayList[mx];
        for (int i=0 ; i<lists.length ; ++i){
            lists[i] = new ArrayList();
        } 
        for (int i=0 ; i<getN(segment) ; ++i){
            Voxel vox = get(i, segment);
            int index = region(vox.getPoint(), nPartitions,dels);
            lists[index].add(vox);
        } 
        // build the clustering threads
        double f = (double)getN(segment)/(double)nClusters;   
        ArrayList<VoxelClusterer> clusterers = new ArrayList<>();
        for (int i=0 ; i<lists.length ; ++i){
            ArrayList<Voxel> list = lists[i];
            if (!list.isEmpty()){
                int nc = (int)((double)list.size()/f);
                VoxelClusterer clusterer = new VoxelClusterer(list,new KMeansPlusPlusClusterer(nc));
                clusterers.add(clusterer);
            }
        }
        
        // do the clustering
        for (VoxelClusterer clusterer : clusterers){
            clusterer.start();
        }
        
        // wait for them all to finish
        for (VoxelClusterer clusterer : clusterers){
            clusterer.join();
        } 

        return new ClusteredDataSource(clusterers.toArray(new VoxelClusterer[0]),0,this.getD());
    }
    // determine which region to put a given voxel
    protected int region(double[] p,int nPart,double[] dels){


        int index = 0;
        int base = 1;
        for (int d=0 ; d<dims.length ; ++d){
            int i = (int)((p[d]-mins[d])/dels[d]);
            if (i == nPart){
                i = nPart-1;
            }
            index = index + i*base;
            base = base*nPart;
        }
        return index;
    }    
    // return the number of voxels in the segment
    @Override
    public int getN(int segment){
        return segmentIndex.get(segment).size();
    }
    // return the ith voxel in the given segment
    @Override
    public Voxel get(int i,int segment){
        // get the voxel from this tiff data source
        Voxel ret = super.get(segmentIndex.get(segment).get(i));
        return ret;
    } 
    double[] mins;  // min coordinates of non-background voxels
    double[] maxs;  // max coordinates of non-background voxels  
    HashMap<Integer,List<Long>> segmentIndex;  // list of voxels in each segment

    
}
