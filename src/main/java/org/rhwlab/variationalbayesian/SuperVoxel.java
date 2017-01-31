/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class SuperVoxel {
    public SuperVoxel(RealVector[] voxels,RealVector centroid){
        this.voxels = voxels;
        this.centroid = centroid;

    }
    public RealVector[] getVoxels(){
        return this.voxels;
    }
    public RealVector getCenter(){
        return this.centroid;
    }
    RealVector[] voxels;
    RealVector centroid;
}
