package toOffer;

/**
 * 04: 替换空格
 * 把字符串中的每个空格替换成 "%20"
 */
public class Solution04 {
    public static String replaceSpace(StringBuffer str) {
        return str.toString().replace(" ", "%20");
    }

    public static void main(String[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append("hello world");
        System.out.println(replaceSpace(sb));
    }
}



/*
C++ 解法: 从后往前复制

class Solution {
    public:
    void replaceSpace(char *string,int length) {
        if(string == NULL && length <= 0) return;

        int originalLength = 0, numberOfBlank = 0, i = 0;
        while(string[i] != '\0') {
            originalLength++;
            if(string[i] == ' ')
                numberOfBlank++;
            i++;
        }

        int newLength = originalLength + numberOfBlank * 2;
        if(newLength > length) return;

        int indexOfOriginal = originalLength;
        int indexOfNew = newLength;
        while(indexOfOriginal >= 0 && indexOfNew > indexOfOriginal) {
            if(string[indexOfOriginal] == ' ') {
                string[indexOfNew--] = '0';
                string[indexOfNew--] = '2';
                string[indexOfNew--] = '%';
            } else {
                string[indexOfNew--] = string[indexOfOriginal];
            }

            indexOfOriginal--;
        }

    }
};*/
