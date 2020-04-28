package toOffer;

/**
 * 16: 数值的整数次方
 */
public class Solution16 {

    public static double myPow(double x, int N) {
        if (x == 0) return 0;

        long n = N;
        if(n < 0) {
            n = -n;
            x = 1/x;
        }

        double res = 1;
        while (n > 0) {
            if ((n & 0x01) == 1) res *= x;
            x *= x;
            n = n >> 1;
        }

        return res;
    }

    public static void main(String[] args) {
        System.out.println("x 的整数次方: " + myPow(2, 10));
        System.out.println("x 的整数次方: " + myPow(2, -2));
        System.out.println("x 的整数次方: " + myPow(-2, 2));
        System.out.println("x 的整数次方: " + myPow(-2, 3));
    }

}
