/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.process.LUT;
import java.awt.Color;

/**
 *
 * @author gevirl
 */
public class DataSetProperties {
    public DataSetProperties(){
        
    }
    public DataSetProperties (boolean auto,float mn,float mx,Color c,boolean sel){
        this.autoContrast = auto;
        this.max = mx;
        this.min = mn;
        this.color = c;
        this.selected = sel;
    }
   
    public boolean autoContrast = true;
    public float min = (float)0.0;
    public float max = Short.MAX_VALUE;
    public LUT lut=null;
    public Color color = Color.white;
    public boolean selected = true;
}
