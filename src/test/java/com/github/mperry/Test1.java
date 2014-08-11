package com.github.mperry;

import com.github.mperry.watch.Watcher;
import fj.P;
import fj.P2;
import fj.data.List;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by mperry on 11/08/2014.
 */
public class Test1 {


	@Test
	public void test1() {

		Watcher.watch();
	}


	@Test
	public void test2() {

		Object[] is = new Integer[]{1, 2, 3};
//		Integer[] is2 = (Integer []) Watcher.<Integer, Integer>toArrayInit(new Integer[]{1, 2, 3}, Integer.class);
//		String s = Arrays.toString(is2);
//		System.out.println(is2);
//		System.out.println(s);


//		Integer[] arr = Test1.toArray(List.list(1, 2, 3));
		Integer[] arr = Test1.toArrayInit(List.list(1, 2, 3));
		System.out.println(Arrays.toString(arr));

	}

	public static <A> A[] toArray(final fj.data.List<A> list) {
		return toArrayInit(list);
	}


	public static <A> A[] toArrayInit(final fj.data.List<A> list, A... args) {
//		A[] array =  Arrays.copyOf(args, list.length());
		A[] array = (A[]) new Object[list.length()];
		boolean loop = !list.isEmpty();
//		int i = 0;
//		while (loop) {
		int z = list.length();
		List<A> result2 = list;
		for (int i = 0; i < z; i++) {
			A head = list.head();
			array[i] = head;
			result2 = list.tail();
//			i++;
//			loop = !list.isEmpty();
		}

		P2<Integer, A[]> p = list.foldLeft((acc, e) -> {
			A[] result = acc._2();
			int index = acc._1();
			result[index] = e;
			return P.p(index + 1, result);
		}, P.p(0, array));
//		return array;
		return p._2();


//		Watcher.toArrayInit(list, );
//		list.toArrayInit().array();
//		return null;
	}



}
