/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import geovista.coordination.CoordinationManager;
import geovista.readers.example.GeoData48States;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;

public class GeoMapMain extends JFrame {

    public GeoMapMain(String name) {
	super(name);

    }

    public static void main_old(String[] args) {
	boolean useProj = true;
	boolean useResource = false;
	// GeoMap map = new GeoMap();
	GeoMapMain app = new GeoMapMain("MapBean Main Class");
	app.addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	app.getContentPane().setLayout(
		new BoxLayout(app.getContentPane(), BoxLayout.X_AXIS));
	// app.getContentPane().add(map);
	app.pack();
	app.setVisible(true);

	GeoMap map2 = new GeoMap();
	app.getContentPane().add(map2);
	app.pack();
	app.setVisible(true);

	String fileName = "C:\\arcgis\\arcexe81\\Bin\\TemplateData\\USA\\counties.shp";
	fileName = "C:\\temp\\shapefiles\\intrstat.shp";
	fileName = "C:\\data\\geovista_data\\shapefiles\\larger_cities.shp";
	fileName = "C:\\data\\geovista_data\\shapefiles\\jin\\CompanyProdLL2000Def.shp";
	fileName = "D:\\geovista_data\\Historical-Demographic\\census\\census80_90_00.shp";

	// ShapeFileDataReader shpRead = new ShapeFileDataReader();

	// logger.info("loading geo data");
	fileName = "C:\\Users\\Frank\\Documents\\ArcGIS\\mid_east.shp";
	ShapeFileDataReader shpRead = new ShapeFileDataReader();
	shpRead.setFileName(fileName);
	CoordinationManager coord = new CoordinationManager();
	ShapeFileToShape shpToShape = new ShapeFileToShape();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	// GeoData48States stateData = new GeoData48States();
	// coord.addBean(map);
	coord.addBean(shpToShape);
	shpProj.setInputDataSetForApps(shpRead.getDataForApps());

	// geoDsa = shpProj.getOutputDataSetForApps();

	// shpToShape.setInputDataSetForApps(geoDsa);

	// map.setDataSet(geoDsa);

	String citiesFileName = "D:\\temp\\shapefiles\\capitals_1_million.shp";
	citiesFileName = "D:/temp/shapefiles/capitals_1_million.shp";
	// citiesFileName = "D:/temp/shapefiles/yeman_roads.shp";
	// citiesFileName = "D:\\temp\\shapefiles\\yeman_roads";
	ShapeFileDataReader shpRead2 = new ShapeFileDataReader();
	shpRead2.setFileName(citiesFileName);
	// ShapeFileProjection shpProj2 = new ShapeFileProjection();

	// shpProj2.setInputAuxiliaryData(shpRead2.getDataForApps());
	// DataSetForApps citiesDSA = shpProj2
	// .getOutputAuxiliarySpatialDataForApps();
	// shpToShape.setInputDataSetForApps(citiesDSA);
	// map.setDataSet(shpRead2.getDataForApps());
	map2.setAuxiliarySpatialData(shpRead2.getDataForApps());
	// map.setCurrColorColumnX(1);
	// map.setCurrColorColumnY(2);

	shpRead.setFileName(fileName);
	// CoordinationManager coord = new CoordinationManager();
	// ShapeFileToShape shpToShape = new ShapeFileToShape();
	// ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoData48States stateData = new GeoData48States();
	coord.addBean(map2);
	coord.addBean(shpToShape);

	if (useResource) {

	    shpProj.setInputDataSetForApps(stateData.getDataForApps());
	} else {
	    if (useProj) {
		// stateData.addActionListener(shpProj);
		shpProj.setInputDataSet(shpRead.getDataSet());
	    }
	}
	Object[] dataSet = null;
	if (useProj) {
	    dataSet = shpProj.getOutputDataSet();
	} else {
	    dataSet = shpRead.getDataSet();
	}

	shpToShape.setInputDataSet(dataSet);
	// shpToShape.setInputDataSet(dataSet);
	// Rectangle2D rect = new Rectangle2D.Float(-30f,-30f,600f,600f);
	// SpatialExtentEvent ext = new SpatialExtentEvent(map,rect);

	// ShapeFileProjection shpProj2 = new ShapeFileProjection();
	// OldProjection proj = shpProj.getProj();
	// shpProj2.setProj(proj);
	shpProj.setInputAuxiliaryData(shpRead2.getDataSet());

	shpToShape.setInputDataSet(shpProj.getOutputAuxiliarySpatialData());
	map2.setAuxiliarySpatialData(shpToShape.getOutputDataSetForApps());

	// map2.setAuxiliarySpatialData(shpToShape2.getOutputDataSet());

	// ShapeFileToShape shpToShape3 = new ShapeFileToShape();
	// fileName = "C:\\data\\geovista_data\\shapefiles\\jin\\States.shp";

	// ShapeFileDataReader shpRead3 = new ShapeFileDataReader();
	// shpRead3.setFileName(fileName);
	// shpToShape3.setInputDataSet(shpRead3.getDataSet());
	// shpToShape3.setInputDataSet(stateData.getDataSet());
	// map2.setAuxiliarySpatialData(shpToShape3.getOutputDataSetForApps());

	// map2.setDataSet(shpToShape2.getOutputDataSet());

    }

