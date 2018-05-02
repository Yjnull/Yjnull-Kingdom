package Chap04;

/**
 * 括号检测
 */
public class BracketChecker {

    private String input;

    public BracketChecker(){}

    public BracketChecker(String input) {
        this.input = input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void check() {
        int len = input.length();
        MyStack<Character> stack = new MyStack<Character>(len);

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            switch (c) {
                case '(':
                case '[':
                case '{':
                    stack.push(c);
                    break;

                case ')':
                case ']':
                case '}':
                    if (!stack.isEmpty()) {
                        char chx = stack.pop();
                        if ( (chx != '{' && c == '}') ||
                                (chx != '[' && c == ']') ||
                                (chx != '(' && c == ')') ) {
                            System.out.println("Error: " + c + " at " + i);
                        }
                    } else {
                        System.out.println("Error: " + c + " at " + i);
                    }
                    break;

                default:
                    break;

            }
        }
        if (!stack.isEmpty()) {
            System.out.println("Error: missing right delimiter");
        }

    }
}
