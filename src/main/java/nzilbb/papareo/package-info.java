/**
 * Java client for the <a href="https://papareo.io/docs">Papa Reo API</a> 
 * published by <a href="https://tehiku.nz/te-hiku-tech/">Te Hiku Media</a>
 * <p> The API performs speech recognition and alignment for recordings of te reo Māori and 
 * New Zealand English.
 *
 * <h2>Usage</h2>
 *
 * <p> Use the {@link PapaReo} class for accessing the web API, something like:
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
 * while (("STARTED".equals(status) || "PENDING".equals(status)) &anp;&amp; patience &gt; 0) {
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
 */
package nzilbb.papareo;
