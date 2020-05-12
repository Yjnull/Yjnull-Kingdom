package toOffer;

/**
 * 字符串匹配算法：BM、KMP
 */
public class StringMatch {
    public static void main(String[] args) {
        StringMatch match = new StringMatch();

        String a = "ababaeabac";
        String b = "abac";
        System.out.println("bm= " + match.bm(a.toCharArray(), a.length(), b.toCharArray(), b.length()));

        System.out.println("--- kmpa get Next ---");
        // String c = "abcabce";
        String c = "mississippi";
        String d = "issip";
        // String c = "aabaaabaaac";
        // String d = "aabaaac";
        int kmp = match.kmp(c.toCharArray(), c.length(), d.toCharArray(), d.length());
        System.out.println("匹配：" + kmp);
    }

    // --- KMP 算法 ---------------------------------------------------------------------------------------------------
    public int kmp(char[] s, int m, char[] p, int n) {
        int[] next = new int[n];
        next[0] = 0;
        for(int i = 1, j = 0; i < n; i++) {
            // aabaaac
            //    aabaaac
            System.out.println("next[" + i + "] = " + next[i] + ", next[" + j + "] = " + next[j]);

            while (j > 0 && p[i] != p[j]) {
                j = next[j-1];
            }

            if (j == -1) {j = 0; continue;}

            if(p[i] == p[j]) {
                next[i] = j+1;
                j++;
            }
        }

        for (int i = 0, j = 0; i < m; i++) {
            while (j > 0 && s[i] != p[j]) {
                j = next[j-1];
            }

            if (s[i] == p[j]) {
                j++;
            }

            if (j == n) return i - n + 1;
        }

        return -1;
    }


    // --- BM 算法 -----------------------------------------------------------------------------------------------------
    public int bm(char[] a, int n, char[] b, int m) {
        // 构建坏字符哈希表
        int[] bc = new int[SIZE];
        generateBC(b, m, bc);
        // 因为好后缀也是模式串本身的后缀子串，所以进行预处理，计算模式串中，每个后缀子串对应的另一个可匹配子串的位置
        int[] suffix = new int[m];
        boolean[] prefix = new boolean[m];
        generateGS(b, m, suffix, prefix);

        int i = 0;
        while(i <= n - m) {
            int j; // 坏字符对应模式串中的下标是 j
            for(j = m -1; j >= 0; j--) {
                if(b[j] != a[i+j]) break;
            }

            if (j < 0) {
                return i;
            }

            int x = j - bc[(int)a[i+j]]; // 坏字符规则

            int y = 0;
            if(j < m-1) {
                y = moveByGS(j, m, suffix, prefix);
            }

            i = i + Math.max(x,y);

        }
        return -1;
    }

    private int moveByGS(int j, int m, int[] suffix, boolean[] prefix) {
        int k = m - 1 - j; // 好后缀长度
        if(suffix[k] != -1) return j - suffix[k] + 1;
        for(int r = j+2; r <= m-1; r++) {
            if(prefix[m-r]) {
                return r;
            }
        }
        return m;
    }

    /*
    * 好后缀规则：good suffix
    * - 在模式串中找到跟 好后缀 匹配的子串
    * - 上一步没找到子串的话，那么在 好后缀 的后缀子串中，查找最长的，能跟模式串的前缀子串匹配的后缀子串；
    */
    private void generateGS(char[] b /*模式串*/, int m, int[] suffix, boolean[] prefix) {
        for(int i = 0; i < m; i++) {
            suffix[i] = -1;
            prefix[i] = false;
        }

        for(int i = 0; i < m-1; i++) {
            int j = i;
            int k = 0; // 公共后缀子串长度
            // c a b c a c
            while(j >=0 && b[j] == b[m-1-k]) {
                --j;
                ++k;
                suffix[k] = j+1;
            }
            if(j == -1) prefix[k] = true;
        }
    }

    // 坏字符规则
    private static final int SIZE = 256;
    private void generateBC(char[] b, int m, int[] bc) {
        for(int i = 0; i < SIZE; i++) {
            bc[i] =  -1;
        }

        // a b d a
        for(int i = 0; i < m; i++) {
            int ascii = (int)b[i];
            bc[ascii] = i;
        }
    }
}
