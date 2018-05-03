package Chap07;

/**
 * Created by yangy on 2018/5/3.
 * 高级排序
 */
public class HighSort {
    /**
     *  希尔排序，基于插入排序的改进
     *  h ——> Knuth 间隔序列  h=3*h + 1
     */
    public static int[] shellSort(int[] a) {
        int out, in;
        int temp;

        int h = 1;
        while (h < a.length / 3) {
            h = 3*h + 1;
        }

        while (h > 0) {
            for ( out = h; out < a.length; out++) {
                temp = a[out];
                in = out;

                while (in > h - 1 && a[in - h] > temp) {
                    a[in] = a[in - h];
                    in -= h;
                }
                a[in] = temp;
            }
            h = (h - 1) / 3;
        }

        return a;
    }

    public static void main(String[] args) {
        int[] theArray = new int[]{2, 5, 9, 1, 33, 6, 8, 3};
        shellSort(theArray);
        System.out.println("希尔排序");
        for (int a :
                theArray) {
            System.out.print(a + ", ");
        }
    }
}
