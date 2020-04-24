package toOffer;

/**
 *  13: 机器人的运动范围
 *  地上有一个m行n列的方格，从坐标 [0,0] 到坐标 [m-1,n-1] 。一个机器人从坐标 [0, 0] 的格子开始移动，
 *  它每次可以向左、右、上、下移动一格（不能移动到方格外），也不能进入行坐标和列坐标的数位之和大于k的格子。
 *  例如，当k为18时，机器人能够进入方格 [35, 37] ，因为3+5+3+7=18。但它不能进入方格 [35, 38]，因为3+5+3+8=19。请问该机器人能够到达多少个格子？
 *
 */
public class Solution13 {

    public static void main(String[] args) {
        System.out.println("机器人的运动范围: " + movingCount(1, 2, 3));
        System.out.println("机器人的运动范围: " + movingCount(0, 3, 1));
    }

    public static int movingCount(int threshold, int rows, int cols) {
        if (threshold < 0 || rows <= 0 || cols <= 0) {
            return 0;
        }

        boolean[][] visited = new boolean[rows][cols];
        return movingCountCore(visited, threshold, rows, cols, 0, 0);
    }

    private static int movingCountCore(boolean[][] visited, int threshold, int rows, int cols, int row, int col) {
        int count = 0;
        // 机器人从 [0,0] 开始，想到达 [rows-1,cols-1]，那么可以只考虑往右和往下走
        // 满足 3 个条件：
        // 1. 索引没越界
        // 2. 行列坐标的数位和小于等于 threshold
        // 3. 没访问过
        if (row < rows && col < cols
                && (digitSum(row) + digitSum(col) <= threshold)
                && !visited[row][col]) {
            visited[row][col] = true;
            // 考察 1 + 往下走 + 往右走
            count = 1 + movingCountCore(visited, threshold, rows, cols, row + 1, col)
                    + movingCountCore(visited, threshold, rows, cols, row, col + 1);
        }
        return count;
    }

    private static int digitSum(int num) {
        int sum = 0;
        while (num > 0) {
            sum += num % 10;
            num = num / 10;
        }
        return sum;
    }

}
