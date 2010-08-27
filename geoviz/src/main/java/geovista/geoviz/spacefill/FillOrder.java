/* -------------------------------------------------------------------
 Java source file for the class FillOrder
 Original Author: Frank Hardisty
 $Author: hardisty $
 $Id: FillOrder.java,v 1.3 2006/02/17 17:28:07 hardisty Exp $
 $Date: 2006/02/17 17:28:07 $
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 -------------------------------------------------------------------   */

package geovista.geoviz.spacefill;

import java.util.Arrays;
import java.util.logging.Logger;

public class FillOrder {
	protected final static Logger logger = Logger.getLogger(FillOrder.class.getName());
  public static final int NULL_VALUE = Integer.MIN_VALUE;

  public static final int FILL_ORDER_SCAN_LINE = 0;
  public static final int FILL_ORDER_BOSTROPHEDON = 1;
  public static final int FILL_ORDER_SPIRAL = 2;
  public static final int FILL_ORDER_PEANO = 3;
  public static final int FILL_ORDER_MAX = 3;

  private static final String SHORT_DESC_SCAN_LINE = "Scan line";
  private static final String SHORT_DESC_BOSTROPHEDON = "Bostrophedon";
  private static final String SHORT_DESC_SPIRAL = "Spiral";
  private static final String SHORT_DESC_PEANO = "Peano";

  //H for heading
  private static final int H_SOUTH = 0;
  private static final int H_EAST = 1;
  private static final int H_NORTH = 2;
  private static final int H_WEST = 3;

//bostrophedon
  //private final transient String name;

  public FillOrder() {

  }

  public static String[] findFillOrderDescriptions() {
    String[] desc = new String[FillOrder.FILL_ORDER_MAX + 1]; //zero based arrays
    desc[FillOrder.FILL_ORDER_SCAN_LINE] = FillOrder.SHORT_DESC_SCAN_LINE;
    desc[FillOrder.FILL_ORDER_BOSTROPHEDON] = FillOrder.SHORT_DESC_BOSTROPHEDON;
    desc[FillOrder.FILL_ORDER_SPIRAL] = FillOrder.SHORT_DESC_SPIRAL;
    desc[FillOrder.FILL_ORDER_PEANO] = FillOrder.SHORT_DESC_PEANO;
    return desc;
  }

  /**
   * this method returns a two-dimensional array int[i][j] where
   * i = row
   * j = column
   * and the entry indicates the rank order.
   *
   * This class generates only square (or as square as possible) arrays.
   *
   * The second arg is optional. If included and the right size, it will be
   * used as the array to return. If not, a new array will be created.
   */
  public static int[][] findFillOrder(long nVals, int[][] fillOrder, int kind) {
    if (kind == FillOrder.FILL_ORDER_SCAN_LINE) {
      return getFillOrderScanLine(nVals, fillOrder);
    }
    else if (kind == FillOrder.FILL_ORDER_BOSTROPHEDON) {
      return getFillOrderBostrophedon(nVals, fillOrder);
    }
    else if (kind == FillOrder.FILL_ORDER_SPIRAL) {
      return getFillOrderSpiral(nVals, fillOrder);
    }
    else if (kind == FillOrder.FILL_ORDER_PEANO) {
      return getFillOrderPeano(nVals, fillOrder);
    }
    else {
      throw new IllegalArgumentException("Outside range of legal fill orders");
    }
    //return null;
  }

  private static int[][] makeArray(int[][] fillOrder, int sizeX, int sizeY) {
    int[][] indexArray = null;
    //dim array if necassary
    if (fillOrder == null) {
      indexArray = new int[sizeY][sizeX];
    }
    else if (fillOrder.length == sizeY && fillOrder[0].length == sizeX) {
      indexArray = fillOrder;
    }
    else {
      indexArray = new int[sizeY][sizeX];
    }
    return indexArray;
  }

