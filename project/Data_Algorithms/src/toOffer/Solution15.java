package toOffer;

/**
 * 15: 二进制中 1 的个数
 */
public class Solution15 {

    public static int hammingWeight(int n) {
        // int count = 0;
        // while (n != 0) {
        //     count++;
        //     n &= n-1;
        // }
        // return count;
        int count = 0;
        while (n != 0) {
            count += n & 1;
            n = n >>> 1;
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println("二进制中 1 的个数 = " + hammingWeight(-3));
    }

}
