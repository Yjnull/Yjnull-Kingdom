package toOffer;

/**
 * 11: 旋转数组的最小数字
 */
public class Solution11 {

    // 剑指 offer 题解
    public static int minArray(int[] numbers) {
        if(numbers == null || numbers.length <= 0) return -1;

        int left = 0, right = numbers.length - 1, middle = left;
        while(numbers[left] >= numbers[right]) {
            if(right - left == 1) {
                middle = right;
                break;
            }
            middle = left + ((right - left) >> 1);

            if(numbers[middle] == numbers[left] && numbers[left] == numbers[right]) {
                int min = numbers[left];
                for(int i = left + 1; i < right; i++) {
                    if(numbers[i] < min) min = numbers[i];
                }
                return min;
            }

            if(numbers[middle] >= numbers[left]) {
                left = middle;
            } else {
                right = middle;
            }
        }

        return numbers[middle];
    }

    // LeetCode 题解
    public static int minArrayLeetCode(int[] numbers) {
        if(numbers == null || numbers.length <= 0) return -1;

        int i = 0, j = numbers.length - 1;
        while(i < j) {
            int m = (i + j) >> 1;
            if(numbers[m] > numbers[j]) i = m + 1;
            else if(numbers[m] < numbers[j]) j = m;
            else j--;
        }
        return numbers[i];
    }

    public static void main(String[] args) {
        // int[] a = new int[]{3,6,8,9,11};
        // int[] a = new int[]{11,9,8,6,3};
        int[] a = new int[]{6,11,3,9,8};
        quickSort(a);
        for(int temp: a) {
            System.out.print(temp + ", ");
        }
        System.out.println();

        int[] b = new int[]{3,4,5,1,2};
        int[] b2 = new int[]{2,2,2,0,1};
        System.out.println("minArray " + minArray(b) + ", minArrayLeetCode " + minArrayLeetCode(b));
        System.out.println("minArray " + minArray(b2) + ", minArrayLeetCode " + minArrayLeetCode(b2));
    }




    // --- 隶属于 2.4.2 查找和排序 那一节，所以写个快排 -------------------------------------------------------------------
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

        int index = (int) (lo + Math.random() * (hi - lo + 1));
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

}
