package com.kpfu.itis;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.text.SimpleDateFormat;
import java.util.List;

public class Parse {

    private static final String START_URL = "http://m.mathnet.ru/php/archive.phtml?jrnid=uzku&wshow=issue&bshow=contents&series=0&year=2016&volume=158%22%20%5C%20%22&issue=1&option_lang=rus&bookID=1621";
    private static final String HOME_URL = "http://m.mathnet.ru";
    private static final String GET_ALL_LINKS = "//td[@width='90%']/a[@class='SLink']";
    private static final String GET_ABSTRACT = "//b[contains(text(),'Аннотация')]/following::text()[preceding::b[1][contains(text(),'Аннотация')] and not(parent::b)]";
    private static final String GET_KEYWORDS = "//b[contains(text(), 'Ключевые')]/following-sibling::i[1]//text()";
    private static final String UNKNOWN_SYMBOLS_REGEX = "[^А-Яа-я\\s]";
    private static PorterParser porterParser;
    private static MystemParser mystemParser;

    public void parse() throws IOException, InterruptedException {
        porterParser = new PorterParser();
        mystemParser = new MystemParser();

        WebClient webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage startPage = webClient.getPage(START_URL);

        List<HtmlAnchor> links = startPage.getByXPath(GET_ALL_LINKS);

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            docFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("issue");
            root.setAttribute("title", startPage.getTitleText());
            root.setAttribute("href", START_URL);
            doc.appendChild(root);

            Element articlesElem = doc.createElement("articles");
            root.appendChild(articlesElem);

            for (HtmlAnchor link : links) {
                String pageUrl = HOME_URL + link.getHrefAttribute();
                HtmlPage parsePage = webClient.getPage(pageUrl);

                Element articleElem = doc.createElement("article");
                articleElem.setAttribute("href", pageUrl);

                Element title = doc.createElement("title");
                title.appendChild(doc.createTextNode(link.getTextContent()));
                articleElem.appendChild(title);

                Element titlePorterElem = doc.createElement("title-porter");
                titlePorterElem.appendChild(doc.createTextNode(getPorterString(link.getTextContent())));
                articleElem.appendChild(titlePorterElem);

                Element titleMystemElem = doc.createElement("title-mystem");
                titleMystemElem.appendChild(doc.createTextNode(getMystemString(link.getTextContent())));
                articleElem.appendChild(titleMystemElem);

                StringBuilder abstractWords = new StringBuilder();
                List<Object> abstractList = parsePage.getByXPath(GET_ABSTRACT);
                for (Object abstractItem : abstractList) {
                    abstractWords.append(abstractItem.toString());
                }

                Element abstractElem = doc.createElement("abstract");
                abstractElem.appendChild(doc.createTextNode(abstractWords.toString()));
                articleElem.appendChild(abstractElem);

                Element abstractPorterElem = doc.createElement("abstract-porter");
                abstractPorterElem.appendChild(doc.createTextNode(getPorterString(abstractWords.toString())));
                articleElem.appendChild(abstractPorterElem);

                Element abstractMystemElem = doc.createElement("abstract-mystem");
                abstractMystemElem.appendChild(doc.createTextNode(getMystemString(abstractWords.toString())));
                articleElem.appendChild(abstractMystemElem);

                StringBuilder keyWords = new StringBuilder();
                List<Object> keywordsList = parsePage.getByXPath(GET_KEYWORDS);
                for (Object keywordItem : keywordsList) {
                    keyWords.append(keywordItem.toString());
                }

                Element keywordsElem = doc.createElement("keywords");
                String[] keywords = keyWords.toString().split(", |,");
                // remove last '.' symbol for the last keyword
                if (keywords[keywords.length - 1].endsWith(".")) {
                    keywords[keywords.length - 1] = keywords[keywords.length - 1].substring(0, keywords[keywords.length - 1].length() - 1);
                }
                for (String keyword : keywords) {
                    Element keyWord = doc.createElement("keyword");
                    keyWord.appendChild(doc.createTextNode(keyword));
                    keywordsElem.appendChild(keyWord);
                }
                // to write all key words in one tag
//                keywordsElem.appendChild(doc.createTextNode(keyWords.toString()));
                articleElem.appendChild(keywordsElem);

                articlesElem.appendChild(articleElem);
                Thread.sleep(1000);
            }

            doc.normalizeDocument();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(new File(getXmlFileName()));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

        } catch (ParserConfigurationException | TransformerException exc) {
            exc.printStackTrace();
        }

        webClient.close();
    }

    private String getXmlFileName() {
        SimpleDateFormat time_formatter = new SimpleDateFormat("yyyy-MM-dd-_HH:mm:ss");
        String current_time_str = time_formatter.format(System.currentTimeMillis());
        return System.getProperty("user.dir") + "/" + current_time_str.replaceAll(":", "_") + ".xml";
    }

    private String getPorterString(String str) {
        String[] porterWordsArray = str.replaceAll(UNKNOWN_SYMBOLS_REGEX, "").split("\\s+");
        StringBuilder porterWords = new StringBuilder();
        for (String word : porterWordsArray) {
            porterWords.append(porterParser.stem(word)).append(" ");
        }
        return porterWords.toString();
    }

    private String getMystemString(String str) throws IOException, InterruptedException {
        StringBuilder mystemWords = new StringBuilder();
        String[] mystemWordsArray = str.replaceAll(UNKNOWN_SYMBOLS_REGEX, "").split("\\s+");
        for (String word : mystemWordsArray) {
            mystemWords.append(mystemParser.stem(word)).append(" ");
        }
        return mystemWords.toString();
    }

}