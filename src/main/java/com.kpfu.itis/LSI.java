package com.kpfu.itis;

import Jama.Matrix;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LSI {

    static void calcLSI() throws IOException, SAXException, ParserConfigurationException {
        String type = "mystem";
        String text = "abstract";
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

//        read xml file with title & abstract
        Document readDoc = docBuilder.parse(new File(System.getProperty("user.dir") + "/" + IntersectSearch.INDEX_FILE_NAME));
        readDoc.normalizeDocument();

        String[] words;
        Integer[] docIds;
        int wordsStartsFrom = -1;
        int wordsEndsBy = -1;
        List<String> wordsList = new ArrayList<>();
        List<Integer> titlesList = new ArrayList<>();
        NodeList wordsNodeList = readDoc.getElementsByTagName("word");
        NodeList titlesNodeList = readDoc.getElementsByTagName("title");

        for (int i = 0; i < titlesNodeList.getLength(); i++) {
            if (titlesNodeList.item(i).getParentNode().getLocalName().equals("titles"))
                titlesList.add(Integer.valueOf(titlesNodeList.item(i).getAttributes().getNamedItem("id").getNodeValue()));
        }
        docIds = titlesList.toArray(new Integer[titlesList.size()]);


        for (int i = 0; i < wordsNodeList.getLength(); i++) {
            if (type.equals(wordsNodeList.item(i).getParentNode().getNodeName()) &&
                    text.equals(wordsNodeList.item(i).getParentNode().getParentNode().getNodeName())) {
                if (wordsStartsFrom < 0)
                    wordsStartsFrom = i;

                wordsList.add(wordsNodeList.item(i).getAttributes().getNamedItem("word").getTextContent());
                wordsEndsBy = i;
            }
        }
        words = wordsList.toArray(new String[wordsList.size()]);

        int count = 0;
        System.out.println("LENGTH: " + words.length);
        System.out.println(wordsStartsFrom + " --- " + wordsEndsBy);
        double[][] scores = new double[wordsList.size()][titlesList.size()];
        for (int i = wordsStartsFrom; i < wordsEndsBy; i++) {
            if (type.equals(wordsNodeList.item(i).getParentNode().getNodeName()) &&
                    text.equals(wordsNodeList.item(i).getParentNode().getParentNode().getNodeName())) {
                System.out.println("word: \"" + wordsNodeList.item(i).getAttributes().getNamedItem("word").getNodeValue() + "\"");
                for (int j = 0; j < wordsNodeList.item(i).getChildNodes().getLength(); j++) {
                    if (!"".equals(wordsNodeList.item(i).getChildNodes().item(j).getTextContent().trim())) {
                        Double score = Double.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("score").getTextContent());
                        scores[count][Integer.parseInt(wordsNodeList.item(i).getChildNodes().item(j).getTextContent())] = score;
                        System.out.println("doc = " + wordsNodeList.item(i).getChildNodes().item(j).getTextContent() + ", score = " + score);
                    }
                }
                System.out.println("count: " + count);
                count++;
            }
        }

        printDoubleArray(words, docIds, scores);

        Matrix matrix = new Matrix(scores);



    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

        LSI.calcLSI();

    }

    static void printDoubleArray(String[] words, Integer[] docIds, double[][] scores) {
        System.out.print("--------");
        for (int i = 0; i < docIds.length; i++)
            System.out.print(docIds[i] + "   ");
        System.out.println();
        for (int i = 0; i < words.length; i++) {
            System.out.print(words[i] + " ");
            for (int j = 0; j < docIds.length; j++) {
                System.out.print(scores[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

}
