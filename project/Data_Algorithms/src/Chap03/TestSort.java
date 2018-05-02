package Chap03;

import java.util.Arrays;

public class TestSort {
    public static void main(String[] args) {
        SimpleSort simpleSort = new SimpleSort();
        System.out.println(Arrays.toString(simpleSort.bubbleSort(new int[]{3, 1, 2})));
        //System.out.println(Arrays.toString(simpleSort.selectSort(new int[]{3, 1, 2, 3, 9, 7, 6, 2, 5})));
        //System.out.println(Arrays.toString(simpleSort.insertSort(new int[]{3, 1, 2, 3, 9, 7, 6, 2, 5})));
    }
}
