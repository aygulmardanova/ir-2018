package com.kpfu.itis;

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
            if (type.equals(wordsNodeList.item(i).getParentNode().getNodeName())) {
                if (wordsStartsFrom < 0)
                    wordsStartsFrom = i;
//                System.out.println(wordsNodeList.item(i).getChildNodes().item(0).getAttributes().getNamedItem("score").getTextContent());
                wordsList.add(wordsNodeList.item(i).getAttributes().getNamedItem("word").getTextContent());
            }
        }
        words = wordsList.toArray(new String[wordsList.size()]);
        System.out.println(wordsStartsFrom + "!!!!");
        double[][] scores = new double[wordsList.size()][titlesList.size()];
        for (int i = wordsStartsFrom; i < wordsStartsFrom + words.length; i++) {
            if (type.equals(wordsNodeList.item(i).getParentNode().getNodeName())) {
                System.out.println(i - wordsStartsFrom + " . ");
//                for (int j = 0; j < wordsNodeList.item(i).getChildNodes().getLength(); j++) {
//
//                    Double score = Double.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("score").getTextContent());
////                    scores[i][Integer.parseInt(wordsNodeList.item(i).getChildNodes().item(j).getTextContent())] = score;
//                    System.out.println(wordsNodeList.item(i).getChildNodes().item(j).getTextContent() + " - score");
//
//                }
//
            }
        }

    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

        LSI.calcLSI();

    }

}
