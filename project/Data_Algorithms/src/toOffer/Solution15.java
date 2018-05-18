package toOffer;

/**
 * 15: 链表中倒数第k个结点
 * 输入一个链表，输出该链表中倒数第k个结点。
 */
public class Solution15 {
    public ListNode FindKthToTail(ListNode head,int k) {
        if(head == null || k <= 0) return null;

        ListNode pAhead = head , pBehind = null;
        for(int i = 0; i < k - 1; i++) {
            if(pAhead.next != null)
                pAhead = pAhead.next;
            else return null;
        }

        pBehind = head;
        while(pAhead.next != null) {
            pAhead = pAhead.next;
            pBehind = pBehind.next;
        }
        return pBehind;
    }
}


class ListNode {
    int val;
    ListNode next = null;

    ListNode(int val) {
        this.val = val;
    }
}