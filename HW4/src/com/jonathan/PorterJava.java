package com.jonathan;

public class PorterJava {
    public static void main(String[] argv)
    {
        String word = "running" ;
        String stem = PorterStemmer.stem(word);
        System.out.println(word + " -> " + stem);
    }

}
