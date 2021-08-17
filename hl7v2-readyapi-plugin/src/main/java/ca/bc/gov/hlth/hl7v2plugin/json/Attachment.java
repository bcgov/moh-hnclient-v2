package ca.bc.gov.hlth.hl7v2plugin.json;

public class Attachment {
	private String data;
	private String contentType;

	public Attachment() {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}