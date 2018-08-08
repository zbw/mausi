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
package eu.zbw.a1.mausi;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.main.MauiTopicExtractor;
import com.entopix.maui.main.MauiWrapper;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.util.Evaluator;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.util.Topic;
import com.entopix.maui.vocab.VocabularyStoreFactory;
import com.entopix.maui.vocab.VocabularyStore_HT;

import eu.zbw.a1.mausi.kb.MausiThesaurus;
import eu.zbw.a1.mausi.util.MausiIo;
import weka.core.Option;
import weka.core.Utils;

/***
 * <p>
 * Mausi is a (yet simple) short text indexing system. <br/>
 * mausi == AUtoMatic Indexing System (somehow, the order of letters got mixed up...)
 * </p>
 * 
 * <p>
 * Actually, it is a wrapper around pre-existing applications and builds upon <b>maui</b>, see:<br/>
 * <a href="https://github.com/zelandiya/maui">https://github.com/zelandiya/maui</a>
 * </p>
 * <p>
 * <i>mausi</i> extra options:<br />
 * <code>-score</code> compute confidence in assignment and write 3 column output<br />
 * </p>
 * <p>
 * You can try to pass any maui option to MausiWrapperApp, see {{@link MauiModelBuilder}.
 * </p>
 * <p>
 * However, you can NOT YET access <i>hidden options</i>:
 * </p>
 * <p>
 * <code>-wkf</code> enables wikipedia keyphraseness features<br />
 * <code>-wgf</code> enables wikipedia generality features<br />
 * </p>
 * </p>
 * 
 * <p>
 * Run with control script (see /control/mausi.sh):<br/>
 * <code>
 * sh mausi.sh
 * </code>
 * </p>
 * 
 * @author Toepfer Martin
 * 
 */
public class MausiWrapperApp {

  private static final String OPTSK_PAIRS = "pairs";

  private static final String DUMMY = "§§dummy§§";

  public static final String OPTK_CSV_PREDICTED = "csv_predicted";

  public static final String OPTK_CSV_TEST = "csv_test";

  public static final String OPTK_CSV_TRAIN = "csv_train";

  public static final String OPTK_DIR_MODELS = "dir_models";

  public static final String VERSION = "0.1-SNAPSHOT";

  public static final String LNPERC = "--------------------------------------------------------------------------------";

  private static final Logger log = LoggerFactory.getLogger(MausiWrapperApp.class);

  public static final String[] EXAMPLES = { //
      "German multinationals and ethics : a case panel study",
      "Germany and its European partners : political crisis and banking", //

      // 6669 http://zbw.eu/stw/descriptor/19019-5 Crustacea Krebstiere
      // << 14160-1 "Aquatic animals"@en "Meerestiere"@de
      // << 14161-6 "Fishery product"@en "Fischereiprodukt"@de
      "Crustacea : increasing prices", "fishery : an emerging market?",
      "what do fishers do when the cod has gone", //
      "The Norwegian winter herring fishery : a story of technological progress and stock collapse "//
      /**
       * Seefischerei | Marine fisheries | Seefische | Saltwater fish | Technischer Fortschritt |
       * Technological change | Fischereiressourcen | Fishery resources | Atlantischer Ozean |
       * Atlantic Ocean | 1910-1970
       */
  };

