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
   
    public boolean autoContrast = true;
    public float min;
    public float max;
    public LUT lut=null;
    public Color color = Color.white;
}
