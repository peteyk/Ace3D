/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;

/**
 *
 * @author gevirl
 */
public class OldFaithfulSuperVoxelDataSource implements SuperVoxelDataSource {
    public OldFaithfulSuperVoxelDataSource(String file) throws Exception {
        super();
        List<double[]> dataList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (line != null){
            String[] tokens = line.split(" |\t|,");
            double[] values = new double[tokens.length];
            for (int i=0 ; i<tokens.length ; ++i){
                values[i] = Double.valueOf(tokens[i]);
            }
            dataList.add(values);
            line = reader.readLine();
        }
        voxels = new SuperVoxel[dataList.size()];
        int i=0;
        for (double[] data : dataList){
            RealVector v = new ArrayRealVector(data);
            RealVector[] va = new RealVector[1];
            va[0] = v;
            voxels[i] = new SuperVoxel(va,v);
            ++i;
        }
    }
    @Override
    public int getN() {
        return this.voxels.length;
    }

    @Override
    public int getD() {
        return 2;
    }


    
    static public void main(String[] args)throws Exception {
        OldFaithfulSuperVoxelDataSource source = new OldFaithfulSuperVoxelDataSource("/net/waterston/vol2/home/gevirl/Downloads/VBEMGMM/faithful.txt");
        SuperVoxelGaussianMixture gm = new SuperVoxelGaussianMixture();
        gm.setSource(source);
        gm.init(10);
        gm.run();
    }



    @Override
    public SuperVoxel get(int i) {
        return this.voxels[i];
    }
    SuperVoxel[] voxels;

    @Override
    public int getT() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
