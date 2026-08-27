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
package cz.cuni.mff.hurkovalu.publication_search.aggregation;

import com.sun.xml.bind.v2.schemagen.xmlschema.Import;
import cz.cuni.mff.hurkovalu.publication_search.Author;
import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Lucie Hurkova
 */
public class Filters implements Serializable {
    
    private Map<Author, Author> uniqueAuthors;
    private Map<String, List<Author>> uniqueSurnames;
    private Map<String, String> uniqueJournals;
    private TimeRange validYears;

    public Filters(Map<Author, Author> uniqueAuthors, Map<String,
            List<Author>> uniqueSurnames, Map<String, String> uniqueJournals,
            TimeRange validYears) {
        this.uniqueAuthors = uniqueAuthors;
        this.uniqueSurnames = uniqueSurnames;
        this.uniqueJournals = uniqueJournals;
        this.validYears = validYears;
    }
    
    public boolean isAuthorValid(String authorString) {
        String[] splitInput = authorString.split(" ");
        if (splitInput.length == 1) {
            return uniqueSurnames.containsKey(splitInput[0]);
        } else if (splitInput.length > 1) {
            Author author = new Author(splitInput[splitInput.length-1],
                    Arrays.copyOfRange(splitInput, 0, splitInput.length-1));
            return uniqueAuthors.containsKey(author);
        }
        return false;
    }
    
    public boolean isJournalValid(String journalString) {
        return uniqueJournals.containsKey(journalString);
    }
    
    public boolean isTimeRangeValid(TimeRange years) {
        return (years.start() >= validYears.start()) && (years.end() <= validYears.end());
    }
    
    public TimeRange getTimeRange() {
        return validYears;
    }
    
    public Filter createEmptyFilter() {
        return new Filter(null, null, null);
    }
    
    public Filter createFilter(String authorString, String journalString, TimeRange years) {
        String journal = null;
        Set<Author> authors = null;
        TimeRange correctYears = null;
        if (isAuthorValid(authorString)) {
            String[] splitAuthor = authorString.split(" ");
            if (splitAuthor.length == 1) {
                authors = new HashSet<>(uniqueSurnames.get(splitAuthor[0]));
            } else {
                Author author = new Author(splitAuthor[splitAuthor.length-1],
                    Arrays.copyOfRange(splitAuthor, 0, splitAuthor.length-1));
                authors = Collections.singleton(author);
            }
        }
        
        if (isJournalValid(journalString)) {
            journal = journalString;
        }
        
        if (isTimeRangeValid(years)) {
            correctYears = years;
        }
        
        return new Filter(authors, journal, correctYears);
    }
    
    public class Filter {
        Set<Author> authors;
        String journal;
        TimeRange years;
        
        Filter(Set<Author> authors, String journal, TimeRange years) {
            this.authors = authors;
            this.journal = journal;
            this.years = years;
        }
        
        public boolean apply(Publication p) {
            return applyAuthor(p) && applyJournal(p) && applyYears(p);
        }
        
        private boolean applyAuthor(Publication p) {
           if (authors == null) {
               return true;
           } else {
                for (Author a: p.getAuthors()) {
                    if (authors.contains(a)) return true;
                }
                return false;
           }
        }
        
        private boolean applyJournal(Publication p) {
            if (journal == null) {
                return true;
            } else {
                return journal.equals(p.getJournal());
            }
        }
        
        private boolean applyYears(Publication p) {
            if (years == null) {
                return true;
            } else {
                return (p.getYear() >= years.start()) && (p.getYear() <= years.end());
            }
        }
    }
    
    public static record TimeRange(int start, int end) implements Serializable {
        
    }
    
}
