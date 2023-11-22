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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Client for the <a href="https://api.papareo.io/docs">Papa Reo web API</a>
 * published by <a href="https://tehiku.nz/">Te Hiku Media</a>.
 *
 * <h2>Usage</h2>
 *
 * <p> Use the this class for accessing the web API, something like:
 * <pre>
 * // Create client with access token
 * PapaReo papaReo = new PapaReo().setToken(token);
 * 
 * // short utterance transcription:
 * File wav = new File("short-utterance.wav");
 * String text = papaReo.transcribeUtterance(new FileInputStream(wav));
 * System.out.println(text);
 * 
 * // long recording transcription:
 * File wav = new File("long-speech.wav");
 * 
 * // start transcription task
 * String taskId = papaReo.transcribeLarge(new FileInputStream(wav));
 * 
 * // wait for it to complete
 * String status = papaReo.transcribeLargeStatus(taskId);
 * while (("STARTED".equals(status) || "PENDING".equals(status)) &amp;&amp; patience &gt; 0) {
 *   try {Thread.sleep(1000);} catch(Exception exception) {}
 *   status = papaReo.transcribeLargeStatus(taskId);
 * }
 * 
 * // save the resulting VTT file
 * InputStream stream = papaReo.transcribeLargeDownload(taskId);
 * File vtt = new File("long-speech.vtt");
 * java.nio.file.Files.copy(
 *       stream, 
 *       vtt.toPath(), 
 *       StandardCopyOption.REPLACE_EXISTING);
 * stream.close();
 * </pre>
 *
 * <p> Note that the access token can also be specified by:
 * <ul>
 *  <li> setting the <var>PAPAREO_TOKEN</var> environment variable, or </li>
 *  <li> setting the <var>papareo.token</var> system property
 *       (e.g. <tt>java -Dpapareo.token=xxxx</tt> ...). </li>
 * </ul>
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 */
public class PapaReo {

  /** Base URL for requests to the Papa Reo API: https://api.papareo.io/tuhi/ */
  public static final String baseUrl = "https://api.papareo.io/tuhi/";

  private CloseableHttpClient httpclient;
  private HttpRequestBase currentRequest;
  private String currentTaskId;

  /**
   * Default constructor.
   * If the <var>papareo.token</var> system property is set, it will be used as the token.
   * Otherwise, if the <var>PAPAREO_TOKEN</var> environment variable is set,
   * it will be used as the token.
   * Otherwise, {@link #setToken(String)} will have to be called to specify the token.
   */
  public PapaReo() {
    httpclient = HttpClients.createDefault();
    String t = System.getProperty("papareo.token");
    if (t == null || t.length() == 0) {
      t = System.getenv("PAPAREO_TOKEN");
    }
    if (t != null && t.length() > 0) {
      setToken(t);
    }
  }

  /**
   * Constructor with token.
   * @param token API access token.
   */
  public PapaReo(String token) {
    httpclient = HttpClients.createDefault();
    setToken(token);
  }
  
  /**
   * Whether to print debugging status messages to System.err or not.
   * @see #getDebug()
   * @see #setDebug(boolean)
   */
  protected boolean debug = false;
  /**
   * Getter for {@link #debug}: Whether to print debugging status messages to System.err or not.
   * @return Whether to print debugging status messages to System.err or not.
   */
  public boolean getDebug() { return debug; }
  /**
   * Setter for {@link #debug}: Whether to print debugging status messages to System.err or not.
   * @param newDebug Whether to print debugging status messages to System.err or not.
   */
  public PapaReo setDebug(boolean newDebug) { debug = newDebug; return this; }
  
  /**
   * Print a debug message if in debug mode.
   * @param message
   */
  public void debug(String message) {
    if (debug) System.err.println(message);
  } // end of debug()
  
  /**
   * API access token.
   * <p> There is no getter, in order to keep the token secret.
   * @see #setToken(String)
   */
  private String token;
  /**
   * Setter for {@link #token}: API access token.
   * <p> This is declared final to prevent access to the token via inheritance.
   * @param newToken API access token.
   */
  public final PapaReo setToken(String newToken) {
    // only set the token if it's not blank.
    if (newToken != null && newToken.length() > 0) {
      token = newToken;
    }
    return this;
  }
  
  /**
   * The User-Agent header string used by this client.
   * @see #getUserAgent()
   */
  protected String userAgent = null;
  /**
   * Getter for {@link #userAgent}: The User-Agent header string used by this client.
   * @return The User-Agent header string used by this client.
   */
  public String getUserAgent() {
    if (userAgent == null) {
      // generate the user agent header
      userAgent = getClass().getName()
        + " (" + Optional.ofNullable(
          getClass().getPackage().getImplementationVersion())
        .orElse("dev") + ")";
    }
    debug("user-agent: "+userAgent);
    return userAgent;
  }
  
  /**
   * Determines whether the client object has an access token or not.
   * @return true if the access token has been set, false otherwise.
   */
  public boolean hasToken() {
    return token != null;
  } // end of hasToken()

