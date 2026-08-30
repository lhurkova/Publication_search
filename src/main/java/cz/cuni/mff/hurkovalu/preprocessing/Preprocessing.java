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
package cz.cuni.mff.hurkovalu.preprocessing;

import cz.cuni.mff.hurkovalu.publication_search.Publication;
import cz.cuni.mff.hurkovalu.publication_search.aggregation.Filters;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;

/**
 * Class for preprocessing of the database XML files.
 * @author Lucie Hurkova
 */
public class Preprocessing {
    
    private static final Logger LOGGER = Logger.getLogger(Preprocessing.class.getName());
    private static final String EXTENSION = ".xml.gz";
    private Filters filters;
    private List<Publication> publications;
    private int numberOfFiles;
    private volatile int processedFiles;
    private Path dataDirectory;
    private Path serializationDirectory;
    
    public Preprocessing(Path dataDirectory, Path serializationDirectory) {
        this.dataDirectory = dataDirectory;
        this.serializationDirectory = serializationDirectory;
    }
    
    /**
     * Reads all XML files in the given directory and returns all valid publications described in the files.
     * @return valid publications
     */
    public List<Publication> processDirectory() {
        Database pubs = readPublications();
        if (pubs != null) {
            publications = pubs.p;
            filters = pubs.f;
            if (publications != null && filters != null) {
                return publications;
            }
        }
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl", getClass().getClassLoader());
        try {
            saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            PubMedHandler handler = new PubMedHandler();
            
            try (Stream<Path> files = Files.walk(dataDirectory)) {
                numberOfFiles = (int) files.filter(this::checkExtensions)
                        .count();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "processDirectory", e);
                System.exit(1);
            }
            
            try (Stream<Path> files = Files.walk(dataDirectory)) {
                files.filter(this::checkExtensions)
                    .forEach(p -> {
                            processedFiles++;
                            try (InputStream in = new GZIPInputStream(Files.newInputStream(p))) {
                                parser.parse(in, handler);
                            } catch (IOException | SAXException e) {
                                LOGGER.log(Level.SEVERE, "processDirectory", e);
                            }
                        });
            }
            handler.computeCitations();
            handler.computeStaticFeatures();
            publications = handler.getPublications();
            filters = new Filters(handler.getUniqueAuthors(), handler.getUniqueSurnames(),
                    handler.getUniqueJournals(), handler.getValidYears());
            return publications;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return new LinkedList<>();
    }
    
    private boolean checkExtensions(Path file) {
        String name = file.getName(file.getNameCount()-1).toString();
        return name.endsWith(EXTENSION);
    }
    
    public void storePublications() {
        Path pubStore = serializationDirectory.resolve("publications.ser");
        if (Files.isReadable(pubStore)) return;
        try (FileOutputStream file = new FileOutputStream(pubStore.toFile());
                ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(file, 1024*1024))) {
            out.writeObject(new Database(filters, publications));
            System.out.println("Publications has been serialized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Database readPublications() {
        try (FileInputStream file = new FileInputStream(serializationDirectory.resolve("publications.ser").toFile());
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(file, 1024*1024))) {
            return (Database) in.readObject();
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "readPublications", e);
        }
        return null;
    }
    
    record Database(Filters f, List<Publication> p) implements Serializable {};
    
    /**
     * Returns {@link Filters} created during processing of the directory.
     * @return {@link Filters} created during processing of the directory
     */
    public Filters getFilters() {
        return filters;
    }
    
    public int getProgress() {
        if (numberOfFiles == 0) return -1;
        return (100 * processedFiles)/numberOfFiles;
    }
}
