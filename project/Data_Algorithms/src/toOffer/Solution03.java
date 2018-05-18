package toOffer;

/**
 * 03: 二维数组中的查找
 * 在一个二维数组中，每一行都按照从左到右递增的顺序排序，每一列都按照从上到下递增的顺序排序。
 * 请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
 */
public class Solution03 {
    public boolean Find(int target, int [][] array) {
        if (array == null) return false;
        int row = 0, column = array[0].length - 1;
        while (row < array.length && column >= 0) {
            if (array[row][column] > target)
                column--;
            else if (array[row][column] < target)
                row++;
            else
                return true;
        }

        return false;
    }

    public static void main(String[] args) {
        System.out.println(new Solution03().Find(7, new int[][]{ {1,2,8,9}, {2,4,9,12}, {4,7,10,13}, {6,8,11,15} }));
        //System.out.println(new Solution03().Find(7, null));
    }
}
