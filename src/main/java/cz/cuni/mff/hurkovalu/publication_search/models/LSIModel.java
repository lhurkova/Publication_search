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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
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
import org.apache.commons.math4.legacy.linear.RealMatrix;
import org.apache.commons.math4.legacy.linear.RealVector;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.IntStream;
import org.apache.commons.math4.legacy.exception.DimensionMismatchException;
import org.apache.commons.math4.legacy.exception.MathArithmeticException;
import org.apache.commons.math4.legacy.exception.OutOfRangeException;

/**
 * Class computing the Latent Semantic Indexing model.
 * @author Lucie Hurkova
 */
public class LSIModel implements Model, Serializable {

    private static final Logger LOGGER = Logger.getLogger(LSIModel.class.getName());
    private static final int K = 50;
    private transient List<Publication> publications;
    private Map<String, Integer> wordVector = new HashMap<>();
    private RealMatrix docConceptMatrix; //documents in columns
    private RealMatrix transformMatrix;
    private static final String MATRIX_FILE_NAME = "SNAP.dat";
    private transient Path svdScript;
    private transient Path svdLibPath;
    
    private volatile int processedPublications;
    private volatile boolean publicationsProcessed = false;
    
    /**
     * Creates a new instance of {@link LSIModel} with given publications and path to SVD script
     * @param publications database
     * @param svdScript path to SVD script
     */
    public LSIModel(List<Publication> publications, Path svdScript) {
        this.publications = publications;
        this.svdScript = svdScript;
        this.svdLibPath = svdScript.getParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processPublications() {
        int termIndex = 0;
        int lastWords = 0;
        int recordsCount = 0;
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
            LOGGER.log(Level.FINE, "Done: {0} words {1}", new Object[]{docIndex, wordVector.size() - lastWords});
            lastWords = wordVector.size();
            processedPublications++;
        }
        publicationsProcessed = true;
        try (PrintStream output = new PrintStream(new BufferedOutputStream(new FileOutputStream(svdLibPath.resolve(MATRIX_FILE_NAME).toFile()), 1024*1024))) {
            for (Map.Entry<Integer, Map<Integer, Integer>> entry: docTermMatrix.entrySet()) {
                for(Map.Entry<Integer, Integer> jVal: entry.getValue().entrySet()) {
                    output.println((entry.getKey()+1)+" "+(jVal.getKey()+1)+" "+jVal.getValue());
                }
            }
            output.flush();
            String scriptPath = svdScript.toAbsolutePath().toString();
            ProcessBuilder pb = new ProcessBuilder(scriptPath, Integer.toString(termIndex),
                    Integer.toString(2 * publications.size()), Integer.toString(recordsCount));
            pb.inheritIO();
            pb.directory(new File(scriptPath).getParentFile());
            Process process = pb.start();
            try {
                int exitCode = process.waitFor();
                System.out.println(termIndex + " x " + 2 * publications.size());
                File matrixDir = svdLibPath.toFile();
                RealMatrix U = readMatrix(new File(matrixDir, "U"));
                RealMatrix V = readMatrix(new File(matrixDir, "V"));
                RealMatrix S = readDiagonalMatrix(new File(matrixDir, "S"));
                docConceptMatrix = S.multiply(V.transpose());
                transformMatrix = U.transpose();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    private void putValueInMatrixMap(Map<Integer, Map<Integer, Integer>> matrixMap, int i, int j, int val) {
        if (!matrixMap.containsKey(i)) {
            matrixMap.put(i, new TreeMap<>());
        }
        matrixMap.get(i).put(j, val);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void matchQuery(String query) {
        RealVector wordQueryVector = new ArrayRealVector(wordVector.size());
        for (String token : WordUtils.getTokens(query)) {
            if (wordVector.containsKey(token)) {
                wordQueryVector.setEntry(wordVector.get(token), 1);
            }
        }
        RealVector queryVector = transformMatrix.operate(wordQueryVector);
        IntStream.range(0, publications.size()).parallel().forEach(i -> computeQueryDis(i, queryVector));
    }

    private void computeQueryDis(int i, RealVector queryVector) throws MathArithmeticException, DimensionMismatchException, OutOfRangeException {
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

    private RealMatrix readMatrix(File file) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 1024*11024)) {
            int rowIndex = 0;
            String line = reader.readLine();
            String[] splitLine = line.split(" +");
            RealMatrix matrix = new Array2DRowRealMatrix(Integer.parseInt(splitLine[0]), Integer.parseInt(splitLine[1]));
            line = reader.readLine();
            while (line != null) {
                int colIndex = 0;
                Scanner s = new Scanner(line);
                while(s.hasNextDouble()) {
                    matrix.setEntry(rowIndex, colIndex++, s.nextDouble());
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getProgress() {
        if (publicationsProcessed) return -1;
        if (publications.isEmpty()) return 0;
        return (processedPublications * 100)/publications.size();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToFile(Path directory) {
        Path lsiStore = directory.resolve("lsi.ser");
        if (Files.isReadable(lsiStore)) return;
        try (FileOutputStream file = new FileOutputStream(lsiStore.toFile());
                ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(this);
            System.out.println("LSI model has been serialized");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "saveToFile", e);
        }
    }
    
    /**
     * Factory method to load serialized {@link LSIModel} from file.
     * @param directory directory containing serialized model
     * @param publications database used for the model
     * @return loaded model
     */
    public static LSIModel loadFromFile(Path directory, List<Publication> publications) {
        Path serFile = directory.resolve("lsi.ser");
        try (FileInputStream file = new FileInputStream(serFile.toFile());
                ObjectInputStream in = new ObjectInputStream(file)) {
            LSIModel model = (LSIModel) in.readObject();
            model.publications = publications;
            return model;
        } catch (FileNotFoundException e) {    
        } catch (IOException | ClassNotFoundException e) {
            
            LOGGER.log(Level.SEVERE, "loadFromFile", e);
        }
        try {
            Files.deleteIfExists(serFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "loadFromFile", e);
        }
        return null;
    }

}