  //the second arg is optional. if included and the right size, will be used
  //if not, new array
  private static int[][] getFillOrderScanLine(long nVals, int[][] fillOrder) {
    //start with a square array
    double root = Math.sqrt( nVals);
    int sizeX = (int) Math.ceil(root);

    //now trim
    long sizeArray = sizeX * sizeX;
    long leftOver = sizeArray - nVals;
    //how many rows is this?
    double divide = ( (double) leftOver / (double) sizeX);
    double whole = Math.floor(divide);
    int numRows = (int) whole;
    int sizeY = sizeX - numRows;
    int[][] indexArray = FillOrder.makeArray(fillOrder, sizeX, sizeY);

    int counter = 0;
    for (int pixelY = sizeY - 1; pixelY > -1; pixelY--) {
      for (int pixelX = 0; pixelX < sizeX; pixelX++) {
        if (counter < nVals) {
          indexArray[pixelY][pixelX] = counter;
        }
        else {
          indexArray[pixelY][pixelX] = FillOrder.NULL_VALUE;
        }
        counter++;

      }
    }
    return indexArray;
  } //end method

  //the second arg is optional. if included and the right size, will be used
  //if not, new array
  private static int[][] getFillOrderBostrophedon(long nVals, int[][] fillOrder) {
    //start with a square array
    double root = Math.sqrt( nVals);
    int sizeX = (int) Math.ceil(root);

    //now trim
    long sizeArray = sizeX * sizeX;
    long leftOver = sizeArray - nVals;
    //how many rows is this?
    double divide = ( (double) leftOver / (double) sizeX);
    double whole = Math.floor(divide);
    int numRows = (int) whole;
    int sizeY = sizeX - numRows;
    int[][] indexArray = FillOrder.makeArray(fillOrder, sizeX, sizeY);

    int counter = 0;
    for (int pixelY = 0; pixelY < sizeY; pixelY++) {
      double remainder = (double) pixelY / 2;
      boolean isOdd;
      if (remainder == 0) {
        isOdd = false;
      }
      else {
        isOdd = true;
      }
      if (isOdd) {
        for (int pixelX = sizeX - 1; pixelX > -1; pixelX--) {
          if (counter < nVals) {
            indexArray[pixelY][pixelX] = counter;
          }
          else {
            indexArray[pixelY][pixelX] = FillOrder.NULL_VALUE;
          }
          counter++;
        } //next pixelX
      }
      else {
        for (int pixelX = 0; pixelX < sizeX; pixelX++) {
          if (counter < nVals) {
            indexArray[pixelY][pixelX] = counter;
          }
          else {
            indexArray[pixelY][pixelX] = FillOrder.NULL_VALUE;
          }
          counter++;
        } //next pixelX
      } //end if odd
    }
    return indexArray;
  } //end method

  //the second arg is optional. if included and the right size, will be used
  //if not, new array
  private static int[][] getFillOrderSpiral(long nVals, int[][] fillOrder) {
    //start with a square array
    double root = Math.sqrt( nVals);
    int sizeY = (int) Math.ceil(root);

    //now trim
    long sizeArray = sizeY * sizeY;
    long leftOver = sizeArray - nVals;
    //how many rows is this?
    double divide = ( (double) leftOver / (double) sizeY);
    double whole = Math.floor(divide);
    int numCols = (int) whole;
    int sizeX = sizeY - numCols;
    int[][] indexArray = FillOrder.makeArray(fillOrder, sizeX, sizeY);

    int counter = 0;
    int pixelX = 0;
    int pixelY = 0;
    //find our start point
    double half = (double) sizeX / 2;
    half = Math.ceil(half);
    pixelX = (int) half - 1;

    half = (double) sizeY / 2;
    half = Math.ceil(half);
    pixelY = (int) half - 1;

    indexArray[pixelY][pixelX] = counter;
    counter++;
    int nSpaces = sizeX * sizeY;

    int heading = FillOrder.H_SOUTH;

    int currSize = 1;

    while (counter < nSpaces) {
      try {
        if (counter == (currSize * currSize)) {
          //go one unit forward in the current heading, then turn.
          pixelX = FillOrder.findNextSpiralPixelX(pixelX, heading);
          pixelY = FillOrder.findNextSpiralPixelY(pixelY, heading);
          indexArray[pixelY][pixelX] = counter;
          counter++;
          //heading = FillOrder.findNextHeading(heading);
          currSize++;
        }
        else {
          // turn. then go currSize -1 units in the new heading,
          heading = FillOrder.findNextHeading(heading);
          for (int i = 1; i < currSize; i++) {
            pixelX = FillOrder.findNextSpiralPixelX(pixelX, heading);
            pixelY = FillOrder.findNextSpiralPixelY(pixelY, heading);
            if (counter < nVals) {
              indexArray[pixelY][pixelX] = counter;
            }
            else {
              indexArray[pixelY][pixelX] = FillOrder.NULL_VALUE;
            }

            counter++;
            if (counter >= nSpaces) {
              return indexArray;
            }
          } //next i
          //heading = FillOrder.findNextHeading(heading);
        } //end if
      }
      catch (Exception ex) {
        logger.finest(
            "A Bad Thing happened in FillOrder.getFillOrderSpiral(). Diagnostic info:");
        logger.finest("PixelX = " + pixelX);
        logger.finest("PixelY = " + pixelY);
        logger.finest("heading " + heading);
        logger.finest("counter " + counter);
        ex.printStackTrace();
      }
    } //end while

    return indexArray;
  } //end method

