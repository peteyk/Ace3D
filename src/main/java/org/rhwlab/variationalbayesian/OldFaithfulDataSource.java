/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import org.rhwlab.dispim.datasource.Voxel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.rhwlab.dispim.datasource.VoxelDataSource;

/**
 *
 * @author gevirl
 */
public class OldFaithfulDataSource implements VoxelDataSource {
    public OldFaithfulDataSource(String file) throws Exception {
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
        data = new RealVector[dataList.size()];
        int i=0;
        for (double[] d : dataList){
            data[i] = new ArrayRealVector(d);
            ++i;
        }
    }
    @Override
    public int getN() {
        return this.data.length;
    }

    @Override
    public int getD() {
        return 2;
    }

    public RealVector get(int i) {
        return data[i];
    }

    public List<CentroidCluster<Clusterable>> cluster(int K){
        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(K);
        ArrayList<DoublePoint> points = new ArrayList<>();
        for (RealVector v : data){
            DoublePoint point = new DoublePoint(v.toArray());
            points.add(point);
        }
        return clusterer.cluster(points); 
    }    
    RealVector[] data;
    
    static public void main(String[] args)throws Exception {
        OldFaithfulDataSource source = new OldFaithfulDataSource("/net/waterston/vol2/home/gevirl/Downloads/VBEMGMM/faithful.txt");
        GaussianMixture gm = new GaussianMixture();
        gm.setSource(source);
        gm.setAlpha0(0.001);
        gm.setBeta0(1.0);
        gm.setNu0(20.0);
        RealMatrix W0 = MatrixUtils.createRealIdentityMatrix(source.getD());
        W0 = W0.scalarMultiply(200.0);  
        gm.setW0(W0);
        gm.init(15);
        gm.run();
    }

    @Override
    public Voxel get(long i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
