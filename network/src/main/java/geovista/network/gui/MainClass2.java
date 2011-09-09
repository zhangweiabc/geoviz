package geovista.network.gui;

import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMap;
import geovista.readers.example.GeoData48States;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

public class MainClass2 {
	public static void main(String[] args) {
		Start();
	}
	
	public static void Start(){

		JFrame app = new JFrame("test frame");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setLayout(new FlowLayout());
		// NodeLinkView nlView = new NodeLinkView();
		// nlView.setBorder(BorderFactory.createLineBorder(Color.green, 3));
		// getGraphPanel();

		String[] testfile={"C:/Users/localadmin/Desktop/Work with Chanda/WaterConflictNileBasinReorder.txt"};
		//String fileName = "C:\\Users\\localadmin\\Documents\\GeoVista\\Gate's project\\Data\\Niger\\data\\data3\\NigerNewWGS84.shp";
		String fileName="C:\\Users\\localadmin\\Desktop\\Work with Chanda\\ArcMapData\\NileBasinCountries.shp";
		ShapeFileDataReader shpRead = new ShapeFileDataReader();
		shpRead.setFileName(fileName);
		NodeLinkView nlv = new NodeLinkView(testfile);
		DendrogramView dv=new DendrogramView(testfile);

		CoordinationManager coord = new CoordinationManager();

		ShapeFileToShape shpToShape = new ShapeFileToShape();
		ShapeFileProjection shpProj = new ShapeFileProjection();
		GeoData48States stateData = new GeoData48States();

		//nlv.setBorder(BorderFactory.createLineBorder(Color.green, 3));
		//dv.setBorder(BorderFactory.createLineBorder(Color.black, 3));

		Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
		GeoMap map = new GeoMap();
		app.add(map);
		Dimension mapDim=map.getSize();
		JFrame jif2=new JFrame();
		jif2.add(nlv);
		//int size=Math.min(screen.width/2, screen.height/2);
		jif2.setBounds(screen.width*1/4, 0, screen.height/2+100, screen.height/2);
		jif2.setVisible(true);
		
		JFrame jif3=new JFrame();
		//dv.setSize(mapDim);
		jif3.add(dv);
		jif3.setBounds(0, screen.height/2, screen.width, screen.height/2);
		jif3.setVisible(true);
		/*app.add(jif1);
		app.add(jif2);
		app.add(jif3);*/

		coord.addBean(shpToShape);
		coord.addBean(nlv);
		coord.addBean(map);
		coord.addBean(dv);
		shpProj.setInputDataSet(shpRead.getDataSet());
		Object[] dataSet = null;
		dataSet = shpProj.getOutputDataSet();
		shpToShape.setInputDataSet(dataSet);

		app.pack();
		app.setVisible(true);
	}
}
