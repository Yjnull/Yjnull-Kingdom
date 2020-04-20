package toOffer;

import java.util.Random;

/**
 * 11: 旋转数组的最小数字
 */
public class Solution11 {

    // --- 隶属于 2.4.2 查找和排序 那一节，所以写个快排 -------------------------------------------------------------------
    private static final Random RANDOM = new Random();

    public static void quickSort(int[] a) {
        quickSort(a, 0, a.length - 1);
    }

    private static void quickSort(int[] a, int p, int r) {
        if (p >= r) return;

        int q = partition(a, p, r); // 获取分区点
        quickSort(a, p, q - 1);
        quickSort(a, q + 1, r);
    }

    private static int partition(int[] a, int lo, int hi) {
        if (a == null || a.length <= 0 || lo < 0 || hi >= a.length) {
            System.out.println("Invalid Parameters");
            return -1;
        }

        int index = RANDOM.nextInt(hi - lo + 1) + lo;
        swap(a, index, hi);

        int pivot = a[hi];
        int i = lo;
        for (int j = lo; j <= hi - 1; j++) {
            if (a[j] < pivot) {
                if (i != j) swap(a, i, j);
                i++;
            }
        }

        System.out.println("分区点 q = " + i + ", lo = " + lo);
        swap(a, i, hi);
        return i;
    }

    private static void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }
    // --- 隶属于 2.4.2 查找和排序 那一节，所以写个快排 -------------------------------------------------------------------

    

    public static void main(String[] args) {
        // int[] a = new int[]{3,6,8,9,11};
        // int[] a = new int[]{11,9,8,6,3};
        int[] a = new int[]{6,11,3,9,8};
        quickSort(a);
        for(int temp: a) {
            System.out.print(temp + ", ");
        }
        System.out.println();
    }
}
