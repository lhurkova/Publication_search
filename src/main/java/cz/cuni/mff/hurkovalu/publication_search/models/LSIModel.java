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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.ArrayRealVector;
import org.apache.commons.math4.legacy.linear.DiagonalMatrix;
import org.apache.commons.math4.legacy.linear.OpenMapRealMatrix;
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;

/**
 * Class computing the Latent Semantic Indexing model.
 * @author Lucie Hurkova
 */
public class LSIModel implements Model {

    private static final int K = 50;
    private List<Publication> publications;
    private Map<String, Integer> wordVector = new HashMap<>();
    private RealMatrix docConceptMatrix; //documents in columns
    private RealMatrix transformMatrix;
    private static final String SVD_LIB_PATH = "../svds-C/Tests";
    private static final String SVD_SCRIPT = "svdstest";
    private static final String MATRIX_FILE_NAME = "SNAP.dat";

    public LSIModel(List<Publication> publications) {
        this.publications = publications;
    }

    @Override
    public void processPublications() {

        OpenMapRealMatrix matrix = new OpenMapRealMatrix(2 * publications.size(), 20000);
        int termIndex = 0;
        int lastWords = 0;
        int recordsCount = 0;
        //TODO save transposed term document matrix
        Map<Integer, Map<Integer, Integer>> docTermMatrix = new TreeMap<>(); // first term than doc
        for (int docIndex = 0; docIndex < publications.size(); docIndex++) {
            Publication publication = publications.get(docIndex);
            Set<String> currWords = new HashSet<>();
            for (String token : WordUtils.getTokens(publication.getPubAbstract())) {
                if (!currWords.contains(token)) {
                    if (!wordVector.containsKey(token)) {
                        wordVector.put(token, termIndex++);
                    }
                    putValueInMatrixMap(docTermMatrix, wordVector.get(token), docIndex * 2, 1);
                    recordsCount++;
                    currWords.add(token);
                }
            }

            for (String token : WordUtils.getTokens(publication.getTitle())) {
                if (!currWords.contains(token)) {
                    if (!wordVector.containsKey(token)) {
                        wordVector.put(token, termIndex++);
                    }
                    putValueInMatrixMap(docTermMatrix, wordVector.get(token), docIndex * 2 + 1, 1);
                    recordsCount++;
                    currWords.add(token);
                }
            }
            System.out.println("Done: " + docIndex + " words " + (wordVector.size() - lastWords));
            lastWords = wordVector.size();
        }
        try (PrintStream output = new PrintStream(new File(SVD_LIB_PATH, MATRIX_FILE_NAME))) {
            for (Map.Entry<Integer, Map<Integer, Integer>> entry: docTermMatrix.entrySet()) {
                for(Map.Entry<Integer, Integer> jVal: entry.getValue().entrySet()) {
                    output.println((entry.getKey()+1)+" "+(jVal.getKey()+1)+" "+jVal.getValue());
                }
            }
            output.flush();
            String scriptPath = Paths.get(SVD_LIB_PATH, SVD_SCRIPT).toAbsolutePath().toString();
            ProcessBuilder pb = new ProcessBuilder(scriptPath, Integer.toString(termIndex),
                    Integer.toString(2 * publications.size()), Integer.toString(recordsCount));
            pb.inheritIO();
            pb.directory(new File(scriptPath).getParentFile());
            Process process = pb.start();
            try {
                int exitCode = process.waitFor();
                System.out.println(termIndex + " x " + 2 * publications.size());
                File matrixDir = new File(SVD_LIB_PATH);
                RealMatrix U = readMatrix(new File(matrixDir, "U"));
                RealMatrix V = readMatrix(new File(matrixDir, "V"));
                RealMatrix S = readDiagonalMatrix(new File(matrixDir, "S"));
                docConceptMatrix = S.multiply(V.transpose());
                transformMatrix = U.transpose();
            } catch (InterruptedException ex) {
                Logger.getLogger(LSIModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(LSIModel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void putValueInMatrixMap(Map<Integer, Map<Integer, Integer>> matrixMap, int i, int j, int val) {
        if (!matrixMap.containsKey(i)) {
            matrixMap.put(i, new TreeMap<>());
        }
        matrixMap.get(i).put(j, val);
    }

    @Override
    public void matchQuery(String query) {
        RealVector queryVector = new ArrayRealVector(wordVector.size());
        for (String token : WordUtils.getTokens(query)) {
            if (wordVector.containsKey(token)) {
                queryVector.setEntry(wordVector.get(token), 1);
            }
        }
        queryVector = transformMatrix.operate(queryVector);
        for (int i = 0; i < publications.size(); i++) {
            double absSim = 0;
            if (docConceptMatrix.getColumnVector(2 * i).getNorm() != 0) {
                absSim = Math.abs(queryVector.cosine(docConceptMatrix.getColumnVector(2 * i)));
            }
            publications.get(i).setFeture(absSim, 0);
            double titleSim = 0;
            if (docConceptMatrix.getColumnVector(2 * i + 1).getNorm() != 0) {
                titleSim = Math.abs(queryVector.cosine(docConceptMatrix.getColumnVector(2 * i + 1)));
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
