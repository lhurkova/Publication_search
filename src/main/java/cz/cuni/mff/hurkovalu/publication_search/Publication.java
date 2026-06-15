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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucie Hurkova
 */
public class Publication implements Serializable {
    
    private String pubAbstract;
    private Integer year;
    private Integer references;
    private Integer citations;
    private String journal;
    private List<Author> authors;
    private Integer id;
    private String doi;
    private String title;

    public List<Author> getAuthors() {
        return authors;
    }

    public int getCitations() {
        return citations;
    }

    public String getDoi() {
        return doi;
    }

    public int getId() {
        return id;
    }

    public String getJournal() {
        return journal;
    }

    public String getPubAbstract() {
        return pubAbstract;
    }

    public int getReferences() {
        return references;
    }

    public int getYear() {
        return year;
    }
    
    public String getTitle() {
        return title;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public void setCitations(int citations) {
        this.citations = citations;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public void setPubAbstract(String pubAbstract) {
        this.pubAbstract = pubAbstract;
    }

    public void setReferences(int references) {
        this.references = references;
    }

    public void setYear(int year) {
        this.year = year;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void addAuthor(Author author) {
        if (authors == null) {
            authors = new ArrayList<>();
        }
        authors.add(author);
    }
    
    public boolean isComplete() {
        return ((pubAbstract != null) && (year != null) && (references != null)
                && (journal != null) && (authors != null) && (id != null)
                && (title != null));
    }
    
}
