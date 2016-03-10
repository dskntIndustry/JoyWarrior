package ch.dsknt.chart;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;

import org.knowm.xchart.Chart_XY;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.internal.chartpart.Chart;

import ch.dsknt.usb.USBHandler;

public class RTChart
{
	private USBHandler usbhandler = new USBHandler();
	private final static int MAX_WINDOWS_SAMPLES = 1024;
	
	private List<Double> yData = new CopyOnWriteArrayList<Double>();
	private List<Double> xData = new CopyOnWriteArrayList<Double>();
	private List<Double> zData = new CopyOnWriteArrayList<Double>();
	private List<Double> tData = new CopyOnWriteArrayList<Double>();
	public static final String SERIES_NAME_X = "X";
	public static final String SERIES_NAME_Y = "Y";
	public static final String SERIES_NAME_Z = "Z";
	private final XChartPanel<Chart<?, ?>> chartPanel;
	Chart_XY chart;

	public RTChart()
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth();
		double height = screenSize.getHeight();
		getData(1);
		// Create Chart
		chart = new Chart_XY((int)width-15, (int)height);
		chartPanel = buildPanel();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JFrame frame = new JFrame("Seismic Data Visualizer");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(chartPanel);
				frame.pack();
				frame.setVisible(true);
			}
		});
		TimerTask chartUpdaterTask = new TimerTask()
		{
			@Override
			public void run()
			{
				updateData();
				chartPanel.updateSeries(SERIES_NAME_X, null, getXData(), null);
				chartPanel.updateSeries(SERIES_NAME_Y, null, getYData(), null);
				chartPanel.updateSeries(SERIES_NAME_Z, null, getZData(), null);
				
			}
		};
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(chartUpdaterTask, 0, USBHandler.READ_UPDATE_DELAY_MS);
	}

	public XChartPanel<Chart<?, ?>> buildPanel()
	{
		return new XChartPanel<Chart<?, ?>>(getChart());
	}

	public Chart<?, ?> getChart()
	{

		chart.setTitle("JoyWarrior Real-Time plot v1");
		chart.setXAxisTitle("Sample number");
		chart.setYAxisTitle("Acceleration [m/s^2]");
		chart.getStyler().setYAxisMin(-10);
		chart.getStyler().setYAxisMax(10);
		chart.getStyler().setMarkerSize(0);
		chart.addSeries(SERIES_NAME_X, null, xData);
		chart.getSeriesMap().get(SERIES_NAME_X).setLineStyle((new BasicStroke(0.9f)));
		chart.addSeries(SERIES_NAME_Y, null, yData);
		chart.getSeriesMap().get(SERIES_NAME_Y).setLineStyle((new BasicStroke(0.9f)));
		chart.addSeries(SERIES_NAME_Z, null, zData);
		chart.getSeriesMap().get(SERIES_NAME_Z).setLineStyle((new BasicStroke(0.9f)));
		return chart;
	}

	private void getData(int numPoints)
	{
		for(int i = 0; i < numPoints; i++)
		{
			usbhandler.doDataRead();
			xData.add((double)usbhandler.getX());
			yData.add((double)usbhandler.getY());
			zData.add((double)usbhandler.getZ());
		}
	}

	public void updateData()
	{
		getData(1);
		
		// Limit the total number of points
		while(xData.size() > MAX_WINDOWS_SAMPLES)
		{
			xData.remove(0);
		}

		while(yData.size() > MAX_WINDOWS_SAMPLES)
		{
			yData.remove(0);
		}

		while(zData.size() > MAX_WINDOWS_SAMPLES)
		{
			zData.remove(0);
		}
		
	}

	public List<Double> getXData()
	{
		return xData;
	}

	public List<Double> getYData()
	{
		return yData;
	}

	public List<Double> getZData()
	{
		return zData;
	}
	
	public List<Double> getTime()
	{
		return tData;
	}
}