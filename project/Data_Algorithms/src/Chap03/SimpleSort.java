package Chap03;

/**
 * 简单排序： 冒泡、选择、插入
 * 未考虑边界条件，只是大体思想的体现
 */
public class SimpleSort {
    /**
     * 冒泡排序, 只有前面的值比后面的大才两两交换
     * @param a
     * @return
     */
    public int[] bubbleSort(int[] a){
        if (a == null || a.length == 0) return null;
        for (int i = a.length - 1; i > 0; i--) {
            for (int j = 0; j < i ; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                }
            }
        }
        return a;
    }

    /**
     * 选择排序, 选中最小值
     * @param a
     * @return
     */
    public int[] selectSort(int[] a) {
        if (a == null || a.length == 0) return null;
        int min;
        for (int out = 0; out < a.length - 1; out++) {
            min = out;
            for (int in = out + 1; in < a.length; in++) {
                if (a[in] < a[min]) min = in;
            }
            swap(a, out, min);
        }

        return a;
    }

    /**
     * 插入排序, out 下标左边总是有序的
     * @param a
     * @return
     */
    public int[] insertSort(int[] a) {
        int in, out;
        for (out = 1; out < a.length; out++) {
            int temp = a[out];
            in = out;
            while (in > 0 && a[in - 1] >= temp) {
                a[in] = a[in - 1];
                in--;
            }
            a[in] = temp;
        }
        return a;
    }

    private void swap(int[] a, int one, int two) {
        int temp = a[one];
        a[one] = a[two];
        a[two] = temp;
    }
}
