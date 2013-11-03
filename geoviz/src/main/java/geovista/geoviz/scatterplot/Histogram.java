/* Licensed under LGPL v. 2.1 or any later version;

 see GNU LGPL for details.
 Authors: Xiping Dai and Frank Hardisty 
 Edited by Wei Luo to allow the drawing of the value for each bin.
 */

package geovista.geoviz.scatterplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.jts.EmptyShape;
import geovista.common.ui.ShapeReporter;

public class Histogram extends JPanel implements MouseListener,
	MouseMotionListener, ComponentListener, DataSetListener,
	SelectionListener, IndicationListener, ShapeReporter {
    private static double AXISSPACEPORTION = 1.0 / 6.0;
    private static int DEFAULT_HIST_NUM = 5;
    transient private double[] data;  // record all numeric data for one column
    transient private String variableName;
    transient private DataArray dataArray;
    transient private DataArray histArray;
    transient private double[] histogramArray;
    transient private double[] accumulativeFrequency;
    transient private double[] dataX;
    private int binCount = DEFAULT_HIST_NUM;
    transient private double barWidth;
    private boolean axisOn = true;
    transient private boolean accFrequency = false;
    transient private int plotOriginX;
    transient private int plotOriginY;
    transient private int plotEndX;
    transient private int plotEndY;
    transient private double[] xAxisExtents;
    transient private double[] yAxisExtents;
    transient private int[] exsInt; // bar x position
    transient private int[] whyInt;
    transient private int[] accumulativeInt;
    transient private int[] selectionInt;
    transient private double[] classBoundareis;
    transient private int[] classBoundariesInt;
    // private Vector selRecords = new Vector();
    int[] savedSelection;
    transient private BitSet selectedRecords;
    transient private int indicatedObs;

    transient private int indicatedBin;
    private final Set<Integer> indicatedNeighborBins;

    transient private Vector<Integer>[] histRecords;
    transient private Rectangle[] histRecs;
    transient private double[] selectionArray; // the count of selected
    // observation in each histogram
    // bin.
    transient private Color background;
    transient private Color foreground;
    private Color histFillColor = Color.GRAY;
    private final Color neighborHistFillColor = Color.DARK_GRAY;
    private final Color indicationColor = Color.GREEN;
    private final float strokeSize = 3f;
    BasicStroke stroke = new BasicStroke(strokeSize, BasicStroke.CAP_ROUND,
	    BasicStroke.JOIN_ROUND);

    transient private final JPopupMenu popup;
    transient private JDialog dialog1;
    transient private JDialog dialog2;
    private final JTextField histNumberField = new JTextField(16);
    private final JTextField yAxisMinField = new JTextField(16);
    private final JTextField yAxisMaxField = new JTextField(16);
    private final JTextField xAxisMinField = new JTextField(16);
    private final JTextField xAxisMaxField = new JTextField(16);
    private final EventListenerList listenerListAction = new EventListenerList();
    private boolean drawBox;
    private int mouseX2;
    private int mouseY2;
    private int mouseX1;
    private int mouseY1;
    protected final static Logger logger = Logger.getLogger(Histogram.class
	    .getName());

    public Histogram() {
	// initialization goes more smoothly with some data to chew on
	double[] data = { 1, 2, 3, 4, 3, 2, 1, 2, 3, 4, 5, 66 };
	dataX = data;
	Dimension size = new Dimension(300, 200);
	setPreferredSize(size);
	this.setSize(size);
	indicatedBin = -1;// deslected
	indicatedNeighborBins = new HashSet<Integer>();

	addMouseListener(this);
	addMouseMotionListener(this);

	// Create the popup menu.
	popup = new JPopupMenu();
	JMenuItem menuItem = new JMenuItem("Set Histogram Range");
	menuItem.addActionListener(new ActionListener() {

	    /**
	     * put your documentation comment here
	     * 
	     * @param e
	     */
	    public void actionPerformed(ActionEvent e) {
		showDialog1(400, 400);
	    }
	});
	popup.add(menuItem);
	menuItem = new JMenuItem("Set Frequency Range");
	menuItem.addActionListener(new ActionListener() {

	    /**
	     * put your documentation comment here
	     * 
	     * @param e
	     */
	    public void actionPerformed(ActionEvent e) {
		showDialog2(400, 400);
	    }
	});
	popup.add(menuItem);
	menuItem = new JMenuItem("Accumulative Frequency");
	menuItem.addActionListener(new ActionListener() {

	    /**
	     * put your documentation comment here
	     * 
	     * @param e
	     */
	    public void actionPerformed(ActionEvent e) {
		setAccFrequency(!accFrequency);
		repaint();
	    }
	});
	popup.add(menuItem);
	addComponentListener(this);
	// setData(data);

    }

    public void setData(double[] data) {
	this.data = data;
	// this.sortedData = new double[data.length];
	// System.arraycopy(data, 0, sortedData, 0, this.data.length);
	// Arrays.sort(sortedData);
	dataArray = new DataArray(data);
	xAxisExtents = dataArray.getExtent().clone();
	selectedRecords = new BitSet(data.length);
	histogramCalculation();
	setupDataforDisplay();
	setAccumulativeFrequency();

    }

    public double[] getData() {
	return data;
    }

    public void setVariableName(String name) {
	variableName = name;
	this.repaint();
    }

    public String getVariableName() {
	return variableName;
    }

    public void setHistNumber(int num) {
	binCount = num;
	setData(data);

    }

    public int getHistNumber() {
	return binCount;
    }

    public void setAxisOn(boolean axisOn) {
	this.axisOn = axisOn;
    }

    public void setHistgramFillColor(Color color) {
	histFillColor = color;
    }

    public boolean getAxisOn() {
	return axisOn;
    }

    public void setClassBoundaries(double[] boundaries) {
	classBoundareis = boundaries;
	setupDataforDisplay();
	this.repaint();
    }

    public void setSelections(BitSet selectedObs) {
	selectedRecords = selectedObs;
	// reset selectionarray

	selectionArray = new double[binCount];

	// add selected observations to the appropriate bins
	for (int i = 0; i < data.length; i++) {

	    if (selectedRecords.get(i)) {
		if (data[i] >= xAxisExtents[0] && data[i] <= xAxisExtents[1]) {
		    int j = (int) Math.floor((data[i] - xAxisExtents[0])
			    / barWidth);
		    j = ((binCount <= j) ? binCount - 1 : j);
		    if (logger.isLoggable(Level.FINEST)) {
			logger.finest("adding " + j);
		    }
		    selectionArray[j]++;
		}
	    }
	}
	setSelectionScreen();
    }

    public BitSet getSelections() {
	return selectedRecords;
    }

    /*
     * public Vector getSelection (){ return this.selRecords; }
     */

    /**
     * Minimum and maximum values for xAxis. xAxisExtents[0] = min,
     * xAxisExtents[1] = max.
     * 
     * @param double[] xAxisExtents
     */
    public void setXAxisExtents(double[] xAxisExtents) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("set up axis ..." + xAxisExtents[0]);
	}
	this.xAxisExtents = xAxisExtents.clone();

	histogramCalculation();
	setupDataforDisplay();
	repaint();
    }

    /**
     * put your documentation comment here
     * 
     * @return
     */
    public double[] getXAxisExtents() {
	return xAxisExtents;
    }

    public void setAccFrequency(boolean accFrequency) {
	this.accFrequency = accFrequency;
    }

    /**
     * put your documentation comment here
     * 
     * @param c
     */
    @Override
    public void setBackground(Color c) {
	if (c == null) {
	    return;
	}
	background = c;
	int colorTotal = c.getRed() + c.getGreen() + c.getBlue();
	int greyColor = 128 * 3;
	if (colorTotal < greyColor) {
	    foreground = Color.white;
	} else {
	    foreground = Color.black;
	}
	this.repaint();
    }

    private void histogramCalculation() {
	if (data == null || xAxisExtents == null) {
	    return;
	}
	if (data.length < binCount) {
	    binCount = data.length;
	}

	histogramArray = new double[binCount];
	histRecords = new Vector[binCount];
	histRecs = new Rectangle[binCount];

	for (int i = 0; i < binCount; i++) {
	    histRecords[i] = new Vector<Integer>();
	}

	barWidth = (xAxisExtents[1] - xAxisExtents[0]) / binCount;
	for (int i = 0; i < data.length; i++) {
	    if (data[i] >= xAxisExtents[0] && data[i] <= xAxisExtents[1]) {
		int j = (int) Math
			.floor((data[i] - xAxisExtents[0]) / barWidth);
		j = ((binCount <= j) ? binCount - 1 : j);

		histogramArray[j]++;
		histRecords[j].add(i);
	    }
	}

	dataX = new double[binCount];
	accumulativeFrequency = new double[binCount];
	for (int i = 0; i < binCount; i++) {
	    dataX[i] = i * barWidth + xAxisExtents[0];
	    if (i == 0) {
		accumulativeFrequency[i] = histogramArray[i];
	    } else {
		accumulativeFrequency[i] = accumulativeFrequency[i - 1]
			+ histogramArray[i];
	    }
	}

	histArray = new DataArray(histogramArray);
	double[] zeroFloor = { 0, histArray.getExtent()[1] };
	histArray.setExtent(zeroFloor);
	yAxisExtents = histArray.getMaxMinCoorValue();
    }

    @Override
    public void paintComponent(Graphics g) {
	g.setColor(background);
	if (histFillColor != null) {
	    g.fillRect(0, 0, getSize().width, getSize().height);
	}
	g.setColor(foreground);
	if (axisOn == true) {
	    drawAxis(g);
	}
	drawBars(g);
	fillIndication(g);
	if (selectionArray != null) {
	    drawSelection(g);
	}
	if (accFrequency == true) {
	    drawAccumulativeFrequency(g);
	}
	if (classBoundariesInt != null) {
	    // logger.finest("class boundaries not null");
	    drawClassBoundaries(g);
	}
	drawIndication(g);
	drawSelectionBox((Graphics2D) g);

    }

    private void drawSelectionBox(Graphics2D g2) {
	// draw selection box
	if (!drawBox) {
	    return;
	}

	Stroke tempStroke = g2.getStroke();
	float[] dash = new float[] { 5f, 7f, 5f };
	Stroke boxStroke =

	new BasicStroke((float) 2.0, BasicStroke.CAP_ROUND,
		BasicStroke.JOIN_ROUND, (float) 10.0, dash, 0);

	g2.setStroke(boxStroke);
	g2.setPaintMode();
	g2.setColor(Color.black);
	// g2.setXORMode(Color.white);

	// let's take drawing the selection rectangle by cases
	// not elegant, but the alternative is introducing more class variables
	int selectX = 0;
	int selectY = 0;
	int selectWidth = 0;
	int selectHeight = 0;

	if ((mouseX1 <= mouseX2) && (mouseY1 <= mouseY2)) {
	    selectX = mouseX1;
	    selectY = mouseY1;
	    selectWidth = mouseX2 - mouseX1;
	    selectHeight = mouseY2 - mouseY1;
	}

	if ((mouseX2 < mouseX1) && (mouseY1 <= mouseY2)) {
	    selectX = mouseX2;
	    selectY = mouseY1;
	    selectWidth = mouseX1 - mouseX2;
	    selectHeight = mouseY2 - mouseY1;
	}

	if ((mouseX1 <= mouseX2) && (mouseY2 < mouseY1)) {
	    selectX = mouseX1;
	    selectY = mouseY2;
	    selectWidth = mouseX2 - mouseX1;
	    selectHeight = mouseY1 - mouseY2;
	}

	if ((mouseX2 < mouseX1) && (mouseY2 < mouseY1)) {
	    selectX = mouseX2;
	    selectY = mouseY2;
	    selectWidth = mouseX1 - mouseX2;
	    selectHeight = mouseY1 - mouseY2;
	}

	g2.drawRect(selectX, selectY, selectWidth, selectHeight);
	g2.setStroke(tempStroke);
    }

    private void drawBars(Graphics g) {
	logger.finest("drawing bars: " + binCount);
	int len = binCount;
	if (exsInt == null) {
	    logger.info("Histogram, drawBars, trying to draw data without x data");
	    return;
	}
	for (int i = 0; i < len - 1; i++) {
	    g.drawRect(exsInt[i], whyInt[i], exsInt[i + 1] - exsInt[i],
		    plotOriginY - whyInt[i]);

	    if (indicatedNeighborBins.contains(i)) {
		g.setColor(neighborHistFillColor);
	    } else {
		g.setColor(histFillColor);
	    }
	    g.fillRect(exsInt[i] + 1, whyInt[i] + 1, exsInt[i + 1] - exsInt[i]
		    - 1, plotOriginY - whyInt[i] - 1);
	    g.setColor(foreground);

	}
	g.drawRect(exsInt[len - 1], whyInt[len - 1],
		plotEndX - exsInt[len - 1], plotOriginY - whyInt[len - 1]);
	if (histFillColor != null) {
	    g.setColor(histFillColor);
	    g.fillRect(exsInt[len - 1] + 1, whyInt[len - 1] + 1, plotEndX
		    - exsInt[len - 1] - 1, plotOriginY - whyInt[len - 1] - 1);
	    g.setColor(foreground);
	}
    }

    private void setSelectionScreen() {
	if (selectionArray == null) {
	    return;
	}
	selectionInt = new int[selectionArray.length];
	double scale;
	scale = getScale(plotOriginY, plotEndY, yAxisExtents[0],
		yAxisExtents[1]);
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("scale = " + scale);
	}
	selectionInt = getValueScreen(selectionArray, scale, plotOriginY, 0);
    }

    private void drawSelection(Graphics g) {
	for (int i = 0; i < binCount; i++) {
	    int x, y, width, height;

	    if (selectionArray[i] > 0) {
		x = exsInt[i];
		y = selectionInt[i];
		height = plotOriginY - selectionInt[i];
		if (i == binCount - 1) {
		    width = plotEndX - exsInt[binCount - 1];
		} else {
		    width = exsInt[i + 1] - exsInt[i];

		}
		g.drawRect(x, y, width, height);
		g.setColor(Color.blue);
		g.fillRect(x + 1, y + 1, width - 1, height - 1);
		g.setColor(foreground);
	    }
	}

    }

    private void drawIndication(Graphics g) {
	int len = binCount;
	if (indicatedBin < 0) {
	    return;
	}
	Graphics2D g2 = (Graphics2D) g;
	Stroke currStroke = g2.getStroke();
	RenderingHints hints = g2.getRenderingHints();
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		RenderingHints.VALUE_ANTIALIAS_ON);
	g2.setStroke(stroke);

	if (indicatedBin < len - 1) {
	    g2.drawRect(exsInt[indicatedBin], whyInt[indicatedBin],
		    exsInt[indicatedBin + 1] - exsInt[indicatedBin],
		    plotOriginY - whyInt[indicatedBin]);

	} else if (indicatedBin == len - 1) {
	    g2.drawRect(exsInt[len - 1], whyInt[len - 1], plotEndX
		    - exsInt[len - 1], plotOriginY - whyInt[len - 1]);

	}

	g2.setStroke(currStroke);
	g2.setRenderingHints(hints);
    }

    private void fillIndication(Graphics g) {
	int len = binCount;
	if (indicatedBin < 0) {
	    return;
	}

	if (indicatedBin < len - 1) {

	    g.setColor(indicationColor);
	    g.fillRect(exsInt[indicatedBin] + 1, whyInt[indicatedBin] + 1,
		    exsInt[indicatedBin + 1] - exsInt[indicatedBin] - 1,
		    plotOriginY - whyInt[indicatedBin] - 1);

	    g.setColor(foreground);

	} else if (indicatedBin == len - 1) {

	    g.setColor(indicationColor);
	    g.fillRect(exsInt[len - 1] + 1, whyInt[len - 1] + 1, plotEndX
		    - exsInt[len - 1] - 1, plotOriginY - whyInt[len - 1] - 1);
	    g.setColor(foreground);

	}

    }

    private void drawAxis(Graphics g) {
	int plotWidth, plotHeight;
	plotWidth = (int) this.getSize().getWidth();
	plotHeight = (int) this.getSize().getHeight();
	g.setColor(foreground);
	g.drawLine(plotOriginX, plotEndY, plotOriginX, plotOriginY);
	g.drawLine(plotOriginX, plotOriginY, plotEndX, plotOriginY);
	// draw tick bars for scales on Y coordinate
	int fontSize;
	if (plotWidth < plotHeight) {
	    if (plotWidth < 300) {
		fontSize = 9;
	    } else {
		fontSize = (plotWidth / 32);
	    }
	} else {
	    if (plotHeight < 300) {
		fontSize = 9;
	    } else {
		fontSize = (plotHeight / 32);
	    }
	}
	Font font = new Font("", Font.PLAIN, fontSize);
	g.setFont(font);
	// draw the labels on y axis (frequency).
	String scaleStringY;
	if (histArray == null) {
	    logger.warning("Histogram, drawAxis, histArray is null.");
	    return;
	}
	double barNumber = histArray.getTickNumber();
	double yBarDistance = ((plotOriginY - plotEndY) / barNumber);
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("drawaxis: " + plotOriginY + " " + plotEndY + " "
		    + yBarDistance + " " + barNumber);
	}
	for (int i = 0; i <= barNumber; i++) {
	    g.drawLine(plotOriginX - 3, plotEndY + (int) (i * yBarDistance),
		    plotOriginX, plotEndY + (int) (i * yBarDistance));
	    if (Math.abs(histArray.getMajorTick()) <= 1) {
		scaleStringY = Float.toString((float) (yAxisExtents[1] - i
			* histArray.getMajorTick()));
	    } else {
		scaleStringY = Integer.toString((int) (yAxisExtents[1] - i
			* histArray.getMajorTick()));
	    }
	    g.drawString(scaleStringY, plotOriginX
		    - (int) (plotWidth * AXISSPACEPORTION / 2), plotEndY
		    + (int) (i * yBarDistance + yBarDistance * 1 / 6));
	}
	// draw the labels on x axis.
	// First tick.
	String scaleStringX;
	g.drawLine(plotOriginX, plotOriginY, plotOriginX, plotOriginY + 3);
	if (Math.abs(xAxisExtents[0]) <= 1) {
	    scaleStringX = Float.toString((float) xAxisExtents[0]);
	} else {
	    scaleStringX = Integer.toString((int) xAxisExtents[0]);
	}
	g.drawString(scaleStringX, plotOriginX - 3, plotOriginY
		+ (int) (plotHeight * AXISSPACEPORTION / 4));
	// Last tick.
	g.drawLine(plotEndX, plotOriginY, plotEndX, plotOriginY + 3);
	if (Math.abs(xAxisExtents[1]) <= 1) {
	    scaleStringX = Float.toString((float) xAxisExtents[1]);
	} else {
	    scaleStringX = Integer.toString((int) xAxisExtents[1]);
	}
	g.drawString(scaleStringX, plotEndX - 8, plotOriginY
		+ (int) (plotHeight * AXISSPACEPORTION / 4));
	
	
	// ticks in the middle
	
	 
	 int len = data.length; 
	 
	 
	 
	 double sortedData[] = data.clone();
	 double temp1[]   = new double[data.length]; 
		for (int i=0; i<len;i++){
		
		    for (int j=i+1;j<len;j++){
			if(sortedData[i]>sortedData[j]){
				temp1[i] 	=	sortedData[i];
				sortedData[i] 	= 	sortedData[j];
				sortedData[j]	= 	temp1[i];
			}
	
		}
		}
		
		
	double barBinValues[] = new double [exsInt.length];
	for (int i=0; i<barBinValues.length; i++){
	    barBinValues[i]   =  sortedData[(int) accumulativeFrequency[i]-1];
	}
	
	for (int i=0; i<exsInt.length-1; i++){    
	    g.drawString(Double.toString(barBinValues[i]), exsInt[i+1], plotOriginY
			+ (int) (plotHeight * AXISSPACEPORTION / 4));
	}
	
	
	font = new Font("", Font.PLAIN, fontSize + 3);
	g.setFont(font);
	// draw X axis attribute string
	g.drawString(variableName, plotOriginX + (plotEndX - plotOriginX) / 2
		- plotWidth / 12, plotOriginY + plotHeight / 6 - 5);
	// draw Y axis attribute string. Need rotation for drawing the string
	// vertically.
	Graphics2D g2d = (Graphics2D) g;
	g2d.rotate(-Math.PI / 2, plotOriginX - plotWidth / 9, plotOriginY
		- (plotOriginY - plotEndY) / 3);
	g2d.drawString("Frequency", plotOriginX - plotWidth / 9, plotOriginY
		- (plotOriginY - plotEndY) / 3);
	g2d.rotate(+Math.PI / 2, plotOriginX - plotWidth / 9, plotOriginY
		- (plotOriginY - plotEndY) / 3);
    }

    private void setAccumulativeFrequency() {

	accumulativeInt = new int[accumulativeFrequency.length];
	double scale;
	scale = getScale(plotOriginY, plotEndY, 0, data.length);
	accumulativeInt = getValueScreen(accumulativeFrequency, scale,
		plotOriginY, 0);
    }

    private void drawAccumulativeFrequency(Graphics g) {
	int len = binCount;
	g.setColor(Color.blue);
	Graphics2D g2d = (Graphics2D) g;
	g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT,
		BasicStroke.JOIN_BEVEL));
	for (int i = 0; i < len - 1; i++) {
	    g.drawLine(exsInt[i], accumulativeInt[i], exsInt[i + 1],
		    accumulativeInt[i]);
	}
	g.drawLine(exsInt[len - 1], accumulativeInt[len - 1], plotEndX,
		accumulativeInt[len - 1]);
	g.drawLine(plotEndX, plotOriginY, plotEndX, plotEndY);
	g2d.setStroke(new BasicStroke(1));
	// g.drawString((new
	// Integer((int)(this.accumulativeFrequency[len-1]))).toString(),
	// this.plotEndX + 1, this.plotEndY + 5);
	g.drawString((new Integer((int) (accumulativeFrequency[len - 1])))
		.toString(), plotEndX + 1, accumulativeInt[len - 1] + 5);
	g.setColor(foreground);
    }

    private void drawClassBoundaries(Graphics g) {
	g.setColor(Color.white);
	for (int i = 1; i < classBoundariesInt.length - 1; i++) {
	    g.drawLine(classBoundariesInt[i], 0, classBoundariesInt[i],
		    getHeight());
	}
    }

    public void componentHidden(ComponentEvent e) {

    }

    public void componentMoved(ComponentEvent e) {

    }

    public void componentResized(ComponentEvent e) {
	logger.finest("in component resized");
	if (data == null) {
	    return;
	}
	if (accumulativeFrequency == null) {
	    // histogramCalculation();
	    return;
	}
	setupDataforDisplay();
	setAccumulativeFrequency();
	setSelectionScreen();
	this.repaint();
    }

    public void componentShown(ComponentEvent e) {
    }

    /**
     * Calculate scale between real data and integer data for showing up on
     * screen.
     * 
     * @param min
     * @param max
     * @param dataMin
     * @param dataMax
     * @return scale
     */
    private double getScale(int min, int max, double dataMin, double dataMax) {
	double scale;
	scale = (max - min) / (dataMax - dataMin);
	// scale = (max - min) / (dataMax);
	return scale;
    }

    /**
     * Convert the numeric values of observations to integer value worked on
     * screen.
     * 
     * @param dataArray
     * @param scale
     * @param min
     * @param dataMin
     * @return valueScreen
     */
    private int[] getValueScreen(double[] dataArray, double scale, int min,
	    double dataMin) {
	int[] valueScreen = new int[dataArray.length];
	for (int i = 0; i < dataArray.length; i++) {
	    if (Double.isNaN(dataArray[i])) {
		valueScreen[i] = Integer.MIN_VALUE;
	    } else {
		valueScreen[i] = (int) ((dataArray[i] - dataMin) * scale + min);
		if (logger.isLoggable(Level.FINEST)) {
		    logger.finest("dataArray[i] = " + dataArray[i]);
		    logger.finest("valueScreen[i] = " + valueScreen[i]);
		}
	    }
	}
	return valueScreen;
    }

    private void setupDataforDisplay() {
	if (xAxisExtents != null) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.finest("In setup data for display ..." + xAxisExtents[0]);
	    }
	}
	if (yAxisExtents == null) {
	    logger.info("Histogram trying to setupDataFordisplay and yAxisExtents = null");
	    return;
	}
	if (axisOn) {
	    plotOriginX = (int) (getWidth() * AXISSPACEPORTION);
	    plotOriginY = (int) (getHeight() * (1 - AXISSPACEPORTION));
	    plotEndX = (getWidth()) - (int) (getWidth() * AXISSPACEPORTION / 2);
	    plotEndY = (int) (getHeight() * AXISSPACEPORTION / 2);
	} else {
	    plotOriginX = 0;
	    plotOriginY = (int) (this.getSize().getHeight() - 2);
	    plotEndX = (int) (this.getSize().getWidth()) - 3;
	    plotEndY = 3;
	}
	int len = binCount;
	exsInt = new int[len];
	whyInt = new int[len];
	// get positions on screen
	double scale;
	scale = getScale(plotOriginX, plotEndX, xAxisExtents[0],
		xAxisExtents[1]);
	exsInt = getValueScreen(dataX, scale, plotOriginX, xAxisExtents[0]);
	scale = getScale(plotOriginY, plotEndY, yAxisExtents[0],
		yAxisExtents[1]);
	whyInt = getValueScreen(histogramArray, scale, plotOriginY,
		yAxisExtents[0]);
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("setupdisplay: " + plotOriginY + " " + plotEndY + " "
		    + scale);
	}
	for (int i = 0; i < binCount - 1; i++) {
	    histRecs[i] = new Rectangle(exsInt[i], whyInt[i], exsInt[i + 1]
		    - exsInt[i], plotOriginY - whyInt[i]);
	}
	histRecs[binCount - 1] = new Rectangle(exsInt[len - 1],
		whyInt[len - 1], plotEndX - exsInt[len - 1], plotOriginY
			- whyInt[len - 1]);
	// get class boundaries' positions on screen
	if (classBoundareis != null) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.finest("x and y boundaries are not null.");
	    }
	    classBoundariesInt = new int[classBoundareis.length];
	    classBoundariesInt = getValueScreen(classBoundareis, scale,
		    plotOriginX, xAxisExtents[0]);
	}

    }

    /**
     * New data ranges setup dialog.
     * 
     * @param x
     * @param y
     */
    private void showDialog1(int x, int y) {

	if (dialog1 == null) {
	    JFrame dummyFrame = new JFrame();
	    dialog1 = new JDialog(dummyFrame, "Data Range Configuer", true);
	    JButton actionButton;
	    JButton resetButton;
	    dialog1.setLocation(x, y);
	    dialog1.setSize(300, 100);
	    dialog1.getContentPane().setLayout(new GridLayout(4, 2));
	    // create buttons for action
	    actionButton = new JButton("Apply");
	    actionButton.addActionListener(new java.awt.event.ActionListener() {

		/**
		 * Button to set up new data ranges shown up in scatter plot.
		 * 
		 * @param e
		 */
		public void actionPerformed(ActionEvent e) {
		    try {
			actionButton_actionPerformed(e);
		    } catch (Exception exception) {
			exception.printStackTrace();
		    }
		}
	    });
	    resetButton = new JButton("Reset");
	    resetButton.addActionListener(new java.awt.event.ActionListener() {

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void actionPerformed(ActionEvent e) {
		    resetButton_actionPerformed(e);
		}
	    });
	    histNumberField.setText(Integer.toString(binCount));
	    xAxisMinField.setText(Double.toString(xAxisExtents[0]));
	    xAxisMaxField.setText(Double.toString(xAxisExtents[1]));
	    dialog1.getContentPane().add(new JLabel(("Histogram Number")));
	    dialog1.getContentPane().add(histNumberField);
	    dialog1.getContentPane().add(new JLabel(("DataRange Min")));
	    dialog1.getContentPane().add(xAxisMinField);
	    dialog1.getContentPane().add(new JLabel(("DataRange Max")));
	    dialog1.getContentPane().add(xAxisMaxField);
	    dialog1.getContentPane().add(actionButton);
	    dialog1.getContentPane().add(resetButton);
	}
	dialog1.setVisible(true);
    }

    /**
     * Set up new data ranges to show.
     * 
     * @param e
     */
    private void actionButton_actionPerformed(ActionEvent e) {
	// get the input data from text field
	binCount = Integer.parseInt(histNumberField.getText());
	xAxisExtents[0] = Double.parseDouble(xAxisMinField.getText());
	xAxisExtents[1] = Double.parseDouble(xAxisMaxField.getText());
	histogramCalculation();
	setupDataforDisplay();
	setAccumulativeFrequency();
	// fireActionPerformed(COMMAND_DATARANGE_SET);
	logger.finest("ok, fire event.");
	repaint();
	dialog1.setVisible(false);
    }

    /**
     * put your documentation comment here
     * 
     * @param e
     */
    private void resetButton_actionPerformed(ActionEvent e) {
	binCount = Histogram.DEFAULT_HIST_NUM;
	xAxisExtents = dataArray.getExtent().clone();
	// yAxisExtents = (double[])this.histArray.getMaxMinCoorValue().clone();

	histNumberField.setText(Integer.toString(binCount));
	xAxisMinField.setText(Double.toString(xAxisExtents[0]));
	xAxisMaxField.setText(Double.toString(xAxisExtents[1]));
	histogramCalculation();
	setupDataforDisplay();
	setAccumulativeFrequency();
	// fireActionPerformed(COMMAND_DATARANGE_SET);
	repaint();
	dialog1.setVisible(false);
    }

    /**
     * New data ranges setup dialog.
     * 
     * @param x
     * @param y
     */
    private void showDialog2(int x, int y) {
	JFrame dummyFrame = new JFrame();
	dialog2 = new JDialog(dummyFrame, "Frequency Range Configuer", true);
	JButton actionButton;
	JButton resetButton;
	dialog2.setLocation(x, y);
	dialog2.setSize(300, 100);
	dialog2.getContentPane().setLayout(new GridLayout(3, 2));
	yAxisMinField.setText(Double.toString(yAxisExtents[0]));
	yAxisMaxField.setText(Double.toString(yAxisExtents[1]));
	// create buttons for action
	actionButton = new JButton("Apply");
	actionButton.addActionListener(new java.awt.event.ActionListener() {

	    /**
	     * Button to set up new data ranges shown up in scatter plot.
	     * 
	     * @param e
	     */
	    public void actionPerformed(ActionEvent e) {
		try {
		    actionButton2_actionPerformed(e);
		} catch (Exception exception) {
		    exception.printStackTrace();
		}
	    }
	});
	resetButton = new JButton("Reset");
	resetButton.addActionListener(new java.awt.event.ActionListener() {

	    /**
	     * put your documentation comment here
	     * 
	     * @param e
	     */
	    public void actionPerformed(ActionEvent e) {
		resetButton2_actionPerformed(e);
	    }
	});
	dialog2.getContentPane().add(new JLabel(("Frequency" + " Min")));
	dialog2.getContentPane().add(yAxisMinField);
	dialog2.getContentPane().add(new JLabel(("Frequency" + " Max")));
	dialog2.getContentPane().add(yAxisMaxField);
	dialog2.getContentPane().add(actionButton);
	dialog2.getContentPane().add(resetButton);
	dialog2.setVisible(true);
    }

    /**
     * Set up new data ranges to show.
     * 
     * @param e
     */
    private void actionButton2_actionPerformed(ActionEvent e) {
	// get the input data from text field
	yAxisExtents[0] = Double.parseDouble(yAxisMinField.getText());
	yAxisExtents[1] = Double.parseDouble(yAxisMaxField.getText());
	histArray.setExtent(yAxisExtents);
	setupDataforDisplay();
	// fireActionPerformed(COMMAND_DATARANGE_SET);
	logger.finest("ok, fire event.");
	repaint();
	dialog2.setVisible(false);
    }

    /**
     * put your documentation comment here
     * 
     * @param e
     */
    private void resetButton2_actionPerformed(ActionEvent e) {
	histArray.setDataExtent();
	yAxisExtents = histArray.getMaxMinCoorValue().clone();
	histogramCalculation();
	yAxisMinField.setText(Double.toString(yAxisExtents[0]));
	yAxisMaxField.setText(Double.toString(yAxisExtents[1]));
	setupDataforDisplay();
	// fireActionPerformed(COMMAND_DATARANGE_SET);
	repaint();
	dialog2.setVisible(false);
    }

    /**
     * put your documentation comment here
     * 
     * @param e
     */
    private void maybeShowPopup(MouseEvent e) {
	if (e.isPopupTrigger()) {
	    popup.show(e.getComponent(), e.getX(), e.getY());
	}
    }

    public void mouseClicked(MouseEvent e) {

	// If shift and cntrl not pressed, reset selection.
	if (e.isShiftDown() == false && e.isControlDown() == false) {
	    // this.selRecords.clear();
	    selectedRecords.clear();

	    if (selectionArray == null || selectionArray.length != binCount) {
		selectionArray = new double[binCount];
	    } else {
		for (int i = 0; i < selectionArray.length; i++) {
		    selectionArray[i] = 0;
		}
	    }
	}

	// select performed.

	for (int i = 0; i < binCount; i++) {
	    if (e.getX() >= histRecs[i].x && e.getX() < histRecs[i].getMaxX()) {
		// if (this.histRecs[i].contains(e.getX(),e.getY())){
		for (int j = 0; j < histRecords[i].size(); j++) {
		    int index = (histRecords[i].get(j)).intValue();
		    if (e.isControlDown() && selectedRecords.get(index)) {

			selectedRecords.set(index, false);
		    } else {
			// new selection, shift selection, ctrl selection if not
			// previosly true
			selectedRecords.set(index, true);
		    }
		}
	    }

	}

	updateSelection();

    }

    private void updateSelection() {
	int counter = 0;
	counter = selectedRecords.cardinality();
	int[] selInts = new int[counter];
	counter = 0;
	for (int i = 0; i < selectedRecords.length(); i++) {
	    boolean val = selectedRecords.get(i);
	    if (val) {
		selInts[counter] = i;
		counter++;
	    }
	}
	fireSelectionChanged(selInts);
	setSelections(selectedRecords);
	repaint();
    }

    public void mousePressed(MouseEvent e) {
	if (e.isPopupTrigger()) {
	    maybeShowPopup(e);
	}
	mouseX1 = e.getX();
	mouseY1 = e.getY();
	mouseX2 = e.getX();
	mouseY2 = e.getY();
    }

    public void mouseReleased(MouseEvent e) {
	if (e.isPopupTrigger()) {
	    maybeShowPopup(e);
	}

	mouseX2 = e.getX();
	mouseY2 = e.getY();

	if (mouseX1 > mouseX2) {
	    int temp = mouseX1;
	    mouseX1 = mouseX2;
	    mouseX2 = temp;
	}

	if (mouseY1 > mouseY2) {
	    int temp = mouseY1;
	    mouseY1 = mouseY2;
	    mouseY2 = temp;
	}

	// If shift not pressed, reset selection.
	if (e.isShiftDown() == false && e.isControlDown() == false) {

	    selectedRecords.clear();

	    if (selectionArray == null || selectionArray.length != binCount) {
		selectionArray = new double[binCount];
	    } else {
		for (int i = 0; i < selectionArray.length; i++) {
		    selectionArray[i] = 0;
		}
	    }
	}

	// select performed.
	int width = mouseX2 - mouseX1;
	int height = mouseY2 - mouseY1;
	Rectangle selRect = new Rectangle(mouseX1, mouseY1, width, height);
	for (int i = 0; i < binCount; i++) {

	    if (selRect.intersects(histRecs[i])) {
		// if (this.histRecs[i].contains(e.getX(),e.getY())){
		for (int j = 0; j < histRecords[i].size(); j++) {
		    int index = (histRecords[i].get(j)).intValue();
		    if (e.isControlDown()) {
			if (selectedRecords.get(index)) {
			    selectedRecords.set(index, false);
			} else {
			    selectedRecords.set(index, true);
			}
		    } else {
			selectedRecords.set(index, true);
		    }
		}
	    }

	}
	updateSelection();
	drawBox = false;
	repaint();
    }

    public void mouseDragged(MouseEvent e) {
	drawBox = true;
	mouseX2 = e.getX();
	mouseY2 = e.getY();

	repaint();
    }

    public void mouseMoved(MouseEvent e) {
	if (histRecs == null) {
	    return;
	}
	for (int i = 0; i < binCount; i++) {
	    if (e.getX() >= histRecs[i].x && e.getX() < histRecs[i].getMaxX()) {
		indicatedBin = i;
		this.repaint();
		return;
	    }
	}
	indicatedBin = -1;
	mouseX2 = e.getX();
	mouseY2 = e.getY();
	this.repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * adds an ActionListener to the button
     */
    public void addActionListener(ActionListener l) {
	listenerListAction.add(ActionListener.class, l);
    }

    /**
     * removes an ActionListener from the button
     */
    public void removeActionListener(ActionListener l) {
	listenerListAction.remove(ActionListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    public void fireActionPerformed() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerListAction.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	ActionEvent e2 = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
		"OK");
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == ActionListener.class) {
		// Lazily create the event:
		((ActionListener) listeners[i + 1]).actionPerformed(e2);
	    }
	}
    }

    /**
     * adds an SelectionListener.
     * 
     * @see EventListenerList
     */
    public void addSelectionListener(SelectionListener l) {
	listenerList.add(SelectionListener.class, l);
    }

    /**
     * removes an SelectionListener from the component.
     * 
     * @see EventListenerList
     */
    public void removeSelectionListener(SelectionListener l) {
	listenerList.remove(SelectionListener.class, l);

    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    protected void fireSelectionChanged(int[] newSelection) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	SelectionEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == SelectionListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new SelectionEvent(this, newSelection);
		}
		((SelectionListener) listeners[i + 1]).selectionChanged(e);
	    }
	}// next i

    }

    public void dataSetChanged(DataSetEvent e) {
	setVariableName(e.getDataSetForApps().getNumericArrayName(0));
	setData(e.getDataSetForApps().getNumericDataAsDouble(0));

    }

    public void selectionChanged(SelectionEvent e) {
	int[] selected = e.getSelection();
	savedSelection = selected;
	if (selected == null || data == null) {
	    return;
	}
	if (selectedRecords == null) {
	    selectedRecords = new BitSet(data.length);
	}
	selectedRecords.clear();
	for (int whichObs : selected) {
	    selectedRecords.set(whichObs, true);

	}
	setSelections(selectedRecords);
	this.repaint();

    }

    public SelectionEvent getSelectionEvent() {
	return new SelectionEvent(this, savedSelection);
    }

    public void indicationChanged(IndicationEvent e) {
	indicatedObs = e.getIndication();
	if (data == null) {
	    return;
	}
	indicatedNeighborBins.clear();
	if (indicatedObs < 0) {
	    indicatedBin = -1;
	    this.repaint();
	    return;
	}
	int bin;

	bin = (int) Math.floor((data[indicatedObs] - xAxisExtents[0])
		/ barWidth);
	bin = ((binCount <= bin) ? binCount - 1 : bin);
	indicatedBin = bin;

	for (int obs : e.getNeighbors()) {
	    indicatedNeighborBins.add(obs);
	}
	this.repaint();
    }

    public Component renderingComponent() {
	return this;
    }

    public Shape reportShape() {
	if (indicatedBin < 0) {
	    return EmptyShape.INSTANCE;
	}
	return histRecs[indicatedBin];
    }

    public static void main(String[] args) {
	JFrame app = new JFrame();
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	Histogram histo = new Histogram();
	histo.setVariableName("var name");
	histo.setData(new double[] { 1, 2, 3,3,3, 44, 56 });
	app.add(histo);
	app.pack();
	app.setVisible(true);

    }

}
