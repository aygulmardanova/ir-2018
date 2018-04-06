package com.kpfu.itis;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class LSI {

    private final int MAX_WORD_SIZE = 20;

    void calcLSI(String query) throws IOException, SAXException, ParserConfigurationException {
        String type = "mystem";
        String text = "abstract";
        int k = 2;
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
        double[][] scores = new double[wordsList.size()][titlesList.size()];
        for (int i = wordsStartsFrom; i < wordsEndsBy; i++) {
            if (type.equals(wordsNodeList.item(i).getParentNode().getNodeName()) &&
                    text.equals(wordsNodeList.item(i).getParentNode().getParentNode().getNodeName())) {
//                System.out.println("word: \"" + wordsNodeList.item(i).getAttributes().getNamedItem("word").getNodeValue() + "\"");
                for (int j = 0; j < wordsNodeList.item(i).getChildNodes().getLength(); j++) {
                    if (!"".equals(wordsNodeList.item(i).getChildNodes().item(j).getTextContent().trim())) {
                        Double score = Double.valueOf(wordsNodeList.item(i).getChildNodes().item(j).getAttributes().getNamedItem("score").getTextContent());
                        scores[count][Integer.parseInt(wordsNodeList.item(i).getChildNodes().item(j).getTextContent())] = score;
//                        System.out.println("doc = " + wordsNodeList.item(i).getChildNodes().item(j).getTextContent() + ", score = " + score);
                    }
                }
                count++;
            }
        }

        Matrix matrix = Matrix.constructWithCopy(scores);
        SingularValueDecomposition svd = matrix.svd();

        Matrix U = svd.getU();
        Matrix S = svd.getS();
        Matrix V = svd.getV().transpose();

        printMatrix(U, "---U---");
        printMatrix(S, "---S---");
        printMatrix(V, "---V---");

        /*System.out.println();
        System.out.println("U: " + U.getRowDimension() + ", " + U.getColumnDimension());
        System.out.println("S: " + S.getRowDimension() + ", " + S.getColumnDimension());
        System.out.println("V: " + V.getRowDimension() + ", " + V.getColumnDimension());
        System.out.println();*/

//        U - k cols
//        S - k cols * k rows
//        V - k cols -> V' - k rows
        Matrix Uk = U.getMatrix(0, U.getRowDimension() - 1, 0, k - 1);
        Matrix Sk = S.getMatrix(0, k - 1, 0, k - 1);
        Matrix Vk = V.getMatrix(0, k - 1, 0, V.getColumnDimension() - 1);

        printMatrix(Uk, "---U_" + k + "---");
        printMatrix(Sk, "---S_" + k + "---");
        printMatrix(Vk, "---V_" + k + "---");

        Map<String, Double> tfIdfs = new TfIdf().getTfIdfForQuery(query, type, text);
        tfIdfs.keySet().forEach(key -> System.out.println(key + ": = " + tfIdfs.get(key)));

        double[] q = getQArray(wordsList, tfIdfs);

        Matrix qMatrix = Matrix.constructWithCopy(new double[][]{q}).times(Uk).times(Sk.inverse());
        printMatrix(qMatrix, "---qMatrix---");

        Matrix dMatrix = Sk.inverse().times(Vk);
        printMatrix(dMatrix, "---dMatrix---");

        Set<Integer> docIdsForQuery = new IntersectSearch().search(query, type);
        Map<Integer, Double> docScores = new TreeMap<>();
        for (int i = 0; i < Vk.getColumnDimension(); i++) {
            if (docIdsForQuery.contains(i))
                docScores.put(i, calcSim(qMatrix.getArray(), Vk.transpose().getArray()[i]));
            System.out.println("Sim(\"" + query + "\", doc" + docIds[i] + ") = " + String.format("%.3f", calcSim(qMatrix.getArray(), Vk.transpose().getArray()[i])));
        }
        docScores.keySet().stream()
                .filter(docIdsForQuery::contains);
        docScores = docScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        System.out.println("\nRESULT OUTPUT");
//        printScoresMatrix(words, docIds, scores);
//        writeScoresMatrixToFile(words, docIds, scores);
        System.out.println("Docs sim in DESC mode");
        Map<Integer, Double> finalDocScores = docScores;
        docScores.keySet().forEach(docScore -> System.out.println("Sim(\"" + query + "\", doc" + docScore + ") = " + String.format("%.3f", finalDocScores.get(docScore))));
        writeDocsSimsToFile(query, words, docIds, docScores);
    }

    private void printMatrix(Matrix matrix, String name) {

        System.out.println(name);
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                System.out.print(String.format("%.3f", matrix.get(i, j)) + " ");
            }
            System.out.println();
        }
    }

    private void printScoresMatrix(String[] words, Integer[] docIds, double[][] scores) {
        System.out.println("Scores matrix (Matrix A)");
        System.out.print("                      ");
        for (Integer docId : docIds)
            System.out.print(docId + "     ");
        System.out.println();
        for (int i = 0; i < words.length; i++) {
            StringBuilder wordBuilder = new StringBuilder(words[i]);
            if (wordBuilder.length() < MAX_WORD_SIZE) {
                for (int l = wordBuilder.length(); l < MAX_WORD_SIZE; l++)
                    wordBuilder.append(" ");
            }
            System.out.print(wordBuilder.toString() + " ");
            for (int j = 0; j < docIds.length; j++) {
                System.out.print(String.format("%.3f", scores[i][j]) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private void writeScoresMatrixToFile(String[] words, Integer[] docIds, double[][] scores) throws IOException {

        FileWriter fileWriter = new FileWriter(System.getProperty("user.dir") + "/" + "a_matrix.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);

        printWriter.println("Scores matrix (Matrix A)");
        printWriter.print("                      ");
        for (Integer docId : docIds)
            printWriter.print(docId + "     ");
        printWriter.println();
        for (int i = 0; i < words.length; i++) {
            StringBuilder wordBuilder = new StringBuilder(words[i]);
            if (wordBuilder.length() < MAX_WORD_SIZE) {
                for (int l = wordBuilder.length(); l < MAX_WORD_SIZE; l++)
                    wordBuilder.append(" ");
            }
            printWriter.print(wordBuilder.toString() + " ");
            for (int j = 0; j < docIds.length; j++) {
                printWriter.print(String.format("%.3f", scores[i][j]) + " ");
            }
            printWriter.println();
        }

        printWriter.close();
    }

    private void writeDocsSimsToFile(String query, String[] words, Integer[] docIds, Map<Integer, Double> docScores) throws IOException {
        String fileName = "sim_" + query.replaceAll(" ", "_") + ".txt";
        FileWriter fileWriter = new FileWriter(System.getProperty("user.dir") + "/" + fileName);
        PrintWriter printWriter = new PrintWriter(fileWriter);

        printWriter.println("Docs sim in DESC mode for query \"" + query + "\":\n");
        docScores.keySet().forEach(docScore -> printWriter.println("Sim(\"" + query + "\", doc" + docScore + ") = " + String.format("%.3f", docScores.get(docScore))));

        printWriter.close();
    }

    private double[] getQArray(List<String> words, Map<String, Double> map) {
        double[] arr = new double[words.size()];
        for (int i = 0; i < words.size(); i++) {
            if (map.containsKey(words.get(i)))
                arr[i] = map.get(words.get(i));
            else
                arr[i] = 0;
        }
        return arr;
    }

    private double calcSim(double[][] query, double[] doc) {
        double queryLength = 0.0;
        double docsLength = 0.0;
        double scalMult = 0.0;

        for (int i = 0; i < doc.length; i++) {
            scalMult += query[0][i] * doc[i];
            queryLength += Math.pow(query[0][i], 2);
            docsLength += Math.pow(doc[i], 2);
        }
        return scalMult / ((Math.sqrt(queryLength)) * (Math.sqrt(docsLength)));
    }
}