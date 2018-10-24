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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.entopix.maui.filters.MauiFilter;
import com.hp.hpl.jena.rdf.model.Literal;

import eu.zbw.a1.mausi.kb.MausiThesaurus;

public class MausiBasicTest {

  private static MausiThesaurus stw;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    String sPth = System.getenv("STW_PTH");
    if (sPth == null) {
      Assert.fail("STW_PTH environment variable not specified");
    }
    Path stwPth = Paths.get(sPth);
    stw = new MausiThesaurus(stwPth);
    Literal version = stw.getVersion();
    System.out.println("run tests with STW: " + version);
    // following assertions may refer to a specific version of the STW.
    if (!"9.04".equals(version.toString())) {
      System.err.printf(
              "warning: this version (STW %s) of the STW may not comply with the tests, see test code for details",
              version.toString());
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    if (stw != null) {
      stw.close();
    }
  }

  @Test
  public void testApp() throws URISyntaxException, IOException {
    ClassLoader clo = getClass().getClassLoader();
    Path pthCsvTrain = Paths.get(clo.getResource("training-data.csv").toURI());
    MausiThesaurus thes = this.stw;
    Path pthModel = Files.createTempFile("mausi_", ".maui_model");
    System.out.println("temporary mausi model file:" + pthModel);
    List<String> opsLs = new LinkedList<>(Arrays.asList("-f skos -d".split(" ")));
    opsLs.add("-m");
    opsLs.add(pthModel.toString());
    String[] opsTrain = opsLs.toArray(new String[0]);
    //
    //
    // TRAINING
    MauiFilter model = MausiWrapperApp.csvTraining(pthCsvTrain, thes, opsTrain);
    System.out.println(model.toString());
    //
    //
    // APPLY
    opsLs.add("-v");
    opsLs.add(MausiThesaurus.getStwLocByEnv().toString());
    // opsLs.add("-pairs"); // --> test-data.csv
    String[] opsTest = opsLs.toArray(new String[0]);
    Path pthCsvTest = Paths.get(clo.getResource("test-data-eval.csv").toURI());
    OutputStream os = new ByteArrayOutputStream(); // System.out;
    MausiWrapperApp.batchEvaluation(pthCsvTest, thes, os, pthModel, opsTest);
    os.close();
    String theOutput = os.toString();
    System.out.println("");
    System.out.println("mausi output predicitons:");
    System.out.println(theOutput);
    String[] lines = theOutput.split("\n");
    Map<String, Set<String>> results = new TreeMap<>();
    for (String ln : lines) {
      String[] cells = ln.split("\t");
      String docid = cells[0];
      String cid = cells[1].trim();
      if (!results.containsKey(docid)) {
        results.put(docid, new TreeSet<String>());
      }
      results.get(docid).add(cid);
    }
    // System.out.println(results);

    Set<String> doc1 = results.get("800000001");
    String cEconomy = "10025-6";
    Assert.assertTrue(String.format("%s not found in %s.", cEconomy, doc1.toString()),
            doc1.contains(cEconomy));
    Assert.assertTrue(results.get("800000002").contains("12964-6"));
  }

}
