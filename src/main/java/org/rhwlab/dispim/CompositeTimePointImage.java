/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.awt.AlphaComposite;
import java.awt.CompositeContext;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.List;

/**
 *
 * @author gevirl
 */
public class CompositeTimePointImage extends TimePointImage {
    public CompositeTimePointImage(List<TimePointImage> images){
        super(images.get(0).image,images.get(0).minmax,images.get(0).time,images.get(0).dims,images.get(0).dataset);
        this.images = images;
    }
    @Override
    public BufferedImage getBufferedImage(int dim,long slice){

        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP);
        BufferedImage source = images.get(0).getBufferedImage(dim, slice);

        for (int i=1 ; i<images.size() ;++i){
            BufferedImage dest = images.get(i).getBufferedImage(dim, slice);
            CompositeContext context = composite.createContext(source.getColorModel(),dest.getColorModel(),null); 
            context.compose(source.getRaster(),dest.getRaster(),source.getRaster());
        }
        return source;
    }
    List<TimePointImage> images;
}
