/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.cuni.mff.hurkovalu.preprocessing;

import cz.cuni.mff.hurkovalu.publication_search.Author;
import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.time.Year;

/**
 *
 * @author Lucie Hurkova
 */
public class PubMedHandler  extends DefaultHandler {
    private static final String PUBMED_ARTICLE = "PubmedArticle";
    private static final String MEDLINE_CITATION = "MedlineCitation";
    private static final String ARTICLE = "Article";
    private static final String JOURNAL = "Journal";
    private static final String JOURNAL_ISSUE = "JournalIssue";
    private static final String TITLE = "Title";
    private static final String PUB_DATE = "PubDate";
    private static final String YEAR = "Year";
    private static final String ARTICLE_TITLE = "ArticleTitle";
    private static final String ABSTRACT = "Abstract";
    private static final String AUTHOR_LIST = "AuthorList";
    private static final String AUTHOR = "Author";
    private static final String LAST_NAME = "LastName";
    private static final String FORE_NAME = "ForeName";
    private static final String PUBMED_DATA = "PubmedData";
    private static final String ARTICLE_ID_LIST = "ArticleIdList";
    private static final String ARTICLE_ID = "ArticleId";
    private static final String REFERENCE_LIST = "ReferenceList";
    private static final String REFERENCE = "Reference";
    private static final String PUBMED = "pubmed";
    private static final String DOI = "doi";
    
    private List<String> hierarchy;
    private Publication currPub;
    private int refCount = 0;
    private String currLastName;
    private String[] currForeNames;
    private String currIdType;
    
    int minRef = Integer.MAX_VALUE;
    int maxRef = 0;
    int minCite = Integer.MAX_VALUE;
    int maxCite = 0;
    int minAge = Integer.MAX_VALUE;
    int maxAge = 0;
    int currYear = Year.now().getValue();
    
    private Map<Integer, Integer> citations = new HashMap<>();
    
    private StringBuilder data;
    
