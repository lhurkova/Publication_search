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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Lucie Hurkova
 */
public class TFIDFModelTest {
    private static List<Publication> publications = new ArrayList<>();
    private static List<Map<Integer, Double>> expectedVectors = new ArrayList<>();
    
    @BeforeClass
    public static void createPublications() {
        Publication p1 = new Publication();
        Publication p2 = new Publication();
        Publication p3 = new Publication();
        p1.setPubAbstract("cell");
        p1.setTitle("neuron");
        p2.setPubAbstract("bacteria cell");
        p2.setTitle("development of bacteria");
        p3.setPubAbstract("development of neurons");
        p3.setTitle("development");
        publications.add(p1);
        publications.add(p2);
        publications.add(p3);
        
        Map<Integer, Double> v1A = new HashMap<>();
        v1A.put(1, Math.log(6.0/2.0));
        Map<Integer, Double> v1T = new HashMap<>();
        v1T.put(3, Math.log(6.0/2.0));
        
        Map<Integer, Double> v2A = new HashMap<>();
        v2A.put(1, 0.5 * Math.log(6.0/2.0));
        v2A.put(0, 0.5 * Math.log(6.0/2.0));
        Map<Integer, Double> v2T = new HashMap<>();
        v2T.put(0, 0.5 * Math.log(6.0/2.0));
        v2T.put(2, 0.5 * Math.log(6.0/3.0));
        
        Map<Integer, Double> v3A = new HashMap<>();
        v3A.put(3, 0.5 * Math.log(6.0/2.0));
        v3A.put(2, 0.5 * Math.log(6.0/3.0));
        Map<Integer, Double> v3T = new HashMap<>();
        v3T.put(2, Math.log(6.0/3.0));
        
        expectedVectors.add(v1A);
        expectedVectors.add(v1T);
        expectedVectors.add(v2A);
        expectedVectors.add(v2T);
        expectedVectors.add(v3A);
        expectedVectors.add(v3T);
    }
    
    public TFIDFModelTest() {
    }

    /**
     * Test of processPublications method, of class TFIDFModel.
     */
    @Test
    public void testProcessPublications() {
        TFIDFModel instance = new TFIDFModel(publications, true);
        instance.processPublications();
        for (int i = 0; i < publications.size(); i++) {
            Map<Integer, Double> expectedAbs = expectedVectors.get(2*i);
            Map<Integer, Double> resultAbs = publications.get(i).getAbstractVector();
            Map<Integer, Double> expectedTitle = expectedVectors.get(2*i+1);
            Map<Integer, Double> resultTitle = publications.get(i).getTitleVector();

            assertEquals(expectedAbs.size(), resultAbs.size());
            for (Map.Entry<Integer, Double> entry: resultAbs.entrySet()) {
                assertTrue(expectedAbs.containsKey(entry.getKey()));
                assertEquals(expectedAbs.get(entry.getKey()), entry.getValue(), 10e-5);
            }
            
            assertEquals(expectedTitle.size(), resultTitle.size());
            for (Map.Entry<Integer, Double> entry: resultTitle.entrySet()) {
                assertTrue(expectedTitle.containsKey(entry.getKey()));
                assertEquals(expectedTitle.get(entry.getKey()), entry.getValue(), 10e-5);
            }
        }
    }
    
}