  /**
   * Cancel the current operation if any.
   */
  public void cancel() {
    if (currentRequest != null) {
      currentRequest.abort();
    }
    if (currentTaskId != null) {
      try {
        transcribeLargeCancel(currentTaskId);
      } catch (Throwable t) {
      } finally {
        currentTaskId = null;
      }
    }
  } // end of cancel()
  
  /**
   * Implementation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition/transcribe_tuhi_transcribe_post">
   * transcribe</a> method:
   * Bilingual Transcribe English and Māori, optionally including metadata on timing and
   * confidences.  
   * @param audio_file The contents of the audio file to transcribe.
   * @param with_metadata Whether to return extended data (e.g. alignment information)
   * with the transcript or not.
   * @return The JSON object response of the request, which should include a
   * "transcription" attribute that contains the text of the transcript.
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   * @see #transcribeUtterance(InputStream)
   */
  public JsonObject transcribe(InputStream audio_file, boolean with_metadata)
    throws IOException, PapaReoException {
    debug("transcribe: with_metadata "+with_metadata);
    HttpPost request = new HttpPost(baseUrl + "transcribe");
    currentRequest = request;
    try {
      request.addHeader("Authorization", "Token " + token);
      request.addHeader("User-Agent", getUserAgent());
      HttpEntity entity = MultipartEntityBuilder
        .create()      
        .addBinaryBody("audio_file", audio_file, ContentType.create("audio/wav"), "audio_file.wav")
        .addTextBody("with_metadata", ""+with_metadata)
        .build();
      request.setEntity(entity);
      HttpResponse httpResponse = HttpClients.createDefault().execute(request);
      StatusLine status = httpResponse.getStatusLine();
      debug("status "+status);
      if (status.getStatusCode() != 200) {
        throw new PapaReoException(httpResponse);
      } else {
        HttpEntity result = httpResponse.getEntity();
        JsonObject json = Json.createReader(
          new InputStreamReader(result.getContent())).readObject();
        debug("json "+json);
        return json;
      }
    } finally {
      currentRequest = null;
    }
  }

  /**
   * Convenience method for calling {@link #transcribe(InputStream,boolean)} and getting
   * back just the transcript.
   * @param audio_file The contents of the audio file to transcribe.
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   * @see #transcribe(InputStream,boolean)
   */
  public String transcribeUtterance(InputStream audio_file)
    throws IOException, PapaReoException {
    debug("transcribeUtterance... ");
    JsonObject response = transcribe(audio_file, false);
    debug("response: " + response);
    if (!response.containsKey("transcription")) throw new PapaReoException(response);
    return response.getString("transcription");
  }

  /**
   * Implementation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_tuhi_transcribe_large_post">
   * transcribe/large</a> method:
   * Start a transcription for large audio files of te reo Māori.
   * @param audio_file The contents of the audio file to transcribe.
   * @return The task ID of the running task.
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   */
  public String transcribeLarge(InputStream audio_file)
    throws IOException, PapaReoException {
    debug("transcribeLarge... ");
    HttpPost request = new HttpPost(baseUrl + "transcribe/large");
    currentRequest = request;
    try {
      request.addHeader("Authorization", "Token " + token);
      request.addHeader("User-Agent", getUserAgent());
      HttpEntity entity = MultipartEntityBuilder
        .create()      
        .addBinaryBody("audio_file", audio_file, ContentType.create("audio/wav"), "audio_file.wav")
        .build();
      request.setEntity(entity);
      HttpResponse httpResponse = HttpClients.createDefault().execute(request);
      StatusLine status = httpResponse.getStatusLine();
      debug("status "+status);
      if (status.getStatusCode() != 202) {
        throw new PapaReoException(httpResponse);
      } else {
        HttpEntity result = httpResponse.getEntity();
        JsonObject json = Json.createReader(
          new InputStreamReader(result.getContent())).readObject();
        debug("json "+json);
        if (!json.containsKey("task_id")) throw new PapaReoException(json);
        return json.getString("task_id");
      }
    } finally {
      currentRequest = null;
    }
  }
  
  /**
   * Implementation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_status_tuhi_transcribe_large__task_id__status_get">
   * transcribe/large/{task_id}/status</a> method:
   * Check the status of our large audio file transcription.
   * @param task_id The task ID returned by {@link #transcribeLarge(InputStream)}.
   * @return The current status of the task: one of "STARTED", "PENDING", "REVOKED", or "SUCCESS".
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   * @see #transcribeLarge(InputStream)
   */
  public String transcribeLargeStatus(String task_id)
    throws IOException, PapaReoException {
    debug("transcribeLargeStatus "+task_id);
    HttpGet request = new HttpGet(baseUrl + "transcribe/large/"+task_id+"/status");
    currentRequest = request;
    try {
      request.addHeader("Authorization", "Token " + token);
      request.addHeader("User-Agent", getUserAgent());
      HttpResponse httpResponse = HttpClients.createDefault().execute(request);
      StatusLine status = httpResponse.getStatusLine();
      debug("status "+status);
      if (status.getStatusCode() != 200) {
        throw new PapaReoException(httpResponse);
      } else {
        HttpEntity result = httpResponse.getEntity();
        JsonObject json = Json.createReader(
          new InputStreamReader(result.getContent())).readObject();
        debug("json "+json);
        if (!json.containsKey("status")) throw new PapaReoException(json);
        return json.getString("status");
      }
    } finally {
      currentRequest = null;
    }
  }
  
