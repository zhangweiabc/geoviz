package geovista.network.gui;

import java.io.IOException;

import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.scoring.BarycenterScorer;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.algorithms.scoring.DistanceCentralityScorer;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.Graph;

public class KeyValuesofNodeMeasures {
	
	public Graph graph;
	String[] measureNames;
	// Node Score Part
    static DegreeScorer degreeScorer;
    /*BarycenterScorer barycenterScorer;
    BetweennessCentrality betweennessCentrality;
    ClosenessCentrality closenessCentrality;
    DistanceCentralityScorer distanceCentralityScorer;
    EigenvectorCentrality eigenvectorCentrality;*/
    
    static Integer[] measureDegree;
    /*Double[] measureBarycenter;
	Double[] measureBetweennessCentrality;
	Double[] measureClosenessCentrality;
	Double[] measureDistanceCentralityScorer;
	Object[] measureEigenvectorCentrality;*/
	
	static int maxDegree=0;

	public KeyValuesofNodeMeasures(Graph g) {
		graph=g;	
	}
	
	public static void DegreeCaculation(Graph graph){
		
		measureDegree					= new Integer[graph.getVertexCount()-1];
	    /*measureBarycenter 				= new Double [graph.getVertexCount()-1];
		measureBetweennessCentrality 	= new Double [graph.getVertexCount()-1];
		measureClosenessCentrality 		= new Double [graph.getVertexCount()-1];
		measureDistanceCentralityScorer = new Double [graph.getVertexCount()-1];
		measureEigenvectorCentrality 	= new Object [graph.getVertexCount()-1];*/
		
		for (int i = 0; i < measureDegree.length; i++) {
			measureDegree[i]					=degreeScorer.getVertexScore(i);
		    
			if (measureDegree[i]>maxDegree){
				maxDegree=measureDegree[i];
			}
			
		    /*measureBarycenter[i]				=barycenterScorer.getVertexScore(i);
			measureBetweennessCentrality[i]		=betweennessCentrality.getVertexScore(i);
			measureClosenessCentrality[i]		=closenessCentrality.getVertexScore(i);
			measureDistanceCentralityScorer[i]	=distanceCentralityScorer.getVertexScore(i);
			measureEigenvectorCentrality[i]		=eigenvectorCentrality.getVertexScore(i);*/
		}
	}
	
	public static int getMaxDegree(){
		//DegreeCaculation();
		return maxDegree;
	}
	
	public static void main(String[] args) {
		Graph g=null;
		try {
			ReadMatrix rm = new ReadMatrix();
			g = rm
			.readMatrixtoDirectedGraph("C:\\Users\\weiluo\\Documents\\PSU\\GeoVista\\Social-spatial\\Data\\COW_Trade_2.01\\network_2005_reorder.txt");
	}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			DegreeCaculation(g);
			System.out.println(getMaxDegree());
			}
	}
