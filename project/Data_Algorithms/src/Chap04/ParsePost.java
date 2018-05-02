package Chap04;

/**
 * 后缀表达式求值
 */
public class ParsePost {
    private MyStack<Integer> theStack;
    private String input;

    public ParsePost(String input) {
        this.input = input;
    }

    public int doParse() {
        theStack = new MyStack<>(20);
        char ch;
        int num1, num2, interAns;

        for (int i = 0; i < input.length(); i++) {
            ch = input.charAt(i);
            theStack.displayStack(ch + " ");
            if (ch >= '0' && ch <= '9')
                theStack.push(ch-'0');
            else {
                num2 = theStack.pop();
                num1 = theStack.pop();
                switch (ch) {
                    case '+':
                        interAns = num1 + num2;
                        break;
                    case '-':
                        interAns = num1 - num2;
                        break;
                    case '*':
                        interAns = num1 * num2;
                        break;
                    case '/':
                        interAns = num1 / num2;
                        break;
                    default:
                        interAns = 0;
                        break;
                }
                theStack.push(interAns);
            }
        }
        interAns = theStack.pop();
        return interAns;
    }
}
