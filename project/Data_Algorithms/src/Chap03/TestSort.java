package Chap03;

public class TestSort {
    public static void main(String[] args) {
        SimpleSort simpleSort = new SimpleSort();
        int[] ab = new int[]{8, 34, 64, 32, 51, 21};
        simpleSort.bubbleSort(ab);
        for (int anAb : ab) {
            System.out.print(anAb + ", ");
        }
    }
}
