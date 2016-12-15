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
public abstract class DataSourceBase implements DataSource {
    // return coordinates of a voxel given a linear index 
    public long[] getCoords(long i){
        long[] coord = new long[dims.length];
        long index = i;
        for (int d =0 ; d<dims.length-1 ; ++d){
            coord[d] = index % dims[d];
            index = index - coord[d];
            index = index/dims[d];
        }
        coord[dims.length-1] = index;        
        return coord;    
    }


    @Override
    public int getN() {
        return (int)N;
    }
    
    @Override
    public int getD() {
        return dims.length;
    }
    
    long[] dims; 
    long N;
}
