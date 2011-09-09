package geovista.network.gui;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.picking.PickedInfo;

public class NodeHighlight<V> implements Transformer<V,Paint>{

	public HashSet<String> selectedNodes;
	protected PickedInfo<V> pi;
	public VisualizationViewer<V, Number> vv;
	public ArrayList<Integer> synchroFlag;
	public NodeHighlight(PickedInfo<V> pi){
		this.pi=pi;
		selectedNodes=new HashSet<String>();
	}
	public int ID=0;
	public int node_no=0;
	public int node_count=0;
	public boolean passive=false; //it indicates whether it is the source of the
	                              // selection event.
	@Override
/**
 * Each time a repaint method is called, this method will be called for each vertex.
 */
	public Paint transform(V v) {
		node_count--;
		float alpha = (new Double(0.9)).floatValue();
		Color c;
		if(!passive && pi.isPicked(v)){
			if(!passive)
				selectedNodes.add(v.toString());
			//vv.repaint();
			//System.out.println(v.toString());
			synchronized(synchroFlag){
				if(synchroFlag.isEmpty() && !passive)
					synchroFlag.add(new Integer(ID));
			}
			if(!passive)
				c= new Color(0, 1f, 0, alpha);
			else
				c= new Color(1f, 0, 0, alpha);
		}
		else if(selectedNodes.contains(v.toString())){
			selectedNodes.remove(v.toString());
			c= new Color(0, 1f, 0, alpha);
		}
		else
			c= new Color(1f, 0, 0, alpha);
		if(node_count==0){
			node_count=node_no;
			if(!passive){
				synchronized(synchroFlag){
					if(!synchroFlag.isEmpty() && !selectedNodes.isEmpty())
						synchroFlag.notifyAll(); //notify the waiting thread that
					                            // a selection event occurs.
				}
			}
			else
				selectedNodes.clear();
			passive=false;
		}
		return c;
	}
}
