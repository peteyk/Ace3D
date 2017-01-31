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
public interface SuperVoxelDataSource {
    public int getN();  // the number of super voxels
    public int getD();  // the dimension of each data point
    public int getT();  // the total number of data points in the super voxels
    public SuperVoxel get(int i);  // return the ith super voxel 
}