  public static void main(String[] args) {
    // wrapper for efficient data read / write / analysis workflow
    log.info("mausi info: main");
    log.warn("not yet released version");
    // TODO see notes below
    if (args.length > 0) {
      final int iVersion = Utils.getOptionPos("version", args);
      if (iVersion > -1) {
        System.out.println("mausi - version " + VERSION);
        return;
      }
      final int iLoad = Utils.getOptionPos("load", args);
      final int iF = Utils.getOptionPos("f", args);
      final int iV = Utils.getOptionPos("v", args);
      if (iLoad > -1) {
        // load pre-existing model and apply to...
        try {
          String pthModel = args[iLoad + 1];
          Path modelPth = Paths.get(pthModel);
          System.out.printf("try to load '%s'%n", pthModel);
          String vocabularyName = args[iV + 1];
          String vocabularyFormat = args[iF + 1];
          MauiWrapper mauiw = new MauiWrapper(pthModel, vocabularyName, vocabularyFormat);
          mauiw.setModelParameters(vocabularyName, null, null, null);
          if (Utils.getOptionPos("example", args) > -1) {
            for (String title : EXAMPLES) {
              ArrayList<Topic> topicsFromText = mauiw.extractTopicsFromText(title, 15);
              System.out.printf("%s%n", title);
              for (Topic topic : topicsFromText) {
                String prefLabel = topic.getTitle();
                double probability = topic.getProbability();
                System.out.printf(">> %s (%.3f)%n", prefLabel, probability);
              }
            }
          } else {
            List<String> opsList = new ArrayList<>(Arrays.asList(args));
            opsList.add("-m");
            opsList.add(pthModel);
            Path pthCsvTest = Paths.get(Utils.getOption(OPTK_CSV_TEST, args));
            Path pthCsvPredicted = Paths.get(Utils.getOption(OPTK_CSV_PREDICTED, args));
            try (MausiThesaurus thes = new MausiThesaurus(Paths.get(vocabularyName));
                    OutputStream wr = Files.newOutputStream(pthCsvPredicted);) {
              batchEvaluation(pthCsvTest, thes, wr, modelPth, opsList.toArray(new String[0]));
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        // Training and evaluation setting:
        try {
          Path pthModelDir = Paths.get(Utils.getOption(OPTK_DIR_MODELS, args));
          Path pthCsvTrain = Paths.get(Utils.getOption(OPTK_CSV_TRAIN, args));
          Path pthCsvTest = Paths.get(Utils.getOption(OPTK_CSV_TEST, args));
          Path pthCsvPredicted = Paths.get(Utils.getOption(OPTK_CSV_PREDICTED, args));
          batchComplete(pthCsvTrain, pthCsvTest, pthCsvPredicted, pthModelDir, args);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } else {
      System.out.println("no parameters specified");
    }
  }

  /***
   * Combined model training and application on batches of records.
   * 
   * @param pthCsvTrain
   * @param pthCsvTest
   * @param pthCsvPredicted
   * @param pthModelDir
   * @param ops
   */
  public static void batchComplete(Path pthCsvTrain, Path pthCsvTest, Path pthCsvPredicted,
          Path pthModelDir, String... ops) {
    int thesOptIdx = Utils.getOptionPos("v", ops);
    if (thesOptIdx < 0) {
      throw new RuntimeException("missing thesaurus option -v");
    }
    Path pthThesaurus = Paths.get(ops[thesOptIdx + 1]);
    // String thesDir = System.getenv("ECONORM_DIR_STW");
    // Path pthThesaurus = Paths.get(thesDir, "stw.rdf");
    System.out.printf("§§ %s = %s%n", "thesaurus path", pthThesaurus.toString());
    System.out.printf("§§ %s = %s%n", "path to csv train data", pthCsvTrain.toString());
    System.out.printf("§§ %s = %s%n", "path to csv test  data", pthCsvTest.toString());
    // add model param to ops
    Path modelPth = Paths.get(pthModelDir.toString(),
            pthCsvTrain.getFileName().toString() + ".maui_model");
    List<String> opsList = new ArrayList<>(Arrays.asList(ops));
    opsList.add("-m");
    opsList.add(modelPth.toString());
    //
    MausiThesaurus thes = new MausiThesaurus(pthThesaurus);
    // train & save model
    MauiFilter model = csvTraining(pthCsvTrain, thes, opsList.toArray(new String[0]));
    // test
    try (OutputStream wr = Files.newOutputStream(pthCsvPredicted);) {
      batchEvaluation(pthCsvTest, thes, wr, modelPth, opsList.toArray(new String[0]));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /***
   * 
   * @param pthCsvTrain
   *          path to training data file.
   * @param thes
   *          mausi thesaurus instance
   * @param ops
   *          maui options
   * @return a maui filter instance
   */
  public static MauiFilter csvTraining(Path pthCsvTrain, MausiThesaurus thes, String... ops) {
    // similar to: MauiModelBuilder.main(ops);
    List<MauiDocument> trainDocuments = MausiIo.loadDocumentsByCsvTriplets(thes,
            pthCsvTrain.toString());
    MauiModelBuilder mmb = new MauiModelBuilder();
    Enumeration<Option> opsAvailable = mmb.listOptions();
    MauiFilter mauiFilter = null;
    String[] mauiOps = appendDummyOps(ops, opsAvailable, "l", "m");
    int modelOptIdx = Utils.getOptionPos("m", mauiOps);
    boolean hasModelFile = modelOptIdx >= 0 && !DUMMY.equals(mauiOps[modelOptIdx + 1]);
    try {
      mmb.setOptions(mauiOps); // note, this call erases options from array !!!

      final int iStemmer = Utils.getOptionPos("stemmer", ops);
      if (iStemmer > -1) {
        String stemmerName = Utils.getOption("stemmer", ops);
        String stemmerString = "com.entopix.maui.stemmers.".concat(stemmerName);
        Stemmer stemmer = (Stemmer) Class.forName(stemmerString).newInstance();
        mmb.stemmer = stemmer;
      }

      mauiFilter = mmb.buildModel(trainDocuments);
      if (hasModelFile) {
        mmb.saveModel(mauiFilter);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return mauiFilter;
  }

  public static void batchEvaluation(Path pthCsvTest, MausiThesaurus thes,
          OutputStream outPedictedCsv, Path modelPth, String... ops) {
    int idxPairs = Utils.getOptionPos(OPTSK_PAIRS, ops);
    boolean hasConceptsCol3 = idxPairs < 0;
    // similar to MauiTopicExtractor.main(new String[] {})
    MauiTopicExtractor topicExtractor = new MauiTopicExtractor();

    // (re-)set some required options to dummy values
    Enumeration<Option> opsAvailable = topicExtractor.listOptions();
    String[] mauiOps = appendDummyOps(ops, opsAvailable, "l", "m");
    // apply model on test data:
    VocabularyStoreFactory.setPrefferedVocabStoreType(VocabularyStore_HT.class);
    try {
      // Checking and Setting Options selected by the user:
      topicExtractor.setOptions(mauiOps);
      log.info("Extracting keyphrases with options: ");
      // Reading Options, which were set above and output them:
      String[] optionSettings = topicExtractor.getOptions();
      log.info(String.join(" ", optionSettings));
    } catch (Exception e) {
      // early error >> print usage info:
      log.error("Error during initialization...", e);
      log.error(e.getMessage());
      log.error("\nOptions:\n");
      Enumeration<Option> en = topicExtractor.listOptions();
      while (en.hasMoreElements()) {
        Option option = en.nextElement();
        log.error(option.synopsis());
        log.error(option.description());
      }
    }
    try {
      // Loading selected Model and Documents:
      log.info("-- Loading the model... ");
      topicExtractor.loadModel();
      // System.out.println("§§ vocab name: " + topicExtractor.vocabularyName);
      List<MauiDocument> documents = MausiIo.loadDocumentsByCsv(thes, pthCsvTest.toString(),
              hasConceptsCol3);
      // extract:
      List<MauiTopics> topics = topicExtractor.extractTopics(documents);
      // filter:
      // TODO topics =
      // evaluate:
      double[] prf = Evaluator.evaluateTopics(topics); // returns avg: Prec, Rec, Fmeasure
      // print:
      System.out.println(LNPERC);
      MausiIo mio = new MausiIo(topicExtractor);
      OutputStream out = outPedictedCsv == null ? System.out : outPedictedCsv;
      boolean optAdditonalInfo = Utils.getOptionPos("a", mauiOps) >= 0;
      boolean optScore = Utils.getOptionPos("score", ops) >= 0;
      mio.printTopicsCsv(out, documents, topics, "default", optAdditonalInfo, optScore);
    } catch (Exception e) {
      // error >> print usage info:
      log.error("Error running MauiTopicExtractor..", e);
      log.error(e.getMessage());
      log.error("\nOptions:\n");
      Enumeration<Option> en = topicExtractor.listOptions();
      while (en.hasMoreElements()) {
        Option option = en.nextElement();
        log.error(option.synopsis());
        log.error(option.description());
      }
    }
  }

  /**
   * copy opsKeep from before and add/append opsOverrideKeys
   * 
   * @param before
   * @param opsKeep
   * @param opsOverrideKeys
   * @return
   */
  public static String[] appendDummyOps(String[] before, Enumeration<Option> opsKeep,
          String... opsOverrideKeys) {
    String[] beforeCopy = Arrays.copyOf(before, before.length);
    // copy options:
    List<String> mauiOps = new LinkedList<>();
    while (opsKeep.hasMoreElements()) {
      Option option = (Option) opsKeep.nextElement();
      try {
        String key = option.name();
        String val = Utils.getOption(key, beforeCopy);
        if (val.length() > 0) {
          log.info(String.format("§§ copy option: %s = %s", key, val));
          mauiOps.add("-" + key);
          mauiOps.add(val);
        }
      } catch (Exception e) {
        // Utils.getOption simply throws unspecific Exceptions...
        // pass: option not specified
        e.printStackTrace();
      }
    }
    // (re-)set some required options to dummy values
    String[] copiedOps = mauiOps.toArray(new String[0]);
    try {
      for (int i = 0; i < opsOverrideKeys.length; i++) {
        String key = opsOverrideKeys[i];
        if (Utils.getOption(key, copiedOps).isEmpty()) {
          mauiOps.add("-" + key);
          mauiOps.add(DUMMY);
        }
      }
    } catch (Exception e1) {
      // Utils.getOption simply throws unspecific Exceptions...
      // pass: option not specified
      e1.printStackTrace();
    }
    return mauiOps.toArray(new String[0]);
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

}
