package kunj.com.app;

import kunj.com.analyze.ClassAnalyzer;

public class Test {
    static{
        ClassAnalyzer.inject();
    }
    public static void main(String[] args) {
        System.out.println("this is kunJ framework.");
    }
}