    public static void main(String[] args) {
	boolean useProj = true;

	// GeoMap map = new GeoMap();
	GeoMapMain app = new GeoMapMain("MapBean Main Class");
	app.addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	app.getContentPane().setLayout(
		new BoxLayout(app.getContentPane(), BoxLayout.X_AXIS));
	// app.getContentPane().add(map);
	app.pack();
	app.setVisible(true);

	GeoMap map2 = new GeoMap();
	app.getContentPane().add(map2);
	app.pack();
	app.setVisible(true);

	String fileName = "C:\\Users\\Frank\\Documents\\ArcGIS\\mid_east.shp";
	ShapeFileDataReader shpRead = new ShapeFileDataReader();
	shpRead.setFileName(fileName);
	// CoordinationManager coord = new CoordinationManager();
	ShapeFileToShape shpToShape = new ShapeFileToShape();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoData48States stateData = new GeoData48States();
	// coord.addBean(map);
	// coord.addBean(shpToShape);
	shpProj.setInputDataSetForApps(shpRead.getDataForApps());

	// geoDsa = shpProj.getOutputDataSetForApps();

	// shpToShape.setInputDataSetForApps(geoDsa);

	// map.setDataSet(geoDsa);

	String citiesFileName = "D:\\temp\\shapefiles\\capitals_1_million.shp";
	citiesFileName = "D:/temp/shapefiles/capitals_1_million.shp";
	// citiesFileName = "D:/temp/shapefiles/yeman_roads.shp";
	// citiesFileName = "D:\\temp\\shapefiles\\yeman_roads";
	ShapeFileDataReader shpRead2 = new ShapeFileDataReader();
	shpRead2.setFileName(citiesFileName);
	// ShapeFileProjection shpProj2 = new ShapeFileProjection();

	// shpProj2.setInputAuxiliaryData(shpRead2.getDataForApps());
	// DataSetForApps citiesDSA = shpProj2
	// .getOutputAuxiliarySpatialDataForApps();
	// shpToShape.setInputDataSetForApps(citiesDSA);
	// map.setDataSet(shpRead2.getDataForApps());
	map2.setAuxiliarySpatialData(shpRead2.getDataForApps());
	// map.setCurrColorColumnX(1);
	// map.setCurrColorColumnY(2);

	shpRead.setFileName(fileName);
	// CoordinationManager coord = new CoordinationManager();
	// ShapeFileToShape shpToShape = new ShapeFileToShape();
	// ShapeFileProjection shpProj = new ShapeFileProjection();

	// coord.addBean(map2);
	// coord.addBean(shpToShape);

	if (useProj) {
	    // stateData.addActionListener(shpProj);
	    shpProj.setInputDataSet(shpRead.getDataSet());
	}

	Object[] dataSet = null;
	if (useProj) {
	    dataSet = shpProj.getOutputDataSet();
	} else {
	    dataSet = shpRead.getDataSet();
	}

	shpToShape.setInputDataSet(dataSet);

	shpProj.setInputAuxiliaryData(shpRead2.getDataSet());

	shpToShape.setInputDataSet(shpProj.getOutputAuxiliarySpatialData());

	map2.setDataSet(shpToShape.getOutputDataSetForApps());
	map2.setAuxiliarySpatialData(shpProj.getOutputDataSetForApps());

    }

}