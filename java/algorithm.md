# 数据结构与算法



## 6. 递归
1. 汉诺塔

2. 归并排序  O(N*logN)
缺点：需要在存储器中有另一个大小等于被排序的数据项目的数组。如果初始数组几乎占满整个存储器，那么归并排序将不能工作。

## 7. 高级排序
> 选择排序、快速排序、希尔排序、堆排序**不是稳定**的排序算法。
> 冒泡排序、插入排序、基数排序、归并排序**是稳定**的排序算法。

1. 希尔排序 O(N^3/2 ~ N^7/6)
希尔排序是基于插入排序的。 由于插入排序中，一个很小的数据在很靠近右端的位置上时，会造成大量的不必要的移动，所以导致效率很慢，如果数据项能大跨度地移动就好了。希尔排序则是根据这个点的优化。

2. 快速排序  O(N*logN)


## 8. 二叉树
1. 二叉树：每个节点最多有两个子节点。
2. 二叉搜索树：一个节点的左子节点的关键字值小于这个父节点，右子节点的关键字值大于等于这个父节点。（**本节讨论的都是基于二叉搜索树**）
3. 非平衡树：大部分的节点在根的一边或者是另一边，就可以说它是不平衡的。
4. 树的遍历
 - 前序
```
private void preOrder(Node localRoot) {
	if(localRoot != null) {
    	System.out.println();
    	inOrder(localRoot.leftChild);
        inOrder(localRoot.rightChild);
    }
}
```
 - 中序
中序遍历二叉搜索树会使所有的节点**按关键字升序**被访问到。
```
private void inOrder(Node localRoot) {
	if(localRoot != null) {
    	inOrder(localRoot.leftChild);
        System.out.println();
        inOrder(localRoot.rightChild);
    }
}
```

 - 后序
```
private void postOrder(Node localRoot) {
	if(localRoot != null) {
    	inOrder(localRoot.leftChild);
        inOrder(localRoot.rightChild);
         System.out.println();
    }
}
```