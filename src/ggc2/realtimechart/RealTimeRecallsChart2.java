package ggc2.realtimechart;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 * ---------------------
 * DynamicDataDemo3.java
 * ---------------------
 * (C) Copyright 2004, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited).
 * Contributor(s):   -;
 *
 * $Id: DynamicDataDemo3.java,v 1.5 2004/04/26 19:11:54 taqua Exp $
 *
 * Changes
 * -------
 * 02-Mar-2004 : Version 1 (DG);
 *
 */


import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class RealTimeRecallsChart2 extends ApplicationFrame {

	public static JFreeChart xylineChart;
	//public int counter = 0;
	public static double percentNeg = 0;
	
   public RealTimeRecallsChart2( String applicationTitle, String chartTitle ) {
      super(applicationTitle);
      xylineChart = ChartFactory.createXYLineChart(
         chartTitle ,
         "Instances" ,
         "Recall" ,
         createDataset() ,
         PlotOrientation.VERTICAL ,
         true , true , false);
         
      ChartPanel chartPanel = new ChartPanel( xylineChart );
      chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 367 ) );
      final XYPlot plot = xylineChart.getXYPlot( );
      
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer( );
      renderer.setSeriesPaint( 0 , Color.RED );
      renderer.setSeriesPaint( 1 , Color.BLUE);
      renderer.setSeriesPaint( 2 , Color.DARK_GRAY);
      renderer.setSeriesPaint( 3 , Color.CYAN);
      renderer.setSeriesPaint( 4 , Color.GREEN);
      renderer.setSeriesPaint( 5 , Color.ORANGE);
      renderer.setSeriesShapesVisible(0, false);
      renderer.setSeriesShapesVisible(1, false);
      renderer.setSeriesShapesVisible(2, false);
      renderer.setSeriesShapesVisible(3, false);
      renderer.setSeriesShapesVisible(4, false);
      renderer.setSeriesShapesVisible(5, false);
      
//      renderer.setSeriesStroke( 0 , new BasicStroke( 4.0f ) );
//      renderer.setSeriesStroke( 1 , new BasicStroke( 3.0f ) );
//      renderer.setSeriesStroke( 2 , new BasicStroke( 2.0f ) );
      plot.setRenderer( renderer ); 
      setContentPane( chartPanel ); 
   }
   
   private XYDataset createDataset( ) {
      final XYSeries recPos = new XYSeries( "Recall1" );          
//      firefox.add( 1.0 , 1.0 );          
//      firefox.add( 2.0 , 4.0 );          
//      firefox.add( 3.0 , 3.0 );          
      
      final XYSeries recNeg = new XYSeries( "Recall0" );          
//      chrome.add( 1.0 , 4.0 );          
//      chrome.add( 2.0 , 5.0 );          
//      chrome.add( 3.0 , 6.0 );          
      
      final XYSeries percNeg = new XYSeries( "% G-mean" );          
//    chrome.add( 1.0 , 4.0 );          
//    chrome.add( 2.0 , 5.0 );          
//    chrome.add( 3.0 , 6.0 );          
      
      final XYSeries percLeafsNeg = new XYSeries( "average prediction" );      
      
      final XYSeries recall0Val = new XYSeries( "% recall0Val" );
      
      final XYSeries recall1Val = new XYSeries( "% recall1Val" );
      
      final XYSeriesCollection dataset = new XYSeriesCollection( );          
      dataset.addSeries( recPos );          
      dataset.addSeries( recNeg );
      dataset.addSeries( percNeg );
      dataset.addSeries( percLeafsNeg  );
      dataset.addSeries( recall0Val  );
      dataset.addSeries( recall1Val  );
      
      
      return dataset;
   }

   public static void main( String[ ] args ) {
	   RealTimeRecallsChart2 chart = new RealTimeRecallsChart2("Defective Software Prediction",
         "Defective Software Prediction");
      chart.pack( );
      
      RefineryUtilities.centerFrameOnScreen( chart );          
      chart.setVisible( true );
      
      ((XYSeriesCollection)xylineChart.getXYPlot().getDataset()).getSeries(0).add(1, 3);
      ((XYSeriesCollection)xylineChart.getXYPlot().getDataset()).getSeries(0).add(2, 2);
      
      ((XYSeriesCollection)xylineChart.getXYPlot().getDataset()).getSeries(1).add(1, 2);
      ((XYSeriesCollection)xylineChart.getXYPlot().getDataset()).getSeries(1).add(2, 1);
   }
}