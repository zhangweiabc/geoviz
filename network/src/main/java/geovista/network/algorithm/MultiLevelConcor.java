package geovista.network.algorithm;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a child class of Concor, it implements the multi-level concor.
 * @author peifeng
 * @version 1.0
 *
 */
public class MultiLevelConcor extends Concor {

	protected String[] labels;
	protected int[] signature;
	protected int[] effs; //number of effective bits in signature
	protected HashMap<String,Integer> labelIndex;
	protected double[][][] originMatrix;
	public int maxEff;
	
	public MultiLevelConcor(String[] labels, double[][][] matrix){
		super();
		originMatrix=matrix.clone();
		this.labels=labels;
		signature=new int[matrix[0].length];
		effs=new int[matrix[0].length];
		labelIndex=new HashMap<String, Integer>();
		
		//set initial values for codes, i.e., all 0s
		// and build hashmaps from label to index
		for(int i=0; i<signature.length; i++){
			signature[i]=0;
			effs[i]=0;
			labelIndex.put(labels[i], new Integer(i));
		}
		maxEff=0;
	}
	
	
/**
 * 
 * @param level level of result trees
 * @param max_iteration maximum number of iterations
 * @return the number of levels the clustering tree has
 */
	public int concor(int level, int max_iteration){
		if(level<maxEff)
			return maxEff;
		double[][][] matrix=null;
		
		//double iteration=Math.pow(2, level)-1;
		ArrayList<ArrayList<String>> queue=new ArrayList<ArrayList<String>>();
		HashMap<Integer, String> indexRecorder=new HashMap<Integer, String>();
		if(maxEff==0){
			matrix=Clone(originMatrix);
			for(int i=0; i<labels.length; i++)
				indexRecorder.put(new Integer(i), labels[i]);
		}
		else{
			matrix=Resume(level, indexRecorder, queue);
			if(matrix==null)
				UnifySignature();
		}
		//matrix[0][0]=-1;
		while(true){
			matrix[0]=super.concor(matrix, max_iteration, false);
			ArrayList<ArrayList<String>> result=InterpretResult(matrix[0], indexRecorder);
			UpdateSignature(result);
			while(!result.isEmpty()){
				ArrayList<String> group=result.remove(0);
				if(group.size()>1 && maxEff<level)
					queue.add(group);
			}
			if(queue.isEmpty())
				break;
			ArrayList<String> group=queue.remove(0);
			matrix=GetSubMatrix(group, indexRecorder);
		}
		UnifySignature();
		return maxEff;
	}
	
	protected void UnifySignature(){
		for(int i=0; i<signature.length; i++){
			signature[i]=signature[i]<<(maxEff-effs[i]);
			effs[i]=maxEff;
		}
	}
	
	protected double[][][] Clone(double[][][] matrix){
		double[][][] m2=new double[matrix.length][matrix[0].length][matrix[0][0].length];
		for(int i=0; i<m2.length; i++)
			for(int j=0; j<m2[i].length; j++)
				for(int k=0; k<m2[i][j].length; k++)
					m2[i][j][k]=matrix[i][j][k];
		return m2;
	}
	
	protected ArrayList<ArrayList<String>> InterpretResult(double[][] matrix, 
			HashMap<Integer, String> hm){
		ArrayList<String> g0=new ArrayList<String>();
		ArrayList<String> g1=new ArrayList<String>();
		
		for(int i=0; i<matrix.length; i++){
			if(matrix[0][i]>0)
				g0.add(hm.get(new Integer(i)));
			else
				g1.add(hm.get(new Integer(i)));
		}
		ArrayList<ArrayList<String>> result=new ArrayList<ArrayList<String>>();
		result.add(g0);
		result.add(g1);
		return result;
	}
	
	protected void UpdateSignature(ArrayList<ArrayList<String>> result){
		ArrayList<String> g=result.get(0);
		for(int k=0; k<g.size(); k++){
			String s=g.get(k);
			int i=labelIndex.get(s);
			signature[i]=signature[i]<<1; //cluster 0
			effs[i]++;
			if(k==0 && effs[i]>maxEff)
				maxEff=effs[i];
		}
		
		g=result.get(1);
		for(int k=0; k<g.size(); k++){
			String s=g.get(k);
			int i=labelIndex.get(s);
			signature[i]=(signature[i]<<1)|1; //cluster 1
			effs[i]++;
			if(k==0 && effs[i]>maxEff)
				maxEff=effs[i];
		}
	}
	
	protected double[][][] GetSubMatrix(ArrayList<String> group, 
			HashMap<Integer, String> hm){
		int[] index=new int[group.size()];
		for(int i=0; i<index.length; i++)
			index[i]=labelIndex.get(group.get(i)).intValue();
		
		hm.clear();
		double[][][] matrix=new double[originMatrix.length][index.length][index.length];
		for(int k=0; k<matrix.length; k++){
			for(int i=0; i<index.length; i++){
				matrix[k][i][i]=originMatrix[k][index[i]][index[i]];
				hm.put(new Integer(i), group.get(i));
				for(int j=i+1; j<index.length; j++){
					matrix[k][i][j]=originMatrix[k][index[i]][index[j]];
					matrix[k][j][i]=originMatrix[k][index[j]][index[i]];
				}
			}
		}
		return matrix;
	}
	
/**
 * This method resumes the concor to do further clustering
 * @param level
 * @param indexRecorder
 * @param queue
 * @return the submatrix to be clustered or null if the size all current clusters is 1
 */
	protected double[][][] Resume(int level, HashMap<Integer, String> indexRecorder,
			ArrayList<ArrayList<String>> queue){
		int cluster=(int)Math.pow(2, maxEff);
		for(int i=0; i<cluster; i++)
			queue.add(new ArrayList<String>());
		for(int i=0; i<signature.length; i++){
			ArrayList<String> temp=queue.remove(signature[i]);
			temp.add(labels[i]);
			queue.add(signature[i], temp);
		}
		
		ArrayList<String> group=queue.remove(0);
		while(group.size()<2 && !queue.isEmpty()){
			group=queue.remove(0);
		}
		if(group.size()<2){
			maxEff++;
			return null;
		}
		return GetSubMatrix(group, indexRecorder);
	}
	
/**
 * This method returns the signature (0 or 1) of the node in the given level
 * @param name
 * @param level
 * @return the signature (0 or 1) or -1 if this node is undefined or the level is illegal.
 */
	public int signature(String name, int level){
		Integer index=labelIndex.get(name);
		if(index==null)
			return -1;
		if(level>effs[index.intValue()])
			return -1;
		return (signature[index.intValue()]>>(level-1))&1;
	}
	
	public int signature(String name){
		Integer index=labelIndex.get(name);
		if(index==null)
			return -1;
		return signature[index.intValue()];
	}
	
	public int signature(int index){
		if(index>=signature.length)
			return -1;
		else
			return signature[index];
	}
	
	public String label(int index){
		return index<labels.length?labels[index]:"";
	}
/**
 * This method returns the number of effective bits for given label
 * @param name
 * @return -1 if the label is not found.
 */
	public int signatureEff(String label){
		Integer index=labelIndex.get(label);
		if(index==null)
			return -1;
		return effs[index.intValue()];
	}
}
