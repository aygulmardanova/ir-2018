package com.kpfu.itis;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class IntersectSearch {

    private static final String INDEX_FILE_NAME = InvertedIndex.PREFIX + InvertedIndex.XML_FILE_NAME;
    private static final String TEXT_CONTENT_REMOVE_EMPTIES_REGEXP = "[\n\t\\s]+";

    public Set<Integer> search(String sentence, String type) throws IOException, ParserConfigurationException, SAXException {

        PorterParser porterParser = new PorterParser();
        MystemParser mystemParser = new MystemParser();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();

//        read xml file with title & abstract
        Document readDoc = docBuilder.parse(new File(System.getProperty("user.dir") + "/" + INDEX_FILE_NAME));
        readDoc.normalizeDocument();

        NodeList wordsNodeList = readDoc.getElementsByTagName("word");

//        parsing
        String[] words = sentence.toLowerCase().split("\\s+");

        Map<String, Set<Integer>> map = new TreeMap<>();
        Set<Integer> allDocIds = new HashSet<>();
        NodeList titles = readDoc.getElementsByTagName("title");
        for (int i = 0; i < titles.getLength(); i++) {
            if (titles.item(i).getParentNode().getLocalName().equals("titles"))
                allDocIds.add(Integer.valueOf(titles.item(i).getAttributes().getNamedItem("id").getNodeValue()));
        }

        for (String word : words) {

            Set<Integer> docIds = new TreeSet<>();
            String searchWord;
            switch (type) {
                case "mystem":
                    searchWord = mystemParser.stem(word);
                    break;
                case "porter":
                    if (word.startsWith("-"))
                        searchWord = porterParser.stem(word.replace("-", ""));
                    else
                        searchWord = porterParser.stem(word);
                    break;
                default:
                    return null;
            }

            String[] strIds;
            for (int i = 0; i < wordsNodeList.getLength(); i++) {
                if (wordsNodeList.item(i).getParentNode().getLocalName().equals(type) &&
                        (wordsNodeList.item(i).getParentNode().getParentNode().getLocalName().equals("title") ||
                                wordsNodeList.item(i).getParentNode().getParentNode().getLocalName().equals("abstract")) &&
                        searchWord.equals(wordsNodeList.item(i).getAttributes().getNamedItem("word").getNodeValue())) {

                    strIds = wordsNodeList.item(i).getTextContent().trim().replaceAll(TEXT_CONTENT_REMOVE_EMPTIES_REGEXP, " ").split("\\s");
                    Arrays.stream(strIds).forEach(strId -> docIds.add(Integer.valueOf(strId)));

                }
            }
            if (word.startsWith("-"))
                map.put(word, getInversion(docIds, allDocIds));
            else
                map.put(word, docIds);
        }

        Set<Integer> result = new TreeSet<>();
        List<String> wordsList = new ArrayList<>(map.keySet());
        for (int i = 0; i < wordsList.size() - 1; i++) {
            if (!(result.isEmpty() && i != 0))
                result = getIntersection(new TreeSet<>(map.get(wordsList.get(i))), new TreeSet<>(map.get(wordsList.get(i + 1))));
        }

//      Print the results
        System.out.println("Documents for input \"" + sentence + "\":");
        System.out.println("count: " + result.size());
        result.forEach(integer -> System.out.print(integer + " "));
        System.out.println("\n");
        System.out.println("Output for words:");
        map.keySet().forEach(key -> System.out.println(key + " = " + map.get(key)));

        return result;
    }

    private Set<Integer> getIntersection(Set<Integer> res, Set<Integer> set) {
        if (res == null || set == null)
            return res;

        res.retainAll(set);
        return res;
    }

    private Set<Integer> getInversion(Set<Integer> part, Set<Integer> full) {
        Set<Integer> res = new TreeSet<>();
        for (Integer in : full) {
            if (!part.contains(in))
                res.add(in);
        }
        return res;
    }
}