  private static int findNextHeading(int heading) {
    int newHeading = 0;
    if (heading <= 2) {
      newHeading = heading + 1;
    }
    return newHeading;
  }

  private static int findNextSpiralPixelX(int pixelX, int heading) {
    int newX = pixelX;
    if (heading == FillOrder.H_EAST) {
      newX = newX + 1;
    }
    else if (heading == FillOrder.H_WEST) {
      newX = newX - 1;
    }
    return newX;
  }

  private static int findNextSpiralPixelY(int pixelY, int heading) {
    int newY = pixelY;
    if (heading == FillOrder.H_SOUTH) {
      newY = newY + 1;
    }
    else if (heading == FillOrder.H_NORTH) {
      newY = newY - 1;
    }
    return newY;
  }

  //the second arg is optional. if included and the right size, will be used
  //if not, new array
  private static int[][] getFillOrderPeano(long nVals, int[][] fillOrder) {
    //start with a square array
    int pow = 0; //pow is the number of nested "z" patterns
    int nSpaces = 1; //total number of spaces in array
    Double d = null;
    while (nSpaces < nVals) {
      pow++;
      d = new Double(Math.pow(4, pow));
      nSpaces = d.intValue();
    }

    int sizeX = FillOrder.findWidthPeano(nVals, pow);
    int sizeY = FillOrder.findHeightPeano(nVals, pow);
    nSpaces = sizeX * sizeY;
    int[][] indexArray = FillOrder.makeArray(fillOrder, sizeX, sizeY);

    int counter = 0;
    int pixelX = 0;
    int pixelY = 0;
    //find our start point



    try {
      FillOrder.setIndexesPeano(indexArray, pixelY, pixelX, counter, pow, (int) nVals);
      //indexArray[pixelY][pixelX] = counter;

      /*
               d = new Double(Math.pow(2,pow));
               powSideLen = d.intValue();
               for (int i = 0; i < powSideLen; i++) {
        for (int j = 0; j < powSideLen; j++) {

        }
               }
       */

    }
    catch (Exception ex) {
      logger.finest("Help! ");
      logger.finest("PixelX = " + pixelX);
      logger.finest("PixelY = " + pixelY);
      logger.finest("pow " + pow);
      logger.finest("counter " + counter);
      ex.printStackTrace();
    } //end try

    return indexArray;
  } //end method

  private static int findWidthPeano(long nVals, int pow) {
    if (nVals == 1) {
      return 1;
    }
    Double d = null;
    int width = 0;
    long valsCopy = nVals;
    d = new Double(Math.pow(4, pow - 1));
    int nForThisPow = d.intValue();
    int div = (int) valsCopy / nForThisPow;
    valsCopy = valsCopy - (div * nForThisPow);

    //take this by cases
    if (div == 0) {
      width = findWidthPeano(valsCopy, pow - 1);
    }
    else if (div == 1) {
      if (pow <= 1) {
        width = 1;
      }
      else {
        width = pow + findWidthPeano(valsCopy, pow - 1);
      }
    }
    else if (div == 2) {
      d = new Double(Math.pow(2, pow));
      width = d.intValue();
    }
    else if (div == 3) {
      d = new Double(Math.pow(2, pow));
      width = d.intValue();
    }
    else if (div == 4) {
      d = new Double(Math.pow(2, pow));
      width = d.intValue();
    }

    return width;
  }

