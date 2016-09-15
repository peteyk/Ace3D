/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;


/**
 *
 * @author gevirl
 */
public interface SegmentedDataSource extends DataSource{
    public ClusteredDataSource kMeansCluster(int segment,int nClusters,int nPartitions)throws Exception;
    public int getN(int segment);
    public Voxel get(int i,int segment);
}
