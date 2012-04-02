/* 
 Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty
 */

package geovista.readers.example;

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
public class GeoData48States extends GeoDataClassResource {

    @Override
    protected DataSetForApps makeDataSetForApps() {

	return ShapeFileDataReader.makeDataSetForAppsCsv(this.getClass(),
		"states48");

    }

}
