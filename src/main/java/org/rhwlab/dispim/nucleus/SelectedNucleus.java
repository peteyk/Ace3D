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
        Nucleus[] ret = new Nucleus[2];
        ret[0] = selected;
        ret[1] = marked;
        return ret;
    }
    public void setSelectedNucleus(Nucleus nuc){
        this.selected = nuc;

        this.fireValueChangedEvent();
    }
    public void setMarked(Nucleus mark){

        this.marked = mark;
        this.fireValueChangedEvent();
    }
    public Nucleus getMarked(){
        return marked;
    }
    public Nucleus getSelected(){
        return selected;
    }
    Nucleus selected;
    Nucleus marked;  // marked as potential parent for linking to the selected nucleus
}
