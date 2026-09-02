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
package cz.cuni.mff.hurkovalu.publication_search.models;

import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Class computing the TF-IDF model.
 * @author Lucie Hurkova
 */
public class TFIDFModel implements Model, Serializable {
    
    private static final Logger LOGGER = Logger.getLogger(TFIDFModel.class.getName());
    private transient List<Publication> publications;
    private Map<String, WordInfo> wordVector;
    private AtomicInteger processedPublications = new AtomicInteger(0);
    private volatile int termsRead;
    private volatile int vectorsComputed;
    private volatile int termsCount = -1;
    private boolean sorted = false;
    
    /**
     * Creates a new instance of {@link TFIDFModel} with given publications and path to SVD script
     * @param publications database
     */
    public TFIDFModel(List<Publication> publications) {
        this.publications = publications;
    }
    
    TFIDFModel(List<Publication> publications, boolean sorted) {
        this.publications = publications;
        this.sorted = sorted;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void processPublications() {
        Map<String, int[]> termOccurences = sorted ? new TreeMap<>() : new HashMap<>();
        List<Map<String, Integer>> filteredAbstracts = new ArrayList<>(Collections.nCopies(publications.size(), null));
        List<Map<String, Integer>> filteredTitles = new ArrayList<>(Collections.nCopies(publications.size(), null));
        IntStream.range(0, publications.size()).parallel().forEach(index -> {
            Publication p = publications.get(index);
            filteredAbstracts.set(index, processText(p.getPubAbstract(), termOccurences));
            filteredTitles.set(index, processText(p.getTitle(), termOccurences));
            processedPublications.getAndIncrement();
        });
        
        int index = 0;
        int documentCount = filteredAbstracts.size() + filteredTitles.size();
        wordVector = new HashMap<>();
        termsCount = termOccurences.size();
        for (Map.Entry<String, int[]> entry : termOccurences.entrySet()) {
            String word = entry.getKey();
            int[] occurences = entry.getValue();
            if (occurences[0] > 1) { //count in all documents
                double idf = Math.log((double)documentCount/(double)occurences[1]);
                wordVector.put(word, new WordInfo(word, index, idf));
                index++;
            }
            termsRead++;
        }
        System.out.println("Terms read "+termsRead);
        
        for (int i = 0; i < publications.size(); i++) {
            Publication publication = publications.get(i);
            publication.setAbstractVector(computeTFIDF(filteredAbstracts.get(i)));
            publication.setTitleVector(computeTFIDF(filteredTitles.get(i)));
            vectorsComputed++;
        }
        System.out.println("Vectors computed "+vectorsComputed);
    }
    
    private Map<String, Integer> processText(String text, Map<String, int[]> termOccurences) {
        List<String> words = WordUtils.getTokens(text);
        Map<String, Integer> documentWords = new HashMap<>();
        for (String word : words) {
            synchronized (this) {
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
        List<String> words = WordUtils.getTokens(text);
        Map<String, Integer> documentWords = new HashMap<>();
        for (String word : words) {
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void matchQuery(String query) {
        Map<String, Integer> filteredQuery = processText(query);
        Map<Integer, Double> queryVector = computeTFIDF(filteredQuery);
        publications.parallelStream().forEach(publication -> {
            publication.setFeture(computeCosSimilarity(queryVector, publication.getAbstractVector()), 0);
            publication.setFeture(computeCosSimilarity(queryVector, publication.getTitleVector()), 1);
        });
    }
    
    /**
     * Computes the cosine similarity between the given vectors.
     * @param vector1 first vector
     * @param vector2 second vector
     * @return cosine similarity between the given vectors
     */
    static public double computeCosSimilarity(Map<Integer, Double> vector1, Map<Integer, Double> vector2) {
        Set<Integer> intersection = new HashSet<>(vector1.keySet());
        intersection.retainAll(vector2.keySet());
        if (intersection.isEmpty()) return 0;
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getProgress() {
        if (publications.isEmpty()) return 0;
        return (80 * processedPublications.get())/publications.size() + (10 * vectorsComputed)/publications.size() + (10 * termsRead)/termsCount;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToFile(Path directory) {
        Path tfStore = directory.resolve("tfidf.ser");
        if (Files.isReadable(tfStore)) return;
        try (FileOutputStream file = new FileOutputStream(tfStore.toFile());
                ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "saveToFile", e);
        }
    }
    
    /**
     * Factory method to load serialized {@link TFIDFModel} from file.
     * @param directory directory containing serialized model
     * @param publications database used for the model
     * @return loaded model
     */
    public static TFIDFModel loadFromFile(Path directory, List<Publication> publications) {
        Path serFile = directory.resolve("tfidf.ser");
        try (FileInputStream file = new FileInputStream(serFile.toFile());
                ObjectInputStream in = new ObjectInputStream(file)) {
            TFIDFModel model = (TFIDFModel) in.readObject();
            model.publications = publications;
            return model;
        } catch (FileNotFoundException e) {    
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "loadFromFile", e);
        }
        try {
            Files.deleteIfExists(serFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "loadFromFile", e);
        }
        return null;
    }  
}
