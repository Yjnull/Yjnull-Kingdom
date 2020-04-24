package toOffer;

/**
 *  14: 剪绳子
 *  给你一根长度为 n 的绳子，请把绳子剪成整数长度的 m 段（m、n都是整数，n>1并且m>1），
 *  每段绳子的长度记为 k[0],k[1]...k[m] 。请问 k[0]*k[1]*...*k[m] 可能的最大乘积是多少？
 *  例如，当绳子的长度是8时，我们把它剪成长度分别为2、3、3的三段，此时得到的最大乘积是18。
 *
 */
public class Solution14 {

    public static void main(String[] args) {
        System.out.println("剪绳子：" + cuttingRope(10));
    }

    private static int cuttingRope(int n) {
        // f(n) = max(f(i)*f(n-i)) , 0 < i < n
        if(n < 2) return 0;
        if(n == 2) return 1;
        if(n == 3) return 2;

        int[] result = new int[n+1];
        result[0] = 0;
        result[1] = 1;
        result[2] = 2;
        result[3] = 3;
        // 4 4, 5 6, 6 9, 7 12, 8 18

        int max = 0;
        for(int i = 4; i <= n; i++) {
            max = 0;
            for(int j = 1; j <= i/2; j++) {
                int temp = result[j] * result[i - j];
                if(temp > max) max = temp;
                result[i] = max;
            }
        }

        return result[n];
    }
}
