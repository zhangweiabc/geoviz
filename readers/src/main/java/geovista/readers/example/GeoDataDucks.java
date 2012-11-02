/* 
 Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty
 */

package geovista.readers.example;

import java.io.InputStream;
import java.util.logging.Logger;

import geovista.common.data.DataSetForApps;
import geovista.readers.csv.CSVFileDataReader;

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
public class GeoDataDucks extends GeoDataClassResource {

    final static Logger logger = Logger.getLogger(GeoDataDucks.class.getName());

    @Override
    protected DataSetForApps makeDataSetForApps() {
	InputStream is = this.getClass().getResourceAsStream(
		"resources/long-tailed-duck.csv");
	CSVFileDataReader reader = new CSVFileDataReader();
	reader.setInputStream(is);

	return reader.getDataForApps();

    }

}
