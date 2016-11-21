/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.BHCNucleusSet;

/**
 *
 * @author gevirl
 */
public class SegmentationScatterPlot extends JPanel {
    public void setNuceli(BHCNucleusSet nucFile){
        XYSeriesCollection collect = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        collect.addSeries(series);
        for (BHCNucleusData nuc : nucFile.getNuclei()){
            series.add(nuc.getVolume(),nuc.getAverageIntensity());
        }
        JFreeChart chart = ChartFactory.createScatterPlot
                (String.format("Time: %d Nuclei: %d",nucFile.getTime(),nucFile.getNuclei().size()),"Volume","AvgIntensity", collect, PlotOrientation.VERTICAL,false,true,true);
        XYPlot plot = (XYPlot)chart.getPlot();
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotHeight(4);
        renderer.setDotWidth(4);
        plot.setRenderer(renderer);
        ChartPanel panel = new ChartPanel(chart);
        this.add(panel);        
    }
}
