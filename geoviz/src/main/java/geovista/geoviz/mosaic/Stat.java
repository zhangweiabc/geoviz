package geovista.geoviz.mosaic;

public class Stat {

	public static double pnorm(double q) {

		double up = 0.9999999;
		double lp = 0.0000001;
		while (Math.abs(up - lp) > 0.0001) {
			if (qnorm((up + lp) / 2) <= q) {
				lp = (up + lp) / 2;
			} else {
				up = (up + lp) / 2;
			}
		}
		return up;
	}

	public static double qnorm(double p) {

		double a0 = 2.515517;
		double a1 = 0.802853;
		double a2 = 0.010328;

		double b1 = 1.432788;
		double b2 = 0.189269;
		double b3 = 0.001308;

		double t = Math.pow(-2 * Math.log(1 - p), 0.5);

		return t - (a0 + a1 * t + a2 * t * t)
				/ (1 + b1 * t + b2 * t * t + b3 * t * t * t);
	}

	public static double dchisq(double q, int df) {

		if (q > 0) {
			return 1 / (Math.pow(2, (double) df / 2) * Gamma((double) df / 2))
					* Math.pow(q, (double) df / 2 - 1)
					* Math.pow(Math.E, -q / 2);
		}
		return 0;
	}

	public static double pchisq(double q, int df) {

		double up = 0.9999999;
		double lp = 0.0000001;
		while (Math.abs(up - lp) > 0.0001) {
			if (qchisq((up + lp) / 2, df) <= q) {
				lp = (up + lp) / 2;
			} else {
				up = (up + lp) / 2;
			}
		}
		return up;
	}

	public static double qchisq(double p, int df) {

		return df
				* Math.pow(1 - 2 / (9 * (double) df) + qnorm(p)
						* Math.pow(2 / (9 * (double) df), 0.5), 3);
	}

	public static double Gamma(double p) {

		if (p == 1.0) {
			return 1;
		} else if (p == 0.5) {
			return Math.pow(Math.PI, 0.5);
		} else {
			return (p - 1) * Gamma(p - 1);
		}
	}

	public static double round(double x, int n) {
		return Math.round(x * Math.pow(10, n)) / Math.pow(10, n);
	}

	public static String roundToString(double x, int n) {
		String tmp;
		tmp = "" + Stat.round(x, n);
		if (tmp.endsWith(".0")) {
			return tmp.substring(0, tmp.length() - 2);
		}
		return tmp;
	}

	public static String roundToSpaceString(double x, int n) {
		String tmp;
		tmp = Stat.roundToString(x, n);
		int head = tmp.indexOf('.');
		if (head == -1) {
			head = tmp.length();
		}
		return ("          " + tmp).substring(head, tmp.length() + 10);
	}
}
