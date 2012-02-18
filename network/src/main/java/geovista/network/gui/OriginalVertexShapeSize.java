package geovista.network.gui;

import java.awt.Shape;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;

public class OriginalVertexShapeSize<V, E> extends
		AbstractVertexShapeTransformer<V> implements Transformer<V, Shape> {

	/**
	 * Controls the shape, size, and aspect ratio for each vertex.
	 * 
	 * @author Joshua O'Madadhain modified by wei luo
	 */

	// protected boolean stretch = false;
	//protected boolean scale = false;
	// protected boolean funny_shapes = false;
	//protected Transformer<V, Double> voltages;
	protected Graph<V, E> graph;

	// protected AffineTransform scaleTransform = new AffineTransform();

	public OriginalVertexShapeSize(Graph<V, E> graphIn) {
		this.graph = graphIn;
		//this.voltages = voltagesIn;
		setSizeTransformer(new Transformer<V, Integer>() {

			public Integer transform(V v) {
				return 5;

			}
		});
	}

	/*
	 * setAspectRatioTransformer(new Transformer<V,Float>() {
	 * 
	 * public Float transform(V v) { if (stretch) { return
	 * (float)(graph.inDegree(v) + 1) / (graph.outDegree(v) + 1); } else {
	 * return 1.0f; } }}); }
	 */

	/*
	 * public void setStretching(boolean stretch) { this.stretch = stretch; }
	 */

	/*public void setScaling(boolean scale) {
		this.scale = scale;
	}*/

	/*
	 * public void useFunnyShapes(boolean use) { this.funny_shapes = use; }
	 */

	public Shape transform(V v) {
		/*
		 * if (funny_shapes) { if (graph.degree(v) < 5) { int sides =
		 * Math.max(graph.degree(v), 3); return factory.getRegularPolygon(v,
		 * sides); } else return factory.getRegularStar(v, graph.degree(v)); }
		 * else
		 */
		return factory.getEllipse(v);
	}
}