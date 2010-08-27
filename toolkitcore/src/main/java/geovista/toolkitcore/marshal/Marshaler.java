/**
 * 
 */
package geovista.toolkitcore.marshal;

import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import geovista.animation.ConditioningAnimator;
import geovista.animation.IndicationAnimator;
import geovista.animation.SelectionAnimator;
import geovista.cartogram.GeoMapCartogram;
import geovista.common.ui.NotePad;
import geovista.common.ui.VariablePicker;
import geovista.geoviz.map.GeoMap;
import geovista.geoviz.map.GeoMapUni;
import geovista.geoviz.map.GraduatedSymbolsMap;
import geovista.geoviz.parvis.ParallelPlot;
import geovista.geoviz.radviz.RadViz;
import geovista.geoviz.scatterplot.SingleHistogram;
import geovista.geoviz.scatterplot.SingleScatterPlot;
import geovista.geoviz.spacefill.SpaceFill;
import geovista.geoviz.spreadsheet.TableViewer;
import geovista.geoviz.spreadsheet.VariableTransformer;
import geovista.geoviz.star.StarPlot;
import geovista.geoviz.star.StarPlotMap;
import geovista.matrix.MapAndScatterplotMatrix;
import geovista.matrix.MapMatrix;
import geovista.matrix.MapScatterplotTreemapMatrix;
import geovista.matrix.TreemapAndScatterplotMatrix;
import geovista.sound.SonicClassifier;
import geovista.toolkitcore.GeoVizToolkit;
import geovista.toolkitcore.ToolkitBean;
import geovista.touchgraph.LinkGraph;
import geovista.touchgraph.PCAViz;
import geovista.touchgraph.SubspaceLinkGraph;

/**
 * @author localadmin
 * 
 */
public class Marshaler {

	XStream streamer;
	public final static Marshaler INSTANCE = new Marshaler();
	static final Logger logger = Logger.getLogger(Marshaler.class.getName());

	private Marshaler() {

		streamer = new XStream(new PureJavaReflectionProvider());

		streamer.alias("NotePad", NotePad.class);
		streamer.registerConverter(new NotePadConverter());

		streamer.alias("VariablePicker", VariablePicker.class);
		streamer.registerConverter(new VariablePickerConverter());

		streamer.alias("TableViewer", TableViewer.class);
		streamer.registerConverter(new TableViewerConverter());

		streamer.alias("VariableTransformer", VariableTransformer.class);
		streamer.registerConverter(new VariableTransformerConverter());

		streamer.alias("SingleHistogram", SingleHistogram.class);
		streamer.registerConverter(new SingleHistogramConverter());

		streamer.alias("GeoMapUni", GeoMapUni.class);
		streamer.registerConverter(new GeoMapUniConverter());

		streamer.alias("SpaceFill", SpaceFill.class);
		streamer.registerConverter(new SpaceFillConverter());

		streamer.alias("SonicClassifier", SonicClassifier.class);
		streamer.registerConverter(new SonicClassifierConverter());

		streamer.alias("GeoMapCartogram", GeoMapCartogram.class);
		streamer.registerConverter(new GeoMapCartogramConverter());

		streamer.alias("SingleScatterPlot", SingleScatterPlot.class);
		streamer.registerConverter(new SingleScatterPlotConverter());

		streamer.alias("GeoMap", GeoMap.class);
		streamer.registerConverter(new GeoMapConverter());

		streamer.alias("LinkGraph", LinkGraph.class);
		streamer.registerConverter(new LinkGraphConverter());

		streamer.alias("ParallelPlot", ParallelPlot.class);
		streamer.registerConverter(new ParallelPlotConverter());

		streamer.alias("StarPlot", StarPlot.class);
		streamer.registerConverter(new StarPlotConverter());

		streamer.alias("StarPlotMap", StarPlotMap.class);
		streamer.registerConverter(new StarPlotMapConverter());

		streamer.alias("GraduatedSymbolsMap", GraduatedSymbolsMap.class);
		streamer.registerConverter(new GraduatedSymbolsMapConverter());

		streamer.alias("RadViz", RadViz.class);
		streamer.registerConverter(new RadVizConverter());

		streamer.alias("PCAViz", PCAViz.class);
		streamer.registerConverter(new PCAVizConverter());

		streamer.alias("SubspaceLinkGraph", SubspaceLinkGraph.class);
		streamer.registerConverter(new SubspaceLinkGraphConverter());

		streamer
				.alias("MapAndScatterplotMatrix", MapAndScatterplotMatrix.class);
		streamer.registerConverter(new MapAndScatterplotMatrixConverter());

		streamer.alias("TreemapAndScatterplotMatrix",
				TreemapAndScatterplotMatrix.class);
		streamer.registerConverter(new TreemapAndScatterplotMatrixConverter());

		streamer.alias("MapScatterplotTreemapMatrix",
				MapScatterplotTreemapMatrix.class);
		streamer.registerConverter(new MapScatterplotTreemapMatrixConverter());

		streamer.alias("MapMatrix", MapMatrix.class);
		streamer.registerConverter(new MapMatrixConverter());

		streamer.alias("IndicationAnimator", IndicationAnimator.class);
		streamer.registerConverter(new IndicationAnimatorConverter());

		streamer.alias("SelectionAnimator", SelectionAnimator.class);
		streamer.registerConverter(new SelectionAnimatorConverter());

		streamer.alias("ConditioningAnimator", ConditioningAnimator.class);
		streamer.registerConverter(new ConditioningAnimatorConverter());

		streamer.alias("GeoVizToolkit", GeoVizToolkit.class);
		streamer.registerConverter(new GeoVizToolkitConverter());

		streamer.alias("ToolkitBean", ToolkitBean.class);
		streamer.registerConverter(new ToolkitBeanConverter());

		streamer.registerConverter(new JInternalFrameConverter());

	}

	// as yet unimplemented. If the constructor with every class listed
	// approach gets cumbersome, then it would not be too hard.
	public void registerConverter(Class clazz) {
		logger.finest(clazz.getName());
	}

	static String JAVA_NEWLINE = "\n";
	static String XML_NEWLINE = "&#010";

	static String newLinesToXML(String input) {
		return input.replaceAll(JAVA_NEWLINE, XML_NEWLINE);
	}

	static String newLinesToJava(String input) {

		return input.replaceAll(XML_NEWLINE, JAVA_NEWLINE);
	}

	public static void main(String[] args) {
		String testString = "123 \n 456";

		String newString = newLinesToXML(testString);
		String oldString = newLinesToJava(newString);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JTextArea area = new JTextArea();
		area.setText(oldString);
		frame.add(area);
		frame.pack();
		frame.setVisible(true);

	}

	public String vizToXML(GeoVizToolkit gvt) {
		logger.info(gvt.toString());
		return null;
	}

	public String toXML(Object obj) {
		String xml = streamer.toXML(obj);
		return xml;
	}

	public Object fromXML(String xml) {
		Object gvz2 = streamer.fromXML(xml);
		return gvz2;
	}

}
