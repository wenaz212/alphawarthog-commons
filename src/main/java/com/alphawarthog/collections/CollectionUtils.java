package com.alphawarthog.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CollectionUtils {
	
	/*
	 * AB, AC, BA, BC, CA, CB
	 * ABC, ABD, ACB, ACD, ADB, ADC, BAC, BAD, BCA, BCD, BDA, BDC, 
	 * CAB, CAD, CBA, CBD, CDA, CDB, DAB, DAC, DBA, DBC, DCA, DCB
  */
	public static <E> List<List<E>> permutation(List<E> elements, int n) {
		List<List<E>> result = new ArrayList<List<E>>();
		
		if (n == 1) {
			for (E element : elements) {
				List<E> list = new ArrayList<E>();
				list.add(element);
				result.add(list);
			}
		} else {
			for (E element : elements) {
				List<E> clone = new ArrayList<E>(elements);
				clone.remove(element);
				List<List<E>> recursiveResult = permutation(clone, n - 1);
				for (List<E> recursiveElement : recursiveResult) {
					recursiveElement.add(0, element);
					result.add(recursiveElement);
				}
			}
		}
		
		return result;
	}
	
	public static <E> List<List<E>> permutation(List<E> elements) {
		return permutation(elements, elements.size());
	}
	
	public static <E> List<Set<E>> combination(List<E> elements, int n) {
		List<Set<E>> result = new ArrayList<Set<E>>();
		
		List<List<E>> permutationResult = permutation(elements, n);
		for (List<E> permutation : permutationResult) {
			Set<E> set = new TreeSet<E>(permutation);
			if (!result.contains(set)) {
				result.add(set);
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		List<String> elements = new ArrayList<String>();
		elements.add("C");
		elements.add("D");
		elements.add("A");
	  elements.add("B");
		System.out.println(permutation(elements, 2));
		System.out.println(combination(elements, 2));
	}
}
