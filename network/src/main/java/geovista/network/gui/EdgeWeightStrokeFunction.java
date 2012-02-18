package geovista.network.gui;

import java.awt.BasicStroke;
import java.awt.Stroke;
//import java.util.Map;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.Graph;

//import edu.uci.ics.jung.visualization.RenderContext;

public final class EdgeWeightStrokeFunction<V, E>
implements Transformer<E,Stroke>
{
    protected static final Stroke basic = new BasicStroke((float)0.1);
    //protected static final Stroke heavy = new BasicStroke((float) 0.1);
    //protected static final Stroke dotted = RenderContext.DOTTED;
    
    //protected boolean weighted = false;
    //protected Map<E,Number> edge_weight;
    protected Graph<V, E> graph;
    public EdgeWeightStrokeFunction(Graph<V, E> graphIn){
    	this.graph = graphIn;
    	
    	
    }
    /*public EdgeWeightStrokeFunction(Map<E,Number> edge_weight)
    {
        this.edge_weight = edge_weight;
    }
    
    public void setWeighted(boolean weighted)
    {
        this.weighted = weighted;
    }*/
    
    public Stroke transform(E e)
    {
        /*if (weighted)
        {
            if (drawHeavy(e))
                return heavy;
            else
                return dotted;
        }
        else*/
            return basic;
    }
    
    /*protected boolean drawHeavy(E e)
    {
        double value = edge_weight.get(e).doubleValue();
        if (value > 0.7)
            return true;
        else
            return false;
    }*/
    
}
