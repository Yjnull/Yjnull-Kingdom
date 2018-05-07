package Chap07;

import sun.jvm.hotspot.utilities.IntArray;

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

    /**
     *  划分
     */
    public static int partitionIn(int left, int right, int pivot, int a[]) {
         int leftPtr = left - 1;
         int rightPtr = right + 1;

         while (true) {
             //边界条件，考虑所有的数据都小于枢纽
             while (leftPtr < right && a[++leftPtr] < pivot)
                 ;
             while (rightPtr > left && a[--rightPtr] > pivot)
                 ;

             if (leftPtr >= rightPtr) {
                 break;
             } else {
                 swap(leftPtr, rightPtr, a);
             }
         }

         return leftPtr;
    }

    /**
     *  为了快排修改的划分方法1
     */
    public static int partitionIn1(int left, int right, int pivot, int a[]) {
        int leftPtr = left - 1;
        int rightPtr = right;    //划分时，把最右端的数据排除在外

        while (true) {
            while (a[++leftPtr] < pivot)
                ;
            while (rightPtr > 0 && a[--rightPtr] > pivot)
                ;

            if (leftPtr >= rightPtr) {
                break;
            } else {
                swap(leftPtr, rightPtr, a);
            }
        }
        swap(leftPtr, right, a);

        return leftPtr;
    }

    /**
     *  为了快排修改的划分方法2  三数据项取中划分
     */
    public static int partitionIn2(int left, int right, int pivot, int a[]) {
        int leftPtr = left;
        int rightPtr = right - 1;

        while (true) {
            while (a[++leftPtr] < pivot)
                ;
            while (a[--rightPtr] > pivot)
                ;

            if (leftPtr >= rightPtr) {
                break;
            } else {
                swap(leftPtr, rightPtr, a);
            }
        }
        swap(leftPtr, right - 1, a);

        return leftPtr;
    }

    /**
     *  三数据项取中找到枢纽值 pivot
     */
    private static int medianOf3(int left, int right, int a[]) {
        int center = (left + right) / 2;
        if (a[left] > a[center]) {
            swap(left, center, a);
        }

        if (a[left] > a[right]) {
            swap(left, right, a);
        }

        if (a[center] > a[right]) {
            swap(center, right, a);
        }
        swap(center, right - 1, a);
        return a[right - 1];
    }

    public static void recQuickSort(int left, int right, int a[]) {
        int size = right - left + 1;
        /*if (size <= 1)
            return;
        if (size == 2) {
            if (a[left] > a[right])
                swap(left, right, a);
            return;
        }
        if (size == 3) {
            if (a[left] > a[right - 1])
                swap(left, right - 1, a);
            if (a[left] > a[right])
                swap(left, right, a);
            if (a[right - 1] > a[right])
                swap(right - 1, right, a);
        } else {
            int pivot = medianOf3(left, right, a);
            int partition = partitionIn2(left, right, pivot, a);
            recQuickSort(left, partition - 1, a);
            recQuickSort(partition + 1, right, a);
        }*/

        if (size < 10)
            insertionSort(left, right, a);
        else {
            int pivot = medianOf3(left, right, a);
            int partition = partitionIn2(left, right, pivot, a);
            recQuickSort(left, partition - 1, a);
            recQuickSort(partition + 1, right, a);
        }
    }


    private static void swap(int leftPtr, int rightPtr, int a[]) {
        int temp = a[leftPtr];
        a[leftPtr] = a[rightPtr];
        a[rightPtr] = temp;
    }

    public static void insertionSort(int left, int right, int a[]) {
        int out, in;

        for (out = left + 1; out <= right; out++) {
            int temp = a[out];
            in = out;

            while (in > left && a[in - 1] >= temp) {
                a[in] = a[in - 1];
                in--;
            }
            a[in] = temp;
        }
    }

    public static void main(String[] args) {
        //int[] theArray = new int[]{2, 5, 9, 1, 33, 7, 8, 3};
        int[] theArray = new int[]{42, 89, 63, 12, 94, 27, 78, 3, 50, 36};
        //int[] theArray = new int[]{12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        recQuickSort(0, theArray.length - 1, theArray);
        //System.out.println(partitionIn(0, theArray.length - 1, 36, theArray));
        for (int a : theArray) {
            System.out.print(a + ", ");
        }
        /*shellSort(theArray);
        System.out.println("希尔排序");
        for (int a :
                theArray) {
            System.out.print(a + ", ");
        }*/
    }
}
