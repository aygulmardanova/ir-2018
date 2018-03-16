package com.kpfu.itis;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class InvertedIndex {

    private final String xmlFileName = "2018-02-28-_01_05_07.xml";
    private final String prefix = "index_";

    public void createIndex() throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

//        read xml file with title & abstract
        Document readDoc = docBuilder.parse(new File(System.getProperty("user.dir") + "/" + xmlFileName));
        readDoc.normalizeDocument();

//        builder for result xml-file
        Document writeDoc = docBuilder.newDocument();
        Element root = writeDoc.createElement("words");
        root.setAttribute("fileName", xmlFileName);
        writeDoc.appendChild(root);

//        write titles list
        NodeList titlesNodes = readDoc.getElementsByTagName("title");
        Element titles = writeDoc.createElement("titles");
        root.appendChild(titles);
        for (int i = 0; i < titlesNodes.getLength(); i++) {
            Element title = writeDoc.createElement("title");
            title.setAttribute("id", String.valueOf(i));
            title.setTextContent(titlesNodes.item(i).getTextContent().trim().replaceAll("\n", ""));
            titles.appendChild(title);
        }

//        root elem for title
        Element titleElem = writeDoc.createElement("title");
        root.appendChild(titleElem);

//        write porter title words
        Element originalTitle = writeDoc.createElement("original");
        writeWords(readDoc, writeDoc, "title", originalTitle);
        titleElem.appendChild(originalTitle);

//        write porter title words
        Element porterTitle = writeDoc.createElement("porter");
        writeWords(readDoc, writeDoc, "title-porter", porterTitle);
        titleElem.appendChild(porterTitle);

//        write mystem title words
        Element mystemTitle = writeDoc.createElement("mystem");
        writeWords(readDoc, writeDoc, "title-mystem", mystemTitle);
        titleElem.appendChild(mystemTitle);

//        root elem for abstract
        Element abstractElem = writeDoc.createElement("abstract");
        root.appendChild(abstractElem);

//        write porter abstract words
        Element originalAbstract = writeDoc.createElement("original");
        writeWords(readDoc, writeDoc, "abstract", originalAbstract);
        abstractElem.appendChild(originalAbstract);

//        write porter abstract words
        Element porterAbstract = writeDoc.createElement("porter");
        writeWords(readDoc, writeDoc, "abstract-porter", porterAbstract);
        abstractElem.appendChild(porterAbstract);

//        write mystem abstract words
        Element mystemAbstract = writeDoc.createElement("mystem");
        writeWords(readDoc, writeDoc, "abstract-mystem", mystemAbstract);
        abstractElem.appendChild(mystemAbstract);

        writeDoc.normalizeDocument();
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(writeDoc);
            StreamResult result = new StreamResult(new File(System.getProperty("user.dir") + "/" + prefix + xmlFileName));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private void writeWords(Document readDoc, Document writeDoc, String type, Element curRootElem) {
        NodeList typeNodes = readDoc.getElementsByTagName(type);
        TreeMap<String, Set<Integer>> map = new TreeMap<>();
        for (int i = 0; i < typeNodes.getLength(); i++) {
            appendWords(typeNodes.item(i).getTextContent().trim().toLowerCase().replaceAll("[(),.]", ""), map, i);
        }
        for (String word : map.keySet()) {
            Element wordElem = writeDoc.createElement("word");
            wordElem.setAttribute("word", word);
            wordElem.setAttribute("count", String.valueOf(map.get(word).size()));
            for (Integer docId : map.get(word)) {
                Element docIdElem = writeDoc.createElement("doc");
                docIdElem.setTextContent(String.valueOf(docId));
                wordElem.appendChild(docIdElem);
            }
            curRootElem.appendChild(wordElem);
        }
    }

    private void appendWords(String text, TreeMap<String, Set<Integer>> map, int docId) {
        String[] words = text.split("[-–\\u00A0\\s]+");
        for (String word : words) {
            if (map.containsKey(word)) {
                map.get(word).add(docId);
            } else {
                Set<Integer> set = new HashSet<>();
                set.add(docId);
                map.put(word, set);
            }
        }
    }

}
