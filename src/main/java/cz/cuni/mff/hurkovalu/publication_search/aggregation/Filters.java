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
 * Class for verifying the filter inputs and for creating new filters.
 * @author Lucie Hurkova
 */
public class Filters implements Serializable {
    
    private Map<Author, Author> uniqueAuthors;
    private Map<String, List<Author>> uniqueSurnames;
    private Map<String, String> uniqueJournals;
    private TimeRange validYears;
    
    /**
     * Creates a new instance of {@link Filters} with given unique authors, unique authors surnames, unique journals and valid time range.
     * @param uniqueAuthors unique authors
     * @param uniqueSurnames unique authors surnames
     * @param uniqueJournals unique journals
     * @param validYears valid time range
     */
    public Filters(Map<Author, Author> uniqueAuthors, Map<String,
            List<Author>> uniqueSurnames, Map<String, String> uniqueJournals,
            TimeRange validYears) {
        this.uniqueAuthors = uniqueAuthors;
        this.uniqueSurnames = uniqueSurnames;
        this.uniqueJournals = uniqueJournals;
        this.validYears = validYears;
    }
    
    /**
     * Returns true if the given String represents a valid name of a author that exists in the database.
     * @param authorString String containing the name of the author
     * @return true if the given String represents a valid name of a author
     */
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
    
    /**
     * Returns true if the given String represents a valid name of a journal that exists in the database.
     * @param journalString String containing the name of the journal
     * @return true if the given String represents a valid name of a journal
     */
    public boolean isJournalValid(String journalString) {
        return uniqueJournals.containsKey(journalString);
    }
    
    /**
     * Returns true if the given time range is inside of the time range of the database.
     * @param years time range
     * @return true if the given time range is inside of the time range of the database
     */
    public boolean isTimeRangeValid(TimeRange years) {
        return (years.start() >= validYears.start()) && (years.end() <= validYears.end());
    }
    
    /**
     * Gets the time range of the database.
     * @return time range of the database
     */
    public TimeRange getTimeRange() {
        return validYears;
    }
    
    /**
     * Creates a filter with default options.
     * @return filter with default options
     */
    public Filter createEmptyFilter() {
        return new Filter(null, null, null, this);
    }
    
    /**
     * Creates a filter with given constrains.
     * @param authorString name of the author
     * @param journalString name of the journal
     * @param years time range of the publication
     * @return filter with given constrains
     */
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
        
        return new Filter(authors, journal, correctYears, this);
    }
    
    /**
     * Class representing a single filter applied on publications.
     */
    public class Filter {
        Set<Author> authors;
        String journal;
        TimeRange years;
        Filters filters;
        
        Filter(Set<Author> authors, String journal, TimeRange years, Filters filters) {
            this.authors = authors;
            this.journal = journal;
            this.years = years;
            this.filters = filters;
        }
        
        /**
         * Returns true if the filter applies on the given publication.
         * @param p publication
         * @return true if the filter applies on the given publication
         */
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
        
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("");
            if (authors != null) {
                s.append("Author: ");
                if (authors.size() == 1) {
                    s.append(authors.toArray(new Author[1])[0].toString());
                } else {
                    s.append(authors.toArray(new Author[1])[0].lastName());
                }
            }
            if (journal != null) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append("Journal: ").append(journal);
            }
            if (years != null) {
                if (!years.equals(filters.getTimeRange())) {
                    if (s.length() > 0) {
                        s.append(", ");
                    }
                    s.append("Time range: ");
                    s.append("(").append(years.start()).append(", ").append(years.end()).append(")");
                }
            }
            return s.toString();
        }
    }
    
    /**
     * Class representing a time range in years.
     * @param start start of the time range in years
     * @param end end of the time range in years
     */
    public static record TimeRange(int start, int end) implements Serializable {
        
    }
    
}
