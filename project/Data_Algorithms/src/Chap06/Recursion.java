package Chap06;

/**
 * Created by yangy on 2018/5/2.
 */
public class Recursion {
    static int nDisks = 2;
    /**
     * 汉诺塔
     * @param topN
     * @param from
     * @param inter
     * @param to
     */
    public static void doTowers(int topN, char from, char inter, char to) {
        if (topN == 1)
            System.out.println("Disk 1 from " + from + " to " + to );
        else {
            doTowers(topN - 1, from, to, inter);
            System.out.println("Disk " + topN + " from " + from + " to " + to);
            doTowers(topN - 1, inter, from, to);
        }
    }

    /**
     * 归并排序 递归实现
     */
    public static void recMergeSort(int[] theArray, int[] workSpace, int lower, int upper) {
        if (lower == upper) return;
        else {
            int mid =( lower + upper ) / 2;
            recMergeSort(theArray, workSpace, 0, mid);
            recMergeSort(theArray, workSpace, mid + 1, upper);
            merge(theArray, workSpace, lower, mid + 1, upper);
        }
    }

    private static void merge(int[] theArray, int[] workSpace, int lower, int high, int upper) {
        int j = 0;
        int lowerBound = lower;
        int mid = high - 1;
        int n = upper - lower + 1;
        while (lower <= mid && high <= upper) {
            if (theArray[lower] < theArray[high])
                workSpace[j++] = theArray[lower++];
            else
                workSpace[j++] = theArray[high++];
        }

        while (lower <= mid)
            workSpace[j++] = theArray[lower++];

        while (high <= upper)
            workSpace[j++] = theArray[high++];

        for (j = 0; j < n; j++) {
            theArray[j + lowerBound] = workSpace[j];
        }
    }

    /**
     * 递归解决  背包问题
     */
    public static void knapsack(int target, int start, int[] items, boolean[] selects){
        if (target < 0 || target > 0 && start >= items.length) return;

        if (target == 0) {
            for (int i = 0; i < start; i++) {
                if (selects[i])
                    System.out.print(items[i] + " ");
            }
            System.out.println();
            return;
        }

        selects[start] = true;
        knapsack(target - items[start], start + 1, items, selects);
        selects[start] = false;
        knapsack(target, start + 1, items, selects);
    }

    /**
     * 递归  实现组合
     */
    public static void groups(int x, int y, char[] items, boolean[] selects) {

        if ( y == 0) {
            for (int i = 0; i < items.length; i++) {
                if (selects[i]) System.out.print(items[i] + " ");
            }
            System.out.println();
            return;
        }

        if (x < y) return;

        selects[items.length - x] = true;
        groups(x - 1, y-1, items, selects);
        selects[items.length - x] = false;
        groups(x - 1, y, items, selects);


    }

    public static void main(String[] args) {
        //doTowers(nDisks, 'A', 'B', 'C');
        /*int[] theArray = new int[]{2, 5, 9, 1, 33, 6, 8, 3};
        int[] workSpace = new int[theArray.length];
        recMergeSort(theArray, workSpace, 0, theArray.length - 1);
        for (int i = 0; i < theArray.length; i++) {
            System.out.print(theArray[i] + ", ");
        }*/
        /*int[] items = new int[]{11, 8, 7, 6, 5};
        boolean[] selects = new boolean[items.length]; // 记录是否被选择
        knapsack(20, 0, items, selects);*/

        char[] items = new char[]{'A', 'B', 'C', 'D', 'E'};
        boolean[] selects = new boolean[items.length];
        StringBuilder stringBuilder = new StringBuilder();
        groups(items.length, 3, items, selects);
    }

}
