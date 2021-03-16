/**
 * 
 */
package ca.bc.gov.hlth.hnclientv2.json;

/**
 * @author Tony.Ma
 * @date Jan. 15, 2021
 *
 */
public class FHIRJsonMessage {

	private String v2EncodingMessageData;
	private String resourceType;
	private String status;
	private String contentType;	
	
	
	public String getV2MessageData() {
		return v2EncodingMessageData;
	}

	public void setV2MessageData(String v2MessageData) {
		this.v2EncodingMessageData = v2MessageData;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