    private List<Publication> publications = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case PUBMED_ARTICLE:
                if (currPub == null && hierarchy == null) {
                    currPub = new Publication();
                    hierarchy = new LinkedList<>();
                }
                break;
            case MEDLINE_CITATION:
                if (hierarchy.isEmpty()) {
                    hierarchy.add(MEDLINE_CITATION);
                }
                break;
            case ARTICLE:
                if (MEDLINE_CITATION.equals(hierarchy.getLast())) {
                    hierarchy.add(ARTICLE);
                }
                break;
            case JOURNAL:
                if (ARTICLE.equals(hierarchy.getLast())) {
                    hierarchy.add(JOURNAL);
                }
                break;
            case JOURNAL_ISSUE:
                if (JOURNAL.equals(hierarchy.getLast())) {
                    hierarchy.add(JOURNAL_ISSUE);
                }
                break;
            case PUB_DATE:
                if (JOURNAL_ISSUE.equals(hierarchy.getLast())) {
                    hierarchy.add(PUB_DATE);
                }
                break;
            case AUTHOR_LIST:
                if (ARTICLE.equals(hierarchy.getLast())) {
                    hierarchy.add(AUTHOR_LIST);
                }
                break;
            case AUTHOR:
                if (AUTHOR_LIST.equals(hierarchy.getLast())) {
                    hierarchy.add(AUTHOR);
                }
                break;
            case PUBMED_DATA:
                if (hierarchy.isEmpty()) {
                    hierarchy.add(PUBMED_DATA);
                }
                break;
            case ARTICLE_ID_LIST:
                if (PUBMED_DATA.equals(hierarchy.getLast())
                        || REFERENCE.equals(hierarchy.getLast())) {
                    hierarchy.add(ARTICLE_ID_LIST);
                }
                break;
            case ARTICLE_ID:
                if (ARTICLE_ID_LIST.equals(hierarchy.getLast())) {
                    currIdType = attributes.getValue("IdType");
                }
                break;
            case REFERENCE_LIST:
                if (PUBMED_DATA.equals(hierarchy.getLast())) {
                    hierarchy.add(REFERENCE_LIST);
                }
                break;
            case REFERENCE:
                if (REFERENCE_LIST.equals(hierarchy.getLast())) {
                    hierarchy.add(REFERENCE);
                }
                break;
        }
        
        data = new StringBuilder();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String last_element = null;
        if (hierarchy != null && !hierarchy.isEmpty()) {
            last_element = hierarchy.getLast();
        }
        switch (qName) {
            case PUBMED_ARTICLE:
                if (currPub.isComplete()) {
                    publications.add(currPub);
                    maxRef = Integer.max(maxRef, currPub.getReferences());
                    minRef = Integer.min(minRef, currPub.getReferences());
                    maxAge = Integer.max(maxAge, currPub.getAge());
                    minAge = Integer.min(minAge, currPub.getAge());
                }
                currPub = null;
                hierarchy = null;
                break;
            case MEDLINE_CITATION:
                if (MEDLINE_CITATION.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+MEDLINE_CITATION+", got: "+last_element);
                }
                break;
            case ARTICLE:
                if (ARTICLE.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+ARTICLE+", got: "+last_element);
                }
                break;
            case JOURNAL:
                if (JOURNAL.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+JOURNAL+", got: "+last_element);
                }
                break;
            case JOURNAL_ISSUE:
                if (JOURNAL_ISSUE.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+JOURNAL_ISSUE+", got: "+last_element);
                }
                break;
            case PUB_DATE:
                if (PUB_DATE.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+PUB_DATE+", got: "+last_element);
                }
                break;
            case YEAR:
                if (PUB_DATE.equals(last_element)) {
                    int year = Integer.parseInt(data.toString());
                    currPub.setYear(year);
                }
                break;
            case TITLE:
                if (JOURNAL.equals(last_element)) {
                    currPub.setJournal(data.toString());
                }
                break;
            case ARTICLE_TITLE:
                if (ARTICLE.equals(last_element)) {
                    currPub.setTitle(data.toString());
                }
                break;
            case AUTHOR_LIST:
                if (AUTHOR_LIST.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+AUTHOR_LIST+", got: "+last_element);
                }
                break;
            case AUTHOR:
                if (AUTHOR.equals(last_element)) {
                    currPub.addAuthor(new Author(currLastName, currForeNames));
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+AUTHOR+", got: "+last_element);
                }
                break;
            case LAST_NAME:
                if (AUTHOR.equals(last_element)) {
                    currLastName = data.toString();
                }
                break;
            case FORE_NAME:
                if (AUTHOR.equals(last_element)) {
                    currForeNames = data.toString().split(" ");
                }
                break;
            case ABSTRACT:
                if (ARTICLE.equals(last_element)) {
                    currPub.setPubAbstract(data.toString());
                }
                break;
            case PUBMED_DATA:
                if (PUBMED_DATA.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+PUBMED_DATA+", got: "+last_element);
                }
                break;
            case ARTICLE_ID_LIST:
                if (ARTICLE_ID_LIST.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+ARTICLE_ID_LIST+", got: "+last_element);
                }
                break;
            case ARTICLE_ID:
                if (ARTICLE_ID_LIST.equals(last_element)) {
                    if (REFERENCE.equals(hierarchy.get(hierarchy.size() - 2))) {
                        refCount++;
                        if (PUBMED.equals(currIdType)) {
                            int id = Integer.parseInt(data.toString());
                            Integer citeCount = citations.get(id);
                            if (citeCount == null) {
                                citations.put(id, 1);
                            } else {
                                citations.put(id, citeCount+1);
                            }
                        }
                    } else {
                        switch (currIdType) {
                            case PUBMED:
                                int id = Integer.parseInt(data.toString());
                                currPub.setId(id);
                                break;
                            case DOI:
                                currPub.setDoi(data.toString());
                                break;
                        }
                    }
                }
                break;
            case REFERENCE_LIST:
                if (REFERENCE_LIST.equals(last_element)) {
                    currPub.setReferences(refCount);
                    refCount = 0;
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+REFERENCE_LIST+", got: "+last_element);
                }
                break;
            case REFERENCE:
                if (REFERENCE.equals(last_element)) {
                    hierarchy.removeLast();
                } else {
                    throw new AssertionError("Expected: "+REFERENCE+", got: "+last_element);
                }
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        data.append(ch, start, length);
    }
    
    public void computeCitations() {
        for (Publication p: publications) {
            int citeCount = 0;
            if (citations.containsKey(p.getId())) {
                citeCount = citations.get(p.getId());
            }
            maxCite = Integer.max(maxCite, citeCount);
            minCite = Integer.min(minCite, citeCount);
            p.setCitations(citeCount);
        }
    }
    
    public void computeStaticFeatures() {
        for (Publication p: publications) {
            p.computeAgeFeature(minAge, maxAge);
            p.computeCiteFeature(minCite, maxCite);
            p.computeRefFeature(minRef, maxRef);
        }
    }
    
    public List<Publication> getPublications() {
        return publications;
    }
    
}
