package geovista.network.algorithm;

import java.util.ArrayList;






/**
 * This is an example of how to use class edu.psu.cse.pda.ypf.MultiLevelConcor to run
 * CONCOR algorithm to get the clustering tree.
 * @author ypf
 *
 */
public class example {

	public static void main(String[] args){
		
		double[][][] matrix={{{1,1,1,1,1,1,1},
                {1,1,1,0,1,1,1},
                {1,1,1,0,1,1,1},
                {1,0,0,1,1,1,1},
                {1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1}}};
        String[] labels={"a", "b", "c", "d", "e", "f", "g"};
        //This is the label of each row/column. It gives semantic meaning to the matrix
        
        MultiLevelConcor mlc=new MultiLevelConcor(labels, matrix);
        //initialize an example of MultiLevelConcor with the labels and raw matrix
        int level=1;
        for(level=2; level<3; level++){
        	mlc.concor(level, 50); //start concor indicating the levels you want and the maximum
            //iterations for each round.
            /**
             * After you call the above function, the signature is produced for each node.
             * Now all you need to do is find the signature to get how the tree looks like.
             * The following shows an example of how to output the cluster in the console.
             */
             int clusterSize=(int)Math.pow(2, level);
             String[] clusters=new String[clusterSize];
             for(int i=0; i<clusters.length; i++)
             clusters[i]="";

             for(int i=0; i<labels.length; i++){
             int sig=mlc.signature(labels[i]);
             if(sig<0)
	             System.err.println("illegal label: "+labels[i]);
             else
	             clusters[sig]+=(" "+labels[i]);
              }

              for(int i=0; i<clusters.length; i++){
              System.out.print("Cluster "+(i+1)+":\n\t");
              System.out.println(clusters[i]);
              }
              System.out.println("\n");
        }
	}
}
