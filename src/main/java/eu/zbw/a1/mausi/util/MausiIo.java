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
package eu.zbw.a1.mausi.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.main.MauiTopicExtractor;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.util.Topic;
import com.entopix.maui.vocab.Vocabulary;

import eu.zbw.a1.mausi.kb.MausiThesaurus;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Several utility methods to support input and output.
 * 
 * @author Toepfer Martin
 *
 */
public class MausiIo {

  private static final Logger log = LoggerFactory.getLogger(MausiIo.class);

  private Vocabulary vocabulary;

  /**
   * 
   * @param tx
   *          tx is only used to initialize the internal vocabulary...
   */
  public MausiIo(MauiTopicExtractor tx) {
    try {
      log.info("--- Loading the vocabulary... " + tx.vocabularyName);
      vocabulary = new Vocabulary();
      vocabulary.setStemmer(tx.stemmer);
      vocabulary.setStopwords(tx.stopwords);
      vocabulary.setLanguage(tx.documentLanguage);
      // vocabulary.setSerialize(serialize);
      vocabulary.initializeVocabulary(tx.vocabularyName, tx.vocabularyFormat);
    } catch (Exception e) {
      log.error("Failed to load thesaurus!", e);
      throw (e);
    }
  }

  public void printTopicsCsv(OutputStream out, List<MauiDocument> documents,
          List<MauiTopics> allDocumentsTopics) {
    printTopicsCsv(out, documents, allDocumentsTopics, "default", true, true);
  }

  public void printTopicsCsv(OutputStream out, List<MauiDocument> documents,
          List<MauiTopics> allDocumentsTopics, String documentEncoding, boolean additionalInfo,
          boolean addProbability) {
    // TODO log.warn("check that documents and their topics have the same order !!!");
    // TODO log.warn("temporarily, ignore thsys concepts !!!");
    if (documents.size() != allDocumentsTopics.size()) {
      throw new IllegalArgumentException("illegal lengths !");
    }
    PrintWriter printer = null;
    Iterator<MauiDocument> docIt = documents.iterator();
    for (MauiTopics documentTopics : allDocumentsTopics) {
      MauiDocument doc = docIt.next();
      String docid = doc.getFileName();
      try {
        if (!documentEncoding.equals("default")) {
          printer = new PrintWriter(new OutputStreamWriter(out, documentEncoding));
        } else {
          printer = new PrintWriter(out);
        }
        List<Topic> fiTops = new LinkedList<>();
        for (Topic topic : documentTopics.getTopics()) {
          String topicTitle = topic.getTitle();
          if (!topicTitle.startsWith("http://")) {
            fiTops.add(topic);
          } else {
            log.warn(String.format("ignore illegal topic : %s @ %s", topicTitle, docid));
          }
        }
        for (Topic topic : fiTops) {
          printer.print(docid + "\t");
          String topicTitle = topic.getTitle();
          // >> recover concept-id:
          String id = "???";
          ArrayList<String> senses = vocabulary.getSenses(topicTitle);
          if (senses.size() < 1) {
            throw new RuntimeException("error @ recovering topic-id of '" + topicTitle + "'");
          }
          for (String sense : senses) {
            String cid = sense.substring(sense.lastIndexOf("/") + 1);
            String cidLabel = vocabulary.getTerm(sense);
            if (topicTitle.equals(cidLabel)) {
              id = cid;
            }
          }
          if ("???".equals(id)) {
            throw new RuntimeException("error @ recovering topic-id of '" + topicTitle + "'");
          }
          printer.print(id);
          // << done
          if (addProbability) {
            printer.print("\t");
            printer.print(topic.getProbability());
          }
          if (additionalInfo) {
            printer.print("\t");
            printer.print(topicTitle);
          }
          printer.println();
        }
        printer.flush();
      } catch (IOException e) {
        log.error(e.getMessage());
      }
    }
    printer.flush();
  }

