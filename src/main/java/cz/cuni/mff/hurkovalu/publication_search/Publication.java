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
package cz.cuni.mff.hurkovalu.publication_search;

import cz.cuni.mff.hurkovalu.publication_search.models.TFIDFModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Year;

/**
 * Class representing a publication from the database.
 * @author Lucie Hurkova
 */
public class Publication implements Serializable {
    
    public static final int NUM_OF_FEATURES = 5;
    public static final int[] WEIGHTS = new int[] {20, 10, 2, 2, 1}; // abstract, title, age, citations, references
    
    private String pubAbstract;
    private int year;
    private int references;
    private int citations;
    private String journal;
    private List<Author> authors;
    private int id;
    private String doi;
    private String title;
    private Map<Integer, Double> titleVector;
    private Map<Integer, Double> abstractVector;
    private double[] features = new double[NUM_OF_FEATURES];
    
    /**
     * Gets authors of the publication.
     * @return authors of the publication
     */
    public List<Author> getAuthors() {
        return authors;
    }
    
    /**
     * Gets number of citations of the publication.
     * @return number of citations of the publication
     */
    public int getCitations() {
        return citations;
    }
    
    /**
     * Gets DOI of the publication.
     * @return DOI of the publication
     */
    public String getDoi() {
        return doi;
    }
    
    /**
     * Gets PubMed ID of the publication.
     * @return ID of the publication
     */
    public int getId() {
        return id;
    }
    
    /**
     * Gets journal that published the publication.
     * @return journal that published the publication
     */
    public String getJournal() {
        return journal;
    }
    
    /**
     * Gets abstract of the publication.
     * @return abstract of the publication
     */
    public String getPubAbstract() {
        return pubAbstract;
    }
    
    /**
     * Gets number of references of the publication.
     * @return number of references of the publication
     */
    public int getReferences() {
        return references;
    }
    
    /**
     * Gets the year in which the publication was published.
     * @return year that the publication was published
     */
    public int getYear() {
        return year;
    }
    
    /**
     * Gets the title of the publication.
     * @return title of the publication
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the authors of the publication
     * @param authors authors of the publication
     */
    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }
    
    /**
     * Sets the number of citations of the publication.
     * @param citations number of citations of the publication
     */
    public void setCitations(int citations) {
        this.citations = citations;
    }
    
    /**
     * Sets DOI of the publication.
     * @param doi DOI of the publication
     */
    public void setDoi(String doi) {
        this.doi = doi;
    }
    
    /**
     * Sets PubMed ID of the publication.
     * @param id ID of the publication
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Sets the journal that published the publication.
     * @param journal journal that published the publication
     */
    public void setJournal(String journal) {
        this.journal = journal;
    }
    
    /**
     * Sets abstract of the publication.
     * @param pubAbstract abstract of the publication
     */
    public void setPubAbstract(String pubAbstract) {
        this.pubAbstract = pubAbstract;
    }
    
    /**
     * Sets the number of references of the publication.
     * @param references number of references of the publication
     */
    public void setReferences(int references) {
        this.references = references;
    }
    
    /**
     * Sets the year in which the publication was published.
     * @param year year in which the publication was published
     */
    public void setYear(int year) {
        this.year = year;
    }
    
    /**
     * Sets the title of the publication.
     * @param title title of the publication
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Adds the given author to the current list of authors of the publication.
     * @param author author of the publication
     */
    public void addAuthor(Author author) {
        if (authors == null) {
            authors = new ArrayList<>();
        }
        authors.add(author);
    }
    
    /**
     * Returns true if all information about the publication is filled.
     * @return true if all information about the publication is filled
     */
    public boolean isComplete() {
        return ((pubAbstract != null) && (pubAbstract.length() > 300) && (year != 0) && (references != 0)
                && (journal != null) && (authors != null) && (id != 0)
                && (title != null) && (!title.isBlank()));
    }
    
    /**
     * Sets the title vector for the {@link TFIDFModel}.
     * @param titleVector title vector for the {@link TFIDFModel}
     */
    public void setTitleVector(Map<Integer, Double> titleVector) {
        this.titleVector = titleVector;
    }
    
    /**
     * Gets the title vector for the {@link TFIDFModel}.
     * @return title vector for the {@link TFIDFModel}
     */
    public Map<Integer, Double> getTitleVector() {
        return titleVector;
    }
    
    /**
     * Sets the abstract vector for the {@link TFIDFModel}.
     * @param abstractVector abstract vector for the {@link TFIDFModel}
     */
    public void setAbstractVector(Map<Integer, Double> abstractVector) {
        this.abstractVector = abstractVector;
    }

    /**
     * Gets the abstract vector for the {@link TFIDFModel}.
     * @return abstract vector for the {@link TFIDFModel}
     */
    public Map<Integer, Double> getAbstractVector() {
        return abstractVector;
    }
    
    /**
     * Sets the value of a feature with the given index.
     * @param value new value of the feature
     * @param index index of the feature
     */
    public void setFeture(double value, int index) {
        features[index] = value;
    }
    
    /**
     * Gets the value of a feature with given index.
     * @param index index of the feature
     * @return value of the feature
     */
    public double getFeature(int index) {
        return features[index];
    }
    
    /**
     * Computes the aggregation function from the publication features.
     * @return aggregation function from the publication features
     */
    public double aggregationFunc() {
        return Publication.aggregationFunc(features);
    }
    
    /**
     * Computes the aggregation function from given features.
     * @param features features of a publication
     * @return aggregation function from given features
     */
    public static double aggregationFunc(double[] features) {
        double value = 0;
        for (int i = 0; i < NUM_OF_FEATURES; i++) {
            value += features[i] * WEIGHTS[i];
        }
        return value;
    }
    
    /**
     * Computes the age of the publication.
     * @return age of the publication
     */
    public int getAge() {
        return Year.now().getValue() - year;
    }
    
    /**
     * Computes and sets the time feature of the publication from the age of the publication.
     * @param minVal minimal age of a publication in the database
     * @param maxVal maximal age of a publication in the database
     */
    public void computeAgeFeature(int minVal, int maxVal) {
        features[2] = 1 - normalize(getAge(), minVal, maxVal);
    }
    
    /**
     * Computes and sets the citation feature from the number of citations of the publication.
     * @param minVal minimal number of citations of a publication in the database
     * @param maxVal maximal number of citations of a publication in the database
     */
    public void computeCiteFeature(int minVal, int maxVal) {
        features[3] = normalize(citations, minVal, maxVal);
    }
    
    /**
     * Computes and sets the reference feature from the number of references of the publication.
     * @param minVal minimal number of references of a publication in the database
     * @param maxVal maximal number of references of a publication in the database
     */
    public void computeRefFeature(int minVal, int maxVal) {
        features[4] = normalize(references, minVal, maxVal);
    }
    
    private static double normalize(double value, double minVal, double maxVal) {
        return (value - minVal)/(maxVal - minVal);
    }
    
}
