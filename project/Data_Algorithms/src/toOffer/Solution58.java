package toOffer;

/**
 * 58: 翻转字符串
 */
public class Solution58 {

    /*private static void reverseString(StringBuilder sb, int left, int right) {
        while (left < right) {
            char tmp = sb.charAt(left);
            sb.setCharAt(left++, sb.charAt(right));
            sb.setCharAt(right--, tmp);
        }
    }

    private static StringBuilder trimSpaces(String s) {
        int left = 0, right = s.length() - 1;
        while (left <= right && s.charAt(left) == ' ') left++;

        while (left <= right && s.charAt(right) == ' ') right--;

        StringBuilder sb = new StringBuilder();
        while (left <= right) {
            char c = s.charAt(left);
            if (c != ' ' || sb.charAt(sb.length() - 1) != ' ') {
                sb.append(c);
            }
            left++;
        }

        return sb;
    }*/

    private static void reverseString(char[] chars, int left, int right) {
        while (left < right) {
            char tmp = chars[left];
            chars[left++] = chars[right];
            chars[right--] = tmp;
        }
    }

    private static int trimSpaces(char[] s) {
        int len = 0;
        boolean startWord = false;
        for (int i = 0; i < s.length; i++) {
            if (s[i] != ' ') {
                s[len++] = s[i];
                startWord = true;
            } else if (startWord) {
                s[len++] = ' ';
                startWord = false;
            }
        }

        if (len > 0 && s[len - 1] == ' ') {
            len--;
        }

        return len;
    }

    // 58（一）：翻转单词顺序
    // 题目：输入一个英文句子，翻转句子中单词的顺序，但单词内字符的顺序不变。
    // 为简单起见，标点符号和普通字母一样处理。例如输入字符串"I am a student. "，
    // 则输出"student. a am I"。
    public static String reverseStatement(String statement) {
        if (statement == null || statement.length() <= 0) {
            return statement;
        }

        char[] chars = statement.toCharArray();
        reverseString(chars, 0, chars.length - 1);

        int len = chars.length, start = 0, end = 0;
        while (start < len) {
            while (end < len && chars[end] == ' ') end++;
            start = end;

            while (end < len && chars[end] != ' ') end++;
            reverseString(chars, start, end - 1);
            start = end;
        }

        return new String(chars, 0, trimSpaces(chars));
    }

    public static String leftRotateString(String statement, int n) {
        if (statement == null || statement.length() <= 0) {
            return statement;
        }

        char[] chars = statement.toCharArray();
        int len = chars.length;

        if (n > 0 && n < len) {
            reverseString(chars, 0, n - 1);
            reverseString(chars, n, len - 1);
            reverseString(chars, 0, len - 1);
        }

        return new String(chars, 0, len);
    }

    public static void main(String[] args) {
        String test = "abcdefg";
        System.out.println(leftRotateString(test, 2));
    }
}
