package toOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 12: 矩阵中的路径
 * 请设计一个函数，用来判断在一个矩阵中是否存在一条包含某个字符串所有字符的路径。
 * 路径可以从矩阵中的任意一格开始，每一步可以在矩阵中向左、右、上、下移动一格。
 * a b t g
 * c f c s
 * j d e h
 *
 * ********** 回溯算法思想 **********
 */
public class Solution12 {
    public static void main(String[] args) {
        // *** 矩阵中的路径 ***
        /*
         * A B C E
         * S F C S
         * A D E E
         */
        char[][] board = new char[][]{{'A','B','C','E'},{'S','F','C','S'},{'A','D','E','E'}};
        System.out.println("is exist: " + exist(board, "ABCS"));

        // *** 八皇后 ***
        System.out.println("*** 八皇后 ***");
        List<List<String>> result = solveNQueens(8);
        for(List<String> temp : result) {
            for (String str : temp) {
                System.out.println(str);
            }
            System.out.println("----------");
        }
        // *** 背包问题 ***
        packageTrace(0,0);
        System.out.println("背包最大可装= " + maxResult);
    }

    /**
     * 矩阵中的路径
     */
    private static int pathLen = 0;
    public static boolean exist(char[][] board, String word) {
        if(board == null || board.length <= 0 || word == null) return false;

        int rows = board.length;
        int cols = board[0].length;
        boolean[][] hasAccess = new boolean[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (hasPathCore(board, hasAccess, word, rows, cols, i, j)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasPathCore(char[][] board, boolean[][] hasAccess, String word, int rows, int cols,
                                       int row, int col) {
        if (pathLen == word.length()) {
            return true;
        }

        boolean hasPath = false;
        if (row >= 0 && row < rows && col >= 0 && col < cols
                && board[row][col] == word.charAt(pathLen)
                && !hasAccess[row][col]) {
            pathLen++;
            hasAccess[row][col] = true;

            hasPath = hasPathCore(board, hasAccess, word, rows, cols, row + 1, col)
                    || hasPathCore(board, hasAccess, word, rows, cols, row - 1, col)
                    || hasPathCore(board, hasAccess, word, rows, cols, row, col + 1)
                    || hasPathCore(board, hasAccess, word, rows, cols, row, col - 1);

            if (!hasPath) {
                pathLen--;
                hasAccess[row][col] = false;
            }
        }

        return hasPath;
    }


    /**
     * N 皇后问题
     */
    private static List<List<String>> solveNQueens(int n) {
        if(n <= 0) return Collections.emptyList();
        List<List<String>> resultList = new ArrayList<>();
        int[] result = new int[n]; // 下标表示行，值表示 queen 存储在哪一列

        nQueens(n, resultList, result, 0);
        return resultList;
    }

    private static void nQueens(int n, List<List<String>> resultList, int[] result, int row) {
        if (row == n) {
            printQueens(resultList, result);
            return;
        }

        for (int i = 0; i < n; i++) {
            if (isOk(n, result, row, i)) {
                result[row] = i;
                nQueens(n, resultList, result, row + 1); // 考察下一行
            }
        }
    }

    private static boolean isOk(int n, int[] result, int row, int col) {
        int leftUp = col - 1;
        int rightUp = col + 1;

        // 逐行往上检查
        for (int i = row - 1; i >= 0; i--) {
            // 第 i 行的 col 列是否有值，有值就不能把 queen 放在这
            if (result[i] == col) {
                return false;
            }
            // 左上角
            if (leftUp >= 0 && result[i] == leftUp) {
                return false;
            }
            // 右上角
            if (rightUp < n && result[i] == rightUp) {
                return false;
            }
            leftUp--;
            rightUp++;
        }

        return true;
    }

    private static void printQueens(List<List<String>> resultList, int[] result) {
        int n = result.length;
        List<String> temp = new ArrayList<>();
        for(int row = 0; row < n; row++) {
            StringBuilder sb = new StringBuilder();
            for(int col = 0; col < n; col++) {
                if(result[row] == col) sb.append("Q ");
                else sb.append(". ");
            }
            temp.add(sb.toString());
        }
        resultList.add(temp);
    }


    /**
     * 背包问题
     */
    private static final int n = 5; // 物品个数
    private static final int maxWeight = 9; // 背包最大承受重量

    private static int maxResult = 0; // 结果: 背包中物品总重量的最大值
    private static int[] goodsWeight = {2, 2, 4, 6, 3};

    private static void packageTrace(int i /*当前处理第几个物品*/, int curWeight /*当前背包重量*/) {
        // curWeight == maxWeight 表示装满了，i == n 表示物品都考察完了
        if (curWeight == maxWeight || i == n) {
            if (curWeight > maxResult) maxResult = curWeight;
            return;
        }

        // 选择不装这个物品
        packageTrace(i + 1, curWeight);

        // 选择装这个物品
        if (curWeight + goodsWeight[i] <= maxWeight) {
            packageTrace(i + 1, curWeight + goodsWeight[i]);
        }
    }


}



