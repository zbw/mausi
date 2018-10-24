/**
* zaptain-mausi | Maui-Wrapper for short-text based STW subject indexing
* Copyright (C) 2016-2018  Martin Toepfer | ZBW -- Leibniz Information Centre for Economics
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package eu.zbw.a1.mausi.kb;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class MausiThesaurus implements Closeable {

  private static final String DEFAULT_STW_FORMAT = "rdf"; // note: maui requires RDF format

  public static final String NS_ZBW_EXT = "http://zbw.eu/namespaces/zbw-extensions/";

  public static final String NS_ZBW = "http://zbw.eu/stw/";

  private Model model;

  public MausiThesaurus(Path pth) {
    FileManager fm = FileManager.get();
    setModel(fm.loadModel(pth.toString()));
  }

  public Literal getVersion() {
    Resource stwres = getModel().getResource("http://zbw.eu/stw");
    Property vp = getModel().getProperty(OWL.versionInfo.toString());
    return getModel().getProperty(stwres, vp).getLiteral();
  }

  public boolean isDeprecated(String conceptId) {
    Resource res = resourceByConceptId(conceptId);
    Property p = getModel().createProperty(OWL.NS, "deprecated");
    NodeIterator iterator = getModel().listObjectsOfProperty(res, p);
    return iterator.hasNext();
  }

  public boolean isDesriptor(String cref) {
    // TODO fix quick hack
    return cref.contains("descriptor");
  }

  public String prefLabelEn(String conceptId) {
    return prefLabel(conceptId, "en");
  }

  public String prefLabel(String conceptId, String prefLanguage) {
    Resource res = resourceByConceptId(conceptId);
    Property p = SKOS.prefLabel;
    NodeIterator iterator = getModel().listObjectsOfProperty(res, p);
    while (iterator.hasNext()) {
      Literal rdfNode = (Literal) iterator.next();
      String language = rdfNode.getLanguage();
      // System.out.printf("%s (%s) <-- %s%n", rdfNode.getString(), language, rdfNode.toString());
      if ("en".equals(language)) {
        return rdfNode.getString();
      }
    }
    throw new RuntimeException(
            "cannot find label for concept: " + conceptId + " @ " + prefLanguage);
    // Statement prop = model.getProperty(res, p);
    // System.out.println(res);
    // System.out.println(p);
    // return prop.getString();
  }

  public Resource resourceByConceptId(String conceptId) {
    return getModel().createResource(NS_ZBW + "descriptor/" + conceptId);
  }

  public Model getModel() {
    return model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public List<RDFNode> getDescriptors() {
    Selector select = new SimpleSelector(null, RDF.type, STW.descriptor);
    StmtIterator stmtIterator = model.listStatements(select);
    List<RDFNode> ls = new LinkedList<>();
    while (stmtIterator.hasNext()) {
      Statement statement = (Statement) stmtIterator.next();
      // System.out.println(statement);
      RDFNode obj = statement.getSubject();
      ls.add(obj);
    }
    return ls;
  }

  public List<RDFNode> getSkosConcepts() {
    Selector select = new SimpleSelector(null, RDF.type, SKOS.concept);
    StmtIterator stmtIterator = model.listStatements(select);
    List<RDFNode> ls = new LinkedList<>();
    while (stmtIterator.hasNext()) {
      Statement statement = (Statement) stmtIterator.next();
      // System.out.println(statement);
      RDFNode obj = statement.getObject();
      ls.add(obj);
    }
    return ls;
  }

  public List<String> getLabels(RDFNode node, String language) {
    List<String> ls = new LinkedList<>();
    {
      Selector select = new SimpleSelector(node.asResource(), SKOS.prefLabel, (RDFNode) null);
      StmtIterator stmtIterator = model.listStatements(select);
      while (stmtIterator.hasNext()) {
        Statement statement = (Statement) stmtIterator.next();
        // System.out.println(statement);
        Literal obj = statement.getObject().asLiteral();
        if (language.equals(obj.getLanguage())) {
          ls.add(obj.getLexicalForm());
        }
      }
    }
    {
      Selector select = new SimpleSelector(node.asResource(), SKOS.altLabel, (RDFNode) null);
      StmtIterator stmtIterator = model.listStatements(select);
      while (stmtIterator.hasNext()) {
        Statement statement = (Statement) stmtIterator.next();
        // System.out.println(statement);
        Literal obj = statement.getObject().asLiteral();
        if (language.equals(obj.getLanguage())) {
          ls.add(obj.getLexicalForm());
        }
      }
    }
    return ls;
  }

  @Override
  public void close() throws IOException {
    this.model.close();
  }

  public static MausiThesaurus createMausiThesaurusByLocByEnv() {
    return new MausiThesaurus(getStwLocByEnv());
  }

  public static Path getStwLocByEnv() {
    return getStwLocByEnv(DEFAULT_STW_FORMAT);
  }

  public static Path getStwLocByEnv(String ext) {
    String sPth = System.getenv("STW_PTH");
    if (sPth != null) {
      return Paths.get(sPth);
    }
    String stwdir = System.getenv("STW_DIR");
    return Paths.get(stwdir, "stw." + ext);
  }

  public static String resource2cid(String resource) {
    return resource.substring(resource.lastIndexOf("/") + 1);
  }

  public static Vocabulary loadDefaultVocabulary() throws InstantiationException,
          IllegalAccessException, ClassNotFoundException, IOException {
    Path stwLoc = MausiThesaurus.getStwLocByEnv();
    if (!stwLoc.toFile().isFile()) {
      throw new FileNotFoundException(stwLoc.toString());
    }
    System.out.println(stwLoc);

    try (MausiThesaurus kb = new MausiThesaurus(stwLoc);) {
      Model model = kb.getModel();

      String stemmerName = "PorterStemmer";
      String stemmerString = "com.entopix.maui.stemmers.".concat(stemmerName);
      Stemmer stemmer = (Stemmer) Class.forName(stemmerString).newInstance();

      Vocabulary voc = new Vocabulary();
      voc.setLanguage("en");
      voc.setLowerCase(true);
      voc.setStemmer(stemmer);
      voc.setReorder(true);
      Stopwords stopwords = new StopwordsEnglish();
      voc.setStopwords(stopwords);
      voc.setVocabularyName("STW");
      voc.initializeFromModel(model);

      return voc;
    }
  }

}
