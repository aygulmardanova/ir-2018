package com.kpfu.itis;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet4Address;
import java.util.*;

public class TfIdf {

    private static final double COEF_TITLE = 0.6;
    private static final double COEF_ABSTRACT = 0.4;

    public void calcTfIdf(String query, String type) throws ParserConfigurationException, SAXException, IOException {

        PorterParser porterParser = new PorterParser();
        MystemParser mystemParser = new MystemParser();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

//        read xml file with title & abstract
        Document readDoc = docBuilder.parse(new File(System.getProperty("user.dir") + "/" + IntersectSearch.INDEX_FILE_NAME));
        readDoc.normalizeDocument();


        String[] words = query.toLowerCase().split(" ");
        for (int i = 0; i < words.length; i++) {
            if ("porter".equals(type)) {
                words[i] = porterParser.stem(words[i]);
            } else if ("mystem".equals(type)) {
                words[i] = mystemParser.stem(words[i]);
            }
        }

        Set<Integer> docIds = new IntersectSearch().search(query, type);
        System.out.println();
        if (docIds.isEmpty()) {
            System.out.println("Для запроса \"" + query + "\" не найдено документов");
            return;
        }

        NodeList wordsNodeList = readDoc.getElementsByTagName("word");
        Map<Integer, Double> docScores = new TreeMap<>();

        docIds.forEach(docId -> Arrays.stream(words).forEach(word -> {

            for (int i = 0; i < wordsNodeList.getLength() - 1; i++) {
                if (word.equals(wordsNodeList.item(i).getAttributes().getNamedItem("word").getTextContent()) &&
                        type.equals(wordsNodeList.item(i).getParentNode().getNodeName())) {

                    for (int j = 0; j < wordsNodeList.item(i).getChildNodes().getLength(); j++) {
                        if (String.valueOf(docId).equals(wordsNodeList.item(i).getChildNodes().item(j).getTextContent().trim())) {

                            Double score = Double.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("score").getTextContent());

                            System.out.println("word: \"" + word + "\" | docId: " + docId + " | score: " + score);
                            if (docScores.containsKey(docId)) {
                                docScores.put(docId, docScores.get(docId) + score);
                            } else {
                                docScores.put(docId, score);
                            }
                            wordsNodeList.item(i);
                        }
                    }
                }

            }

        }));
        docScores.keySet().forEach(docId -> System.out.println(docId + " - " + docScores.get(docId)));

//        SOUT the result
        System.out.println("\nRelevant documents for query \"" + query + "\" (in asc order):");
        Map<Double, Integer> sortedDocIds = getSortedDocIds(docScores);
        sortedDocIds.keySet().forEach(docScore -> System.out.println(sortedDocIds.get(docScore) + " (" + docScore + ")"));

    }

    private Map<Double, Integer> getSortedDocIds(Map<Integer, Double> docScores) {
        Map<Double, Integer> docIds = new TreeMap<>();
        docScores.keySet().forEach(docId -> docIds.put(docScores.get(docId), docId));
        return docIds;
    }

    //    score = k(i) * F(i) = k(i) * tf (i) * idf (i); - для каждого слова для каждого документа
    public void writeWordScoreToXml() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

//        read xml file with title & abstract
        Document indexXmlFile = docBuilder.parse(new File(System.getProperty("user.dir") + "/" + IntersectSearch.INDEX_FILE_NAME));
        indexXmlFile.normalizeDocument();

        Double wordScore;
        NodeList wordsNodeList = indexXmlFile.getElementsByTagName("word");
        NodeList titlesNodes = indexXmlFile.getElementsByTagName("title");

        System.out.println("words count - " + wordsNodeList.getLength());
        System.out.println("titles count - " + titlesNodes.getLength());

//        the last titleNode is related to title texts (original / porter / mystem)
        for (int i = 0; i < wordsNodeList.getLength() - 1; i++) {
            System.out.println(wordsNodeList.item(i).getAttributes().getNamedItem("word").getTextContent() + " - docs count: " + wordsNodeList.item(i).getAttributes().getNamedItem("count").getTextContent());
            for (int j = 0; j < wordsNodeList.item(i).getChildNodes().getLength(); j++) {
                if (!"".equals(wordsNodeList.item(i).getChildNodes().item(j).getTextContent().trim())) {

                    Integer docId = Integer.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getTextContent().trim().replaceAll("\n", " "));
                    System.out.print("docId - " + docId + "; ");
                    System.out.println("occurs = " + wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("occurs").getTextContent());
                    Integer wordOccursForDoc = Integer.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("occurs").getTextContent());
                    Integer totalWordsCountForDoc = Integer.valueOf(titlesNodes.item(docId).getAttributes().getNamedItem("words-count").getTextContent());
                    Integer titlesCount = titlesNodes.getLength();
                    Double titlesCountForWord = Double.valueOf(wordsNodeList.item(i).getAttributes().getNamedItem("count").getTextContent());
                    System.out.println(wordOccursForDoc + " " + totalWordsCountForDoc + " " + titlesCount + " " + titlesCountForWord + "; " + get2Log(titlesCount / titlesCountForWord));

                    Double tf = Double.valueOf(wordOccursForDoc) / Double.valueOf(totalWordsCountForDoc);
                    Double idf = get2Log(titlesCount / titlesCountForWord);
                    System.out.println("tf = " + tf + "; idf = " + idf);
                    wordScore = tf * idf;

                    String textType = wordsNodeList.item(i).getParentNode().getParentNode().getNodeName();
                    System.out.println("word score = " + wordScore);
                    if (textType.contains("title"))
                        wordScore *= COEF_TITLE;
                    else if (textType.contains("abstract"))
                        wordScore *= COEF_ABSTRACT;
                    else
                        return;
                    System.out.println("word score = " + wordScore);

                    wordScore = new BigDecimal(wordScore).setScale(4, RoundingMode.HALF_UP).doubleValue();
                    ((Element) wordsNodeList.item(i).getChildNodes().item(j)).setAttribute("score", String.valueOf(wordScore));
                    System.out.println("word score = " + wordScore);
                }
            }
            System.out.println("\n");

        }

        indexXmlFile.normalizeDocument();
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(indexXmlFile);
            StreamResult result = new StreamResult(new File(System.getProperty("user.dir") + "/" + IntersectSearch.INDEX_FILE_NAME));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private Double get2Log(Double value) {
        return Math.log(value) / Math.log(2.0);
    }
}
