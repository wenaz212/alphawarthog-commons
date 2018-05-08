package com.alphawarthog.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MathUtils {

	private static final Set<Long> primeNumbers = new TreeSet<Long>();
	private static final Map<Integer, Long> zeroBasedFibonacci = new HashMap<Integer, Long>();
	private static final Map<Integer, Long> oneBasedFibonacci = new HashMap<Integer, Long>();
	
	static {
		Set<Long> set = new TreeSet<Long>();
		set.add(2l);
		zeroBasedFibonacci.put(1, 0l);
		zeroBasedFibonacci.put(2, 1l);
		oneBasedFibonacci.put(1, 1l);
		oneBasedFibonacci.put(2, 1l);
	}
	
	public static long fibonacci(int n) {
		return fibonacci(n, false); // classic fibonacci
	}
	
	public static long fibonacci(int n, boolean startsAtZero) {
		Map<Integer, Long> map = startsAtZero ? zeroBasedFibonacci : oneBasedFibonacci;
		
		if (n < 1) {
			throw new IllegalArgumentException("N must be greater than zero");
		} if (map.containsKey(n)) {
			return map.get(n);
		} else {
			long result = fibonacci(n - 1, startsAtZero) + fibonacci(n - 2, startsAtZero);
			map.put(n, result);
			return result;
		}
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
