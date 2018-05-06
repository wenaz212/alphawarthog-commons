package com.alphawarthog.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionUtils {
	
	/*
	 * AB, AC, BA, BC, CA, CB
	 * ABC, ABD, ACB, ACD, ADB, ADC, BAC, BAD, BCA, BCD, BDA, BDC, 
	 * CAB, CAD, CBA, CBD, CDA, CDB, DAB, DAC, DBA, DBC, DCA, DCB
  */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<List> permutation(List elements, int n) {
		List<List> result = new ArrayList<List>();
		
		if (n == 1) {
			for (Object obj : elements) {
				List list = new ArrayList();
				list.add(obj);
				result.add(list);
			}
		} else {
			for (Object o : elements) {
				List clone = new ArrayList(elements);
				clone.remove(o);
				List<List> recursiveResult = permutation(clone, n - 1);
				for (List recursiveElement : recursiveResult) {
					recursiveElement.add(0, o);
					result.add(recursiveElement);
				}
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public static List<List> permutation(List elements) {
		return permutation(elements, elements.size());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<Set> combination(List elements, int n) {
		List<Set> result = new ArrayList<Set>();
		
		List<List> permutationResult = permutation(elements, n);
		for (List permutation : permutationResult) {
			Set set = new TreeSet(permutation);
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
