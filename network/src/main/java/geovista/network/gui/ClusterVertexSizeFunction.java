package geovista.network.gui;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.Graph;

public class ClusterVertexSizeFunction<V> implements Transformer<V,Integer> {
    	int size;
        public ClusterVertexSizeFunction(Integer size) {
            this.size = size;
        }

        public Integer transform(V v) {
            if(v instanceof Graph) {
                return 30;
            }
            return size;
        }
    }