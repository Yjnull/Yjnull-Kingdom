package toOffer;

import java.util.*;

/**
 *  55: 二叉树的深度
 *  扩展：平衡二叉树的判定
 *
 */
public class Solution55 {

    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int x) {
            val = x;
        }
    }

    public static void main(String[] args) {
        // [1,2,2,3,null,null,3,4,null,null,4]
        /*
                1
              2   2
            3  n n 3
          4 n     n 4
         */
        TreeNode root = new TreeNode(1);
        TreeNode leftR2 = new TreeNode(2);
        TreeNode rightR2 = new TreeNode(2);
        root.left = leftR2;
        root.right = rightR2;
        TreeNode leftR3 = new TreeNode(3);
        leftR2.left = leftR3;
        leftR2.right = null;
        TreeNode leftR4 = new TreeNode(3);
        leftR3.left = leftR4;
        leftR3.right = null;

        rightR2.left = null;
        TreeNode rightR3 = new TreeNode(3);
        rightR2.right = rightR3;
        rightR3.left = null;
        rightR3.right = new TreeNode(4);

        System.out.println("isBalanced = " + isBalanced(root));

        System.out.println("a->" + (char)('a' ^ 32));
        System.out.println("a->" + (char)('b' - 32));
    }



    // --- 方法 1------------------------------------------
    /**
     * 简单但是不够高效，需重复遍历节点多次
     */
    public static boolean isBalanced(TreeNode root) {
        return root == null || Math.abs(depth(root.left) - depth(root.right)) <= 1 && isBalanced(root.left) && isBalanced(root.right);
    }

    private static int depth(TreeNode node) {
        if(node == null) return 0;
        return Math.max(depth(node.left), depth(node.right)) + 1;
    }
}

