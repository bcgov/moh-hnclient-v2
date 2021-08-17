/* **************************************************************
   * Licensed Materials - Property of IBM                       *
   * Copyright IBM Canada Ltd 2003 All Rights Reserved        *
   ************************************************************** 
   * Please do not makes any changes to this file without first *
   * updating the revision history below, under description     *
   * please include Harvest Change Request or PSO Tools CR that *
   * caused the change.                                         *
   ************************************************************** */

/*
*  author 			Richard A. Schuller
*  author 			Lawell Kiing
*
*  Last Revision	Name		Comments 
*  ================================================================
*  25/01/1998		RAS									
*/

package ca.bc.gov.hlth.hl7v2plugin.hl7xfer;

/**
 * This class implements a messaging exception for the RAIWEB application.
 * <P>
 * This exception is thrown upon encountering any messaging errors. It has an
 * error message string that stores information about the error and can be
 * displayed upon catching the exception.
 */
public class HL7XferException extends Exception {

	private static final long serialVersionUID = 1L;
	private String iErrorMessage = null;

	public HL7XferException(String anError) {
		iErrorMessage = anError;
	}

	/**
	 * This is a getter method for the error message.
	 * <p>
	 * 
	 * @return String - the error message.
	 */
	public String getErrorMessage() {
		return "HL7XferException: " + iErrorMessage;
	}
}
