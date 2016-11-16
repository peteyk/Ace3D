/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.Cell;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class NucleusPropertiesPanel extends JPanel implements InvalidationListener  {
    public NucleusPropertiesPanel() {
        this.setLayout(new GridLayout(15,2));
        this.add(new JLabel("Selected Nucleus"));
        this.add(name);

        this.add(new JLabel("Parent Nucleus"));
        this.add(parent);
        this.add(new JLabel("Child1 Nucleus"));
        this.add(child1);
        this.add(new JLabel("Child2 Nucleus"));
        this.add(child2);        
        this.add(new JLabel("Center"));
        this.add(center);
        this.add(new JLabel("a Radius"));
        this.add(aRadius);
        this.add(new JLabel("b Radius"));
        this.add(bRadius);
        this.add(new JLabel("c Radius"));
        this.add(cRadius);
        this.add(new JLabel("Eccentricity"));
        this.add(frob);
        this.add(new JLabel("Density"));
        this.add(density);
        this.add(new JLabel("Volume"));
        this.add(volume);
        this.add(new JLabel("Intensity"));
        this.add(intensity);
        
        this.add(new JLabel("In Cell"));
        this.add(cell);
        this.add(new JLabel("Root Cell"));
        this.add(root);        

        cell.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                renameCell();
            }
        });
        this.add(new JLabel("Expression"));
        this.add(express);

        
    }

    public void renameCell(){
        if (embryo == null) return;
        embryo.renameSelectedCell(this.cell.getText().trim());
    }
    @Override
    public void invalidated(Observable observable) {
        if (observable instanceof ImagedEmbryo ){
            embryo = (ImagedEmbryo)observable;
            Nucleus selected = embryo.selectedNucleus();
            if (selected == null) {
                return;
            }
            name.setText(selected.getName());
            root.setText(selected.getRoot());
            express.setText(String.format("%.2f",selected.getExpression()));
            double[] c = selected.getCenter();
            center.setText(String.format("(%d,%d,%d)",(int)c[0],(int)c[1],(int)c[2]));
            aRadius.setText(selected.getRadiusLabel(0));
            bRadius.setText(selected.getRadiusLabel(1));
            cRadius.setText(selected.getRadiusLabel(2));
            double[] ecc = selected.eccentricity();
            frob.setText(String.format("%.3f,%.3f,%.3f",ecc[0],ecc[1],ecc[2]));
            BHCNucleusData bhcNuc = (BHCNucleusData)selected.getNucleusData();
            density.setText(String.format("%f",bhcNuc.getDensity()));
            volume.setText(String.format("%f",bhcNuc.getVolume()));
            intensity.setText(String.format("%f", bhcNuc.getAverageIntensity()));
            if (selected.getCell() != null){
                cell.setText(selected.getCell());
            }else {
                cell.setText("No cell");
            }
            if (selected.getParent() != null){
                parent.setText(selected.getParent().getName());
            } else {
                parent.setText("Not linked");
            }
            Nucleus[] children = selected.nextNuclei();
            if (children.length<1 ){
                child1.setText("Not linked");
            } else {
                child1.setText(children[0].getName());
            }
            if (children.length <2){
                child2.setText("Not linked");
            } else {
                child2.setText(children[1].getName());
            }                
        }
    }
    public String getChild1(){
        return child1.getText();
    }
    public String getChild2(){
        return child2.getText();
    }
    ImagedEmbryo embryo;
    static String initial = "None Selected";
    JLabel name = new JLabel(initial);
    JLabel root = new JLabel(initial);
    JLabel center = new JLabel(initial);
    JLabel aRadius = new JLabel(initial);
    JLabel bRadius = new JLabel(initial);
    JLabel cRadius = new JLabel(initial); 
    JLabel frob = new JLabel(initial);
    JTextField cell = new JTextField(initial);
    JLabel parent = new JLabel(initial);
    JLabel child1 = new JLabel(initial);
    JLabel child2 = new JLabel(initial);
    JLabel express = new JLabel(initial);
    JLabel density = new JLabel(initial);
    JLabel volume = new JLabel(initial);
    JLabel intensity = new JLabel(initial);
}
