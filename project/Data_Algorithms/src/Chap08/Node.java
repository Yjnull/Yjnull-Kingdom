package Chap08;

public class Node {
    int iData;         //关键字值
    double dData;      //其他数据
    Node leftChild;    //左子节点
    Node rightChild;   //右子节点

    public void displayNode() {
        System.out.print('{');
        System.out.print(iData);
        System.out.print(", ");
        System.out.print(dData);
        System.out.print("} ");
    }
}
