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
public class Filters {
    
    private Map<Author, Author> uniqueAuthors;
    private Map<String, List<Author>> uniqueSurnames;
    private Map<String, String> uniqueJournals;

    public Filters(Map<Author, Author> uniqueAuthors, Map<String, List<Author>> uniqueSurnames, Map<String, String> uniqueJournals) {
        this.uniqueAuthors = uniqueAuthors;
        this.uniqueSurnames = uniqueSurnames;
        this.uniqueJournals = uniqueJournals;
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
    
    public Filter createFilter(String authorString, String journalString) {
        String journal = null;
        Set<Author> authors = null;
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
        
        return new Filter(authors, journal);
    }
    
    public class Filter {
        Set<Author> authors;
        String journal;
        
        Filter(Set<Author> authors, String journal) {
            this.authors = authors;
            this.journal = journal;
        }
        
        public boolean apply(Publication p) {
            if (authors == null) {
                if (journal == null) {
                    return true;
                } else {
                    return journal.equals(p.getJournal());
                }
            } else {
                boolean result = false;
                for (Author a: p.getAuthors()) {
                    result = result || authors.contains(a);
                }
                if (journal == null) {
                    return result;
                } else {
                    return result && journal.equals(p.getJournal());
                }
            }
        }
    }
    
}
