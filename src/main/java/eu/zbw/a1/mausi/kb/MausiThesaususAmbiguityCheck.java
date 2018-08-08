/**
* zaptain-mausi | Maui-Wrapper for short-text based STW subject indexing
* Copyright (C) 2016-2018  Martin Toepfer | ZBW -- Leibniz Information Centre for Economics
* 
* This program is free software; you can redistribute it and/or modify 
* it under the terms of the GNU General Public License as published by 
* the Free Software Foundation; either version 2 of the License, or 
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.util.Candidate;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * This AmbiguityCheck main method determines labels of the thesaurus, that have multiple meanings,
 * according to processing with mausi.
 * 
 * @author Toepfer Martin
 *
 */
public class MausiThesaususAmbiguityCheck {

  public static void main(String[] args) throws InstantiationException, IllegalAccessException,
          ClassNotFoundException, IOException {
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
      // voc.setSerialize(true);
      voc.initializeFromModel(model);

      MauiFilter maufi = new MauiFilter();
      maufi.setVocabulary(voc);
      maufi.setMaxPhraseLength(3);

      // processExamples(voc, maufi);

      System.out.println("------------------------------------");
      List<RDFNode> descriptors = kb.getDescriptors();
      System.out.printf("Thesaurus has # %d descriptors%n", descriptors.size());
      for (Iterator iterator = descriptors.iterator(); iterator.hasNext();) {
        RDFNode descriptor = (RDFNode) iterator.next();
        String cref = descriptor.toString();
        String cid = MausiThesaurus.resource2cid(cref);
        List<String> labels = kb.getLabels(descriptor, "en");
        System.out.printf("C:%s [%s]%n", cid, String.join(", ", labels));

        for (Iterator iterator2 = labels.iterator(); iterator2.hasNext();) {
          String phrase = (String) iterator2.next();
          HashMap<String, Candidate> candidates = maufi.getCandidates(phrase);
          if (candidates.size() > 1) {

            System.out.printf("  \"%s\" -> \"%s\" has %d meanings:%n", phrase,
                    voc.pseudoPhrase(phrase), candidates.size());
            for (Entry<String, Candidate> entry : candidates.entrySet()) {
              Candidate candidate = entry.getValue();
              String cref2 = candidate.getName();
              String cid2 = MausiThesaurus.resource2cid(cref2);
              System.out.printf("   $ %s | %s (%s)%n", candidate.getTitle(),
                      candidate.getBestFullForm(), cid);
            }
          }
        }

        // voc.toString()
      }
    }
  }

  private static void processExamples(Vocabulary voc, MauiFilter maufi) {
    // String phrase = "Germany until 1945";
    // System.out.println(voc.getFormatedName(phrase));
    // System.out.println(voc.normalizePhrase(phrase));
    // System.out.println(voc.pseudoPhrase(phrase));
    // HashMap<String,Candidate> candidates = maufi.getCandidates(phrase);
    // for (Entry<String, Candidate> entry : candidates.entrySet()) {
    // System.out.println(entry);
    // }
    List<String> examples = Arrays.asList("fertilizer", "salt", "Germany", "Germany until 1945");
    for (String phrase : examples) {
      // System.out.printf("ambiguous, \"%s\"? %b%n", phrase, voc.isAmbiguous(phrase));
      // ArrayList<String> meanings = voc.getSenses(phrase);
      // if (meanings.size() == 0) {
      // System.out.printf(" - %s (%s)%n", "", "");
      // }
      // for (String meaning : meanings) {
      // String conceptId = meaning.substring(meaning.lastIndexOf("/") + 1);
      // System.out.printf(" ? %s (%s)%n", kb.prefLabelEn(conceptId), conceptId);
      // }

      HashMap<String, Candidate> candidates = maufi.getCandidates(phrase);
      System.out.printf("\"%s\" -> \"%s\" has %d meanings:%n", phrase, voc.pseudoPhrase(phrase),
              candidates.size());
      for (Entry<String, Candidate> entry : candidates.entrySet()) {
        Candidate candidate = entry.getValue();
        String cref = candidate.getName();
        String cid = cref.substring(cref.lastIndexOf("/") + 1);
        System.out.printf("  $ %s | %s (%s)%n", candidate.getTitle(), candidate.getBestFullForm(),
                cid);
      }
    }
  }

  // /**
  // * See {@link MauiFilter#getCandidates(String)}
  // *
  // * @param s
  // * @return
  // */
  // public List<String> mimicGetCandidates(String s) {
  // List<String> meanings = new ArrayList<>();
  // final int maxPhraseLength = 3;
  // StringTokenizer stoki = new StringTokenizer(s, " ");
  // while (stoki.hasMoreTokens()) {
  // String tok = stoki.nextToken();
  // }
  // return meanings;
  // }

}
