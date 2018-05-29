package toOffer;

/**
 *  06: 重建二叉树
 *
 *  输入某二叉树的前序遍历和中序遍历，重建该二叉树
 */
public class Solution06 {
    class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int val) {
            this.val = val;
        }
    }

    public TreeNode reConstructBinaryTree(int [] pre,int [] in){
        if(pre == null || in == null) return null;
        return constructCore(pre, in, 0, pre.length - 1, 0, in.length - 1);
    }

    private TreeNode constructCore(int[] pre, int[] in,
                                   int startPre, int endPre,
                                   int startIn, int endIn){
        //前序遍历的第一个数字是根节点的值
        int rootVal = pre[startPre];
        TreeNode root = new TreeNode(rootVal);
        root.left = root.right = null;

        if(startPre == endPre) {
            if(startIn == endIn && pre[startPre] == in[startIn])
                return root;
            //else throw new Exception("");
        }

        //在中序遍历中找根节点的位置
        int rootInorder = startIn;
        while(rootInorder <= endIn && in[rootInorder] != rootVal)
            ++ rootInorder;
        ;
        int leftLength = rootInorder - startIn;
        int leftPreorderEnd = startPre + leftLength;
        if(leftLength > 0)                 //左子树的长度要大于0，要不然不需要构建
            root.left = constructCore(pre, in, startPre + 1, leftPreorderEnd, startIn, rootInorder - 1);
        if(leftLength < endPre - startPre) //startPre + 左子树的长度要 < endPre 要不然都没有右子树就不需要构建了
            root.right = constructCore(pre, in, leftPreorderEnd + 1, endPre, rootInorder + 1, endIn);

        return root;
    }

    public static void postOrder(TreeNode localRoot) {
        if (localRoot != null) {
            postOrder(localRoot.left);
            postOrder(localRoot.right);
            System.out.print(localRoot.val + " ");
        }
    }

    public static void main(String[] args) {
        TreeNode node = new Solution06().reConstructBinaryTree(
                new int[]{1,2,4,7,3,5,6,8},
                new int[]{4,7,2,1,5,3,8,6}
        );
        postOrder(node);
    }
}



