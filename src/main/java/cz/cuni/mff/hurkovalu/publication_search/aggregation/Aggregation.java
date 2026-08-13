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

import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author Lucie Hurkova
 */
public class Aggregation {
    private List<Publication> publications;
    
    public Aggregation(List<Publication> publications) {
        this.publications = publications;
    }
    
    public List<Publication> getTopK(int k, Filters.Filter filter) {
        Queue<Publication> results = new PriorityQueue<>(
                (p1, p2) -> Double.compare(p1.aggregationFunc(), p2.aggregationFunc())
                );
        double worstScore = 0;
        for (Publication p: publications) {
            if ((p.getFeature(0) != 0 || p.getFeature(1) != 0) && filter.apply(p)) {
                double currAggregation = p.aggregationFunc();
                if (results.size() < k) {
                    results.add(p);
                    if (Double.compare(worstScore, 0) <= 0) {
                        worstScore = currAggregation;
                    } else {
                        worstScore = Double.min(worstScore, currAggregation);
                    }
                } else {
                    if (currAggregation > worstScore) {
                        results.poll();
                        results.add(p);
                        worstScore = results.peek().aggregationFunc();
                    }
                }
            }
        }
        List<Publication> resultsList = new ArrayList<>();
        for (int j = 0; j < k; j++) {
            resultsList.add(results.poll());
        }
        return resultsList.reversed();
    }
    
}
