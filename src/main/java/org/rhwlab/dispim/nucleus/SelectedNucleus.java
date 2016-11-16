/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

/**
 *
 * @author gevirl
 */
public class SelectedNucleus extends javafx.beans.value.ObservableValueBase {

    @Override
    public Object getValue() {
        return selected;
    }
    public void setSelectedNucleus(Nucleus nuc){
        this.selected = nuc;
        this.fireValueChangedEvent();
    }
    Nucleus selected;
}