  private static int findHeightPeano(long nVals, int pow) {
    if (nVals == 1) {
      return 1;
    }
    Double d = null;
    int height = 0;
    long valsCopy = nVals;
    d = new Double(Math.pow(4, pow - 1));
    int nForThisPow = d.intValue();
    int div = (int) valsCopy / nForThisPow;
    valsCopy = valsCopy - (div * nForThisPow);

    //take this by cases
    if (div == 0) {
      height = findHeightPeano(valsCopy, pow - 1);
    }
    else if (div == 1) {
      d = new Double(Math.pow(2, (double) pow - 1));
      height = d.intValue();
    }
    else if (div == 2) {
      if (pow <= 1) {
        height = 1;
      }
      else {
        height = pow + findHeightPeano(valsCopy, pow - 1);
      }
    }
    else if (div == 3) {
      d = new Double(Math.pow(2, pow));
      height = d.intValue();
    }
    else if (div == 4) {
      d = new Double(Math.pow(2, pow));
      height = d.intValue();
    }

    return height;
  }

  private static void setIndexesPeano(int[][] indexArray,
                                      int pixelY, int pixelX,
                                      int currPlace, int pow, int nVals) {

    if (pow == 0) { //recusion bottoms out
      if (pixelY < indexArray.length && pixelX < indexArray[0].length) {
        if (currPlace < nVals) {
          indexArray[pixelY][pixelX] = currPlace;
        }
        else {
          indexArray[pixelY][pixelX] = FillOrder.NULL_VALUE;
        }
      }
    }
    else {
      Double d = null;
      //four cases
      //need to know the length of a side
      d = new Double(Math.pow(2, pow - 1));
      int lenPow = d.intValue();
      //and the number of spaces occupied by sub-boxes
      d = new Double(Math.pow(4, pow - 1));
      int sizeBox = d.intValue();

      //first case, nw box
      setIndexesPeano(indexArray, pixelY, pixelX, currPlace, pow - 1, nVals);
      currPlace = currPlace + sizeBox;

      //second case, ne box
      pixelX = pixelX + lenPow;
      setIndexesPeano(indexArray, pixelY, pixelX, currPlace, pow - 1, nVals);
      currPlace = currPlace + sizeBox;

      //third case, sw box
      pixelX = pixelX - lenPow;
      pixelY = pixelY + lenPow;
      setIndexesPeano(indexArray, pixelY, pixelX, currPlace, pow - 1, nVals);
      currPlace = currPlace + sizeBox;

      //fourth case, se box
      pixelX = pixelX + lenPow;
      setIndexesPeano(indexArray, pixelY, pixelX, currPlace, pow - 1, nVals);
      currPlace = currPlace + sizeBox;
    }

  }

  /*
   * Returns an array the same size as the input array, with the rank order
   * the input array in each place.
   */
  public static int[] findRankOrder(double[] data) {
    int[] returnArray = new int[data.length];
    IndexedDouble[] valArray = new IndexedDouble[data.length];
    for (int i = 0; i < data.length; i++){
      valArray[i] = new IndexedDouble(i,data[i]);
    }
    Arrays.sort(valArray);
    for (int i = 0; i < data.length; i++){
      returnArray[i] = valArray[i].getIndex();
    }
    return returnArray;
  }

  public static void main(String[] args){
    double[] someData = {2,1,1,2};
    int[] answer = FillOrder.findRankOrder(someData);
    for (int i = 0; i < answer.length; i++){
      logger.finest("answer " + i + " = " + answer[i]);
    }

    int[][] order = FillOrder.findFillOrder(3,null,FillOrder.FILL_ORDER_SCAN_LINE);
    logger.finest("order " + order);

  }

}
