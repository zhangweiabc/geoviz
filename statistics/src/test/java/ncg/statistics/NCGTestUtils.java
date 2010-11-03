package ncg.statistics;

/*
 * Various testing function extensions for use with NCG testing code
 * 
 * Author : Peter Foley,  03.11.2010
 * 
 */
import static org.junit.Assert.*;

public class NCGTestUtils {

	/*
	 * Compare an array of doubles
	 */
	public static void assertArrayEquals(double[] init, double[] comparison, double delta) {
		
		assertTrue(init.length == comparison.length);
		
		for ( int i = 0; i < init.length; i++) {
			assertTrue((init[i] - comparison[i]) < delta);
		}	
	}
	
	/*
	 * Compare a 2d array of doubles
	 */
	public static void assertArrayEquals(double[][] init, double[][] comparison, double delta) {

		assertTrue(init.length == comparison.length);
		assertTrue(init[0].length == comparison[0].length);
		
		for ( int i = 0; i < init.length; i++ ) {
			for ( int j = 0; j < init[0].length; j++ ) {
				assertTrue((init[i][j] - comparison[i][j]) < delta);
			}
		}	
	}
}
