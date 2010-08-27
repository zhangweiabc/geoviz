/*
 * STFDataSet.java
 *
 * Created on 15 April 2002
 * by Frank Hardisty
 * based on STFile
 *
  *
 * Licensed under GNU General Public License (GPL).
 * See http://www.gnu.org/copyleft/gpl.html
 */

package geovista.geoviz.parvis;

import java.net.URL;
import java.util.Vector;

import geovista.readers.FileIO;

/**
 * A Simple file parser for reading STF (Simple Table Fomrat) files from URLs.
 *
 * The STF File format is defined as follows:
<pre>
# test.stf
# Comments have # in the first column.
# Type the number of fields, on a line by itself.
3
# Then type field names and types. Field names must not contain
# spaces.
#
PersonName     String
Age            Integer
HourlyWage     Real
#
# Data type is case-insensitive.
# Default data delimiters are tabs and spaces.
# Here's the data, tab-delimited. Notice that the data columns are
# in the order they are listed above.
#
Joe            23      5.75
Mary           18      4.75
Fred           54      100.00
Ginger         48      100.00
#
# Nothing special is required to end the file.

</pre>
 *
 * Once the file is read and parsed, the data can be accessed with the methods
 * defined in the ParallelSpaceModel interface.
 *
 * @author Flo Ledermann flo@subnet.at
 * 
 */
public class STFDataSet extends SimpleParallelSpaceModel {

    /** The url of the file. */
    URL url;



    /**
     * Creates a new STFDataSet with the given url. The content is not read until
     * readContents() is called.
     *
     * @param url The url of the file to read.
     */
    public STFDataSet(Object[] dataSet) {
      Object[] dataIn = dataSet;
      Object[] dataToShow = null;
      int[] orignalToNew = new int[dataIn.length];
      String[] varNames = (String[])dataIn[0];
      boolean[] isNumeric = new boolean[dataIn.length];
      int count = 0;
      for (int i = 0; i < dataIn.length; i++) {
        Object obj = dataIn[i];
        if (obj instanceof double[] || obj instanceof int[]) {
          isNumeric[i] = true;
          count++;
        } else {
          isNumeric[i] = false;
        }
      }
      if (count == 0) return;

      String[] numericVarNames = new String[count];
      dataToShow = new Object[count + 3]; //one for variable names
                                                //one for observation names
                                                //one for spatial data;

      count = 0;
      for (int i = 1; i < isNumeric.length; i++) {
        if (isNumeric[i]){
          dataToShow[count+1] = dataIn[i];
          numericVarNames[count] = varNames[i-1]; // -1 because of the varNames themselves
          orignalToNew[i] = count;//so we can get back if need be
          count++;
        }
      }
      dataToShow[0] = numericVarNames;
      for (int i = 0; i < varNames.length; i++) {
        String lower = varNames[i].toLowerCase();
        if (lower.endsWith("name") && (dataIn[i + 1] instanceof String[])){
          dataToShow[numericVarNames.length +1] = dataIn[i+1];
          //this.spat.setObservationNames((String[])dataIn[i+1]);//+1 to skip varNames
        }
      }

      this.initNumDimensions(numericVarNames.length);
      this.setAxisLabels(numericVarNames);
      Object obj = dataToShow[1];
      double[] someDoubles = null;
      int[] someInts = null;
      int numObservations = -1;
      if (obj instanceof double[]) {
        someDoubles = (double[])obj;
        numObservations = someDoubles.length;
      } else if (obj instanceof int[]) {
        someInts = (int[])obj;
        numObservations = someInts.length;
      }


      String name = null;
      for (int row = 0; row < numObservations; row++) {
      float[] dataVals = new float[numericVarNames.length];
        for (int column = 1; column < dataVals.length + 1; column++) {
          obj = dataToShow[column];
          if (obj instanceof double[]) {
            someDoubles = (double[])obj;
            dataVals[column-1] = (float)someDoubles[row];
          } else if (obj instanceof int[]) {
            someInts = (int[])obj;
            dataVals[column-1] = someInts[row];
          }//end if
        }//next column
        if (dataToShow[numericVarNames.length + 1] != null) {
          String[] names = (String[])dataToShow[numericVarNames.length +1];
          name = names[row];
        }
        this.addRecord(dataVals, name);
      }//next row


    }


    private Vector progressListeners = new Vector();

    public void addProgressListener(ProgressListener l){
        progressListeners.add(l);
    }

    public void removeProgressListener(ProgressListener l){
        progressListeners.remove(l);
    }

    public void fireProgressEvent(ProgressEvent e){
        Vector list = (Vector)progressListeners.clone();
        for (int i=0; i<list.size(); i++){
            ProgressListener l = (ProgressListener)list.elementAt(i);
            l.processProgressEvent(e);
        }
    }

    /**
     * Main method for testing purposes.
     */
    public static void main(String args[]){

        String fileName = "C:\\geovista_old\\data\\test6.csv";

        Object[] dataSet = new Object[4];
        String[] labels = new String[] {"0","1","Name"};
        dataSet[0] = labels;
        try {
          FileIO fio = new FileIO(fileName, "r");
            for (int col = 0; col < 2; col++){
              double[] doubleData = new double[7];
              for (int row = 0; row < 7; row++){
                doubleData[row] = fio.readDouble();
              }
              dataSet[col+1] = doubleData;
            }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        String[] names = new String[7];
        for (int row = 0; row < 7; row++){
          names[row] = "Obs " + row;
        }
        dataSet[3] = names;
      
  }
}
