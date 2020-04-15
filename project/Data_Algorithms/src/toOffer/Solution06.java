package toOffer;

import java.util.ArrayList;
import java.util.Stack;

/**
 *  06: 从尾到头打印链表
 *  解决方法：1. 栈
 *          2. 递归，递归可能导致函数调用栈溢出
 */
public class Solution06 {
    class ListNode {
        int val;
        ListNode next = null;
        ListNode(int val) {
            this.val = val;
        }
    }

    public static ArrayList<Integer> printListFromTailToHead(ListNode listNode) {
        Stack<Integer> stack = new Stack<>();
        ArrayList<Integer> ans = new ArrayList<>();
        ListNode pNode = listNode;
        while(pNode != null) {
            stack.push(pNode.val);
            pNode = pNode.next;
        }

        while(!stack.isEmpty()) {
            ans.add(stack.pop());
        }
        return ans;
    }


    /*private ArrayList<Integer> ans = new ArrayList<>();
    public ArrayList<Integer> printListFromTailToHead2(ListNode listNode) {
        // 递归
        if (listNode != null) {
            if (listNode.next != null) {
                printListFromTailToHead2(listNode.next);
            }
            ans.add(listNode.val);
        }

        return ans;
    }*/

    public static void main(String[] args) {
    }
}



