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
package eu.zbw.a1.mausi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.util.Candidate;
import com.entopix.maui.vocab.Vocabulary;
import com.hp.hpl.jena.rdf.model.Model;

import eu.zbw.a1.mausi.kb.MausiThesaurus;

public class MausiAnnotator {

  private static final Logger log = LoggerFactory.getLogger(MausiAnnotator.class);

  private MausiThesaurus kb;

  private MauiFilter maufi;

  public MausiAnnotator(MausiThesaurus kb)
          throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    this.kb = kb;

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

    MauiFilter maufi = new MauiFilter();
    maufi.setVocabulary(voc);
    maufi.setMaxPhraseLength(3);

    this.maufi = maufi;
  }

  public List<StwAnnotation> process(String content) {
    String phrase = content;
    HashMap<String, Candidate> candidates = maufi.getCandidates(phrase);
    List<StwAnnotation> annos = new ArrayList<>(candidates.size());
    for (Entry<String, Candidate> entry : candidates.entrySet()) {
      Candidate candidate = entry.getValue();
      String cref = candidate.getName();
      if (this.kb.isDesriptor(cref)) {
        String cid = cref.substring(cref.lastIndexOf("/") + 1);
        String fullForm = candidate.getBestFullForm();
        int iBegin = phrase.indexOf(fullForm);
        int iEnd = iBegin + fullForm.length();
        String extracted = phrase.substring(iBegin, iEnd);
        StwAnnotation anno = new StwAnnotation(cid, extracted, iBegin, iEnd);
        annos.add(anno);
      } else {
        String msg = "trying to assign non-descriptor concept: " + cref;
        log.debug(msg);
      }
    }
    return annos;
  }

}
