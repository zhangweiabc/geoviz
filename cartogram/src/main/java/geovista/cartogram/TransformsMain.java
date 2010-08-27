package geovista.cartogram;

import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Created on Dec 10, 2004
 * Translation from C
 */

// Program for the construction of cartograms (density-equalizing map
// projections) using the Gastner-Newman technique. The program needs polygon
// coordinates and a count of cases in each region as input, calculates the new
// coordinates, and writes these to a file. Figures of the original map and the
// cartogram are created.

// WRITTEN BY MICHAEL GASTNER, September 20, 2004.

// If you use output created by this program please acknowledge the use of this
// code and its first publication in:
// "Generating population density-equalizing maps", Michael T. Gastner and
// M. E. J. Newman, Proceedings of the National Academy of Sciences of the
// United States of America, vol. 101, pp. 7499-7504, 2004.

// The input coordinates in POLYGONFILE must be in ArcInfo "generate" format of
// the type:

//         1
//     0.3248690E+06     0.8558454E+06
//     0.3248376E+06     0.8557575E+06
//     0.3248171E+06     0.8556783E+06
//     0.3247582E+06     0.8556348E+06
//     0.3246944E+06     0.8555792E+06
//     0.3246167E+06     0.8555253E+06
//     ...
//     0.3250221E+06     0.8557324E+06
//     0.3249436E+06     0.8557322E+06
//     0.3248690E+06     0.8558454E+06
// END
//         1
//     0.3248690E+06     0.8558454E+06
//     0.3249651E+06     0.8558901E+06
//     0.3250519E+06     0.8559769E+06
//     0.3250691E+06     0.8561246E+06
//     0.3249678E+06     0.8560541E+06
//     0.3249003E+06     0.8560088E+06
//     ...
//     0.3076424E+06     0.8477603E+06
//     0.3075691E+06     0.8477461E+06
//     0.3075595E+06     0.8476719E+06
// END
//         2
//     0.6233591E+06     0.6056502E+06
//     0.6235193E+06     0.6056467E+06
//     0.6235054E+06     0.6055372E+06
//     ...
//     ...
//     ...
//     0.7384296E+06     0.2334260E+06
//     0.7383532E+06     0.2345770E+06
// END
// END

// The number of cases in CENSUSFILE must be given in the form
// region #cases (optional comment), e.g.:

// 43 9 Alabama
// 51 3 Alaska
// 37 8 Arizona
// 47 6 Arkansas
// 25 54 California
// 32 8 Colorado
// 19 8 Connecticut
// 29 3 Delaware
// 28 3 District of Columbia
// 49 25 Florida
// 45 13 Georgia
// 1 4 Hawaii
// ...

// The output coordinates are written to CARTGENFILE in ArcInfo "generate"
// format. A postscript image of the original input is prepared as MAP2PS,
// an image of the cartogram as CART2PS.

// Modified on Oct 29, 2004. The number of divisions in each dimension lx, ly
// will now be determined from the input map. Also fixed array bound
// violations in intpol and newt2, and centered the ps-files.

/**
 * @author Nick
 * 
 */

public class TransformsMain {

	final static Logger logger = Logger.getLogger(TransformsMain.class
			.getName());
	// TRANSLATION: #define in C is translated to public static final within the
	// class

	private String genFileName = "./cartogram.gen"; // Cartogram generate file.
													// - output
	private String dataFileName = "./census.dat"; // Input - open file for input
	private String polygonFileName = "./map.gen"; // Input coordinates. open
													// file for input
	private int maxNSquareLog = 20; // The maximum number of squares is
									// 2^MAXNSQLOG.
	private double blurWidth = 0.1; // Initial width of Gaussian blur.
									// Originally named SIGMA
	private double blurWidthFactor = 1.2; // Must be > 1. it is the factor by
											// which sigma increased upon an
											// unsuccessful pass. Originally
											// named SIGMAFAC
	private int arrayLength;

	public static final int DEFAULT_MAXNSQUARELOG = 10;
	public static final double DEFAULT_BLURWIDTH = 0.1;
	public static final double DEFAULT_BLURWIDTHFACTOR = 1.2;

	private static final String CART2PS = "";// Cartogram image - open file for
												// WRITE oldname "./cart.ps";
	private static final String MAP2PS = ""; // Map image. Open file for WRITE
												// oldname "./map.ps"
	private static final double CONVERGENCE = 1e-100; // Convergence criterion
														// for integrator.
	private static final double HINITIAL = 1e-4; // Initial time step size in
													// nonlinvoltra.
	private static final int IMAX = 50; // Maximum number of iterations in
										// Newton-Raphson routine.
	private static final int MAXINTSTEPS = 3000; // Maximum number of time steps
													// in nonlinvoltra.

	private static final double MINH = 1e-5; // Smallest permitted time step in
												// the integrator.
	private static final int NSUBDIV = 1; // Number of linear subdivisions for
											// digitizing the density.
	private static final double PADDING = 1.5; // Determines space between map
												// and boundary.
	private static final double PI = 3.141592653589793;

	// #define SWAP = (a,b) tempr=(a);(a)=(b);(b)=tempr;
	// SWAP cannot be used as a macro here, but instead it will be coded
	// whenever necessary
	private static final double TIMELIMIT = 1e8;// Maximum time allowed in
												// integrator.
	private static final double TOLF = 1e-3; // Sensitivity w. r. t. function
												// value in newt2.
	private static final double TOLINT = 1e-3; // Sensitivity of the integrator.
	private static final double TOLX = 1e-3; // Sensitivity w. r. t. independent
												// variables in newt2.

	public static final String DISPLFILE = null; // or just string

	// Globals
	GeneralPath[] inputShapes;
	GeneralPath[] outputShapes;
	ArrayFloat gridvx[], gridvy[];
	float maxx, maxy, minpop, minx, miny, polymaxx, polymaxy, polyminx,
			polyminy;

	ArrayFloat rho[], rho_0[], vx[], vy[], x[], y[], xappr[], yappr[];

	float xstepsize, ystepsize;

	int lx, ly, maxid, nblurs = 0, npoly;

	int ncorn[], polygonid[];
	ArrayPoint corn[];

	// TODO - temporary func
	// http://www.cs.princeton.edu/introcs/26function/MyMath.java.html
	// can try using:
	// http://home.online.no/~pjacklam/notes/invnorm/impl/karimov/StatUtil.java
	// fractional error less than 1.2 * 10 ^ -7.
	private static double erf(double z) {
		double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

		// use Horner's method
		double ans = 1
				- t
				* Math
						.exp(-z
								* z
								- 1.26551223
								+ t
								* (1.00002368 + t
										* (0.37409196 + t
												* (0.09678418 + t
														* (-0.18628806 + t
																* (0.27886807 + t
																		* (-1.13520398 + t
																				* (1.48851587 + t
																						* (-0.82215223 + t * (0.17087277))))))))));
		if (z >= 0) {
			return ans;
		}
		return -ans;
	}

	// Function to count the number of polygons.
	private void countpoly() {
		String line;
		BufferedReader inFile = FileTools.openFileRead(polygonFileName);

		if (inFile == null) {
			throw new RuntimeException("Could not open " + polygonFileName);
		}
		while ((line = FileTools.readLine(inFile)) != null) {
			if (line.charAt(0) == 'E') {
				npoly++;
			}
		}
		npoly--; // The .gen file ends with two consecutive "END"s.
		FileTools.closeFile(inFile);
	}

	// Function to count polygon corners. Also determines minimum/maximum x-/y-
	// coordinate.
	private void countcorn() {
		String line;
		BufferedReader inFile = FileTools.openFileRead(polygonFileName);
		float x, y;
		int polyctr = 0, ratiolog;

		countpoly();
		ncorn = new int[npoly];
		corn = new ArrayPoint[npoly];

		// corn = (POINT**)malloc(npoly*sizeof(POINT*));
		FileTools.readLine(inFile); // Skip first line.
		line = FileTools.readLine(inFile);

		StringTokenizer st = new StringTokenizer(line);
		x = Float.parseFloat(st.nextToken());
		y = Float.parseFloat(st.nextToken());
		polyminx = x;
		polymaxx = x;
		polyminy = y;
		polymaxy = y;
		ncorn[0] = 1;
		while ((line = FileTools.readLine(inFile)) != null) {
			if (line.charAt(0) != 'E') {
				StringTokenizer st2 = new StringTokenizer(line);
				x = Float.parseFloat(st2.nextToken());
				y = Float.parseFloat(st2.nextToken());
				if (x < polyminx) {
					polyminx = x;
				}
				if (x > polymaxx) {
					polymaxx = x;
				}
				if (y < polyminy) {
					polyminy = y;
				}
				if (y > polymaxy) {
					polymaxy = y;
				}
				ncorn[polyctr]++;
			} else {
				line = FileTools.readLine(inFile);
				corn[polyctr] = new ArrayPoint(ncorn[polyctr]);

				polyctr++;
			}
		}
		if (Math.ceil(Math.log((polymaxx - polyminx) / (polymaxy - polyminy))
				/ Math.log(2))
				+ Math.floor(Math.log((polymaxx - polyminx)
						/ (polymaxy - polyminy))
						/ Math.log(2)) > 2
				* Math.log((polymaxx - polyminx) / (polymaxy - polyminy))
				/ Math.log(2)) {
			ratiolog = (int) Math.floor(Math.log((polymaxx - polyminx)
					/ (polymaxy - polyminy))
					/ Math.log(2));
		} else {
			ratiolog = (int) Math.ceil(Math.log((polymaxx - polyminx)
					/ (polymaxy - polyminy))
					/ Math.log(2));
		}
		lx = (int) Math.pow(2, (int) (0.5 * (ratiolog + maxNSquareLog)));
		ly = (int) Math.pow(2, (int) (0.5 * (maxNSquareLog - ratiolog)));
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("lx=" + lx + ", ly=" + ly + "\n");
		}
		if ((polymaxx - polyminx) / lx > (polymaxy - polyminy) / ly) {
			maxx = (float) (0.5 * ((1 + PADDING) * polymaxx + (1 - PADDING)
					* polyminx));
			minx = (float) (0.5 * ((1 - PADDING) * polymaxx + (1 + PADDING)
					* polyminx));
			maxy = (float) (0.5 * (polymaxy + polyminy + (maxx - minx) * ly
					/ lx));
			miny = (float) (0.5 * (polymaxy + polyminy - (maxx - minx) * ly
					/ lx));
		} else {
			maxy = (float) (0.5 * ((1 + PADDING) * polymaxy + (1 - PADDING)
					* polyminy));
			miny = (float) (0.5 * ((1 - PADDING) * polymaxy + (1 + PADDING)
					* polyminy));
			maxx = (float) (0.5 * (polymaxx + polyminx + (maxy - miny) * lx
					/ ly));
			minx = (float) (0.5 * (polymaxx + polyminx - (maxy - miny) * lx
					/ ly));
		}

