/* 
 Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty
 */

package geovista.readers.example;

import java.util.logging.Logger;

import geovista.common.data.DataSetForApps;
import geovista.readers.shapefile.ShapeFileDataReader;

/**
 * Reads shapefiles from included resources
 * 
 * Object[0] = names of variables 0bject[1] = data (double[], int[], or
 * String[]) 0bject[1] = data (double[], int[], or String[]) ... Object[n-1] =
 * the shapefile data
 * 
 * also see DBaseFile, ShapeFile
 * 
 */
public class GeoDataGeneralizedStates extends GeoDataClassResource {

	final static Logger logger = Logger
			.getLogger(GeoDataGeneralizedStates.class.getName());
	static DataSetForApps dsa;

	@Override
	protected DataSetForApps makeDataSetForApps() {
		if (dsa == null) {
			logger.info("making new DSA of states");
			dsa = ShapeFileDataReader.makeDataSetForAppsCsv(this.getClass(),
					"48small");
		}
		return dsa;

	}

}
