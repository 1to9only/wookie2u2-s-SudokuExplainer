/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import java.util.Arrays;


/**
 * An Arrays helper utility class instead of repeating ourselves.
 * @author Keith Corlett 2013
 */
public final class MyArrays {

	public static int[] clear(int[] array) {
		clear(array, array.length);
		return array;
	}
	public static void clear(int[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = 0;
	}

	public static void clear(Object[] array) {
		clear(array, array.length);
	}
	public static void clear(Object[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = null;
	}

	public static void clear(boolean[] array) {
		for ( int i=0,n=array.length; i<n; ++i )
			array[i] = false;
	}

//	public static void nullTerminate(Cell[] cells, int first) {
//		for ( int i=first,n=cells.length; i<n; ++i )
//			if ( cells[i] == null )
//				return;
//			else
//				cells[i] = null;
//	}

//	public static int[] unbox(Collection<Integer> c) {
//		final int n = c.size();
//		int[] result = new int[n];
//		int i = 0;
//		Iterator<Integer> it = c.iterator();
//		while ( it.hasNext() )
//			result[i++] = it.next();
//		assert i == n;
//		return result;
//	}

	public static boolean contains(final Object[] cells, final int n, final Object target) {
		for ( int i=0; i<n; ++i )
			if ( cells[i] == target ) // NB: reference equals is OK
				return true;
		return false;
	}

	/**
	 * Does a contain the target Object. For speed, equals is reference-equals,
	 * ie the Objects identity... I do not use the equals method.
	 * @param a
	 * @param target
	 * @return Does a contain target?
	 */
	public static boolean contains(final Object[] a, final Object target) {
		switch ( a.length ) {
			case 2: return a[0]==target || a[1]==target;
			case 3: return a[0]==target || a[1]==target || a[2]==target;
			case 4: return a[0]==target || a[1]==target || a[2]==target || a[3]==target;
			case 1: return a[0]==target;
			case 0: return false;
			default:
				for ( Object e : a )
					if ( e == target )
						return true;
				return false;
		}
	}

	public static String toString(String sep, Object[] a) {
		StringBuilder sb = new StringBuilder();
		for ( int i=0,n=a.length; i<n; ++i ) {
			if ( i > 0 )
				sb.append(sep);
			sb.append(i).append("=").append(a[i]);
		}
		return sb.toString();
	}

	public static String[] leftShift(String[] args) {
		String[] newArgs = new String[args.length-1];
		System.arraycopy(args, 1, newArgs, 0, args.length-1);
		return newArgs;
	}

	public static Cell[] clone(Cell[] src, int n) {
		Cell[] copy = new Cell[n];
		for ( int i=0; i<n; ++i )
			copy[i] = src[i];
		return copy;
	}

	public static int[] clone(int[] src, int n) {
		int[] copy = new int[n];
		for ( int i=0; i<n; ++i )
			copy[i] = src[i];
		return copy;
	}

	public static int[] clone(int[] src) {
		return clone(src, src.length);
	}

	// remove target from a, if it's there, and return a new downsized array
	public static int[] remove(int[] a, int target) {
		final int n = a.length;
		int i = 0;
		for ( ; i<n; ++i )
			if ( a[i] == target )
				break;
		if ( i == n ) // target not found
			return a;
		int[] newA = new int[n-1];
		for ( int j=0; j<i; ++j )
			newA[j] = a[j];
		for ( int j=i+1; j<n; ++j )
			newA[j-1] = a[j];
		return newA;
	}

	public static boolean equals(Cell[] a, Cell[] b, int n) {
		if (a==b)
			return true;
		if (a==null || b==null)
			return false;
		assert a.length == b.length;
		final int minLength = Math.min(a.length, n);
		for (int i=0; i<minLength; i++) {
			Object ab = a[i];
			Object ob = b[i];
			if ( !(ab==null ? ob==null : ab.equals(ob)) )
				return equalsSorted(a, b, n);
		}
		return true;
	}

	public static boolean equals(int[] a, int[] b, int n) {
		if (a==b)
			return true; // even if they're both null
		if (a==null || b==null)
			return false;
		assert a.length == b.length;
		final int minLength = Math.min(a.length, n);
		for ( int i=0; i<minLength; i++ )
			if ( a[i] != b[i] )
				return false;
		return true;
	}

	// sort the two arrays and then compare the first n elements. Actually we'll
	// take a copy of the first n-elements of both array, sort the copies, and
	// them compare them, to avert the side-effect of sorting the source arrays.
	// If you want that side effect then sort the damn arrays yourself and then
	// just use bloody equals.
	public static boolean equalsSorted(Cell[] a, Cell[] b, int n) {
		if (a==b)
			return true;
		if (a==null || b==null) // recall only a or b is null, not both.
			return false;
		if (a.length<n || b.length<n )
			return false;

		Cell[] aa=new Cell[n]; System.arraycopy(a,0, aa,0, n); Arrays.sort(aa);
		Cell[] bb=new Cell[n]; System.arraycopy(b,0, bb,0, n); Arrays.sort(bb);
		assert aa.length==n && bb.length==n;
		for (int i=0; i<n; i++) {
			Object ab = aa[i];
			Object ob = bb[i];
			if ( !(ab==null ? ob==null : ab.equals(ob)) )
				return false;
		}
		return true;
	}

	/**
	 * The in method does a simple O(n) linear search of array for target. I
	 * I use this sometimes because java.util.Arrays.binarySearch assumes array
	 * is sorted ascending, if it isn't then it could be faster to just search
	 * linearly. Don't use this on large arrays, obviously.
	 * @param target to look for
	 * @param array to look in
	 * @return true if target is in array, else false
	 */
	static boolean in(int target, int... array) {
		for ( int i : array )
			if ( i == target )
				return true;
		return false;
	}

	// copies cells into a new array minus (except) c.
	// @param cells is a set (no duplicates, no nulls, not a null terminated array)
	// @param c is the target cell to "ignore"
	// @returns a new array of cells minus c
	public static Cell[] minus(Cell[] cells, Cell c) {
		final int n = cells.length - 1;
		Cell[] result = new Cell[n];
		int cnt = 0;
		for ( Cell cell : cells )
			if ( cell != c )
				result[cnt++] = cell;
		assert cnt == n;
		return result;
	}

	// JAPI incompetence astonishes me. It was a different world. We now know so
	// much. I hope people think I'm an idjit in 30 years. Nah, make it 45!
	public static int[] sort(int[] src) {
		java.util.Arrays.sort(src);
		return src;
	}

//not used
//	/**
//	 * Creates a new boolean[81] and sets the given cells to true.
//	 * @param cells
//	 * @return
//	 */
//	public static boolean[] of(Iterable<Cell> cells) {
//		boolean[] result = new boolean[81];
//		for ( Cell cell : cells )
//			result[cell.i] = true;
//		return result;
//	}

//Never used, and untested. delete it if you like.
//	public static int[] uniq(int[] src) {
//		int cnt = 0; // the number of unique entries in result
//		// make a copy of the src array and sort it (leave src alone)
//		int[] copy = new int[src.length];
//		System.arraycopy(src, 0, copy, 0, cnt);
//		java.util.Arrays.sort(copy);
//		// copy unique values from copy into the uniq array
//		int[] uniq = new int[src.length];
//		int p = Integer.MAX_VALUE;
//		for ( int e : copy )
//			if ( e != p )
//				uniq[cnt++] = e;
//		// finally we return a "standardised" array of cnt elements
//		int[] result = new int[cnt];
//		System.arraycopy(uniq, 0, result, 0, cnt);
//		return result;
//	}

// ==================== here's some crap I prepared earlier ====================
// ================ and then decided that I wasn't going to use ================
// ==================== but I'm so spastically proud if it =====================
// ================== that I simply can't bear to delete it ====================
// =============================== but you can =================================
// ====================== NOW COME AT ME WITH THAT BANANA! =====================

//	/**
//	 * The filter method returns a new {@code LinkedList<E>} containing the
//	 * elements of 'c' which are 'accept'ed by the given 'filter'.
//	 * @param <E> the type of the Filter (primarily) but also the type of the
//	 * Collection, one of which could be abstracted, but which one? I'll cross
//	 * that bridge when I come to it.
//	 * @param c {@code Collection<E>} to be filtered
//	 * @param filter {@code Filter<E>} to 'accept' E's (ie a donkey drop;-)
//	 * @return a new {@code LinkedList<E>} of the 'accept'ed 'c's.
//	 */
//	public static <E> LinkedList<E> filter(Collection<E> c, IFilter<E> filter) {
//		return filter(new LinkedList<E>(), c, filter);
//	}
//	public static <E> LinkedList<E> filter(LinkedList<E> list, Collection<E> c, IFilter<E> filter) {
//		for ( E e : c )
//			if ( filter.accept(e) )
//				list.add(e);
//		return list;
//	}

//	/**
//	 * The filterAnd method returns a new LinkedList of the elements of 'c'
//	 * which are 'accept'ed by all of the given filters,
//	 * <p>ie inclusion is: if filter1 accepts e, AND filter2 accepts e, ...
//	 * <p><b>========================== UNTESTED ==========================</b>
//	 * @param <E> type accepted by the Filter, also the collection type.
//	 * @param c {@code Collection<? extends E>} means than the collection type
//	 * must either be the-type-of or a-subtype-of the E in {@code Filter<E>}.
//	 * @param filters... {@code Filter<E>} an argument list of 'E' filters,
//	 * ALL of which must 'accept' an element e in order for 'e' to be accepted.
//	 * @return a new {@code LinkedList<E>} containing the elements of 'c' that
//	 * are 'accept'ed by the given 'filters'.
//	 */
//	@SuppressWarnings("unchecked")
//	public static <E> LinkedList<E> filterAnd(Collection<E> c, IFilter<E>... filters) {
//		return filterAnd(new LinkedList<E>(), c, filters);
//	}
//	public static <E> LinkedList<E> filterAnd(LinkedList<E> list, Collection<E> c, IFilter<E>... filters) {
//		switch ( filters.length ) {
//		case 1: {
//			IFilter<E> filter = filters[0];
//			for ( E e : c )
//				if ( filter.accept(e) )
//					list.add(e);
//		} break;
//		case 2: {
//			IFilter<E> f1 = filters[0];
//			IFilter<E> f2 = filters[1];
//			for ( E e : c )
//				if ( f1.accept(e) && f2.accept(e) )
//					list.add(e);
//		} break;
//		default:
//			E_LOOP: for ( E e : c ) {
//				for ( IFilter<E> filter : filters )
//					if ( !filter.accept(e) )
//						continue E_LOOP;
//				list.add(e);
//			}
//		}
//		return list;
//	}

//	/**
//	 * The filterOr method returns a new {@code LinkedList<E>} of the elements
//	 * of 'c' which are 'accept'ed by ANY of the given filters,
//	 * <p>ie inclusion is: if filter1 accepts e, OR filter2 accepts e, ...
//	 * <p><b>========================== UNTESTED ==========================</b>
//	 * @param <E> type accepted by the Filter, also the collection type.
//	 * @param c {@code Collection<? extends E>} means than the collection type
//	 * must either be the-type-of or a-subtype-of the E in {@code Filter<E>}.
//	 * @param filters... {@code Filter<E>} an argument list of 'E' filters,
//	 * ALL of which must 'accept' an element e in order for 'e' to be accepted.
//	 * @return a new {@code LinkedList<E>} containing the elements of 'c' that
//	 * are 'accept'ed by the given 'filters'.
//	 */
//	public static <E> LinkedList<E> filterOr(Collection<E> c, IFilter<E>... filters) {
//		return filterOr(new LinkedList<E>(), c, filters);
//	}
//	public static <E> LinkedList<E> filterOr(LinkedList<E> list, Collection<E> c, IFilter<E>... filters) {
//		switch ( filters.length ) {
//		case 1: {
//			IFilter<E> filter = filters[0];
//			for ( E e : c )
//				if ( filter.accept(e) )
//					list.add(e);
//		} break;
//		case 2: {
//			IFilter<E> f1 = filters[0];
//			IFilter<E> f2 = filters[1];
//			for ( E e : c )
//				if ( f1.accept(e) || f2.accept(e) )
//					list.add(e);
//		} break;
//		default:
//			E_LOOP: for ( E e : c )
//				for ( IFilter<E> filter : filters )
//					if ( filter.accept(e) ) {
//						list.add(e);
//						continue E_LOOP;
//					}
//		}
//		return list;
//	}

//	public static <E> int indexOf(E[] a, E target) {
//		for ( int i=0,n=a.length; i<n; ++i )
//			if ( a[i] == target )
//				return i;
//		return -1;
//	}
//
//	public static <E> boolean containsC(E[] a, E target) {
//		for ( E e : a )
//			if ( e == target )
//				return true;
//		return false;
//	}

//	/**
//	 * The narrowCast method selects the classes from array 'a' which implement
//	 * the componentType of array 'r' (which must be an interface), and adds
//	 * them to a new array 'r' which it returns.
//	 * <p>EG: LogicalSolver has an enabledHinters array, some of which are
//	 * IHintNumberActivatableHinters, which we activate for each hint, so:
//	 * {@code 
//	 *		IHintNumberActivatableHinters[] activatableEnabledHinters
//	 *				= MyArrays.narrowCast(enabledHinters
//	 *						, new IHintNumberActivatableHinters[0]);
//	 * }
//	 * <p>Note that I've really written this method just for the challenge.
//	 * There's not a damn thing wrong with doing this with the generic types
//	 * in plain code. It's faster, and simpler that way. I just wanted the
//	 * challenge of doing generics generically.
//	 * <p><b>WARNING:</b> Error-prone code. I am NOT the messiah, I'm just a
//	 * naughty boy. This method relieves you from the tiresome burdens of the
//	 * compiler, which tells you when you've f__ked s__t up. Think boy!
//	 * @param <E> the master type of the array 'a'.
//	 * @param <R> the result (narrower) type of the result array 'r'. Note that
//	 * R <b>MUST</b> be an interface. This method looks only at interfaces.
//	 * @param a the master array
//	 * @param r the result array
//	 * @return a new array containing the elements of 'a' which implement r's
//	 * componentType.
//	 */
//	@SuppressWarnings("unchecked")
//	public static <E, R> R[] narrowCast(E[] a, R[] r) {
//		final Class<?> oc = new Object().getClass(); // objectClass
//		Class<?> rType = r.getClass().getComponentType(); // result type (to narrow to)
//		assert rType.isInterface();
//		List<R> list = new LinkedList<>(); // intermediary collection
//		E_LOOP: for ( E e : a ) // foreach element in array
//			// does element or any of her ancestors implement the resultType?
//			for ( Class<?> c=e.getClass(); oc!=c; c=c.getSuperclass() ) // for e's class and supers
//				for ( Class<?> ci : c.getInterfaces() ) // foreach interface of that class
//					if ( rType == ci ) { // ie if e instanceof $rType
//						list.add((R)e); // <==== unchecked cast, which is OK.
//						continue E_LOOP;
//					}
//		return (R[]) list.toArray(r);
//	}
//
//	public static ARegion[] combine(ARegion[] a, ARegion[] b) {
//		final int n = a.length + b.length;
//		ARegion[] result = new ARegion[n];
//		int cnt = 0;
//		for ( ARegion r : a )
//			result[cnt++] = r;
//		for ( ARegion r : b )
//			result[cnt++] = r;
//		return result;
//	}
//
//	public static int combine(int[] a, int[] b, int[] result) {
//		int cnt = 0;
//		for ( int e : a )
//			result[cnt++] = e;
//		for ( int e : b )
//			result[cnt++] = e;
//		return cnt;
//	}
//
//	public static ARegion[] append(ARegion[] regions, ARegion toAppend) {
//		ARegion[] result = new ARegion[regions.length+1];
//		System.arraycopy(regions, 0, result, 0, regions.length);
//		result[result.length-1] = toAppend;
//		return result;
//	}

	public static ARegion[] join(ARegion[] rowsOrCols, ARegion[] boxs) {
		ARegion[] result = new ARegion[18];
		System.arraycopy(rowsOrCols, 0, result, 0, 9);
		System.arraycopy(boxs, 0, result, 9, 9);
		return result;
	}

	public static boolean isNullOrEmpty(Object[] a) {
		return a==null || a.length==0 || a[0]==null;
	}

	public static String toString(int[] a, int n) {
		StringBuilder sb = new StringBuilder(n*6);
		for ( int i=0; i<n; ++i ) {
			if (i>0) sb.append(' ');
			sb.append(a[i]);
		}
		return sb.toString();
	}
}
