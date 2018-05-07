package com.alphawarthog.math;

import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;

public class MathUtils {

	private static Set<Long> primeNumbers = new TreeSet<Long>();
	
	static {
		primeNumbers.add(2l);
	}
	
	public static boolean isPrime(long n) {
		if (n <= 1) {
			return false;
		} else if (primeNumbers.contains(n)) {
			return true;
		} else {
			long lowerBound = 2l;
			for (long divisor : primeNumbers) {
				lowerBound = divisor;
				if (n % divisor == 0) {
					return false;
				}
			}
			
			lowerBound++;
			double d = 0.0 + n;
			double root = Math.sqrt(d);
			long upperBound = BigDecimal.valueOf(root).longValue();
			for (long divisor = lowerBound; divisor <= upperBound; divisor++) {
				if (!primeNumbers.contains(divisor) && n % divisor == 0) {
					return false;
				}
			}
		}
		
		primeNumbers.add(n);
		return true;
	}
}
