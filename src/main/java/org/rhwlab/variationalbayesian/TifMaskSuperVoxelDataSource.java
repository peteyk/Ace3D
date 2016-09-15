/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.variationalbayesian;

import ij.ImagePlus;
import ij.io.Opener;
import java.util.ArrayList;
import java.util.List;
import net.imglib2.Cursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

/**
 *
 * @author gevirl
 */
public class TifMaskSuperVoxelDataSource implements SuperVoxelDataSource{
    public TifMaskSuperVoxelDataSource(String file){
        ArrayList<DoublePoint> allVoxels = new ArrayList<>();
        final ImagePlus imp = new Opener().openImage( file);
        final Img image = ImagePlusAdapter.wrap( imp );
        Cursor cursor = image.localizingCursor();
        int[] pos = new int[3];
        while(cursor.hasNext()){
            UnsignedByteType obj = (UnsignedByteType)cursor.next();
            int i = obj.getInteger();
            if (obj.getInteger() != 1){
                cursor.localize(pos);
                DoublePoint point = new DoublePoint(pos);
                allVoxels.add(point);
            }
        }
        this.T = allVoxels.size();
        int K = allVoxels.size()/1000;
        superVoxels = new SuperVoxel[K];       
        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(K);
        List<CentroidCluster<Clusterable>> clusters = clusterer.cluster(allVoxels);
        int k =0;
        for (CentroidCluster<Clusterable> cluster : clusters){
            List<Clusterable> points = cluster.getPoints();
            RealVector[] voxels = new RealVector[points.size()];
            for (int i=0 ; i<points.size() ; ++i){
                voxels[i] = new ArrayRealVector(points.get(i).getPoint());
            }
            RealVector center = new ArrayRealVector(cluster.getCenter().getPoint());
            superVoxels[k] = new SuperVoxel(voxels,center);
            ++k;
        } 
        int ausgdf=0;
    }
    @Override
    public int getN() {
        return superVoxels.length;
    }

    @Override
    public int getD() {
        return 3;
    }

    @Override
    public SuperVoxel get(int i) {
        return superVoxels[i];
    }
    SuperVoxel[] superVoxels;
    int T;
    
    static public void main(String[] args) throws Exception {
        TifMaskSuperVoxelDataSource source = new TifMaskSuperVoxelDataSource("/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.tiff");
        SuperVoxelGaussianMixture gm = new SuperVoxelGaussianMixture();
        gm.setSource(source);
        gm.init(50);
        gm.run();
    }

    @Override
    public int getT() {
        return T;
    }

}
