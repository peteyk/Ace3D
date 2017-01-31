/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHCnotused;

import java.io.PrintStream;

/**
 *
 * @author gevirl
 */
public interface StdDataModel {
    public double likelihood();
    public StdDataModel mergeWith(StdDataModel other);
    public int getN();
    public void print(PrintStream stream);    
}
