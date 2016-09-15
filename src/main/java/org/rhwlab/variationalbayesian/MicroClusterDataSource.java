/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import org.rhwlab.dispim.datasource.Voxel;
import java.io.File;
import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.rhwlab.dispim.datasource.DataSource;

/**
 *
 * @author gevirl
 */
public class MicroClusterDataSource implements DataSource {
    public MicroClusterDataSource(String file)throws Exception {
        this.openFromClusters(file);
    }    
    final public void openFromClusters(String xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();
        K = Integer.valueOf(root.getAttributeValue("NumberOfClusters"));
        D = Integer.valueOf(root.getAttributeValue("Dimensions"));
        centers = new RealVector[K];
        N = Integer.valueOf(root.getAttributeValue("NumberOfPoints"));
        List<Element> clusterElements = root.getChildren("Cluster");
        int k = 0;
        for (Element clusterElement : clusterElements){
            String[] tokens = clusterElement.getAttributeValue("Center").split(" ");
            double[] v = new double[tokens.length];
            for (int i=0 ; i<v.length ; ++i){
                v[i] = Double.valueOf(tokens[i]);
            }
            centers[k] = new ArrayRealVector(v);
            ++k;
        }

    }
    @Override
    public int getN() {
        return N;
    }

    @Override
    public int getD() {
        return D;
    }

    @Override
    public Voxel get(long i) {
        return null;
    }
    public int getK(){
        return K;
    }
    public RealVector getCenter(int cl){
        return centers[cl];
    } 
    public RealVector getDataMean(){
        ArrayRealVector ret = new ArrayRealVector(getD());
        for (int k=0 ; k<centers.length ; ++k){
            ret = ret.add(centers[k]);
        }
        return ret.mapDivide(centers.length);
    }    
    public RealVector[] getClusterVectors(int label){
        return null;
    }
    int D;
    int N;
    int K;
    RealVector[] centers;
}
