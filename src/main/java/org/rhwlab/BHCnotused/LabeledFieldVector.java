/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.linear.ArrayFieldVector;

/**
 *
 * @author gevirl
 */
public class LabeledFieldVector extends ArrayFieldVector {
    public LabeledFieldVector(Dfp[] elements,int label){
        super(elements);
        this.label = label;
    }
    public int getLabel(){
        return label;
    }
    int label;
}
