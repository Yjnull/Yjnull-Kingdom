package toOffer;

import java.util.HashSet;
import java.util.Set;

/**
 * 03: 数组中重复的元素
 */
public class Solution03 {

    // [LeetCode](https://leetcode-cn.com/problems/shu-zu-zhong-zhong-fu-de-shu-zi-lcof/)
    // 官方解法
    private int findRepeatNumber(int[] nums) {
        int result = -1;
        Set<Integer> set = new HashSet<>();
        for (int num : nums) {
            if (!set.add(num)) {
                result = num;
                break;
            }
        }

        return result;
    }

    // 剑指 offer 解法
    private int findRepeatNumber2(int[] nums) {
        int result = -1;

        if (nums == null || nums.length <= 0) return result;

        for (int num : nums) {
            if (num < 0 || num > nums.length - 1) {
                return result;
            }
        }

        for (int i = 0; i < nums.length; i++) {
            while (nums[i] != i) {

                if (nums[nums[i]] == nums[i]) {
                    return nums[i];
                }

                int temp = nums[i];
                nums[i] = nums[temp];
                nums[temp] = temp;
            }
        }

        return result;
    }

    /**
     * 03进阶: 不修改数组，找出重复的元素
     * 在一个长度为 n 的数组里，所有数字都在 1 ~ n-1 的范围内
     * 2 3 5 4 3 2 6 7
     */
    private int findRepeatNumber3(int[] nums) {
        int result = -1;

        if (nums == null || nums.length <= 0) return result;

        int start = 1;
        int end = nums.length - 1;

        while (end >= start) {
            int middle = start + ((end - start) >> 1);

            // 遍历整个数组 计数
            int count = 0;
            for (int num : nums) {
                if (num >= start && num <= middle) {
                    count++;
                }
            }

            if (end == start) {
                if (count > 1) {
                    return start;
                } else {
                    break;
                }
            }

            if (count > (middle - start + 1)) {
                end = middle;
            } else {
                start = middle + 1;
            }
        }

        return result;
    }

    public static void main(String[] args) {
        // 0 1 2 2 3 3 5
        // 0 1 2 3 4 5 6
        // System.out.println(new Solution03().findRepeatNumber2(new int[]{2,3,1,0,2,5,3}));
        System.out.println(new Solution03().findRepeatNumber3(new int[]{2,3,5,4,3,2,6,7}));
    }
}
