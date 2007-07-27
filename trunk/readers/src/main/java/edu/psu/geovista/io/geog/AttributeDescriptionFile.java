/* -------------------------------------------------------------------
 GeoVISTA Center (Penn State, Dept. of Geography)
 Java source file for the class GeoData48States
 Copyright (c), 2002, GeoVISTA Center
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
 Original Author: Xiping Dai and Frank Hardisty
 $Author: hardisty $
 $Id: AttributeDescriptionFile.java,v 1.2 2005/09/15 15:04:03 hardisty Exp $
 $Date: 2005/09/15 15:04:03 $
 Reference:		Document no:
 ___				___
 -------------------------------------------------------------------  *
 */


package edu.psu.geovista.io.geog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import edu.psu.geovista.io.FileIO;

/**

 *
 */
public class AttributeDescriptionFile {

  transient private String[] attributeDescriptions;

  public AttributeDescriptionFile() {

  }

  public AttributeDescriptionFile(String fileName) throws IOException{

	  File tryIt = new File(fileName);
	  boolean exists = tryIt.exists();
	  if (!exists){
		  this.attributeDescriptions = null;
		  return;
	  }

	try {
		FileIO fio = new FileIO(fileName,"r");
		Vector desc = new Vector();

		while(!fio.hasReachedEOF()){
			String line = fio.readLine();
			if (line != null){//the line after the last line is always null, but we don't want to read it.
				desc.add(line);
			}
		}
		desc.trimToSize();
		int len = desc.size();
		this.attributeDescriptions = new String[len];
		for (int i = 0; i < len; i ++){
			this.attributeDescriptions[i] = (String)desc.get(i);
		}
	}
	//catch (IOException ex) {
	catch (FileNotFoundException ex) {
		ex.printStackTrace();
	}
  }
    public String[] getAttributeDescriptions() {
        return attributeDescriptions;
    }
    public void setAttributeDescriptions(String[] attributeDescriptions) {
        this.attributeDescriptions = attributeDescriptions;
    }


}
