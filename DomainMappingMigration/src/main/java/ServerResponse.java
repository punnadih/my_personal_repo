package src.main.java;
/**
 * Simple wrapper class to wrap server response
 */
public class ServerResponse {
	public String response;
	public int statusCode;

	public ServerResponse(String response, int statusCode) {
		this.response = response;
		this.statusCode = statusCode;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
}
