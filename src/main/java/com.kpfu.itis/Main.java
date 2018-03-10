package com.kpfu.itis;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, ParserConfigurationException, SAXException {

//        new Parse().parse();
        new InvertedIndex().createIndex();

    }
}