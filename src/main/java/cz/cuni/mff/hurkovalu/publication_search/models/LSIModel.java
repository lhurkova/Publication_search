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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DiagonalMatrix;
import org.apache.commons.math4.legacy.linear.OpenMapRealMatrix;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

/**
 *
 * @author Lucie Hurkova
 */
public class LSIModel implements Model {

    private static final int K = 50;
    private List<Publication> publications;
    private Map<String, Integer> wordVector = new HashMap<>();
    private RealMatrix docConceptMatrix; //documents in columns
    private RealMatrix transformMatrix;

    public LSIModel(List<Publication> publications) {
        this.publications = publications;
    }
    
    @Override
    public void processPublications() {
        
        try (PrintStream output = new PrintStream(new File("../matrix.txt"))) {
        OpenMapRealMatrix matrix = new OpenMapRealMatrix(2*publications.size(), 20000);
        int termIndex = 0;
        int lastWords = 0;
        //TODO save transposed term document matrix
        for (int docIndex = 0; docIndex < publications.size(); docIndex++) {
            Publication publication = publications.get(docIndex);
            Set<String> currWords = new HashSet<>();
            for (String token: WordUtils.getTokens(publication.getPubAbstract())) {
                if (!currWords.contains(token)) {
                    if (!wordVector.containsKey(token)) {
                        wordVector.put(token, termIndex++);
                    }
                    matrix.setEntry(docIndex*2, wordVector.get(token), 1);
                    output.println((docIndex*2+1)+" "+(wordVector.get(token)+1)+" "+1);
                    currWords.add(token);
                }
            }
            
            for (String token: WordUtils.getTokens(publication.getTitle())) {
                if (!currWords.contains(token)) {
                    if (!wordVector.containsKey(token)) {
                        wordVector.put(token, termIndex++);
                    }
                    matrix.setEntry(docIndex*2+1, wordVector.get(token), 1);
                    output.println((docIndex*2+2)+" "+(wordVector.get(token)+1)+" "+1);
                    currWords.add(token);
                }
            }
            System.out.println("Done: "+docIndex+" words "+(wordVector.size()-lastWords));
            lastWords = wordVector.size();
        }
            System.out.println(2*publications.size()+" x "+termIndex);
            File matrixDir = new File("../svds-C/Tests");
            RealMatrix U = readMatrix(new File(matrixDir, "U"));
            RealMatrix V = readMatrix(new File(matrixDir, "V"));
            RealMatrix S = readDiagonalMatrix(new File(matrixDir, "S"));
            docConceptMatrix = S.multiply(V.transpose());
            transformMatrix = U.transpose();
        } catch (IOException ex) {
            Logger.getLogger(LSIModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void matchQuery(String query) {
        RealVector queryVector = new ArrayRealVector(wordVector.size());
        for (String token: WordUtils.getTokens(query)) {
            if (wordVector.containsKey(token)) {
                queryVector.setEntry(wordVector.get(token), 1);
            }
        }
        queryVector = transformMatrix.operate(queryVector);
        for (int i = 0; i < publications.size(); i++) {
            double absSim = 0;
            if (docConceptMatrix.getColumnVector(2*i).getNorm() != 0) {
                absSim = Math.abs(queryVector.cosine(docConceptMatrix.getColumnVector(2*i)));
            } else {
                System.out.println("");
            }
            publications.get(i).setFeture(absSim, 0);
            double titleSim = 0;
            if (docConceptMatrix.getColumnVector(2*i+1).getNorm() != 0) {
                titleSim = Math.abs(queryVector.cosine(docConceptMatrix.getColumnVector(2*i+1)));
            } else {
                System.out.println(i);
            }
            publications.get(i).setFeture(titleSim, 1);
        }
    }
    
    private RealMatrix readMatrix(File file) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int rowIndex = 0;
            String line = reader.readLine();
            String[] splitLine = line.split(" +");
            RealMatrix matrix = new Array2DRowRealMatrix(Integer.parseInt(splitLine[0]), Integer.parseInt(splitLine[1]));
            line = reader.readLine();
            while (line != null) {
                splitLine = line.split(" +");
                for (int colIndex = 0; colIndex < splitLine.length; colIndex++) {
                    matrix.setEntry(rowIndex, colIndex, Double.parseDouble(splitLine[colIndex]));
                }
                rowIndex++;
                line = reader.readLine();
            }
            return matrix;
        }
    }
    
    private RealMatrix readDiagonalMatrix(File file) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int index = 0;
            String line = reader.readLine();
            RealMatrix matrix = new DiagonalMatrix(Integer.parseInt(line));
            line = reader.readLine();
            while (line != null) {
                matrix.setEntry(index, index, Double.parseDouble(line));
                index++;
                line = reader.readLine();
            }
            return matrix;
        }
        
    }
        
        

}
