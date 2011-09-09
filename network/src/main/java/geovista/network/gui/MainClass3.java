package geovista.network.gui;

import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMap;
import geovista.readers.example.GeoData48States;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * 3rd version of main class.
 * 
 * @author peifeng yin
 * 
 */
public class MainClass3 extends JFrame implements ActionListener {

	JMenuBar jmb;
	JMenuItem file;
	JMenu jm;

	WindowAdapter wa;

	JFrame[] jfs;

	NodeLinkView nlv;
	DendrogramView dv;
	GeoMap gm;

	// map parameter
	String fileName;
	ShapeFileDataReader shpRead;
	CoordinationManager coord;
	ShapeFileToShape shpToShape;
	ShapeFileProjection shpProj;
	GeoData48States stateData;

	public MainClass3() {
		super("Multi-Network Visual Analytic Tool");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screen);

		file = new JMenuItem("load data");
		file.addActionListener(this);
		jm = new JMenu("operation");
		jm.add(file);
		jmb = new JMenuBar();
		jmb.add(jm);
		this.setJMenuBar(jmb);
		setVisible(true);

		jfs = new JFrame[3];
		wa = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {

				/*Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
				JFrame jf = (JFrame) e.getSource();
				System.out.println(jf.getTitle() + ": ");
				double temp = ((double) jf.getLocation().x / screen.width);
				double temp2 = ((double) jf.getLocation().y / screen.height);
				System.out.println("\tlocation: " + temp + " screen width and "
						+ temp2 + " screen height");
				temp = ((double) jf.getSize().width / screen.width);
				temp2 = ((double) jf.getSize().height / screen.height);
				System.out.println("Size: " + temp + " screen width anb "
						+ temp2 + " screen height");*/

				
				 for (int i = 0; i < jfs.length; i++) 
					 jfs[i].dispose();
				 
			}
		};
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		int option = chooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File[] file = chooser.getSelectedFiles();
			String[] fp =new String[file.length]; 
			for(int i=0; i<fp.length; i++)
				fp[i]=file[i].getAbsolutePath();
			CreateNodeLinkView(fp);
			CreateDendrogramView(fp);
			Coordinate();
		}
	}

	protected void Coordinate() {
		fileName = "C:\\Users\\weiluo\\Desktop\\Work with Chanda\\ArcMapData\\NileBasinCountries.shp";
		shpRead = new ShapeFileDataReader();
		shpRead.setFileName(fileName);

		coord = new CoordinationManager();

		shpToShape = new ShapeFileToShape();
		shpProj = new ShapeFileProjection();
		stateData = new GeoData48States();

		jfs[2] = new JFrame("Map View");
		jfs[2].addWindowListener(wa);
		jfs[2].setLayout(new FlowLayout());

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		gm = new GeoMap();
		jfs[2].add(gm);

		coord.addBean(shpToShape);
		coord.addBean(nlv);
		coord.addBean(gm);
		coord.addBean(dv);
		shpProj.setInputDataSet(shpRead.getDataSet());
		Object[] dataSet = null;
		dataSet = shpProj.getOutputDataSet();
		shpToShape.setInputDataSet(dataSet);

		jfs[2].pack();
		jfs[2].setLocation(screen.width * 175 / 1000, screen.height / 20);
		jfs[2].setVisible(true);
	}

	protected void CreateNodeLinkView(String[] file) {
		nlv = new NodeLinkView(file);
		JFrame jf = new JFrame("Node Link View");
		jf.add(nlv);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		jf.setBounds(screen.width * 464 / 1000, screen.height / 20,
				screen.width * 35 / 100, screen.height * 44 / 100);

		jf.addWindowListener(wa);

		jf.setVisible(true);
		jfs[0] = jf;
	}

	protected void CreateDendrogramView(String[] file) {
		dv = new DendrogramView(file);
		JFrame jf = new JFrame("Dendrogram View");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		jf.add(dv);
		jf.setBounds(screen.width * 3 / 10, screen.height / 2,
				screen.width * 36 / 100, screen.height * 41 / 100);

		jf.addWindowListener(wa);

		jf.setVisible(true);
		jfs[1] = jf;
	}

	// the main method
	public static void main(String[] args) {
		new MainClass3();
	}
}
