package geovista.network.gui;

import org.apache.commons.collections15.Predicate;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;

public class VertexDisplayPredicate <V,E> implements Predicate<Context<Graph<V,E>,V>>
{
    protected boolean filter_small;
    protected final static int MIN_DEGREE = 10;
    
    public VertexDisplayPredicate(boolean filter)
    {
        this.filter_small = filter;
    }
    
    public void filterSmall(boolean b)
    {
        filter_small = b;
    }
    
    public boolean evaluate(Context<Graph<V,E>,V> context) {
    	Graph<V,E> graph = context.graph;
    	V v = context.element;
//        Vertex v = (Vertex)arg0;
        if (filter_small)
            return (graph.degree(v) >= MIN_DEGREE);
        else
            return true;
    }
}