  /**
   * Assume data structure per row, tab-separated:
   * 
   * <code>
   * docid    content    cids
   * </code>
   * 
   * @param thes
   * @param datasetPath
   * @return
   */
  public static List<MauiDocument> loadDocumentsByCsvTriplets(MausiThesaurus thes,
          String datasetPath) {
    return loadDocumentsByCsv(thes, datasetPath, true);
  }

  /**
   * Assume data structure per row, tab-separated:
   * 
   * <code>
   * docid    content
   * </code>
   * 
   * @param thes
   * @param datasetPath
   * @return
   */
  public static List<MauiDocument> loadDocumentsByCsvPairs(MausiThesaurus thes,
          String datasetPath) {
    return loadDocumentsByCsv(thes, datasetPath, false);
  }

  /**
   * Assume data structure per row, tab-separated, optional argument: cids in third column:
   * 
   * <code>
   * docid    content    [cids]
   * </code>
   * 
   * @param thes
   * @param datasetPath
   * @param hasConceptsCol3
   *          if concepts are assumed in 3rd column and should be used
   * @ @return
   */
  public static List<MauiDocument> loadDocumentsByCsv(MausiThesaurus thes, String datasetPath,
          boolean hasConceptsCol3) {
    File csvFile = new File(datasetPath);
    if (!csvFile.exists()) {
      log.error("File " + csvFile.getAbsolutePath() + " not found!");
      throw new RuntimeException();
    }
    if (!csvFile.isFile()) {
      log.error("File object '" + csvFile.getAbsolutePath() + "' is not a file !");
      throw new RuntimeException();
    }
    if (!csvFile.getName().endsWith("csv")) {
      log.error("File '" + csvFile.getAbsolutePath() + "' has illegal format extension !" + "\n"
              + "Expected '.csv' !");
      throw new RuntimeException();
    }
    // --
    List<MauiDocument> docs = new ArrayList<MauiDocument>();
    //
    Iterator<String> lineIt;
    try {
      lineIt = Files.lines(Paths.get(datasetPath)).iterator();
    } catch (IOException e) {
      log.error("Error while loading documents: " + e.getMessage());
      throw new RuntimeException();
    }
    int lnI = 0;
    while (lineIt.hasNext()) {
      String line = (String) lineIt.next();
      String[] parts = line.split("\t");
      String docId = parts[0];
      String content = parts[1];
      List<String> labelsTruth = new LinkedList<>();
      if (hasConceptsCol3) {
        if (parts.length < 3) {
          throw new RuntimeException(
                  "illegal line length @ line: " + lnI + " | file: " + csvFile.getName());
        }
        String conceptIds = parts.length == 3 ? parts[2] : "";
        // convert concept ids to english preferred labels
        String[] cids = conceptIds.split(";");
        for (int i = 0; i < cids.length; i++) {
          String cid = cids[i].trim();
          if (thes.isDeprecated(cid)) {
            log.warn(String.format("deprecated descriptor ! %s @ %s", cid, docId));
          } else {
            String prefLabelEn = thes.prefLabelEn(cid);
            labelsTruth.add(prefLabelEn);
          }
        }
      }
      // create document object:
      MauiDocument testDocument = new MauiDocument(docId, null, content,
              String.join("\n", labelsTruth));
      docs.add(testDocument);
      lnI++;
    }
    return docs;
  }

  /**
   * TODO towards logistic regression / SVM classifiers, sets "theory" as target concept
   * 
   * @return
   * @throws IOException
   */
  public static Instances load50i() throws IOException {
    String cidTarget = "19037-3";
    return load50i(cidTarget);
  }

  /**
   * TODO towards logistic regression / SVM classifiers
   * 
   * loads 50 example titlekw documents into instances with $conceptId as target
   * 
   * @return instances
   * @throws IOException
   */
  public static Instances load50i(String conceptId) throws IOException {
    Path path = Paths.get("../../experiments/showcase_50/docid_title_cids_50.csv");
    return loadCsvConceptTripletsBinaryRelevance(path, conceptId);
  }

