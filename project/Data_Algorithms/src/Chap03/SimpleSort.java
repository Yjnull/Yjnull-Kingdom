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
    public void bubbleSort(int[] a){
        if (a == null || a.length == 0) return ;
        int flag;
        for (int i = a.length - 1; i >= 0; i--) {
            flag = 0;
            for (int j = 0; j < i ; j++) {   /* 一趟冒泡 */
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    flag = 1;  /* 标识发生了交换 */
                }
            }
            if (flag == 0) break;       /* 全程无交换 */
        }
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
     * 插入排序, out 下标左边总是有序的   T(N,I) = O(N+I)  N 是元素个数，I 是逆序对
     * @param a
     * @return
     */
    /*public int[] insertSort(int[] a) {
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
    }*/
    public void insertSort2(int[] a) {
        int P, i;
        int temp;

        for (P = 1; P < a.length; P++) {
            temp = a[P];              /* 取出未排序序列中的第一个元素(摸下一张牌) */
            for (i = P; i > 0 && a[i - 1] > temp; i--)
                a[i] = a[i - 1];          /* 依次与已排序序列中元素比较并右移(移出空位) */
            a[i] = temp;                  /* 放进合适的位置(新牌落位) */
        }
    }


    private void swap(int[] a, int one, int two) {
        int temp = a[one];
        a[one] = a[two];
        a[two] = temp;
    }
}