		// Uncomment the next lines for interactive choice of boundary
		// conditions.

		/*
		 * printf("For the %i polygon(s) under consideration:\n",npoly);
		 * printf("minimum x = %f\tmaximum x = %f\n",polyminx,polymaxx);
		 * printf("minimum y = %f\tmaximum y = %f\n",polyminy,polymaxy); while
		 * (!bnd_ok) { printf("Type in your choice of boundaries.\nminx = ");
		 * scanf("%f",&minx); printf("maxx = "); scanf("%f",&maxx);
		 * printf("miny = "); scanf("%f",&miny); printf("maxy = ");
		 * scanf("%f",&maxy); if (minx>polyminx || maxx<polymaxx ||
		 * miny>polyminy || maxy<polymaxy)
		 * printf("Invalid choice, does not enclose the polygon(s).\n"); else
		 * bnd_ok = TRUE; }
		 */
		FileTools.closeFile(inFile);
	}

	// Function to read polygon corners.
	private void readcorn() {
		String line;
		BufferedReader inFile = FileTools.openFileRead(polygonFileName);
		float xcoord, ycoord;
		int i, id, polyctr = 0;

		countcorn();
		polygonid = new int[npoly];
		xstepsize = (maxx - minx) / lx;
		ystepsize = (maxy - miny) / ly;
		if (Math.abs((xstepsize / ystepsize) - 1) > 1e-3) {
			System.err.println("WARNING: Area elements are not square: "
					+ xstepsize + " : " + ystepsize + "\n");
		}
		line = FileTools.readLine(inFile);
		id = FileTools.readInt(line);
		polygonid[polyctr] = id;
		i = 0;
		while ((line = FileTools.readLine(inFile)) != null) {
			if (line.charAt(0) != 'E') {
				StringTokenizer st2 = new StringTokenizer(line);
				xcoord = Float.parseFloat(st2.nextToken());
				ycoord = Float.parseFloat(st2.nextToken());
				corn[polyctr].array[i].x = (xcoord - minx) / xstepsize;
				corn[polyctr].array[i++].y = (ycoord - miny) / ystepsize;
			} else {
				line = FileTools.readLine(inFile);
				i = 0;
				polyctr++;
				if (polyctr < npoly) {
					polygonid[polyctr] = id = FileTools.readInt(line);
				}
			}
		}
		polyminx = (polyminx - minx) / xstepsize;
		polyminy = (polyminy - miny) / ystepsize;
		polymaxx = (polymaxx - minx) / xstepsize;
		polymaxy = (polymaxy - miny) / ystepsize;
		FileTools.closeFile(inFile);
	}

	// Function to calculate the crossing number for a point with respect to a
	// polygon in order to determine whether the point is inside or not. For
	// details see
	// www.ecse.rpi.edu/Homepages/wrf/research/geom/pnpoly.html#The%20C%20Code.
	private int crnmbr(float x, float y, int ncrns, Point polygon[]) {
		int i, j, c = 0;
		for (i = 0, j = ncrns - 1; i < ncrns; j = i++) {
			if ((((polygon[i].y <= y) && (y < polygon[j].y)) || ((polygon[j].y <= y) && (y < polygon[i].y)))
					&& (x < (polygon[j].x - polygon[i].x) * (y - polygon[i].y)
							/ (polygon[j].y - polygon[i].y) + polygon[i].x)) {
				// c = !c;
				c = (1 ^ c);
			}
		}
		return c;
	}

	// Function to determine polygon area. This is needed to determine the
	// average
	// population.
	// The problem in short is to find the area of a polygon whose vertices are
	// given. Recall Stokes' theorem in 3d for a vector field v:
	// integral[around closed curve dA]v(x,y,z).ds =
	// integral[over area A]curl(v).dA.
	// Now let v(x,y,z) = (0,Q(x,y),0) and dA = (0,0,dx*dy). Then
	// integral[around closed curve dA]Q(x,y)dy = integral[over area
	// A]dQ/dx*dx*dy.
	// If Q = x:
	// A = integral[over area A]dx*dy = integral[around closed curve dA]x dy.
	// For every edge from (x[i],y[i]) to (x[i+1],y[i+1]) there is a
	// parametrization
	// (x(t),y(t)) = ((1-t)x[i]+t*x[i+1],(1-t)y[i]+t*y[i+1]), 0<t<1
	// so that the path integral along this edge is
	// int[from 0 to 1]{(1-t)x[i]+t*x[i+1]}(y[i+1]-y[i])dt =
	// 0.5*(y[i+1]-y[i])*(x[i]+x[i+1]).
	// Summing over all edges yields:
	//
	// Area = 0.5*[(x[0]+x[1])(y[1]-y[0]) + (x[1]+x[2])(y[2]-y[1]) + ...
	// ...+(x[n-1]+x[n])(y[n]-y[n-1])+(x[n]+x[0])(y[0]-y[n])]

	private double polygonarea(int ncrns, Point polygon[]) {
		double area = 0;
		int i;

		for (i = 0; i < ncrns - 1; i++) {
			area += 0.5 * (polygon[i].x + polygon[i + 1].x)
					* (polygon[i + 1].y - polygon[i].y);
		}
		area += 0.5 * (polygon[ncrns - 1].x + polygon[0].x)
				* (polygon[0].y - polygon[ncrns - 1].y);
		return Math.abs(area);
	}

	// Function to digitize density.
	private void digdens() {
		String line;
		double area[], totarea = 0.0, totpop = 0.0;
		BufferedReader inFile = FileTools.openFileRead(dataFileName);
		float avgdens, dens[];
		int cases[], i, id, ii, j, jj, ncases, polyctr;

		// Read CENSUSFILE.

		line = FileTools.readLine(inFile);
		id = FileTools.readInt(line);
		maxid = id;
		while ((line = FileTools.readLine(inFile)) != null) {
			id = FileTools.readInt(line);
			if (id > maxid) {
				maxid = id;
			}
		}
		FileTools.closeFile(inFile);
		cases = new int[maxid + 1];

		inFile = FileTools.openFileRead(dataFileName);
		while ((line = FileTools.readLine(inFile)) != null) {
			StringTokenizer st = new StringTokenizer(line);
			id = Integer.parseInt(st.nextToken());
			ncases = Integer.parseInt(st.nextToken());
			totpop += (cases[id] = ncases);
		}
		FileTools.closeFile(inFile);

		// Calculate for each polygon the area of the political unit it belongs
		// to.
		// This will in general not be the area of the polygon, e. g. if it
		// consists
		// of several islands. Here we assume that the polygon identifiers in
		// BOUNDARYFILE are the same for all polygons belonging to one political
		// unit.

		area = new double[npoly];
		for (polyctr = 0; polyctr < npoly; polyctr++) {
			totarea += polygonarea(ncorn[polyctr], corn[polyctr].array);
			area[polyctr] = 0;
			for (i = 0; i < npoly; i++) {
				if (polygonid[i] == polygonid[polyctr]) {
					area[polyctr] += polygonarea(ncorn[i], corn[i].array);
				}
			}
		}

		// Calculate the correct density for each polygon.

		dens = new float[npoly];
		for (polyctr = 0; polyctr < npoly; polyctr++) {
			dens[polyctr] = (float) (cases[polygonid[polyctr]] / area[polyctr]);
		}

		// Calculate the average density.

		avgdens = (float) (totpop / totarea);

		// Digitize density.

		for (i = 0; i <= lx; i++) {
			for (j = 0; j <= ly; j++) {
				rho_0[i].array[j] = 0; // Initialize.
			}
		}

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("digitizing density ...\n");
		}
		for (i = 0; i < lx; i++) {
			if (logger.isLoggable(Level.FINEST)) {
				if (i % 100 == 1 && i != 1) {
					logger.finest("finished " + (i - 1) + " of " + lx + "\n");
				}
			}
			for (j = 0; j < ly; j++) {
				for (ii = 0; ii < NSUBDIV; ii++) {
					for (jj = 0; jj < NSUBDIV; jj++) {
						if (i - 0.5 + (float) (ii + 1) / NSUBDIV < polyminx
								|| i - 0.5 + (float) ii / NSUBDIV > polymaxx
								|| j - 0.5 + (float) (jj + 1) / NSUBDIV < polyminy
								|| j - 0.5 + (float) jj / NSUBDIV > polymaxy) {
							rho_0[i].array[j] += avgdens / (NSUBDIV * NSUBDIV);
							continue;
						}
						for (polyctr = 0; polyctr < npoly; polyctr++) {
							if (crnmbr((float) (i - 0.5 + (float) (2 * ii + 1)
									/ (2 * NSUBDIV)),
									(float) (j - 0.5 + (float) (2 * ii + 1)
											/ (2 * NSUBDIV)), ncorn[polyctr],
									corn[polyctr].array) != 0) {
								rho_0[i].array[j] += dens[polyctr]
										/ (NSUBDIV * NSUBDIV);
								break;
							}
						}
						if (polyctr == npoly) {
							rho_0[i].array[j] += avgdens / (NSUBDIV * NSUBDIV);
						}
					}
				}
			}
		}

		// Fill the edges correctly.

		rho_0[0].array[0] += rho_0[0].array[ly] + rho_0[lx].array[0]
				+ rho_0[lx].array[ly];
		for (i = 1; i < lx; i++) {
			rho_0[i].array[0] += rho_0[i].array[ly];
		}
		for (j = 1; j < ly; j++) {
			rho_0[0].array[j] += rho_0[lx].array[j];
		}
		for (i = 0; i < lx; i++) {
			rho_0[i].array[ly] = rho_0[i].array[0];
		}
		for (j = 0; j <= ly; j++) {
			rho_0[lx].array[j] = rho_0[0].array[j];
		}

		// Replace rho_0 by Fourier transform

		coscosft(rho_0, 1, 1);

		area = null; // free(area);
		cases = null; // free(cases);
		for (i = 0; i < npoly; i++) {
			corn[i].array = null;
			corn[i] = null; // free(corn[i]);
		}
		corn = null; // free(corn);
		dens = null; // free(dens);
		ncorn = null; // free(ncorn);
		polygonid = null; // free(polygonid);
	}

	// Function to replace data[1...2*nn] by its discrete Fourier transform, if
	// isign is input as 1; or replaces data[1...2*nn] by nn times its inverse
	// discrete Fourier transform, if isign is input as -1. data is a complex
	// array
	// of length nn or, equivalently, a real array of length 2*nn. nn MUST be an
	// integer power of 2 (this is not checked for!).
	// From "Numerical Recipes in C".
	private void four1(float data[], long nn, int isign)
	// unsigned long (32bit) changed to long (64 bit)
	{
		double theta, wi, wpi, wpr, wr, wtemp;
		float tempi, tempr;
		long i, istep, j, m, mmax, n;
		float tmpf;

		n = nn << 1;
		j = 1;
		for (i = 1; i < n; i += 2) {
			if (j > i) {
				// This is the bit-reversal section of the routine.
				// SWAP(data[j],data[i]);
				tmpf = data[(int) j];
				data[(int) j] = data[(int) i];
				data[(int) i] = tmpf;

				// SWAP(data[j+1],data[i+1]); // Exchange the two complex
				// numbers.
				tmpf = data[(int) j + 1];
				data[(int) j + 1] = data[(int) i + 1];
				data[(int) i + 1] = tmpf;
			}
			m = n >> 1;
			while (m >= 2 && j > m) {
				j -= m;
				m >>= 1;
			}
			j += m;
		}
		// Here begins the Danielson-Lanczos section of the routine.
		mmax = 2;
		while (n > mmax) // Outer loop executed log_2 nn times.
		{
			istep = mmax << 1;
			// Initialize the trigonometric recurrence.
			theta = isign * (6.28318530717959 / mmax);
			wtemp = Math.sin(0.5 * theta);
			wpr = -2.0 * wtemp * wtemp;
			wpi = Math.sin(theta);
			wr = 1.0;
			wi = 0.0;
			for (m = 1; m < mmax; m += 2) // Here are the two nested inner
											// loops.
			{
				for (i = m; i <= n; i += istep) {
					j = i + mmax; // This is the Danielson-Lanczos formula
					tempr = (float) (wr * data[(int) j] - wi
							* data[(int) j + 1]);
					tempi = (float) (wr * data[(int) j + 1] + wi
							* data[(int) j]);
					data[(int) j] = data[(int) i] - tempr;
					data[(int) j + 1] = data[(int) i + 1] - tempi;
					data[(int) i] += tempr;
					data[(int) i + 1] += tempi;
				}
				wr = (wtemp = wr) * wpr - wi * wpi + wr; // Trigonometric
															// recurrence.
				wi = wi * wpr + wtemp * wpi + wi;
			}
			mmax = istep;
		}
	}

	// Function to calculate the Fourier Transform of a set of n real-valued
	// data
	// points. It replaces this data (which is stored in array data[1...n]) by
	// the
	// positive frequency half of its complex Fourier Transform. The real-valued
	// first and last components of the complex transform are returned as
	// elements
	// data[1] and data[2] respectively. n must be a power of 2. This routine
	// also
	// calculates the inverse transform of a complex data array if it is the
	// transform of real data. (Result in this case must be multiplied by 2/n).
	// From "Numerical Recipes in C".
	private void realft(float data[], long n, int isign)
	// changed unsigned int to long (64 bits)
	{
		double theta, wi, wpi, wpr, wr, wtemp;
		float c1 = (float) 0.5, c2, h1i, h1r, h2i, h2r;
		long i, i1, i2, i3, i4, np3;

		theta = 3.141592653589793 / (n >> 1); // Initialize the recurrence
		if (isign == 1) {
			c2 = (float) -0.5;
			four1(data, n >> 1, 1); // The forward transform is here.
		} else // Otherwise set up for an inverse transform.
		{
			c2 = (float) 0.5;
			theta = -theta;
		}
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		wr = 1.0 + wpr;
		wi = wpi;
		np3 = n + 3;
		for (i = 2; i <= (n >> 2); i++) // Case i=1 done separately below.
		{
			i4 = 1 + (i3 = np3 - (i2 = 1 + (i1 = i + i - 1)));
			// The two separate transforms are separated out of data.
			h1r = c1 * (data[(int) i1] + data[(int) i3]);
			h1i = c1 * (data[(int) i2] - data[(int) i4]);
			h2r = -c2 * (data[(int) i2] + data[(int) i4]);
			h2i = c2 * (data[(int) i1] - data[(int) i3]);
			// Here they are recombined to form the true transform of the
			// original
			// data.
			data[(int) i1] = (float) (h1r + wr * h2r - wi * h2i);
			data[(int) i2] = (float) (h1i + wr * h2i + wi * h2r);
			data[(int) i3] = (float) (h1r - wr * h2r + wi * h2i);
			data[(int) i4] = (float) (-h1i + wr * h2i + wi * h2r);
			wr = (wtemp = wr) * wpr - wi * wpi + wr; // The recurrence.
			wi = wi * wpr + wtemp * wpi + wi;
		}
		if (isign == 1) {
			data[1] = (h1r = data[1]) + data[2]; // Squeeze the first and last
													// data
			// together to get them all within the original array.
			data[2] = h1r - data[2];
		} else {
			data[1] = c1 * ((h1r = data[1]) + data[2]);
			data[2] = c1 * (h1r - data[2]);
			// This is the inverse transform for the case isign = -1.
			four1(data, n >> 1, -1);
		}
	}

	// Function to calculate the cosine transform of a set z[0...n] of
	// real-valued
	// data points. The transformed data replace the original data in array z. n
	// must be a power of 2. For forward transform set isign=1, for back
	// transform
	// isign = -1. (Note: The factor 2/n has been taken care of.)
	// From "Numerical Recipes in C".
	private void cosft(float z[], int n, int isign) {
		double theta, wi = 0.0, wpi, wpr, wr = 1.0, wtemp;
		float a[], sum, y1, y2;
		int j, n2;

		// Numerical Recipes starts counting at 1 which is rather confusing. I
		// will
		// count from 0.

		a = new float[n + 2];
		for (j = 1; j <= n + 1; j++) {
			a[j] = z[j - 1];
		}

		// Here is the Numerical Recipes code.

		theta = PI / n; // Initialize the recurrence.
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		sum = (float) (0.5 * (a[1] - a[n + 1]));
		a[1] = (float) (0.5 * (a[1] + a[n + 1]));
		n2 = n + 2;
		for (j = 2; j <= (n >> 1); j++) {
			wr = (wtemp = wr) * wpr - wi * wpi + wr;
			wi = wi * wpr + wtemp * wpi + wi;
			y1 = (float) (0.5 * (a[j] + a[n2 - j]));
			y2 = (a[j] - a[n2 - j]);
			a[j] = (float) (y1 - wi * y2);
			a[n2 - j] = (float) (y1 + wi * y2);
			sum += wr * y2;
		}
		realft(a, n, 1);
		a[n + 1] = a[2];
		a[2] = sum;
		for (j = 4; j <= n; j += 2) {
			sum += a[j];
			a[j] = sum;
		}

		// Finally I revert to my counting method.

		if (isign == 1) {
			for (j = 1; j <= n + 1; j++) {
				z[j - 1] = a[j];
			}
		} else if (isign == -1) {
			for (j = 1; j <= n + 1; j++) {
				z[j - 1] = (float) (2.0 * a[j] / n);
			}
		}
		a = null;
	}

	// Function to calculate the sine transform of a set of n real-valued data
	// points stored in array z[0..n]. The number n must be a power of 2. On
	// exit
	// z is replaced by its transform. For forward transform set isign=1, for
	// back
	// transform isign = -1.

	// NO OBJECT REFERENCE
	// unsigned long n ->int n
	private void sinft(float z[], int n, int isign) {
		double theta, wi = 0.0, wpi, wpr, wr = 1.0, wtemp;
		float a[], sum, y1, y2;
		int j;
		int n2 = n + 2; // unsigned long

		// See my comment about Numerical Recipe's counting above. Note that the
		// last
		// component plays a completely passive role and does not need to be
		// stored.

		a = new float[n + 1];
		for (j = 1; j <= n; j++) {
			a[j] = z[j - 1];
		}

		// Here is the Numerical Recipes code.

		theta = PI / n; // Initialize the recurrence.
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		a[1] = 0.0f;
		for (j = 2; j <= (n >> 1) + 1; j++) {
			// Calculate the sine for the auxiliary array.

			wr = (wtemp = wr) * wpr - wi * wpi + wr;

			// The cosine is needed to continue the recurrence.

			wi = wi * wpr + wtemp * wpi + wi;

			// Construct the auxiliary array.

			y1 = (float) (wi * (a[j] + a[n2 - j]));
			y2 = (float) (0.5 * (a[j] - a[n2 - j]));

			// Terms j and N-j are related.

			a[j] = y1 + y2;
			a[n2 - j] = y1 - y2;
		}

		// Transform the auxiliary array.

		realft(a, n, 1);

		// Initialize the sum used for odd terms below.

		a[1] *= 0.5;
		sum = a[2] = 0.0f;

		// Even terms determined directly. Odd terms determined by running sum.

		for (j = 1; j <= n - 1; j += 2) {
			sum += a[j];
			a[j] = a[j + 1];
			a[j + 1] = sum;
		}

		// Change the indices.

		if (isign == 1) {
			for (j = 1; j <= n; j++) {
				z[j - 1] = a[j];
			}
		} else if (isign == -1) {
			for (j = 1; j <= n; j++) {
				z[j - 1] = (float) 2.0 * a[j] / n;
			}
		}
		z[n] = 0.0f;
		a = null;
	}

	// Function to calculate a two-dimensional cosine Fourier transform.
	// Forward/
	// backward transform in x: isign1 = +/-1, in y: isign2 = +/-1.

	// NO OBJECT REFERENCE
	private void coscosft(ArrayFloat y[], int isign1, int isign2) {
		float temp[] = new float[lx + 1];
		int i, j; // unsigned long

		for (i = 0; i <= lx; i++) {
			cosft(y[i].array, ly, isign2);
		}
		for (j = 0; j <= ly; j++) {
			for (i = 0; i <= lx; i++) {
				temp[i] = y[i].array[j];
			}
			cosft(temp, lx, isign1);
			for (i = 0; i <= lx; i++) {
				y[i].array[j] = temp[i];
			}
		}
	}

	// Function to calculate a cosine Fourier transform in x and a sine
	// transform
	// in y. Forward/backward transform in x: isign1 = +/-1, in y: isign2 =
	// +/-1.

	// NO OBJECT REFERENCE
	private void cossinft(ArrayFloat y[], int isign1, int isign2) {
		float temp[] = new float[lx + 1];
		int i, j; // unsigned long

		for (i = 0; i <= lx; i++) {
			sinft(y[i].array, ly, isign2);
		}
		for (j = 0; j <= ly; j++) {
			for (i = 0; i <= lx; i++) {
				temp[i] = y[i].array[j];
			}
			cosft(temp, lx, isign1);
			for (i = 0; i <= lx; i++) {
				y[i].array[j] = temp[i];
			}
		}
	}

	// Function to calculate a sine Fourier transform in x and a cosine
	// transform
	// in y. Forward/backward transform in x: isign1 = +/-1, in y: isign2 =
	// +/-1.

	// NO OBJECT REFERENCE
	private void sincosft(ArrayFloat y[], int isign1, int isign2) {
		float temp[] = new float[lx + 1];
		int i, j;// unsigned long

		for (i = 0; i <= lx; i++) {
			cosft(y[i].array, ly, isign2);
		}
		for (j = 0; j <= ly; j++) {
			for (i = 0; i <= lx; i++) {
				temp[i] = y[i].array[j];
			}
			sinft(temp, lx, isign1);
			for (i = 0; i <= lx; i++) {
				y[i].array[j] = temp[i];
			}
		}
	}

	// Function to replace data by its ndim-dimensional discrete Fourier
	// transform,
	// if isign is input as 1. nn[1..ndim] is an integer array containing the
	// lengths of each dimension (number of complex values), which MUST be all
	// powers of 2. data is a real array of length twice the product of these
	// lengths, in which the data are stored as in a multidimensional complex
	// array: real and imaginary parts of each element are in consecutive
	// locations, and the rightmost index of the array increases most rapidly as
	// one proceeds along data. For a two-dimensional array, this is equivalent
	// to
	// storing the arrays by rows. If isign is input as -1, data is replaced by
	// its
	// inverse transform times the product of the lengths of all dimensions.

	// NO OBJECT REFERENCE
	// unsigned long nn[] -> changed to int nn[]
	// data[] must became a d3tensor from [1..][1..][1..] to regular
	private void fourn(D3Tensor data/* float data[] */,/* long */int[] nn,
			int ndim, int isign) {
		int idim;
		long i1, i2, i3, i2rev, i3rev, ip1, ip2, ip3, ifp1, ifp2; // unsigned
																	// long
		long ibit, k1, k2, n, nprev, nrem, ntot; // unsigned long
		double tempi, tempr;
		float theta, wi, wpi, wpr, wr, wtemp;

		for (ntot = 1, idim = 1; idim <= ndim; idim++) {
			ntot *= nn[idim];
		}
		nprev = 1;
		for (idim = ndim; idim >= 1; idim--) {
			n = nn[idim];
			nrem = ntot / (n * nprev);
			ip1 = nprev << 1;
			ip2 = ip1 * n;
			ip3 = ip2 * nrem;
			i2rev = 1;
			for (i2 = 1; i2 <= ip2; i2 += ip1) {
				if (i2 < i2rev) {
					for (i1 = i2; i1 <= i2 + ip1 - 2; i1 += 2) {
						for (i3 = i1; i3 <= ip3; i3 += ip2) {
							i3rev = i2rev + i3 - i2;
							// SWAP(data[i3],data[i3rev]);
							data.swapElements((int) i3, (int) i3rev);

							// SWAP(data[i3+1],data[i3rev+1]);
							data.swapElements((int) i3 + 1, (int) i3rev + 1);
						}
					}
				}
				ibit = ip2 >> 1;
				while (ibit >= ip1 && i2rev > ibit) {
					i2rev -= ibit;
					ibit >>= 1;
				}
				i2rev += ibit;
			}
			ifp1 = ip1;
			while (ifp1 < ip2) {
				ifp2 = ifp1 << 1;
				theta = (float) (2 * isign * PI / (ifp2 / ip1));
				wtemp = (float) Math.sin(0.5 * theta);
				wpr = (float) (-2.0 * wtemp * wtemp);
				wpi = (float) Math.sin(theta);
				wr = 1.0f;
				wi = 0.0f;
				for (i3 = 1; i3 <= ifp1; i3 += ip1) {
					for (i1 = i3; i1 <= i3 + ip1 - 2; i1 += 2) {
						for (i2 = i1; i2 <= ip3; i2 += ifp2) {
							k1 = i2;
							k2 = k1 + ifp1;
							tempr = wr * data.getElement((int) k2) - wi
									* data.getElement((int) k2 + 1);
							tempi = wr * data.getElement((int) k2 + 1) + wi
									* data.getElement((int) k2);
							data.setElement((int) k2, (float) (data
									.getElement((int) k1) - tempr));
							data.setElement((int) k2 + 1, (float) (data
									.getElement((int) k1 + 1) - tempi));
							data.addToElement((int) k1, (float) tempr);
							data.addToElement((int) k1 + 1, (float) tempi);
						}
					}
					wr = (wtemp = wr) * wpr - wi * wpi + wr;
					wi = wi * wpr + wtemp * wpi + wi;
				}
				ifp1 = ifp2;
			}
			nprev *= n;
		}
	}

	// Function to calculate a three-dimensional Fourier transform of
	// data[1..nn1][1..nn2][1..nn3] (where nn1=1 for the case of a logically
	// two-
	// dimensional array). This routine returns (for isign=1) the complex fast
	// Fourier transform as two complex arrays: On output, data contains the
	// zero
	// and positive frequency values of the third frequency component, while
	// speq[1..nn1][1..2*nn2] contains the Nyquist critical frequency values of
	// the
	// third frequency component. First (and second) frequency components are
	// stored for zero, positive, and negative frequencies, in standard
	// wrap-around
	// order. See Numerical Recipes for description of how complex values are
	// arranged. For isign=-1, the inverse transform (times nn1*nn2*nn3/2 as a
	// constant multiplicative factor) is performed, with output data (viewed as
	// real array) deriving from input data (viewed as complex) and speq. For
	// inverse transforms on data not generated first by a forward transform,
	// make
	// sure the complex input data array satisfies property 12.5.2 from NR. The
	// dimensions nn1, nn2, nn3 must always be integer powers of 2.

	private void rlft3(D3Tensor data, DMatrix speq,/* long */int nn1, /* long */
			int nn2,
			/* long */int nn3, int isign) {
		double theta, wi, wpi, wpr, wr, wtemp;
		float c1, c2, h1r, h1i, h2r, h2i;
		int /* long */i1, i2, i3, j1, j2, j3, ii3;
		/* long */int[] nn = new int/* long */[4];

		if (data.getElementsCount() != nn1 * nn2 * nn3) {
			System.err
					.println("rlft3: problem with dimensions or contiguity of data array\n");
			System.exit(1);
		}
		c1 = 0.5f;
		c2 = -0.5f * isign;
		theta = 2 * isign * (PI / nn3);
		wtemp = (float) Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = (float) Math.sin(theta);
		nn[1] = nn1;
		nn[2] = nn2;
		nn[3] = nn3 >> 1;

		// Case of forward transform. Here is where most all of the compute time
		// is
		// spent. Extend data periodically into speq.

		if (isign == 1) {
			data.setOffset(0, 0, -1); // 1,1,0 is actually 0,0,-1
			fourn(data /* &data[1][1][1]-1 */, nn, 3, isign); // MUST CHANGE
																// (CHECK)!
			data.setOffset(0, 0, +1);
			for (i1 = 1; i1 <= nn1; i1++) {
				for (i2 = 1, j2 = 0; i2 <= nn2; i2++) {
					speq.setElement(i1, ++j2, data.getElement(i1, i2, 1));
					speq.setElement(i1, ++j2, data.getElement(i1, i2, 2));
				}
			}
		}
		for (i1 = 1; i1 <= nn1; i1++) {
			// Zero frequency is its own reflection; otherwise locate
			// corresponding
			// negative frequency in wrap-around order.

			j1 = (i1 != 1 ? nn1 - i1 + 2 : 1);

			// Initialize trigonometric recurrence.

			wr = 1.0;
			wi = 0.0;
			for (ii3 = 1, i3 = 1; i3 <= (nn3 >> 2) + 1; i3++, ii3 += 2) {
				for (i2 = 1; i2 <= nn2; i2++) {
					if (i3 == 1) {
						j2 = (i2 != 1 ? ((nn2 - i2) << 1) + 3 : 1);
						h1r = c1
								* (data.getElement(i1, i2, 1) + speq
										.getElement(j1, j2));
						h1i = c1
								* (data.getElement(i1, i2, 2) - speq
										.getElement(j1, j2 + 1));
						h2i = c2
								* (data.getElement(i1, i2, 1) - speq
										.getElement(j1, j2));
						h2r = -c2
								* (data.getElement(i1, i2, 2) + speq
										.getElement(j1, j2 + 1));
						data.setElement(i1, i2, 1, h1r + h2r);
						data.setElement(i1, i2, 2, h1i + h2i);
						speq.setElement(j1, j2, h1r - h2r);
						speq.setElement(j1, j2 + 1, h2i - h1i);
					} else {
						j2 = (i2 != 1 ? nn2 - i2 + 2 : 1);
						j3 = nn3 + 3 - (i3 << 1);
						h1r = c1
								* (data.getElement(i1, i2, ii3) + data
										.getElement(j1, j2, j3));
						h1i = c1
								* (data.getElement(i1, i2, ii3 + 1) - data
										.getElement(j1, j2, j3 + 1));
						h2i = c2
								* (data.getElement(i1, i2, ii3) - data
										.getElement(j1, j2, j3));
						h2r = -c2
								* (data.getElement(i1, i2, ii3 + 1) + data
										.getElement(j1, j2, j3 + 1));
						data.setElement(i1, i2, ii3,
								(float) (h1r + wr * h2r - wi * h2i));
						data.setElement(i1, i2, ii3 + 1, (float) (h1i + wr
								* h2i + wi * h2r));
						data.setElement(j1, j2, j3,
								(float) (h1r - wr * h2r + wi * h2i));
						data.setElement(j1, j2, j3 + 1, (float) (-h1i + wr
								* h2i + wi * h2r));
					}
				}

				// Do the recurrence.

				wr = (wtemp = wr) * wpr - wi * wpi + wr;
				wi = wi * wpr + wtemp * wpi + wi;
			}
		}

		// Case of reverse transform.

		if (isign == -1) {
			// TODO check about the "-1"
			data.setOffset(0, 0, -1);
			fourn(data /* &data[1][1][1]-1 */, nn, 3, isign); // MUST CHANGE
																// (CHECK)
			data.setOffset(0, 0, +1);
		}
	}

	// Function to perform Gaussian blur.
	private void gaussianblur() {
		D3Tensor blur, conv, pop;
		DMatrix speqblur, speqconv, speqpop;
		int i, j, p, q;

		blur = new D3Tensor(1, 1, 1, lx, 1, ly);
		conv = new D3Tensor(1, 1, 1, lx, 1, ly);
		pop = new D3Tensor(1, 1, 1, lx, 1, ly);
		speqblur = new DMatrix(1, 1, 1, 2 * lx);
		speqconv = new DMatrix(1, 1, 1, 2 * lx);
		speqpop = new DMatrix(1, 1, 1, 2 * lx);

		// Fill population and convolution matrix.

		for (i = 1; i <= lx; i++) {
			for (j = 1; j <= ly; j++) {
				if (i > lx / 2) {
					p = i - 1 - lx;
				} else {
					p = i - 1;
				}
				if (j > ly / 2) {
					q = j - 1 - ly;
				} else {
					q = j - 1;
				}
				pop.setElement(1, i, j, rho_0[i - 1].array[j - 1]);
				conv
						.setElement(
								1,
								i,
								j,
								(float) (0.5
										* (erf((p + 0.5)
												/ (Math.sqrt(2.0) * (blurWidth * Math
														.pow(blurWidthFactor,
																nblurs)))) - erf((p - 0.5)
												/ (Math.sqrt(2.0) * (blurWidth * Math
														.pow(blurWidthFactor,
																nblurs)))))
										* (erf((q + 0.5)
												/ (Math.sqrt(2.0) * (blurWidth * Math
														.pow(blurWidthFactor,
																nblurs)))) - erf((q - 0.5)
												/ (Math.sqrt(2.0) * (blurWidth * Math
														.pow(blurWidthFactor,
																nblurs))))) / (lx * ly)));
			}
		}

		// Fourier transform.

		rlft3(pop, speqpop, 1, lx, ly, 1);
		rlft3(conv, speqconv, 1, lx, ly, 1);

		// Multiply pointwise.

		for (i = 1; i <= lx; i++) {
			for (j = 1; j <= ly / 2; j++) {
				blur.setElement(1, i, 2 * j - 1, pop
						.getElement(1, i, 2 * j - 1)
						* conv.getElement(1, i, 2 * j - 1)
						- pop.getElement(1, i, 2 * j)
						* conv.getElement(1, i, 2 * j));
				blur.setElement(1, i, 2 * j, pop.getElement(1, i, 2 * j)
						* conv.getElement(1, i, 2 * j - 1)
						+ pop.getElement(1, i, 2 * j - 1)
						* conv.getElement(1, i, 2 * j));
			}
		}
		for (i = 1; i <= lx; i++) {
			speqblur.setElement(1, 2 * i - 1, speqpop.getElement(1, 2 * i - 1)
					* speqconv.getElement(1, 2 * i - 1)
					- speqpop.getElement(1, 2 * i)
					* speqconv.getElement(1, 2 * i));
			speqblur.setElement(1, 2 * i, speqpop.getElement(1, 2 * i)
					* speqconv.getElement(1, 2 * i - 1)
					+ speqpop.getElement(1, 2 * i - 1)
					* speqconv.getElement(1, 2 * i));
		}

		// Backtransform.

		rlft3(blur, speqblur, 1, lx, ly, -1);

		// Write to rho_0.

		for (i = 1; i <= lx; i++) {
			for (j = 1; j <= ly; j++) {
				rho_0[i - 1].array[j - 1] = blur.getElement(1, i, j);
			}
		}
	}

	// Function to initialize rho_0. The original density is blurred with width
	// SIGMA*pow(SIGMAFAC,nblurs).
	private void initcond() {
		float maxpop;
		int i, j;

		// Reconstruct population density.

		coscosft(rho_0, -1, -1);

		// There must not be negative densities.

		for (i = 0; i < lx; i++) {
			for (j = 0; j < ly; j++) {
				if (rho_0[i].array[j] < -1e10) {
					System.err
							.println("ERROR: Negative density in DENSITYFILE.");
					System.exit(1);
				}
			}
		}

		// Perform Gaussian blur.

		logger.finest("Gaussian blur ...\n");
		gaussianblur();

		// Find the mimimum density. If it is very small suggest an increase in
		// SIGMA.

		minpop = rho_0[0].array[0];
		maxpop = rho_0[0].array[0];
		for (i = 0; i < lx; i++) {
			for (j = 0; j < ly; j++) {
				if (rho_0[i].array[j] < minpop) {
					minpop = rho_0[i].array[j];
				}
			}
		}
		for (i = 0; i < lx; i++) {
			for (j = 0; j < ly; j++) {
				if (rho_0[i].array[j] > maxpop) {
					maxpop = rho_0[i].array[j];
				}
			}
		}
		if (0 < minpop && minpop < 1e-8 * maxpop) {
			System.err.println("Minimimum population very small (" + minpop
					+ "). Integrator");
			System.err
					.println("will probably converge very slowly. You can speed up the");
			System.err.println("process by increasing SIGMA to a value > "
					+ blurWidth * Math.pow(blurWidthFactor, nblurs));
		}

		// Replace rho_0 by cosine Fourier transform in both variables.

		coscosft(rho_0, 1, 1);
	}

	// Function to calculate the velocity field

	// NO OBJECT REFERENCE
	private void calcv(float t) {
		int j, k;

		// Fill rho with Fourier coefficients.

		for (j = 0; j <= lx; j++) {
			for (k = 0; k <= ly; k++) {
				rho[j].array[k] = (float) Math.exp(-((PI * j / lx)
						* (PI * j / lx) + (PI * k / ly) * (PI * k / ly))
						* t)
						* rho_0[j].array[k];
			}
		}

		// Calculate the Fourier coefficients for the partial derivative of rho.
		// Store temporary results in arrays gridvx, gridvy.

		for (j = 0; j <= lx; j++) {
			for (k = 0; k <= ly; k++) {
				gridvx[j].array[k] = (float) -(PI * j / lx) * rho[j].array[k];
				gridvy[j].array[k] = (float) -(PI * k / ly) * rho[j].array[k];
			}
		}

		// Replace rho by cosine Fourier backtransform in both variables.

		coscosft(rho, -1, -1);

		// Replace vx by sine Fourier backtransform in the first and cosine
		// Fourier
		// backtransform in the second variable.

		sincosft(gridvx, -1, -1);

		// Replace vy by cosine Fourier backtransform in the first and sine
		// Fourier
		// backtransform in the second variable.

		cossinft(gridvy, -1, -1);

		// Calculate the velocity field.

		for (j = 0; j <= lx; j++) {
			for (k = 0; k <= ly; k++) {
				gridvx[j].array[k] = -gridvx[j].array[k] / rho[j].array[k];
				gridvy[j].array[k] = -gridvy[j].array[k] / rho[j].array[k];
			}
		}
	}

	// Function to bilinearly interpolate a two-dimensional array. For higher
	// accuracy one could consider higher order interpolation schemes, but that
	// will make the program slower.

	// NO OBJECT REFERENCE
	private float intpol(ArrayFloat[] arr, float x, float y) {
		int gaussx, gaussy;
		float deltax, deltay;

		// Decompose x and y into an integer part and a decimal.

		gaussx = (int) x;
		gaussy = (int) y;
		deltax = x - gaussx;
		deltay = y - gaussy;

		// Interpolate.

		if (gaussx == lx && gaussy == ly) {
			return arr[gaussx].array[gaussy];
		}
		if (gaussx == lx) {
			return (1 - deltay) * arr[gaussx].array[gaussy] + deltay
					* arr[gaussx].array[gaussy + 1];
		}
		if (gaussy == ly) {
			return (1 - deltax) * arr[gaussx].array[gaussy] + deltax
					* arr[gaussx + 1].array[gaussy];
		}
		return (1 - deltax) * (1 - deltay) * arr[gaussx].array[gaussy]
				+ (1 - deltax) * deltay * arr[gaussx].array[gaussy + 1]
				+ deltax * (1 - deltay) * arr[gaussx + 1].array[gaussy]
				+ deltax * deltay * arr[gaussx + 1].array[gaussy + 1];
	}

	// Function to find the root of the system of equations
	// xappr-0.5*h*v_x(t+h,xappr,yappr)-x[j][k]-0.5*h*vx[j][k]=0,
	// yappr-0.5*h*v_y(t+h,xappr,yappr)-y[j][k]-0.5*h*vy[j][k]=0
	// with Newton-Raphson. Returns TRUE after sufficient convergence.

	// MUST CHANGE - object reference - xappr and yappr
	private boolean newt2(float h, float[] retxyAppr, float xguess,/*
																	 * Float
																	 * yAppr,
																	 */
			float yguess, int j, int k) {
		float deltax, deltay, dfxdx, dfxdy, dfydx, dfydy, fx, fy;
		int gaussx, gaussxplus, gaussy, gaussyplus, i;

		float xappr, yappr;
		// Initial guess.

		xappr = xguess;
		yappr = yguess;

		for (i = 1; i <= IMAX; i++) {
			// fx, fy are the left-hand sides of the two equations. Find
			// v_x(t+h,xappr,yappr), v_y(t+h,xappr,yappr) by interpolation.

			fx = (float) (xappr - 0.5 * h * intpol(gridvx, xappr, yappr)
					- x[j].array[k] - 0.5 * h * vx[j].array[k]);
			fy = (float) (yappr - 0.5 * h * intpol(gridvy, xappr, yappr)
					- y[j].array[k] - 0.5 * h * vy[j].array[k]);

			// Linearly approximate the partial derivatives of fx, fy with a
			// finite
			// difference method. More elaborate techniques are possible, but
			// this
			// quick and dirty method appears to work reasonably for our
			// purpose.

			gaussx = (int) (xappr);
			gaussy = (int) (yappr);
			if (gaussx == lx) {
				gaussxplus = 0;
			} else {
				gaussxplus = gaussx + 1;
			}
			if (gaussy == ly) {
				gaussyplus = 0;
			} else {
				gaussyplus = gaussy + 1;
			}
			deltax = x[j].array[k] - gaussx;
			deltay = y[j].array[k] - gaussy;
			dfxdx = (float) (1 - 0.5
					* h
					* ((1 - deltay)
							* (gridvx[gaussxplus].array[gaussy] - gridvx[gaussx].array[gaussy]) + deltay
							* (gridvx[gaussxplus].array[gaussyplus] - gridvx[gaussx].array[gaussyplus])));
			dfxdy = (float) (-0.5 * h * ((1 - deltax)
					* (gridvx[gaussx].array[gaussyplus] - gridvx[gaussx].array[gaussy]) + deltax
					* (gridvx[gaussxplus].array[gaussyplus] - gridvx[gaussxplus].array[gaussy])));
			dfydx = (float) (-0.5 * h * ((1 - deltay)
					* (gridvy[gaussxplus].array[gaussy] - gridvy[gaussx].array[gaussy]) + deltay
					* (gridvy[gaussxplus].array[gaussyplus] - gridvy[gaussx].array[gaussyplus])));
			dfydy = (float) (1 - 0.5
					* h
					* ((1 - deltax)
							* (gridvy[gaussx].array[gaussyplus] - gridvy[gaussx].array[gaussy]) + deltax
							* (gridvy[gaussxplus].array[gaussyplus] - gridvy[gaussxplus].array[gaussy])));

			// If the current approximation is (xappr,yappr) for the zero of
			// (fx(x,y),fy(x,y)) and J is the Jacobian, then we can approximate
			// (in
			// vector notation) for |delta|<<1:
			// f((xappr,yappr)+delta) = f(xappr,yappr)+J*delta.
			// Setting f((xappr,yappr)+delta)=0 we obtain a set of linear
			// equations
			// for the correction delta which moves f closer to zero, namely
			// J*delta = -f.
			// The improved approximation is then x = xappr+delta.
			// The process will be iterated until convergence is reached.

			if ((fx * fx + fy * fy) < TOLF) {
				retxyAppr[0] = xappr;
				retxyAppr[1] = yappr;
				return true;
			}
			deltax = (fy * dfxdy - fx * dfydy)
					/ (dfxdx * dfydy - dfxdy * dfydx);
			deltay = (fx * dfydx - fy * dfxdx)
					/ (dfxdx * dfydy - dfxdy * dfydx);
			if ((deltax * deltax + deltay * deltay) < TOLX) {
				retxyAppr[0] = xappr;
				retxyAppr[1] = yappr;
				return true;
			}
			xappr += deltax;
			yappr += deltay;
			// printf("deltax %f, deltay %f\n",deltax,deltay);
		}
		System.err.println("newt2 failed, increasing sigma to " + blurWidth
				* Math.pow(blurWidthFactor, nblurs));
		retxyAppr[0] = xappr;
		retxyAppr[1] = yappr;
		return false;
	}

	// Function to integrate the nonlinear Volterra equation. Returns TRUE after
	// the displacement field converged, after MAXINTSTEPS integration steps, or
	// if the time exceeds TIMELIMIT.

	// NO OBJECT REFERENCE
	private boolean nonlinvoltra() {
		boolean stepsize_ok;
		BufferedWriter displfile = null;
		if (DISPLFILE != null) {
			try {
				displfile = new BufferedWriter(new FileWriter(DISPLFILE));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		float h, maxchange = 0f, t, vxplus, vyplus, xguess, yguess;
		int i, j, k;

		do {
			initcond();
			nblurs++;
			if (minpop < 0.0) {
				logger
						.finest("Minimum population negative, will increase sigma to "
								+ blurWidth * Math.pow(blurWidthFactor, nblurs));
			}
		} while (minpop < 0.0);
		h = (float) HINITIAL;
		t = 0; // Start at time t=0.

		// (x[j][k],y[j][k]) is the position for the element that was at
		// position
		// (j,k) at time t=0.

		for (j = 0; j <= lx; j++) {
			for (k = 0; k <= ly; k++) {
				x[j].array[k] = j;
				y[j].array[k] = k;
			}
		}
		calcv(0.0f);

		// (gridvx[j][k],gridvy[j][k]) is the velocity at position (j,k).
		// (vx[j][k],vy[j][k]) is the velocity at position (x[j][k],y[j][k]).
		// At t=0 they are of course identical.

		for (j = 0; j <= lx; j++) {
			for (k = 0; k <= ly; k++) {
				vx[j].array[k] = gridvx[j].array[k];
				vy[j].array[k] = gridvy[j].array[k];
			}
		}
		i = 1; // i counts the integration steps.

		// Here is the integrator.

		do {
			stepsize_ok = true;
			calcv(t + h);
			for (j = 0; j <= lx; j++) {
				for (k = 0; k <= ly; k++) {
					// First take a naive integration step. The velocity at time
					// t+h for
					// the element [j][k] is approximately
					// v(t+h,x[j][k]+h*vx[j][k],y[j][k]+h*vy[j][k]).
					// The components, call them vxplus and vyplus, are
					// interpolated from
					// gridvx and gridvy.

					vxplus = intpol(gridvx, x[j].array[k] + h * vx[j].array[k],
							y[j].array[k] + h * vy[j].array[k]);
					vyplus = intpol(gridvy, x[j].array[k] + h * vx[j].array[k],
							y[j].array[k] + h * vy[j].array[k]);

					// Based on (vx[j][k],vy[j][k]) and (vxplus,vyplus) we
					// expect the
					// new position at time t+h to be:

					xguess = (float) (x[j].array[k] + 0.5 * h
							* (vx[j].array[k] + vxplus));
					yguess = (float) (y[j].array[k] + 0.5 * h
							* (vy[j].array[k] + vyplus));

					// Then we make a better approximation by solving the two
					// nonlinear
					// equations:
					// xappr[j][k]-0.5*h*v_x(t+h,xappr[j][k],yappr[j][k])-
					// x[j][k]-0.5*h*vx[j][k]=0,
					// yappr[j][k]-0.5*h*v_y(t+h,xappr[j][k],yappr[j][k])-
					// y[j][k]-0.5*h*vy[j][k]=0
					// with Newton-Raphson and (xguess,yguess) as initial guess.
					// If newt2 fails to converge, exit nonlinvoltra.

					float[] ret = new float[] { xappr[j].array[k],
							yappr[j].array[k] };
					boolean result = newt2(h, ret, xguess, yguess, j, k);
					xappr[j].array[k] = ret[0];
					yappr[j].array[k] = ret[1];
					if (!result) {
						return false;
					}

					// If the integration step was too large reduce the step
					// size.

					if ((xguess - xappr[j].array[k])
							* (xguess - xappr[j].array[k])
							+ (yguess - yappr[j].array[k])
							* (yguess - yappr[j].array[k]) > TOLINT) {
						if (h < MINH) {
							logger.finest("Time step below " + h
									+ ", increasing SIGMA to " + blurWidth
									* Math.pow(blurWidthFactor, nblurs));
							nblurs++;
							return false;
						}
						h /= 10;
						stepsize_ok = false;
						break;
					}
				}
			}
			if (!stepsize_ok) {
				continue;
			}
			t += h;
			maxchange = 0.0f; // Monitor the maximum change in positions.
			for (j = 0; j <= lx; j++) {
				for (k = 0; k <= ly; k++) {
					if ((x[j].array[k] - xappr[j].array[k])
							* (x[j].array[k] - xappr[j].array[k])
							+ (y[j].array[k] - yappr[j].array[k])
							* (y[j].array[k] - yappr[j].array[k]) > maxchange) {
						maxchange = (x[j].array[k] - xappr[j].array[k])
								* (x[j].array[k] - xappr[j].array[k])
								+ (y[j].array[k] - yappr[j].array[k])
								* (y[j].array[k] - yappr[j].array[k]);
					}
					x[j].array[k] = xappr[j].array[k];
					y[j].array[k] = yappr[j].array[k];
					vx[j].array[k] = intpol(gridvx, xappr[j].array[k],
							yappr[j].array[k]);
					vy[j].array[k] = intpol(gridvy, xappr[j].array[k],
							yappr[j].array[k]);
				}
			}
			h *= 1.2; // Make the next integration step larger.
			if (logger.isLoggable(Level.FINEST)) {
				if (i % 10 == 0) {
					logger.finest("time " + t);
				}
			}
			i++;
		} while (i < MAXINTSTEPS && t < TIMELIMIT && maxchange > CONVERGENCE);
		if (maxchange > CONVERGENCE) {
			System.err.println("WARNING: Insufficient convergence within "
					+ MAXINTSTEPS + " steps, time " + TIMELIMIT);
		}

		if (DISPLFILE != null) {

			// Write displacement field to file.
			try {
				displfile.write("time " + t + "\nminx " + minx + "\nmaxx "
						+ maxx + "\nminy " + miny + "\nmaxy " + maxy + "\n");
				displfile.write("sigma " + blurWidth
						* Math.pow(blurWidthFactor, nblurs - 1) + "\n");
				displfile.write("background 0\nlx\nly\n\n"); // ,0,lx,ly); -->
																// warning, lx
																// and ly are
																// not being
																// display in
																// the originial
																// c program
				for (j = 0; j <= lx; j++) {
					for (k = 0; k <= ly; k++) {
						displfile
								.write("j " + j + ", k " + k + ", x "
										+ x[j].array[k] + ", y "
										+ y[j].array[k] + "\n");
					}
				}

				displfile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return true;
	}

	// Function to transform points according to displacement field.

	// NO OBJECT REFERENCE
	Point transf(final Point p) {
		float deltax, deltay, den, t, u;
		int gaussx, gaussy;
		Point a = new Point(), b = new Point(), c = new Point(), d = new Point(), ptr = new Point();

		p.x = (p.x - minx) * lx / (maxx - minx);
		p.y = (p.y - miny) * ly / (maxy - miny);
		gaussx = (int) p.x;
		gaussy = (int) p.y;
		if (gaussx < 0 || gaussx > lx || gaussy < 0 || gaussy > ly) {
			System.err
					.println("ERROR: Coordinate limits exceeded in transf.\n");
			System.exit(1);
		}
		deltax = p.x - gaussx;
		deltay = p.y - gaussy;

		// The transformed point is the intersection of the lines:
		// (I) connecting
		// (1-deltax)*(x,y[gaussx][gaussy])+deltax*(x,y[gaussx+1][gaussy])
		// and
		// (1-deltax)*(x,y[gaussx][gaussy+1])+deltax*(x,y[gaussx+1][gaussy+1])
		// (II) connecting
		// (1-deltay)*(x,y[gaussx][gaussy])+deltay*(x,y[gaussx][gaussy+1])
		// and
		// (1-deltay)*(x,y[gaussx+1][gaussy])+deltay*(x,y[gaussx+1][gaussy+1]).
		// Call these four points a, b, c and d.

		a.x = (1 - deltax) * x[gaussx].array[gaussy] + deltax
				* x[gaussx + 1].array[gaussy];
		a.y = (1 - deltax) * y[gaussx].array[gaussy] + deltax
				* y[gaussx + 1].array[gaussy];
		b.x = (1 - deltax) * x[gaussx].array[gaussy + 1] + deltax
				* x[gaussx + 1].array[gaussy + 1];
		b.y = (1 - deltax) * y[gaussx].array[gaussy + 1] + deltax
				* y[gaussx + 1].array[gaussy + 1];
		c.x = (1 - deltay) * x[gaussx].array[gaussy] + deltay
				* x[gaussx].array[gaussy + 1];
		c.y = (1 - deltay) * y[gaussx].array[gaussy] + deltay
				* y[gaussx].array[gaussy + 1];
		d.x = (1 - deltay) * x[gaussx + 1].array[gaussy] + deltay
				* x[gaussx + 1].array[gaussy + 1];
		d.y = (1 - deltay) * y[gaussx + 1].array[gaussy] + deltay
				* y[gaussx + 1].array[gaussy + 1];

		// Solve the vector equation a+t(b-a) = c+u(d-c) for the scalars t, u.

		if (Math.abs(den = (b.x - a.x) * (c.y - d.y) + (a.y - b.y)
				* (c.x - d.x)) < 1e-12) {
			System.err
					.println("ERROR: Transformed area element has parallel edges.\n");
			// System.exit(1);
		}
		t = ((c.x - a.x) * (c.y - d.y) + (a.y - c.y) * (c.x - d.x)) / den;
		u = ((b.x - a.x) * (c.y - a.y) + (a.y - b.y) * (c.x - a.x)) / den;

		if (t < -1e-3 || t > 1 + 1e-3 || u < -1e-3 || u > 1 + 1e-3) {
			System.err
					.println("WARNING: Transformed area element non-convex.\n");
		}
		ptr.x = (1 - (a.x + t * (b.x - a.x)) / lx) * minx
				+ ((a.x + t * (b.x - a.x)) / lx) * maxx;
		ptr.y = (1 - (a.y + t * (b.y - a.y)) / ly) * miny
				+ ((a.y + t * (b.y - a.y)) / ly) * maxy;
		return ptr;
	}

	// Function to read spatial features from user-specified file and map to
	// cartogram.
	private void cartogram() throws IOException {
		String id, line;
		BufferedReader infile;
		BufferedWriter outfile;
		float xcoord, ycoord;
		Point p = new Point();

		infile = FileTools.openFileRead(polygonFileName);
		outfile = FileTools.openFileWrite(genFileName);
		while ((line = FileTools.readLine(infile)) != null) {
			StringTokenizer s = new StringTokenizer(line);
			String a = s.nextToken();
			String b = null, c = null;
			if (s.hasMoreTokens()) {
				b = s.nextToken();
			}
			if (s.hasMoreTokens()) {
				c = s.nextToken();
			}

			boolean flag = false;

			// if (sscanf(line,"%s %f %f",&id,&xcoord,&ycoord)==3)
			if (b != null && c != null) {
				try {
					id = a;
					xcoord = Float.parseFloat(b);
					ycoord = Float.parseFloat(c);

					p.x = xcoord;
					p.y = ycoord;
					p = transf(p);
					outfile.write(id + " " + p.x + " " + p.y + "\n");
					flag = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			// else if (sscanf(line,"%f %f",&xcoord,&ycoord)==2)
			if (!flag && b != null && a != null) {
				try {
					xcoord = Float.parseFloat(a);
					ycoord = Float.parseFloat(b);

					p.x = xcoord;
					p.y = ycoord;
					p = transf(p);
					outfile.write(p.x + " " + p.y + "\n");
					flag = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			} else {
				id = a;
				outfile.write(id + "\n");
			}
		}
		infile.close();
		outfile.close();
	}

	// Function to prepare a map in postscript standard letter format.

	// NO OBJECT REFERENCE
	private void pspicture(BufferedReader infile, BufferedWriter outfile)
			throws IOException {
		String line;
		float addx, addy, b, conv, g, r, xcoord, ycoord;
		int id;

		if (11 * lx > 8.5 * ly) {
			conv = (float) 8.5 * 72 / lx;
			addx = 0;
			addy = (float) (11 * 36 - 8.5 * 36 * ly / lx);
		} else {
			conv = (float) 11 * 72 / ly;
			addx = (float) (8.5 * 36 - 11 * 36 * lx / ly);
			addy = 0;
		}
		line = FileTools.readLine(infile);
		StringTokenizer s = new StringTokenizer(line);
		id = Integer.parseInt(s.nextToken());

		line = FileTools.readLine(infile);
		s = new StringTokenizer(line);
		xcoord = Float.parseFloat(s.nextToken());
		ycoord = Float.parseFloat(s.nextToken());

		outfile.write("newpath\n" + ((xcoord - minx) * conv / xstepsize + addx)
				+ " " + ((ycoord - miny) * conv / ystepsize + addy)
				+ " moveto\n");
		while ((line = FileTools.readLine(infile)) != null) {
			if (line.charAt(0) != 'E') {
				s = new StringTokenizer(line);
				xcoord = Float.parseFloat(s.nextToken());
				ycoord = Float.parseFloat(s.nextToken());

				outfile.write(((xcoord - minx) * conv / xstepsize + addx) + " "
						+ ((ycoord - miny) * conv / ystepsize + addy)
						+ " lineto\n");
			} else {
				// Determine colors for map (without better knowledge I will do
				// it
				// arbitrarily).

				if (id % 3 == 0) {
					r = (float) id / maxid;
					g = 1 - (float) id / maxid;
					b = Math.abs(1 - 2 * (float) id / maxid);
				} else if (id % 3 == 1) {
					b = (float) id / maxid;
					r = 1 - (float) id / maxid;
					g = Math.abs(1 - 2 * (float) id / maxid);
				} else {
					g = (float) id / maxid;
					b = 1 - (float) id / maxid;
					r = Math.abs(1 - 2 * (float) id / maxid);
				}
				outfile
						.write("closepath\n"
								+ r
								+ " "
								+ g
								+ " "
								+ b
								+ " setrgbcolor\ngsave\nfill\ngrestore\n0 setgray stroke\n");
				line = FileTools.readLine(infile);
				if (line.charAt(0) == 'E') {
					break;
				}
				s = new StringTokenizer(line);
				id = Integer.parseInt(s.nextToken());
				line = FileTools.readLine(infile);
				s = new StringTokenizer(line);
				xcoord = Float.parseFloat(s.nextToken());
				ycoord = Float.parseFloat(s.nextToken());
				outfile.write("newpath\n"
						+ ((xcoord - minx) * conv / xstepsize + addx) + " "
						+ ((ycoord - miny) * conv / ystepsize + addy)
						+ " moveto\n");
			}
		}
		outfile.write("showpage\n");
	}

	// the constructor (taken from the main() function
	public TransformsMain(boolean transformNow) {
		if (transformNow) {

			try {
				makeCartogram();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
	}

	public TransformsMain() {

		// this.makeCartogram();
	}

	public TransformsMain(String genFile, String datFile, String polygonFile)
			throws IOException {
		polygonFileName = polygonFile;
		genFileName = genFile;
		dataFileName = datFile;
		makeCartogram();
	}

	public void makeCartogram() throws IOException {
		boolean n;
		BufferedReader infile;
		BufferedWriter outfile;
		int i;

		// Read the polygon coordinates.

		readcorn();

		// Allocate memory for arrays.

		gridvx = new ArrayFloat[lx + 1];
		gridvx = new ArrayFloat[lx + 1];
		gridvy = new ArrayFloat[lx + 1];
		rho = new ArrayFloat[lx + 1];
		rho_0 = new ArrayFloat[lx + 1];
		vx = new ArrayFloat[lx + 1];
		vy = new ArrayFloat[lx + 1];
		x = new ArrayFloat[lx + 1];
		xappr = new ArrayFloat[lx + 1];
		y = new ArrayFloat[lx + 1];
		yappr = new ArrayFloat[lx + 1];
		arrayLength = x.length;

		for (i = 0; i <= lx; i++) {
			gridvx[i] = new ArrayFloat(ly + 1);
			gridvy[i] = new ArrayFloat(ly + 1);
			rho[i] = new ArrayFloat(ly + 1);
			rho_0[i] = new ArrayFloat(ly + 1);
			vx[i] = new ArrayFloat(ly + 1);
			vy[i] = new ArrayFloat(ly + 1);
			x[i] = new ArrayFloat(ly + 1);
			xappr[i] = new ArrayFloat(ly + 1);
			y[i] = new ArrayFloat(ly + 1);
			yappr[i] = new ArrayFloat(ly + 1);
		}

		// Digitize the density.

		digdens();

		// Solve the diffusion equation.

		do {
			n = nonlinvoltra();
		} while (!n);

		// Make map.
		// note: next lines are for postcript only

		if (!TransformsMain.MAP2PS.equals("")) {

			infile = FileTools.openFileRead(polygonFileName);
			outfile = FileTools.openFileWrite(MAP2PS);
			pspicture(infile, outfile);
			infile.close();
			outfile.close();

		}
		// Print cartogram generate file.

		cartogram();

		// Make postscript of cartogram
		if (!TransformsMain.CART2PS.equals("")) {
			infile = FileTools.openFileRead(genFileName);
			outfile = FileTools.openFileWrite(CART2PS);
			pspicture(infile, outfile);
			infile.close();
			outfile.close();
		}

	}

	public static void main(String[] args) {
		try {
			TransformsMain t = new TransformsMain(true);
			logger.finest("All done! " + t);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("I/O error!\n");
		}
	}

	public void setBlurWidth(double blurWidth) {
		this.blurWidth = blurWidth;
	}

	public void setBlurWidthFactor(double blurWidthFactor) {
		this.blurWidthFactor = blurWidthFactor;
	}

	public void setGenFileName(String genFileName) {
		this.genFileName = genFileName;
	}

	public void setPolygonFileName(String polygonFileName) {
		this.polygonFileName = polygonFileName;
	}

	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}

	public void setMaxNSquareLog(int maxNSquareLog) {
		this.maxNSquareLog = maxNSquareLog;
	}

	public double getBlurWidth() {
		return blurWidth;
	}

	public double getBlurWidthFactor() {
		return blurWidthFactor;
	}

	public String getDataFileName() {
		return dataFileName;
	}

	public String getGenFileName() {
		return genFileName;
	}

	public String getPolygonFileName() {
		return polygonFileName;
	}

	public int getMaxNSquareLog() {
		return maxNSquareLog;
	}

	// we give these different names to provide our own serialization method
	public ArrayFloat[] retreiveX() {
		return x;
	}

	public ArrayFloat[] retreiveY() {
		return y;
	}

	public void putX(ArrayFloat[] x) {
		this.x = x;
	}

	public void putY(ArrayFloat[] y) {
		this.y = y;
	}

	public int getArrayLength() {
		return arrayLength;
	}

	public int getLx() {
		return lx;
	}

	public int getLy() {
		return ly;
	}

	public float getMaxx() {
		return maxx;
	}

	public float getMaxy() {
		return maxy;
	}

	public float getMinx() {
		return minx;
	}

	public float getMiny() {
		return miny;
	}

	public void setArrayLength(int arrayLength) {
		this.arrayLength = arrayLength;// note, this is just for bookkeeping in
										// the serialized version of this class
	}

	public void setLx(int lx) {
		this.lx = lx;
	}

	public void setLy(int ly) {
		this.ly = ly;
	}

	public void setMaxx(float maxx) {
		this.maxx = maxx;
	}

	public void setMaxy(float maxy) {
		this.maxy = maxy;
	}

	public void setMinx(float minx) {
		this.minx = minx;
	}

	public void setMiny(float miny) {
		this.miny = miny;
	}
}

/*
 * finished 500 of 512
 * 
 * Gaussian blur ...
 * 
 * java.lang.ArrayIndexOutOfBoundsException: 2 at
 * fourier.D3Tensor.getElement(D3Tensor.java:57) at
 * fourier.D3Tensor.swapElements(D3Tensor.java:114) at
 * fourier.TransformsMain.fourn(TransformsMain.java:856) at
 * fourier.TransformsMain.rlft3(TransformsMain.java:952) at
 * fourier.TransformsMain.gaussianblur(TransformsMain.java:1050) at
 * fourier.TransformsMain.initcond(TransformsMain.java:1106) at
 * fourier.TransformsMain.nonlinvoltra(TransformsMain.java:1308) at
 * fourier.TransformsMain.<init>(TransformsMain.java:1704) at
 * fourier.TransformsMain.main(TransformsMain.java:1730) Exception in thread
 * "main"
 */
