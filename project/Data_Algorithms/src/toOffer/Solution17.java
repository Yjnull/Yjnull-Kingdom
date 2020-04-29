package toOffer;

public class Solution17 {
    public static final int MAX_LIMIT = 10;

    public void printNumbers(int n) {
        char[] number = new char[n];
        for (int i = 0; i < n; i++) {
            number[i] = '0';
        }

        int curLimit = 0;
        while(!inc(number)) {
            if (++curLimit == MAX_LIMIT) {
                printNum(number, true);
                curLimit = 0;
            } else {
                printNum(number, false);
            }
        }
    }

    private boolean inc(char[] number) {
        boolean isOver = false;
        int len = number.length;
        for (int i = len - 1; i >= 0; i--) {
            int sum = number[i] - '0' + 1;
            if (sum >= 10) {
                if (i == 0) isOver = true;
                else number[i] = '0';
            } else {
                number[i] = (char) ('0' + sum);
                break;
            }
        }
        return isOver;
    }

    // private boolean inc(char[] number) {
    //     boolean isOver = false;
    //     int nTake = 0;
    //     int len = number.length;
    //     // 0 0 0
    //     for(int i = len - 1; i >= 0; i--) {
    //         int sum = number[i] - '0' + nTake;
    //         if(i == len - 1) sum++;
    //         if(sum >= 10) {
    //             if(i == 0) isOver = true;
    //             else {
    //                 sum -= 10;
    //                 nTake = 1;
    //                 number[i] = (char) ('0' + sum);
    //             }
    //         } else {
    //             number[i] = (char) ('0' + sum);
    //             break;
    //         }
    //     }
    //     return isOver;
    // }

    private void printNum(char[] number, boolean newLine) {
        boolean isStart0 = true;

        for (int i = 0; i < number.length; i++) {
            if (isStart0 && number[i] != '0') isStart0 = false;
            if (!isStart0) System.out.print(number[i]);
        }

        if (newLine) System.out.println();
        else System.out.print(' ');
    }

    public static void main(String[] args) {
        // 1..10
        // 66
        new Solution17().printNumbers(3);
    }
}
