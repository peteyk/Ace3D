/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.util.TreeMap;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.rhwlab.BHC.BHCTree;

/**
 *
 * @author gevirl
 */
public class SegmentationLinePlot extends JPanel {
    public void setTree(BHCTree tree){
        XYSeriesCollection collect = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        collect.addSeries(series);
        TreeMap<Integer,Double> postMap = new TreeMap<>();
        tree.allPosteriorProb(postMap);
        for (Integer i : postMap.keySet()){
            Double p = postMap.get(i);
            series.add(i,p);
            if (i >1000){
                break;
            }
        }
        int t = tree.getTime();
        int nu = tree.getNu();

        JFreeChart chart = ChartFactory.createXYLineChart
                (String.format("Time=%d,nu=%d,alpha=%e",tree.getTime(),tree.getNu(),tree.getAlpha()),
                        "Index","Probability", collect, PlotOrientation.VERTICAL,false,true,true);
        XYPlot plot = (XYPlot)chart.getPlot();

        ChartPanel panel = new ChartPanel(chart);
        this.add(panel);        
    }    
}
