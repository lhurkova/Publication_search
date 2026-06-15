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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;

/**
 *
 * @author Lucie Hurkova
 */
public class Preprocessing {
    
    private static final String EXTENSION = ".xml.gz";
    
    public List<Publication> processDirectory(Path directory) {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            PubMedHandler handler = new PubMedHandler();
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(this::checkExtensions)
                    .forEach(p -> {
                            try (InputStream in = new GZIPInputStream(Files.newInputStream(p))) {
                                parser.parse(in, handler);
                            } catch (IOException | SAXException e) {
                                e.printStackTrace();
                            }
                        });
            }
            handler.computeCitations();
            List<Publication> publications = handler.getPublications();
            storePublications(publications);
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
    
    private void storePublications(List<Publication> publications) {
        try (FileOutputStream file = new FileOutputStream("/tmp/publications.ser");
                ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(publications);
            System.out.println("Object has been serialized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
