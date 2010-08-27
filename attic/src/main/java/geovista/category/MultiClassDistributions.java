package geovista.category;

/**
 * <p>Title: Studio applications</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: GeoVSITA Center</p>
 * @author Xiping Dai
 * 
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import geovista.common.data.ArraySort2D;
import geovista.common.data.StatisticsVectors;
import geovista.geoviz.scatterplot.DataArray;

public class MultiClassDistributions extends JPanel implements MouseListener, ComponentListener
{
        private static double AXISSPACEPORTION = 1.0/6.0;
        private Color background;
        private Color foreground;
        private boolean axisOn = true;
        private int plotOriginX;
        private int plotOriginY;
        private int plotEndX;
        private int plotEndY;
        private Vector[] dataVector;
        private int classNumber;
        private Vector oneVarData = new Vector();
        private Vector dataXVector = new Vector();
        private int displayVarIdx;
        private double xAxisMin;
        private double xAxisMax;
        private double yAxisMin;
        private double yAxisMax;
        private double delta;
        private Vector dataDistributions = new Vector();
        private Vector exsIntVector = new Vector();
        private Vector whyIntVector = new Vector();
        private int nonsamplenum=100;
        private boolean gauss;
        private Vector dataGaussians = new Vector();
        private Vector dataGaussiansInt = new Vector();
        private JPopupMenu popup;
        private JCheckBoxMenuItem[] classCheckBox;
        private int currentCheckBox; 
        
    public MultiClassDistributions(){
                this.setBorder(BorderFactory.createLineBorder(Color.gray));
                popup = new JPopupMenu();
    }

        public void setDisplayVariableIndex(int index){
                this.displayVarIdx = index;
                this.initialize();
                this.repaint();
        }

        public int getDisplayVariableIndex(){
                return this.displayVarIdx;
        }

        public void setDataVector(Vector[] dataVector){
                this.dataVector = dataVector;
                this.classNumber = this.dataVector.length;

                this.exsIntVector.setSize(this.classNumber);

                this.classCheckBox = new JCheckBoxMenuItem[this.classNumber];
                for(int i = 0; i < this.classNumber; i ++){
                        this.classCheckBox[i] = new JCheckBoxMenuItem("class" + new Integer(i).toString());
                        this.classCheckBox[i].setName(new Integer(i).toString());
                        this.classCheckBox[i].setSelected(true);
                        this.popup.add(this.classCheckBox[i], i);
                        this.currentCheckBox = i;
                        classCheckBox[i].addActionListener(new ActionListener() {

                                /**
                                 * put your documentation comment here
                                 * @param e
                                 */
                                public void actionPerformed (ActionEvent e) {
                                        boolean isSelected = classCheckBox[currentCheckBox].isSelected();
                                        classCheckBox[currentCheckBox].setSelected(!isSelected);
                                        repaint();
                                }
                });
                }
                addMouseListener(this);

                initialize();

        }

        public Vector[] getDataVector(){
                return this.dataVector;
        }

        public void setAxisOn (boolean axisOn){
                this.axisOn = axisOn;
        }

        public boolean getAxisOn (){
                return this.axisOn;
        }

        public void setGaussOn (boolean gauss){
                this.gauss = gauss;
        }

        public boolean getGaussOn (){
                return this.gauss;
        }

        /**
         * put your documentation comment here
         * @param c
         */
        public void setBackground (Color c) {
                if (c == null)
                        return;
                this.background = c;
                int colorTotal = c.getRed() + c.getGreen() + c.getBlue();
                int greyColor = 128 * 3;
                if (colorTotal < greyColor)
                        this.foreground = Color.white;
                else
                        this.foreground = Color.black;
                this.repaint();
        }

        /*private double[] calculatePDF(double[] data){
                double[] pdfArray;
                double[][] dataWithIndex = new double[data.length][2];
                int len = data.length;
                pdfArray = new double[len];

                for (int i = 0; i < len; i++) {
                  dataWithIndex[i][0] = data[i];
                  dataWithIndex[i][1] = i;
                }
                //now sort
                ArraySort2D.sortDouble(dataWithIndex,0);
                //this.pdfArray[0] = dataWithIndex[0][0];
                //this.pdfArray[len-1] = dataWithIndex[len-1][0];
                for (int i = 0; i < len; i ++){
                        int numberInDelta = 1;
                        int j = 1;
                        double mean = dataWithIndex[i][0];
                        while ((i != 0)&&(dataWithIndex[i-j][0] > dataWithIndex[i][0] - this.delta)){
                                mean += dataWithIndex[i-j][0];
                                j ++;
                                if (j > i){
                                        break;
                                }
                        }
                        numberInDelta += j-1;
                        j = 1;
                        while ((i != len-1)&&(dataWithIndex[i+j][0] < dataWithIndex[i][0] + this.delta)){
                                mean += dataWithIndex[i+j][0];
                                j ++;
                                if ((i+j) == len){
                                        break;
                                }
                        }
                        numberInDelta += j-1;
                        if (numberInDelta ==0){
                                pdfArray[i] = 0;
                        }else{
                                mean /= numberInDelta;
                        pdfArray[i] = numberInDelta/this.delta/2/len;
                        }
                }

                DataArray pdfDataArray = new DataArray(pdfArray);
                double yMin = ((double[])pdfDataArray.getExtent().clone())[0];
                double yMax = ((double[])pdfDataArray.getExtent().clone())[1];
                this.yAxisMin = (this.yAxisMin <= yMin) ? yAxisMin : yMin;
                this.yAxisMax = (this.yAxisMax >= yMax) ? yAxisMax : yMax;
                return pdfArray;
        }*/

        private double[] nonsamplepdf(double[] data, int cl) {
                int len = data.length;
                double[][] dataWithIndex = new double[len][2];
                double[] pdfArray;

                //add index
                if (dataWithIndex == null || dataWithIndex.length != data.length || dataWithIndex[0].length != 2) {
                        dataWithIndex = new double[data.length][2];
                }
                for (int i = 0; i < len; i++) {
                  dataWithIndex[i][0] = data[i];
                  dataWithIndex[i][1] = i;
                }
                //now sort
                ArraySort2D sorter = new ArraySort2D();
                sorter.sortDouble(dataWithIndex,0);
                //this.pdfArray[0] = dataWithIndex[0][0];
                //this.pdfArray[len-1] = dataWithIndex[len-1][0];

                double xMin;
                double xMax;
                DataArray dataArray;

                dataArray = new DataArray(data);
                xMin = ((double[])dataArray.getExtent().clone())[0];
                xMax = ((double[])dataArray.getExtent().clone())[1];
                this.delta = (xMax - xMin)/20.0;

                int minindex, maxindex;
                minindex=maxindex=0;
                double interval=(xMax-xMin)/nonsamplenum;
                pdfArray=new double[nonsamplenum]; // y
                double[] continuousData = new double[nonsamplenum]; // x
                for (int i = 0; i < nonsamplenum; i ++){
                        continuousData[i]=(i+0.5)*interval+xMin;
                        // find minimum
                        while ((dataWithIndex[minindex][0]<continuousData[i]-this.delta)&&minindex<len-1) {
                                minindex+=1;
                        }
                        // find maximum
                        while ((dataWithIndex[maxindex][0]<continuousData[i]+this.delta)&&maxindex<len-1) {
                                maxindex+=1;
                        }
                        if (this.delta != 0){
                                pdfArray[i]=(maxindex-minindex)/(len*this.delta*2.0);
                        }else{
                                pdfArray[i] = 0;
                        }
                }

                this.dataXVector.add(cl, continuousData.clone());

                DataArray pdfDataArray = new DataArray(pdfArray);
                double yMin = ((double[])pdfDataArray.getExtent().clone())[0];
                double yMax = ((double[])pdfDataArray.getExtent().clone())[1];
                this.yAxisMin = (this.yAxisMin <= yMin) ? yAxisMin : yMin;
                this.yAxisMax = (this.yAxisMax >= yMax) ? yAxisMax : yMax;

                this.xAxisMin = (this.xAxisMin <= xMin) ? xAxisMin : xMin;
                this.xAxisMax = (this.xAxisMax >= xMax) ? xAxisMax : xMax;

                if (this.gauss == true){

                        double[] dataGaussian = new double[nonsamplenum];
                        double mean = StatisticsVectors.mean(data);

                        double variance = 0;
                        if(this.delta != 0){
                                variance = StatisticsVectors.variance(data);
                        }

                        for(int i = 0; i < continuousData.length; i ++){
                                if (this.delta != 0){
                                        dataGaussian[i] = 1/Math.sqrt(2*Math.PI*variance) * Math.exp(-Math.pow(continuousData[i]-mean, 2)/2/variance);
                                }else{
                                        dataGaussian[i] = 0;
                                }
                        }
                        this.dataGaussians.add(cl, dataGaussian.clone());
                }

                return pdfArray;
        }

        private void initialize(){
                this.oneVarData.clear();
                this.dataDistributions.clear();
                this.dataXVector.clear();

                for(int i = 0; i < this.classNumber; i ++){
                        int len = this.dataVector[i].size();
                        double[] oneClassData = new double[len];
                        for(int j = 0; j < len; j ++){
                                if (((double[])(this.dataVector[i].get(j))).length <= this.displayVarIdx){
                                        return;
                                }
                                oneClassData[j] = ((double[])(this.dataVector[i].get(j)))[this.displayVarIdx];
                        }
                        this.oneVarData.add(oneClassData.clone());
                }

                this.xAxisMin = 0;
                this.xAxisMax = 0;

                this.addComponentListener(this);

                this.setBackground(Color.white);

                this.yAxisMin = 0;
                this.yAxisMax = 0;
                for (int i = 0; i < this.classNumber; i ++){
                        dataDistributions.add(i, this.nonsamplepdf((double[])this.oneVarData.get(i), i).clone());
                }

                setupDataforDisplay();
                this.validate();
        }

        private void setupDataforDisplay(){

                if (axisOn){
                        plotOriginX = (int)(this.getWidth()*AXISSPACEPORTION);
                        plotOriginY = (int)(this.getHeight()*(1 - AXISSPACEPORTION));
                        plotEndX = (int)(this.getWidth()) - (int)(this.getWidth()*AXISSPACEPORTION/2);
                        plotEndY = (int)(this.getHeight()*AXISSPACEPORTION/2);
                }else {
                        plotOriginX = 0;
                        plotOriginY = (int)(this.getSize().getHeight() - 2);
                        plotEndX = (int)(this.getSize().getWidth()) - 3;
                        plotEndY = 3;
                }

                        //get positions on screen
                        double scaleX, scaleY;
                        scaleX = getScale(plotOriginX, plotEndX, xAxisMin, xAxisMax);
                        scaleY = getScale(plotOriginY, plotEndY, yAxisMin, yAxisMax);

                        this.exsIntVector.clear();
                        this.whyIntVector.clear();
                        this.dataGaussiansInt.clear();
                        //double[] dataX = new double[nonsamplenum];
                        for(int cl = 0; cl < this.classNumber; cl ++){
                                int[] exsInt = new int[nonsamplenum];
                                int[] whyInt = new int[nonsamplenum];
                                exsInt = getValueScreen((double[])dataXVector.get(cl), scaleX, plotOriginX, xAxisMin);
                                whyInt = getValueScreen((double[])this.dataDistributions.get(cl), scaleY, plotOriginY, yAxisMin);
                                this.exsIntVector.add(cl, exsInt.clone());
                                this.whyIntVector.add(cl, whyInt.clone());
                        }

                        if (gauss == true){
                                for(int cl = 0; cl < this.classNumber; cl ++){
                                        int[] gaussianInt = new int[nonsamplenum];
                                        gaussianInt = getValueScreen((double[])this.dataGaussians.get(cl), scaleY, plotOriginY, yAxisMin);
                                        this.dataGaussiansInt.add(cl, gaussianInt.clone());
                                }
                        }
        }

        public void paintComponent (Graphics g) {
                g.setColor(background);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(foreground);
                if (this.axisOn == true){
                        drawAxis(g);
                }
                drawPlot(g);
        }

        private void drawPlot (Graphics g) {
                for(int cl = 0; cl < this.classNumber; cl ++){
                        //int[] exsInt = (int[])this.exsIntVector.get(cl);
                        //int[] whyInt = (int[])this.whyIntVector.get(cl);
                        if(cl == 0){
                                g.setColor(Color.BLUE);
                        }else if(cl == 1){
                                g.setColor(Color.cyan);
                        }else if(cl == 2){
                                g.setColor(Color.green);
                        }else if(cl == 3){
                                g.setColor(Color.yellow);
                        }else if(cl == 4){
                                g.setColor(Color.darkGray);
                        }else if(cl == 5){
                                g.setColor(Color.orange);
                        }else if(cl == 6){
                                g.setColor(Color.gray);
                        }else if(cl == 7){
                                g.setColor(Color.pink);
                        }else if (cl == 8){
                                g.setColor(Color.red);
                        }
                        if (this.classCheckBox[cl].isSelected() == true){
                                for (int i = 0; i < nonsamplenum; i ++){
                                        g.drawOval(((int[])this.exsIntVector.get(cl))[i], ((int[])this.whyIntVector.get(cl))[i], 2, 2);
                                }

                                if(gauss == true){
                                        int[] gy = (int[])this.dataGaussiansInt.get(cl);
                                        for (int i = 0; i < nonsamplenum; i ++){
                                        //g.drawOval(((int[])this.exsIntVector.get(cl))[i], gy[i], 2, 2);
                                                g.drawPolyline(((int[])this.exsIntVector.get(cl)), gy, this.nonsamplenum);
                                        }
                                }
                                g.setColor(foreground);
                        }
                }
        }


        private void drawAxis (Graphics g) {
                int plotWidth, plotHeight;
                plotWidth = (int)this.getSize().getWidth();
                plotHeight = (int)this.getSize().getHeight();
                g.setColor(foreground);
                g.drawLine(plotOriginX, plotEndY, plotOriginX, plotOriginY);
                g.drawLine(plotOriginX, plotOriginY, plotEndX, plotOriginY);
                // draw tick bars for scales on Y coordinate
                int fontSize;
                if (plotWidth < plotHeight){
                        if (plotWidth < 300){
                                fontSize = 9;
                        } else {
                                fontSize = (int)(plotWidth/32);
                        }
                }else {
                        if (plotHeight < 300){
                                fontSize = 9;
                        } else {
                                fontSize = (int)(plotHeight/32);
                        }
                }
                Font font = new Font("", Font.PLAIN, fontSize);
                g.setFont(font);
                //draw the labels on y axis (frequency).
                /*String scaleStringY;
                double barNumber = this.pdfDataArray.getTickNumber();
                double yBarDistance = ((plotOriginY - plotEndY)/barNumber);
                        logger.finest("drawaxis: "+plotOriginY+" "+plotEndY+" "+yBarDistance+" "+barNumber);
                for (int i = 0; i <= barNumber; i++) {
                        g.drawLine(plotOriginX - 3, plotEndY + (int)(i*yBarDistance), plotOriginX,
                                                plotEndY + (int)(i*yBarDistance));
                        if (Math.abs(this.pdfDataArray.getMajorTick()) <= 1) {
                                        scaleStringY = Float.toString((float)(yAxisMax - i*this.pdfDataArray.getMajorTick()));
                        }
                        else {
                                        scaleStringY = Integer.toString((int)(yAxisMax - i*this.pdfDataArray.getMajorTick()));
                        }
                        g.drawString(scaleStringY, plotOriginX - (int)(plotWidth*AXISSPACEPORTION/2),
                                                plotEndY + (int)(i*yBarDistance + yBarDistance*1/6));
                }*/
                //draw the labels on x axis.
                //First tick.
                String scaleStringX;
                g.drawLine(plotOriginX, plotOriginY, plotOriginX, plotOriginY + 3);
                if (Math.abs(xAxisMin) <= 1) {
                        scaleStringX = Float.toString((float)xAxisMin);
                } else {
                        scaleStringX = Integer.toString((int)xAxisMin);
                }
                g.drawString(scaleStringX, plotOriginX - 3, plotOriginY + (int)(plotHeight*AXISSPACEPORTION/4));
                //Last tick.
                g.drawLine(plotEndX, plotOriginY, plotEndX, plotOriginY + 3);
                if (Math.abs(xAxisMax) <= 1) {
                        scaleStringX = Float.toString((float)xAxisMax);
                } else {
                        scaleStringX = Integer.toString((int)xAxisMax);
                }
                g.drawString(scaleStringX, plotEndX - 8, plotOriginY + (int)(plotHeight*AXISSPACEPORTION/4));
                font = new Font("", Font.PLAIN, fontSize + 3);
                g.setFont(font);
                //draw X axis attribute string
                //g.drawString(this.variableName, plotOriginX + (plotEndX - plotOriginX)/2 - plotWidth/12,
                //			plotOriginY + plotHeight/6 - 5);
                //draw Y axis attribute string. Need rotation for drawing the string vertically.
                Graphics2D g2d = (Graphics2D)g;
                g2d.rotate(-Math.PI/2, plotOriginX - plotWidth/9, plotOriginY - (plotOriginY
                                        - plotEndY)/3);
                g2d.drawString("Density", plotOriginX - plotWidth/9, plotOriginY - (plotOriginY
                                        - plotEndY)/3);
                g2d.rotate(+Math.PI/2, plotOriginX - plotWidth/9, plotOriginY - (plotOriginY
                                        - plotEndY)/3);
        }

        /**
         * Calculate scale between real data and integer data for showing up on screen.
         * @param min
         * @param max
         * @param dataMin
         * @param dataMax
         * @return scale
         */
        private double getScale (int min, int max, double dataMin, double dataMax) {
                double scale;
                scale = (max - min)/(dataMax - dataMin);
                return  scale;
        }
        /**
         * Convert the numeric values of observations to integer value worked on screen.
         * @param dataArray
         * @param scale
         * @param min
         * @param dataMin
         * @return valueScreen
         */
        private int[] getValueScreen (double[] dataArray, double scale, int min, double dataMin) {
                int[] valueScreen = new int[dataArray.length];
                for (int i = 0; i < dataArray.length; i++) {
                        if (Double.isNaN(dataArray[i])) {
                                valueScreen[i] = Integer.MIN_VALUE;
                        }
                        else {
                                valueScreen[i] = (int)((dataArray[i] - dataMin)*scale + min);
                        }
                }
                return  valueScreen;
}

        public void componentHidden(ComponentEvent e) {

        }

        public void componentMoved(ComponentEvent e) {

        }

        public void componentResized(ComponentEvent e) {
                this.setupDataforDisplay();
                this.repaint();
        }

        public void componentShown(ComponentEvent e) {
        }

        /**
         * put your documentation comment here
         * @param e
         */
        private void maybeShowPopup (MouseEvent e) {
                if (e.isPopupTrigger()) {
                        popup.show(e.getComponent(), e.getX(), e.getY());
                }
        }

        public void mouseClicked(MouseEvent e){
                int count = e.getClickCount();
                //double click, pop up a detail scatter plot.
                if (count == 2){                // This is a double-click or triple...
                                MultiClassDistributions detailSP = new MultiClassDistributions();
                                detailSP.setAxisOn(true);
                                detailSP.setBackground(background);
                                detailSP.setGaussOn(true);
                                detailSP.setDisplayVariableIndex(this.displayVarIdx);
                                detailSP.setDataVector(this.dataVector);


                                JFrame dlgSP = new JFrame("Detailed Distributions");
                                //JDialog dlgSP = new JDialog(dummyFrame, "Detailed Distributions", true);
                                dlgSP.setLocation(300, 300);
                                dlgSP.setSize(300, 300);
                                dlgSP.getContentPane().setLayout(new BorderLayout());
                                dlgSP.getContentPane().add(detailSP, BorderLayout.CENTER);
                                /*detailSP.addActionListener(new ActionListener() {

                                        /**
                                         * put your documentation comment here
                                         * @param e
                                         */
                                        /*public void actionPerformed (ActionEvent e) {
                                                logger.finest("something came from detailed one.");
                                                ScatterPlot detailSP = (ScatterPlot)e.getSource();
                                                String command = e.getActionCommand();
                                                if (command.compareTo(ScatterPlot.COMMAND_POINT_SELECTED) == 0) {
                                                        logger.finest("SPMC.plotUnitPanel.actionPerformed(), point selected");
                                                        //Vector selRecords = detailSP.getSelectedObservations();
                                                        int[] selections = detailSP.getSelections();
                                                        // Don't recall the scatterplot which generated the original event
                                                        //ScatterPlot.this.setSelectedObservations(selRecords);
                                                        ScatterPlot.this.setSelections(selections);
                                                        ScatterPlot.this.fireActionPerformed(COMMAND_POINT_SELECTED);
                                                }
                                                else if(command.compareTo(ScatterPlot.COMMAND_DATARANGE_SET)==0){
                                                        double[] dataArrayX = detailSP.getXAxisExtents();
                                                        double[] dataArrayY = detailSP.getYAxisExtents();
                                                        ScatterPlot.this.setXAxisExtents(dataArrayX);
                                                        ScatterPlot.this.setYAxisExtents(dataArrayY);
                                                        fireActionPerformed(COMMAND_DATARANGE_SET);
                                                }
                                                        //System.err.println("Unknown command! = " + command);
                                        }
                                });*/
                                dlgSP.setVisible(true);
             }
        }

        public void mousePressed(MouseEvent e){
                if (e.isPopupTrigger())
                        maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e){
                if (e.isPopupTrigger()){
                        maybeShowPopup(e);
                }
        }

        public void mouseEntered(MouseEvent e){
        }

        public void mouseExited(MouseEvent e){
        }

}
