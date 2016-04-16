/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import spim.fiji.plugin.queryXML.LoadParseQueryXML;
import spim.fiji.spimdata.SpimData2;
import spim.process.fusion.FusionHelper;

/**
 *
 * @author gevirl
 */
public class  Hdf5ImageSource implements ImageSource {
    public Hdf5ImageSource(LoadParseQueryXML re){
        result = re;
        if (result == null){
            result = new LoadParseQueryXML();
            result.queryXML( "editing", false, false, false, false );
        }
        ArrayList<ViewSetup> setups = result.getViewSetupsToProcess();
        for (ViewSetup setup : setups){
            Hdf5DataSetDesc desc = new Hdf5DataSetDesc(setup);
            this.dataSetMap.put(desc.getName(), desc);
        }
        
        timepoints = result.getTimePointsToProcess();
        angles = result.getData().getSequenceDescription().getAllAnglesOrdered();
        channels = result.getData().getSequenceDescription().getAllChannelsOrdered();  
        illuminations = result.getData().getSequenceDescription().getAllIlluminationsOrdered();
        spimData = result.getData();        
    }
    @Override
    public TimePointImage getImage(String dataset,int time) {
        Hdf5DataSetDesc desc = (Hdf5DataSetDesc)this.dataSetMap.get(dataset);
        return getImage(dataset,time,desc.getAngleId(),desc.getChannelId(),desc.getIllumninationId());
    }
    
    public  TimePointImage getImage(String dataset,int time,int angle,int channel,int illum) {

        final ViewId viewId = SpimData2.getViewId( 
                spimData.getSequenceDescription(),timepoints.get(time),channels.get(channel),angles.get(angle),illuminations.get(illum) );
        ViewRegistrations viewRegs = spimData.getViewRegistrations();
        ViewRegistration viewReg = viewRegs.getViewRegistration(viewId);
        List<ViewTransform> xformList = viewReg.getTransformList();
        AffineTransform3D affine = new AffineTransform3D();
        affine.identity();
        for (ViewTransform xform : xformList){
            if (xform.getName().equals("calibration")){
                affine.set(xform.asAffine3D().get(2,2), 2, 2);
                break;
            }
        }
        SequenceDescription seqDesc = spimData.getSequenceDescription();
        ViewDescription viewDesc = seqDesc.getViewDescription(viewId);

        ImgLoader imgLoader = seqDesc.getImgLoader();
        SetupImgLoader setupImgLoader = imgLoader.getSetupImgLoader(viewId.getViewSetupId());
        
        RandomAccessibleInterval img = setupImgLoader.getImage(viewId.getTimePointId() );
        
        RandomAccessible input = Views.extendZero(img);
        RealRandomAccessible<UnsignedShortType> interpolated = Views.interpolate( input, new NLinearInterpolatorFactory<UnsignedShortType>() );

        
        int nDim = img.numDimensions();
        double[] dims = new double[nDim];
        double[] xformDims = new double[nDim];
        for (int d=0 ; d<nDim ; ++d){
            dims[d] = img.dimension(d);
        }
        affine.apply(dims, xformDims);
        
        long[] min = new long[nDim];
        long[] max = new long[nDim];
        for (int d=0 ; d<nDim ; ++d){
            min[d] = 0;
            max[d] = (long)xformDims[d];
        }
        float[] minmax = FusionHelper.minMax(img);  
	RealRandomAccessible< UnsignedShortType > realview = RealViews.affineReal( interpolated, affine );
        RandomAccessibleInterval< UnsignedShortType > view = Views.interval( Views.raster( realview ),min,max);
        return new TimePointImage(view,minmax,time,xformDims,dataset);
    }
    public void setAngle(int a){
        this.angle = a;
    }
    public void setChannel(int ch){
        this.channel = ch;
    }
    public void setIllumination(int il){
        this.illum = il;
    }
    @Override
    public int getTimes() {
        return timepoints.size();
    } 
    public String getFile(){
        return result.getXMLFileName();
    }

   
    LoadParseQueryXML result;
    TreeMap<String,DataSetDesc> dataSetMap = new TreeMap<>();
    int angle;
    int channel;
    int illum;
    final List< mpicbg.spim.data.sequence.TimePoint > timepoints;
    final List< Angle > angles;
    final List< Channel > channels;
    final List< Illumination > illuminations;
    SpimData2 spimData;

    @Override
    public Collection<DataSetDesc> getDataSets() {
        return this.dataSetMap.values();
    }





}