  /**
   * Implementation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_cancel_tuhi_transcribe_large__task_id__cancel_post">
   * transcribe/large/{task_id}/cancel</a> method:
   * Cancel the transcription for large audio files of te reo Māori.
   * @param task_id The task ID returned by {@link #transcribeLarge(InputStream)}.
   * @return The message returned by the request.
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   * @see #transcribeLarge(InputStream)
   */
  public String transcribeLargeCancel(String task_id)
    throws IOException, PapaReoException {
    debug("transcribeLargeCancel "+task_id);
    HttpPost request = new HttpPost(baseUrl + "transcribe/large/"+task_id+"/cancel");
    currentRequest = request;
    try {
      request.addHeader("Authorization", "Token " + token);
      request.addHeader("User-Agent", getUserAgent());
      HttpResponse httpResponse = HttpClients.createDefault().execute(request);
      StatusLine status = httpResponse.getStatusLine();
      debug("status "+status);
      if (status.getStatusCode() != 200) {
        throw new PapaReoException(httpResponse);
      } else {
        HttpEntity result = httpResponse.getEntity();
        JsonObject json = Json.createReader(
          new InputStreamReader(result.getContent())).readObject();
        debug("json "+json);
        if (!json.containsKey("message")) throw new PapaReoException(json);
        return json.getString("message");
      }
    } finally {
      currentRequest = null;
    }
  }
  
  /**
   * Implementation of
   * <a href="https://api.papareo.io/docs#/Speech%20Recognition%20-%20Large%20Audio%20Files/transcribe_large_download_tuhi_transcribe_large__task_id__download_get">
   * transcribe/large/{task_id}/download</a> method:
   * Download the transcription for large audio files of te reo Māori.
   * @param task_id The task ID returned by {@link #transcribeLarge(InputStream)}.
   * @return The current status of the task - one of "STARTED", "PENDING", "SUCCESS".
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the request.
   * @see #transcribeLarge(InputStream)
   */
  public InputStream transcribeLargeDownload(String task_id)
    throws IOException, PapaReoException {
    debug("transcribeLargeDownload "+task_id);
    HttpGet request = new HttpGet(baseUrl + "transcribe/large/"+task_id+"/download");
    currentRequest = request;
    try {
      request.addHeader("Authorization", "Token " + token);
      request.addHeader("User-Agent", getUserAgent());
      HttpResponse httpResponse = HttpClients.createDefault().execute(request);
      StatusLine status = httpResponse.getStatusLine();
      debug("status "+status);
      if (status.getStatusCode() > 200) {
        throw new PapaReoException(httpResponse);
      } else {
        HttpEntity result = httpResponse.getEntity();
        return result.getContent();
      }
    } finally {
      currentRequest = null;
    }
  }
  
  /**
   * Convenience function that returns the transcript given a large recording.
   * <p> The returned CompletableFuture object, when invoked, uses {@link #transcribeLarge(File)},
   * {@link #transcribeLargeStatus(String)}, and {@link #transcribeLargeDownload(String)}
   * to upload the long recording, wait for transcription to finish, and download the
   * VTT-formatted transcript to a temporary file, which is the result of the
   * CompletableFuture.
   * <p> <em>NB</em> It is the callers responsibility to delete the transcript file when
   * processing is complete.
   * @param audio_file The recording to transcribe.
   * @return The VTT-formatted transcript of the recording, which is the callers
   * responsibility to remove or delete;
   * @throws IOException If a communication error occurs.
   * @throws PapaReoException If the Papa Reo API could not successfully process the
   * request, or {@link #cancel()} was called.
   */
  public File transcribeRecording(File audio_file) throws IOException, PapaReoException {
    debug("transcribeRecording "+audio_file.getName());
    // upload the audio and start transcription
    currentTaskId = transcribeLarge(new FileInputStream(audio_file));
    debug("transcribeRecording taskId "+currentTaskId);
    
    // monitor progress
    String status = transcribeLargeStatus(currentTaskId);
    while (currentTaskId != null
           && ("STARTED".equals(status) || "PENDING".equals(status))) {
      debug("transcribeRecording waiting: " + status);
      try {Thread.sleep(1000);} catch(Exception exception) {}
      status = transcribeLargeStatus(currentTaskId);
    }
    debug("transcribeRecording final status: " + status);

    if (currentTaskId == null) {
      throw new PapaReoException("Cancelled");
    }
    
    // download the result
    InputStream stream = transcribeLargeDownload(currentTaskId);
    File vtt = File.createTempFile(audio_file.getName()+"-", ".vtt");
    vtt.deleteOnExit();
    Files.copy(stream, vtt.toPath(), StandardCopyOption.REPLACE_EXISTING);
    stream.close();
    
    return vtt;
  } // end of transcribeRecording()

} // end of class PapaReo
