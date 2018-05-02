package Chap04;

public class InToPost {
    private MyStack<Character> theStack;
    private String input;
    private String output = "";

    public InToPost(String input) {
        this.input = input;
        int stackSize = this.input.length();
        theStack = new MyStack<>(stackSize);
    }

    /**
     * 将中缀表达式转换成后缀表达式
     *
     * @return
     */
    public String doTrans() {
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            theStack.displayStack("For " + ch + " ");
            switch (ch) {
                case '+':
                case '-':
                    gotOper(ch, 1);
                    break;
                case '*':
                case '/':
                    gotOper(ch, 2);
                    break;
                case '(':
                    theStack.push(ch);
                    break;
                case ')':
                    gotParen(ch);
                    break;
                default:
                    output = output + ch;
                    break;
            }
        }

        while (!theStack.isEmpty()) {
            theStack.displayStack("While ");
            output = output + theStack.pop();
        }
        theStack.displayStack("End   ");
        return output;
    }

    private void gotParen(char ch) {
        while (!theStack.isEmpty()) {
            char chx = theStack.pop();
            if (chx == '(')
                break;
            else output = output + chx;
        }
    }

    public void gotOper(char opThis, int prec1) {
        while (!theStack.isEmpty()) {
            char opTop = theStack.pop();
            if (opTop == '(') {
                theStack.push(opTop);
                break;
            } else {
                int prec2;
                if (opTop == '+' || opTop == '-')
                    prec2 = 1;
                else prec2 = 2;
                if (prec2 < prec1) {
                    theStack.push(opTop);
                    break;
                }else output = output + opTop;
            }

        }

        theStack.push(opThis);
    }

}
