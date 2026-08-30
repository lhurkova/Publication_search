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

import cz.cuni.mff.hurkovalu.publication_search.Author;
import cz.cuni.mff.hurkovalu.publication_search.Publication;
import cz.cuni.mff.hurkovalu.publication_search.aggregation.Filters;
import edu.stanford.nlp.io.StringOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Lucie Hurkova
 */
public class PreprocessingTest {
    
    private static Path xmlDir;
    private static final String XML_FILE = "example.xml.gz";
    private static int year = 1977;
    private static int references = 3;
    private static Author author = new Author("Singla", new String[] {"C", "L"});
    private static String journal = "Cell and tissue research";
    private static String title = "Fine structure of the adhesive pads of Gonionemus vertens.";
    private static String pubAbstract = "The roles of these various cell organelles in adhesion and detachment process are discussed.";
    private static String xmlContent = """
    <PubmedArticleSet>                                   
    <PubmedArticle>
    <MedlineCitation Status="MEDLINE" IndexingMethod="Manual" Owner="NLM">
      <PMID Version="1">18289</PMID>
      <DateCompleted>
        <Year>1977</Year>
        <Month>09</Month>
        <Day>29</Day>
      </DateCompleted>
      <DateRevised>
        <Year>2019</Year>
        <Month>07</Month>
        <Day>20</Day>
      </DateRevised>
      <Article PubModel="Print">
        <Journal>
          <ISSN IssnType="Print">0302-766X</ISSN>
          <JournalIssue CitedMedium="Print">
            <Volume>181</Volume>
            <Issue>3</Issue>
            <PubDate>
              <Year>1977</Year>
              <Month>Jul</Month>
              <Day>15</Day>
            </PubDate>
          </JournalIssue>
          <Title>Cell and tissue research</Title>
          <ISOAbbreviation>Cell Tissue Res</ISOAbbreviation>
        </Journal>
        <ArticleTitle>Fine structure of the adhesive pads of Gonionemus vertens.</ArticleTitle>
        <Pagination>
          <MedlinePgn>395-402</MedlinePgn>
        </Pagination>
        <Abstract>
          <AbstractText>The roles of these various cell organelles in adhesion and detachment process are discussed.</AbstractText>
        </Abstract>
        <AuthorList CompleteYN="Y">
          <Author ValidYN="Y">
            <LastName>Singla</LastName>
            <ForeName>C L</ForeName>
            <Initials>CL</Initials>
          </Author>
        </AuthorList>
        <Language>eng</Language>
        <PublicationTypeList>
          <PublicationType UI="D016428">Journal Article</PublicationType>
        </PublicationTypeList>
      </Article>
      <MedlineJournalInfo>
        <Country>Germany</Country>
        <MedlineTA>Cell Tissue Res</MedlineTA>
        <NlmUniqueID>0417625</NlmUniqueID>
        <ISSNLinking>0302-766X</ISSNLinking>
      </MedlineJournalInfo>
      <CitationSubset>IM</CitationSubset>
      <MeshHeadingList>
        <MeshHeading>
          <DescriptorName UI="D000818" MajorTopicYN="N">Animals</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D001369" MajorTopicYN="N">Axons</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D003063" MajorTopicYN="N">Cnidaria</DescriptorName>
          <QualifierName UI="Q000648" MajorTopicYN="Y">ultrastructure</QualifierName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D004721" MajorTopicYN="N">Endoplasmic Reticulum</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D004847" MajorTopicYN="N">Epithelial Cells</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D006056" MajorTopicYN="N">Golgi Apparatus</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D008854" MajorTopicYN="N">Microscopy, Electron</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D008870" MajorTopicYN="N">Microtubules</DescriptorName>
        </MeshHeading>
        <MeshHeading>
          <DescriptorName UI="D013569" MajorTopicYN="N">Synapses</DescriptorName>
        </MeshHeading>
      </MeshHeadingList>
    </MedlineCitation>
    <PubmedData>
      <History>
        <PubMedPubDate PubStatus="pubmed">
          <Year>1977</Year>
          <Month>7</Month>
          <Day>15</Day>
        </PubMedPubDate>
        <PubMedPubDate PubStatus="medline">
          <Year>1977</Year>
          <Month>7</Month>
          <Day>15</Day>
          <Hour>0</Hour>
          <Minute>1</Minute>
        </PubMedPubDate>
        <PubMedPubDate PubStatus="entrez">
          <Year>1977</Year>
          <Month>7</Month>
          <Day>15</Day>
          <Hour>0</Hour>
          <Minute>0</Minute>
        </PubMedPubDate>
      </History>
      <PublicationStatus>ppublish</PublicationStatus>
      <ArticleIdList>
        <ArticleId IdType="pubmed">18289</ArticleId>
        <ArticleId IdType="doi">10.1007/BF00223113</ArticleId>
      </ArticleIdList>
      <ReferenceList>
        <Reference>
          <Citation>Stain Technol. 1960 Nov;35:313-23</Citation>
          <ArticleIdList>
            <ArticleId IdType="pubmed">13741297</ArticleId>
          </ArticleIdList>
        </Reference>
        <Reference>
          <Citation>J Cell Biol. 1965 May;25:407-8</Citation>
          <ArticleIdList>
            <ArticleId IdType="pubmed">14287192</ArticleId>
          </ArticleIdList>
        </Reference>
        <Reference>
          <Citation>Z Zellforsch Mikrosk Anat. 1973 May 11;139(1):29-45</Citation>
          <ArticleIdList>
            <ArticleId IdType="pubmed">4351032</ArticleId>
          </ArticleIdList>
        </Reference>
      </ReferenceList>
    </PubmedData>
    </PubmedArticle>
    </PubmedArticleSet>                             
                                            """;
    
    @BeforeClass
    public static void createXML() throws IOException {
        xmlDir = Files.createTempDirectory("preprocessiing");
        try (Writer out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(xmlDir.resolve(XML_FILE).toFile())))) {
            out.write(xmlContent);
        }
    }
    
    @AfterClass
    public static void removeDir() throws IOException {
        Files.delete(xmlDir.resolve(XML_FILE));
        Files.delete(xmlDir);
    }
    
    public PreprocessingTest() {
    }

    /**
     * Test of processDirectory method, of class Preprocessing.
     */
    @Test
    public void testProcessDirectory() {
        Preprocessing instance = new Preprocessing(xmlDir, xmlDir);
        List<Publication> result = instance.processDirectory();
        assertEquals(1, result.size());
        Publication p = result.get(0);
        assertEquals(title, p.getTitle());
        assertEquals(author, p.getAuthors().get(0));
        assertEquals(year, p.getYear());
        assertEquals(journal, p.getJournal());
        assertEquals(pubAbstract, p.getPubAbstract());
        assertEquals(references, p.getReferences());
    }
    
    
}
