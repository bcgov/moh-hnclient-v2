package ca.bc.gov.hlth.hl7v2plugin.json;

import java.util.List;

public class HNSESBResponse {
	private List<Content> content;
	private String resourceType;
	private String status;

	public List<Content> getContent() {
		return content;
	}

	public void setContent(List<Content> content) {
		this.content = content;
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

}
