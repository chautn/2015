package com.acme.samples.assembly;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.io.InputStream;


public class Executor {
	
	public static final String BASE_URL = "http://localhost:8080/rest/private/v1/calendar";
	
	/**
	 * java -jar calendar-attachment.jar root gtn Event66f405597f00010125d71a20dd1dc8b4 file1 file2 fileN
	 * @param args
	 * 				username, password, event_id, file_path[]
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		//Assign command arguments.
		final String username, password, event_id;
		if (args.length < 4) {
			System.out.println("Missing arguments!!!");
			return;
		}
		username = args[0];
		password = args[1];
		event_id = args[2];
		for (int i = 3; i < args.length; i++) {
			if (Files.exists(Paths.get(args[i]), LinkOption.NOFOLLOW_LINKS) == false) {
				System.out.println("File not found!!!");
				return;
			}
		}
		
		//Set up Authenticator.
		Authenticator.setDefault(new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password.toCharArray());
			}
		});
		
		//Set up connection.
		
		final String CREATE_ATTACHMENT_URL = BASE_URL + "/events"
																									+ "/" + event_id
																									+ "/attachments";
		
		HttpURLConnection connection = (HttpURLConnection) new URL(CREATE_ATTACHMENT_URL).openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Cache-Control", "no-cache");
		
		//Write to connection output stream.
		String boundary = "*****";
		String twoHyphens = "--";
		String crlf = "\r\n";
		String attachmentName;
		String attachmentFileName;
		byte[] data;

		connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
		DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
		
		for (int i = 3; i < args.length; i++) {
			attachmentName = Paths.get(args[i]).getFileName().toString();
			attachmentFileName = Paths.get(args[i]).getFileName().toString();
			data = Files.readAllBytes(Paths.get(args[i]));
			
			requestStream.writeBytes(twoHyphens + boundary + crlf);
			requestStream.writeBytes(			"Content-Disposition: form-data;"	
																 	+	"name=\"" + attachmentName + "\";" 
																	+ "filename=\"" + attachmentFileName + "\""
																	+ crlf);
			requestStream.writeBytes(crlf);
			requestStream.write(data);
			requestStream.writeBytes(crlf);
		}
		//the last line is closing boundary.
		requestStream.writeBytes(twoHyphens + boundary + twoHyphens + crlf);		
		requestStream.flush();
		requestStream.close();
		
		//Get the response, convert to string and print.
		InputStream responseStream = new BufferedInputStream(connection.getInputStream());
		BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
		String line = "";
		StringBuilder builder = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			builder.append(line).append("\n");
		}
		connection.disconnect();
		String response = builder.toString();
		System.out.print(response);
	}
}
