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
package cz.cuni.mff.hurkovalu.publication_search.topk_operator;

import cz.cuni.mff.hurkovalu.publication_search.Publication;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import javax.print.attribute.HashPrintServiceAttributeSet;

/**
 *
 * @author Lucie Hurkova
 */
public class TopKOperator {
    private List<List<Publication>> sortedObjects;
    
    public TopKOperator(List<Publication> publications) {
        sortedObjects = new ArrayList<>();
        for (int i=0; i < Publication.NUM_OF_FEATURES; i++) {
            final int index = i;
            List<Publication> sortedCopy = new ArrayList<>(publications);
            sortedCopy.sort((p1, p2) -> Double.compare(p2.getFeature(index), p1.getFeature(index)));
            sortedObjects.add(sortedCopy);
        }
    }
    
    public List<Publication> getTopK(int k) {
        Set<Integer> currPubIds = new HashSet<>();
        Queue<Publication> results = new PriorityQueue<>(
                (p1, p2) -> Double.compare(p1.aggregationFunc(), p2.aggregationFunc())
                );
        double kScore = 0;
        double threshold = 0;
        int i = 0;
        while (true) {
            double[] thresholdArray = new double[Publication.NUM_OF_FEATURES];
            for (int j = 0; j < Publication.NUM_OF_FEATURES; j++) {
                Publication p = sortedObjects.get(j).get(i);
                if (!currPubIds.contains(p.getId())) {
                    double score = p.aggregationFunc();
                    if (results.size() < k) {
                        results.add(p);
                        currPubIds.add(p.getId());
                    }
                    else if (score > kScore) {
                        Publication removed = results.poll();
                        currPubIds.remove(removed.getId());
                        results.add(p);
                        currPubIds.add(p.getId());
                    }
                    kScore = results.peek().aggregationFunc();
                }
                thresholdArray[j] = p.getFeature(j);
            }
             
            threshold = Publication.aggregationFunc(thresholdArray);
            if(kScore >= threshold) break;
            i++;
        }
        List<Publication> resultsList = new ArrayList<>();
        for (int j = 0; j < k; j++) {
            resultsList.add(results.poll());
        }
        return resultsList.reversed();
    }
    
    
}
