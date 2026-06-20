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
package cz.cuni.mff.hurkovalu.publication_search.TFIDF;

import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Lucie Hurkova
 */
public class VectorModel {
    
    private static final String SPLIT_REGEX = "(\\s|\\.|,|:|;|!|\\?|\\(|\\)|\\[|\\])+";
    
    private List<Publication> publications;
    private Map<String, WordInfo> wordVector;
    

    public VectorModel(List<Publication> publications) {
        this.publications = publications;
    }
    
    
    public void processPublications() {
        Map<String, int[]> termOccurences = new HashMap<>();
        List<Map<String, Integer>> filteredAbstracts = new ArrayList<>();
        List<Map<String, Integer>> filteredTitles = new ArrayList<>();
        for (Publication p: publications) {
            filteredAbstracts.add(processText(p.getPubAbstract(), termOccurences));
            filteredTitles.add(processText(p.getTitle(), termOccurences));
        }
        
        int index = 0;
        int documentCount = filteredAbstracts.size() + filteredTitles.size();
        wordVector = new HashMap<>();
        for (Map.Entry<String, int[]> entry : termOccurences.entrySet()) {
            String word = entry.getKey();
            int[] occurences = entry.getValue();
            if (occurences[0] > 1) { //count in all documents
                double idf = Math.log((double)documentCount/(double)occurences[1]);
                wordVector.put(word, new WordInfo(word, index, idf));
                index++;
            }
        }
        
        for (int i = 0; i < publications.size(); i++) {
            Publication publication = publications.get(i);
            publication.setAbstractVector(computeTFIDF(filteredAbstracts.get(i)));
            publication.setTitleVector(computeTFIDF(filteredTitles.get(i)));
        }
    }
    
    private Map<String, Integer> processText(String text, Map<String, int[]> termOccurences) {
        String[] words = text.split(SPLIT_REGEX);
        Map<String, Integer> documentWords = new HashMap<>();
        for (String word : words) {
            word = word.toLowerCase().intern();
            if (!stopWords.contains(word)) {
                if (!termOccurences.containsKey(word)) {
                    termOccurences.put(word, new int[2]);
                }
                termOccurences.get(word)[0]++;
                if (!documentWords.containsKey(word)) {
                    documentWords.put(word, 0);
                    termOccurences.get(word)[1]++;
                }
                documentWords.put(word,(documentWords.get(word)+1));
            }
        }
        return documentWords;
    }
    
    private Map<String, Integer> processText(String text) {
        String[] words = text.split(SPLIT_REGEX);
        Map<String, Integer> documentWords = new HashMap<>();
        for (String word : words) {
            word = word.toLowerCase().intern();
            if (wordVector.containsKey(word)) {
                if (!documentWords.containsKey(word)) {
                    documentWords.put(word, 0);
                }
                documentWords.put(word,(documentWords.get(word)+1));
            }
        }
        return documentWords;
    }
        
    private Map<Integer, Double> computeTFIDF(Map<String, Integer> filteredText) {
        int termCount = 0;
        Map<Integer, Double> textVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : filteredText.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();
            if (wordVector.containsKey(word)) {
                double value = (double)count *  wordVector.get(word).IDF();
                textVector.put(wordVector.get(word).index(), value);
                termCount += count;
            }
        }
        
        for (Map.Entry<Integer, Double> entry : textVector.entrySet()) {
            int key = entry.getKey();
            double val = entry.getValue();
            textVector.put(key, val/(double)termCount);
            
        }
        
        return textVector;
        
    }
    
    public SortedMap<Double, Publication> matchQuery(String query) {
        Map<String, Integer> filteredQuery = processText(query);
        Map<Integer, Double> queryVector = computeTFIDF(filteredQuery);
        SortedMap<Double, Publication> results = new TreeMap<>();
        for (Publication publication: publications) {
            results.put(computeCosSimilarity(queryVector, publication.getAbstractVector()), publication);
        }
        return results;
    }
    
    static public double computeCosSimilarity(Map<Integer, Double> vector1, Map<Integer, Double> vector2) {
        Set<Integer> intersection = new HashSet<>(vector1.keySet());
        intersection.retainAll(vector2.keySet());
        double scalarProduct = 0;
        for (Integer coord : intersection) {
            scalarProduct += vector1.get(coord) * vector2.get(coord);
        }
        return Math.abs(scalarProduct)/(computeVectorSize(vector1) * computeVectorSize(vector2));
    }
    
    static private double computeVectorSize(Map<Integer, Double> vector) {
        double size = 0;
        for (Map.Entry<Integer, Double> entry : vector.entrySet()) {
           size += entry.getValue() * entry.getValue();
        }
        return Math.sqrt(size);
    }
        
    
    private Set<String> stopWords = Set.of(
        "",
        "i",
        "me",
        "my",
        "myself",
        "we",
        "our",
        "ours",
        "ourselves",
        "you",
        "your",
        "yours",
        "yourself",
        "yourselves",
        "he",
        "him",
        "his",
        "himself",
        "she",
        "her",
        "hers",
        "herself",
        "it",
        "its",
        "itself",
        "they",
        "them",
        "their",
        "theirs",
        "themselves",
        "what",
        "which",
        "who",
        "whom",
        "this",
        "that",
        "these",
        "those",
        "am",
        "is",
        "are",
        "was",
        "were",
        "be",
        "been",
        "being",
        "have",
        "has",
        "had",
        "having",
        "do",
        "does",
        "did",
        "doing",
        "a",
        "an",
        "the",
        "and",
        "but",
        "if",
        "or",
        "because",
        "as",
        "until",
        "while",
        "of",
        "at",
        "by",
        "for",
        "with",
        "about",
        "against",
        "between",
        "into",
        "through",
        "during",
        "before",
        "after",
        "above",
        "below",
        "to",
        "from",
        "up",
        "down",
        "in",
        "out",
        "on",
        "off",
        "over",
        "under",
        "again",
        "further",
        "then",
        "once",
        "here",
        "there",
        "when",
        "where",
        "why",
        "how",
        "all",
        "any",
        "both",
        "each",
        "few",
        "more",
        "most",
        "other",
        "some",
        "such",
        "so",
        "than",
        "s",
        "t",
        "can",
        "will",
        "just",
        "don",
        "should",
        "now"
    );
    
}
