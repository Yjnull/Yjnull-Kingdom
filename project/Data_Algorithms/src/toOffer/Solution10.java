package toOffer;

/**
 *  10: 斐波那契数列
 */
public class Solution10 {

    /**
     * f(0) = 0, f(1) = 1, f(2) = 1
     */
    public static int fib(int n) {
        if(n <= 0) return 0;
        if(n == 1) return 1;

        int fbOne = 0;
        int fbTwo = 1;
        int fbN = 0;

        for (int i = 2; i <= n; i++) {
            fbN = (fbOne + fbTwo) % 1000000007;

            fbOne = fbTwo;
            fbTwo = fbN;
        }

        return fbN;
    }

    public static void main(String[] args) {
        System.out.println("fib = "  + fib(3));
    }
}



