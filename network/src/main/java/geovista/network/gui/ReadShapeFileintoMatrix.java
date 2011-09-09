package geovista.network.gui;



public class ReadShapeFileintoMatrix {
	
	String[] neiborConnectivity;
	double[][] networkMatrix;
	int[][] intNeiborConnectivity;
	
	public ReadShapeFileintoMatrix(String[] Neibors){
		neiborConnectivity = Neibors;
	}
	
	public double[][] getMatrixfromShapefile() throws Exception{
		
		//int aInt[] = Integer.parseInt(neiborConnectivity);
		//intNeiborConnectivity=this.convertStringArraytoIntArray();
		networkMatrix= new double[neiborConnectivity.length][neiborConnectivity.length];
		/*for(int i=0; i<networkMatrix.length; i++){
			for(int j=0; j<networkMatrix.length;j++){
				System.out.println(networkMatrix[i][j]);
			}
		}*/
		
		System.out.println(intNeiborConnectivity.length);
		//System.out.println(intNeiborConnectivity[2].length());
		for (int i =0; i< neiborConnectivity.length; i++){
			
			for (int j=0; j<neiborConnectivity[i].length();j++){
				//System.out.println(neiborConnectivity[i].length());
			}
			//System.out.println(roadConnections.length);
			//stem.out.println(roadConnections[2].charAt(0));
		}
		
		return networkMatrix;
	}
	
	/*private int[][] convertStringArraytoIntArray() throws Exception {
		
		if (neiborConnectivity != null) {
		int intarray[][] = new int[neiborConnectivity.length][neiborConnectivity.length];
		//System.out.println(neiborConnectivity.length);
		for (int i = 0; i < neiborConnectivity.length; i++) {
			for(int j=0; j<neiborConnectivity.length; j++){
		//intarray[i][j] = Integer.parseInt(neiborConnectivity[i][j]);
		//System.out.println(intarray[i]);
		}
		return intarray;
		}
		return null;
		}

}*/
}
