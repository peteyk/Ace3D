/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class TGMMNucleus extends Nucleus {
    public TGMMNucleus(int time,Element gmm){
        super(time,name(time,gmm),center(gmm),10.0);  // for now make all radii the same
        id = gmm.getAttributeValue("id");
        parent = gmm.getAttributeValue("parent");
        prec = precision(gmm);
    }
    static long[] center(Element gmm){
        long[] ret = new long[3];
        String[] tokens = gmm.getAttributeValue("m").split(" ");
        for (int i =0 ; i<ret.length ; ++i) {
            ret[i] = (long)(Double.parseDouble(tokens[i])+.5);
        }
        return ret;
    }
    static double[][] precision(Element gmm){
        double [][] ret = new double[3][3];
        String[] tokens = gmm.getAttributeValue("W").split(" ");
        for (int i=0 ; i<3 ; ++i){
            for (int j=0 ; j<3 ; ++j){
                ret[i][j] = Double.valueOf(tokens[3*i+j]);
            }
        }
        return ret;
    }
    static String name(int time,String id){
        int n = Integer.valueOf(id);
        return String.format("%03d_%03d", time,n);
    }
    static String name(int time,Element gmm){
        return name(time,gmm.getAttributeValue("id"));
    }
    public String getID(){
        return id;
    }
    public String getParent(){
        return name(this.time-1,parent);
    }
    String id;
    String parent;
    double[][] prec;
}