  /**
   * 
   * @return instances
   * @throws IOException
   */
  public static Instances loadCsvConceptTripletsBinaryRelevance(Path dataPath, String conceptId)
          throws IOException {
    List<String> lns = Files.readAllLines(dataPath);
    Instances trainData = getStructure();
    for (String ln : lns) {
      Deque<String> zz = new LinkedList<>(Arrays.asList(ln.split("\t")));
      if (zz.size() >= 3) {
        String docid = zz.poll();
        String title = zz.poll();
        Set<String> cids = new TreeSet<>(Arrays.asList(zz.poll().split(";")));
        Instance instance = new Instance(2);
        instance.setDataset(trainData);
        instance.setValue(0, cids.contains(conceptId) ? "+" : "-");
        instance.setValue(1, title);
        trainData.add(instance);
      }
    }
    return trainData;
  }

  /**
   * TODO towards logistic regression / SVM classifiers
   * 
   * @return
   */
  public static Instances getStructure() {
    // TODO actually, multiple binary classification problems...

    FastVector classes = new FastVector();
    classes.addElement("+");
    classes.addElement("-");

    FastVector atts = new FastVector();
    atts.addElement(new Attribute("@@class@@", classes));
    atts.addElement(new Attribute("text", (FastVector) null));

    String relName = "__some_data__";
    Instances m_structure = new Instances(relName, atts, 1);
    // m_structure.setClassIndex(m_structure.numAttributes() - 1);
    m_structure.setClassIndex(0);
    return m_structure;
  }

  public static Instances loadCsvTripletsAssoc(Path dataPath) throws IOException {
    return loadCsvTripletsAssoc(dataPath, -1);
  }

  public static Instances loadCsvTripletsAssoc(Path dataPath, int limit) throws IOException {
    List<String> lns = Files.readAllLines(dataPath);
    List<Attribute> attributes = extractCidAssocAttributes(dataPath);
    FastVector attsVec = new FastVector(attributes.size());
    attributes.forEach(attsVec::addElement);

    log.info("create instances...");
    Instances trainData = new Instances("cidsassoc", attsVec, lns.size());
    int lni = 0;
    for (String ln : lns) {
      if (limit > -1 && lni > limit) {
        break;
      }
      if (lni++ % 50 == 0) {
        log.info("@ ln " + lni);
      }
      Deque<String> zz = new LinkedList<>(Arrays.asList(ln.split("\t")));
      if (zz.size() >= 3) {
        Instance instance = new Instance(attributes.size());
        instance.setDataset(trainData);
        String docid = zz.poll();
        String title = zz.poll();
        Set<String> cids = new TreeSet<>(Arrays.asList(zz.poll().split(";")));
        for (Attribute att : attributes) {
          instance.setValue(att, cids.contains(att.name()) ? 1 : Double.NaN);
        }
        trainData.add(instance);
      }
    }
    log.info("done.");
    return trainData;
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  public static List<Attribute> extractCidAssocAttributes(Path dataPath) throws IOException {
    log.info("determine set of attributes...");
    SortedSet<String> cidVoc = new TreeSet<>();
    List<String> lns = Files.readAllLines(dataPath);
    Instances trainData = getStructure();
    for (String ln : lns) {
      Deque<String> zz = new LinkedList<>(Arrays.asList(ln.split("\t")));
      if (zz.size() >= 3) {
        String docid = zz.poll();
        String title = zz.poll();
        Set<String> cids = new TreeSet<>(Arrays.asList(zz.poll().split(";")));
        cidVoc.addAll(cids);
      }
    }
    List<Attribute> atts = new ArrayList<>(cidVoc.size());
    for (String cid : cidVoc) {
      FastVector vector = new FastVector(1);
      vector.addElement(cid);
      atts.add(new Attribute(cid, vector));
    }
    log.info("done.");
    return atts;
  }

  // public static void main(String[] args) {
  // String pthDebug = Paths.get("D:\\Benutzer\\XXXX\\Documents\\code\\zaptain",
  // "proj_201710_titlekw/data_derived_share/dev_train.joint_csv").toString();
  // MausiThesaurus thes = new MausiThesaurus(Paths.get(System.getenv("ECONORM_DIR_STW"),
  // "stw.nt"));
  // loadDocumentsByCsv(thes, pthDebug, true);
  // }
}
