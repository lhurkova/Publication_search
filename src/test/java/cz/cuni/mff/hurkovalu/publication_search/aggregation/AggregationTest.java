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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lucie Hurkova
 */
public class AggregationTest {
    
    public AggregationTest() {
    }

    /**
     * Test of getTopK method, of class Aggregation.
     */
    @Test
    public void testGetTopK() {
        List<Publication> publications = new ArrayList<>();
        Publication p1 = new Publication();
        Publication p2 = new Publication();
        Publication p3 = new Publication();
        Publication p4 = new Publication();
        Publication p5 = new Publication();
        publications.add(p1);
        publications.add(p2);
        publications.add(p3);
        publications.add(p4);
        publications.add(p5);

        double[] features1 = new double[] {0.1, 0.1, 0, 0, 0};
        double[] features2 = new double[] {0.1, 0.1, 0, 0, 1};
        double[] features3 = new double[] {0.1, 0.1, 0, 1, 0};
        double[] features4 = new double[] {0.1, 1, 0, 0, 0};
        double[] features5 = new double[] {1, 1, 1, 1, 1};

        for (int i = 0; i < Publication.NUM_OF_FEATURES; i++) {
            p1.setFeture(features1[i], i);
            p2.setFeture(features2[i], i);
            p3.setFeture(features3[i], i);
            p4.setFeture(features4[i], i);
            p5.setFeture(features5[i], i);
        }
        int k = 4;
        Filters filters = new Filters(null, null, null, null);
        Filters.Filter filter = filters.createEmptyFilter();
        Aggregation instance = new Aggregation(publications);
        List<Publication> expResult = publications.reversed();
        expResult.remove(publications.size()-1);
        List<Publication> result = instance.getTopK(k, filter);
        for (int i = 0; i < k; i++) {
            assertEquals(expResult.get(i), result.get(i));
        }
    }
    
}
