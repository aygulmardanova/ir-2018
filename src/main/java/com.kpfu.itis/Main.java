package com.kpfu.itis;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {

    public static final String query1 = "-это -ядро функция";
    public static final String query2 = "-это -ядро функция неслово";
    public static final String query3 = "-это эксперимент -ядро функция";
    public static final String query4 = "это эллиптический -эксперимент -ядро";
    public static final String query5 = "уравнение функция";
    public static final String query6 = "это функция";
    public static final String query7 = "численный метод";
    public static final String query8 = "задача и исследовать";
    public static final String query9 = "задача и";
    public static void main(String[] args) throws IOException, InterruptedException, ParserConfigurationException, SAXException {

//        new Parse().parse();
//        new InvertedIndex().createIndex();
//        new IntersectSearch().search(query1, "porter");
//        new IntersectSearch().search(query2, "mystem");
//        new IntersectSearch().search(query3, "mystem");
//        new IntersectSearch().search(query4, "porter");
//        new TfIdf().writeWordScoreToXml();
//        new TfIdf().calcTfIdf(query5, "porter");
//        new TfIdf().calcTfIdf(query6, "porter");
        new LSI().calcLSI(Main.query9);
    }
}