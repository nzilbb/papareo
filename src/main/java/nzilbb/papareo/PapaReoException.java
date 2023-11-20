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

import javax.json.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

/**
 * Exception releated to a Papa Reo API call.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class PapaReoException extends Exception {
  
  /**
   * Default constructor.
   */
  public PapaReoException() {
  } // end of constructor
  
  /**
   * Constructor with message.
   */
  public PapaReoException(String message) {
    super(message);
  } // end of constructor
  
  /**
   * Constructor from HTTP response.
   */
  public PapaReoException(HttpResponse httpResponse) {
    super(""+httpResponse.getStatusLine().getStatusCode()
          +": "+httpResponse.getStatusLine().getReasonPhrase());
    this.httpResponse = httpResponse;
  } // end of constructor
  
  /**
   * Constructor from JSON response.
   */
  public PapaReoException(JsonObject json) {
    super(json.containsKey("detail")?json.getString("detail"):json.toString());
    this.json = json;
  } // end of constructor
  
  /**
   * The response that cause the exception.
   * @see #getHttpResponse()
   */
  protected HttpResponse httpResponse;
  /**
   * Getter for {@link #httpResponse}: The response that cause the exception.
   * @return The response that cause the exception.
   */
  public HttpResponse getHttpResponse() { return httpResponse; }

  /**
   * Returns the HTTP status code of the problematic request.
   * @return THe HTTP status code, or -1 if there's no known response.
   */
  public int getStatusCode() {
    if (httpResponse == null) return -1;
    return httpResponse.getStatusLine().getStatusCode();
  } // end of getStatusCode()
  
  /**
   * JSON response if available.
   * @see #getJson()
   * @see #setJson(JsonObject)
   */
  protected JsonObject json;
  /**
   * Getter for {@link #json}: JSON response if available.
   * @return JSON response if available.
   */
  public JsonObject getJson() { return json; }
  
} // end of class PapaReoException
