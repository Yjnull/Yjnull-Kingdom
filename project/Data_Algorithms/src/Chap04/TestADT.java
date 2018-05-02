package Chap04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestADT {
    public static void main(String[] args) throws IOException {
        String input, output;
        BracketChecker bracketChecker = new BracketChecker();
        while (true) {
            //System.out.println("Enter string containing delimiters");
            System.out.println("Enter infix: ");
            System.out.flush();
            input = getInputString();
            if (input.equals("")) break;
            //bracketChecker.setInput(input);
            //bracketChecker.check();
            InToPost theTrans = new InToPost(input);
            output = theTrans.doTrans();

            System.out.println(new ParsePost(output).doParse());
        }
        /*PriorityQ q = new PriorityQ(10);
        q.insert(20);
        q.insert(50);
        q.insert(30);
        q.insert(10);
        q.insert(60);
        while (!q.isEmpty()) {
            System.out.print(q.remove() + " ");
        }*/
    }

    private static String getInputString() throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        return br.readLine();
    }
}
