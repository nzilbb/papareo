//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.papareo.
//
//    nzilbb.papareo is free software; you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.papareo is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Affero General Public License for more details.
//
//    You should have received a copy of the GNU Affero General Public License
//    along with nzilbb.papareo; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.papareo;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.json.JsonObject;

/**
 * Unit tests for the PapaReo API client class.
 * <p> In order to test the client, you must have a valid Papa Reo access token, which can
 * be obtained from Te Hiku Media: <a href="https://papareo.io/docs">https://papareo.io/docs</a>
 * <p> Once you have a token, ensure that it's set as:
 * <ul>
 *  <li> a system property called <var>papareo.token</var>, or </li>
 *  <li> an environment variable called <var>PAPAREO_TOKEN</var>.
 * </ul>
 * <p> Using settings already in the pom.xml, the easiest way is to create a file called
 * <tt>papareo.properties</tt> in your home directory, with contents like:
 * <br><tt>papareo.token=xxxxxx-xxxx-xxxx-xxxx-xxxxxxx</tt>
 * <p> Then these tests will run.
 */
public class TestPapaReo {

  static PapaReo papaReo = null;
  
  /** Set the access token before any tests run. */
  @BeforeClass
  public static void setToken() throws Exception {
    papaReo = new PapaReo();
    if (!papaReo.hasToken()) throw new NullPointerException(
      "No access token is set. Please obtain a valid access token (https://papareo.io/docs)"
      +" and then set the papareo.token system property"
      +" or the PAPAREO_TOKEN environment variable.");
  }
  
  /**
   * Test invocation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition/transcribe_tuhi_transcribe_post">
   * transcribe</a> method with meta-data.
   */
  @Test public void transcribe() throws FileNotFoundException, IOException, PapaReoException {
    File wav = new File(getDir(), "utterance.wav");
    JsonObject response = papaReo.transcribe(new FileInputStream(wav), true);
    System.out.println(response.toString());
    assertTrue("Has success indicator", response.containsKey("success"));
    assertTrue("Was successful", response.getBoolean("success"));
    assertTrue("Has transcription", response.containsKey("transcription"));
    System.out.println("transcript: " + response.getString("transcription"));
    assertTrue("Has model_version", response.containsKey("model_version"));
    assertTrue("Has duration", response.containsKey("duration"));
    assertTrue("Has metadata", response.containsKey("metadata"));
  }
  
  /**
   * Test invocation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition/transcribe_tuhi_transcribe_post">
   * transcribe</a> method without meta-data.
   */
  @Test public void transcribeUtterance()
    throws FileNotFoundException, IOException, PapaReoException {
    File wav = new File(getDir(), "utterance.wav");
    String text = papaReo.transcribeUtterance(new FileInputStream(wav));
    assertTrue("Has transcription", text != null && text.length() > 0);
    System.out.println(text);
  }
  
  /**
   * Test invocation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_tuhi_transcribe_large_post">
   * transcribe/large</a> and
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_cancel_tuhi_transcribe_large__task_id__cancel_post">
   * transcribe/large/{task_id}/cancel</a>
   */
  @Test public void transcribeLargeAndCancel()
    throws FileNotFoundException, IOException, PapaReoException {
    File wav = new File(getDir(), "wordlist.wav");
    String taskId = papaReo.transcribeLarge(new FileInputStream(wav));
    System.out.println("task_id: " + taskId);
    assertTrue("Task ID returned", taskId != null && taskId.length() > 0);
    String message = papaReo.transcribeLargeCancel(taskId);
    assertTrue("Message returned", message != null && message.length() > 0);
    System.out.println("Cancellation message: " + message);
  }
  
  /**
   * Test invocation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_tuhi_transcribe_large_post">
   * transcribe/large</a>,
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_status_tuhi_transcribe_large__task_id__status_get">
   * transcribe/large/{task_id}/status</a>, and
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_download_tuhi_transcribe_large__task_id__download_get">
   * transcribe/large/{task_id}/download</a>
   */
  @Test public void transcribeLargeAndDownload()
    throws FileNotFoundException, IOException, PapaReoException {
    File wav = new File(getDir(), "wordlist.wav");
    // papaReo.setDebug(true);
    // start the transcription task
    String taskId = papaReo.transcribeLarge(new FileInputStream(wav));
    System.out.println("task_id: " + taskId);
    assertTrue("Task ID returned", taskId != null && taskId.length() > 0);

    // wait for it to finish
    String status = papaReo.transcribeLargeStatus(taskId);
    int patience = 60; // wait for up to this number of seconds
    while (("STARTED".equals(status) || "PENDING".equals(status)) && patience > 0) {
      System.out.println("waiting: " + status);
      try {Thread.sleep(1000);} catch(Exception exception) {}
      status = papaReo.transcribeLargeStatus(taskId);
      patience--;
    }
    System.out.println("final status: " + status);

    if (patience <= 0) { // ran out of patience!
      String message = papaReo.transcribeLargeCancel(taskId);
      fail("Transcription took too long, cancelled: " + message);
    } else {
      // download the result
      InputStream stream = papaReo.transcribeLargeDownload(taskId);
      assertTrue("Stream returned", stream != null);
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      
      // ensure the result is a VTT  file
      String line = reader.readLine();
      assertEquals("Is a VTT file", "WEBVTT", line);
      // read the rest of the transcript
      while (line != null) {
        System.out.println(line);
        line = reader.readLine();
      }
      reader.close();
    }
  }
  
  /**
   * Test invocation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_tuhi_transcribe_large_post">
   * transcribe/large</a>,
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_status_tuhi_transcribe_large__task_id__status_get">
   * transcribe/large/{task_id}/status</a>, and
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_download_tuhi_transcribe_large__task_id__download_get">
   * transcribe/large/{task_id}/download</a>
   * as used by the {@link PapaReo#transcribeRecording(File)} method.
   */
  @Test public void transcribeRecording()
    throws FileNotFoundException, IOException, PapaReoException, InterruptedException, ExecutionException {
    File wav = new File(getDir(), "wordlist.wav");
    //papaReo.setDebug(true);
    File vtt = papaReo.transcribeRecording(wav);
    assertNotNull("File returned", vtt);
    System.out.println(vtt.getPath());
    
    // check file
    BufferedReader reader = new BufferedReader(new FileReader(vtt));
    
    // ensure the result is a VTT  file
    String line = reader.readLine();
    assertEquals("Is a VTT file", "WEBVTT", line);
    // read the rest of the transcript
    while (line != null) {
      System.out.println(line);
      line = reader.readLine();
    }
    reader.close();

    vtt.delete();
  }
  
  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() { 
    if (fDir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      } catch(Throwable t) {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }

  /** Standalone entrypoint. */
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.papareo.TestPapaReo");
  }
}

