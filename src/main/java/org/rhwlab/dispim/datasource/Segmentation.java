/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class Segmentation {
    public Segmentation(VoxelDataSource source,int bck){
        
        segmentIndex = new HashMap<>();
        mins = new double[source.getD()];
        maxs = new double[source.getD()];
        for (int d=0 ; d<source.getD() ; ++d ){
            mins[d] = Double.MAX_VALUE;
            maxs[d] = 0.0;
        }
        
        for (long i=0 ; i<source.getN() ; ++i){  // process each voxel in the segmented tiff
            Voxel segVox = source.get(i);
            int seg = segVox.getIntensity();  // intensity identifies the segment
            if (seg != bck){  // do not build a segment for the background
                addVoxelToSegment(source,i,seg);
            }
        }        
    }

    public void addVoxelToSegment(VoxelDataSource source,long i,int seg){
        Voxel segVox = source.get(i);
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
    
    public long getVoxelIndex(int segment,int segIndex){
        return segmentIndex.get(segment).get(segIndex);
    }
    public int getN(int segment){
        return segmentIndex.get(segment).size();
    }
    public double[] getMins(){
        return mins;
    }
    public double[] getMaxs(){
        return maxs;
    }
    double[] mins;  // min coordinates of non-background voxels
    double[] maxs;  // max coordinates of non-background voxels  
    HashMap<Integer,List<Long>> segmentIndex;  // list of voxels in each segment    
}
