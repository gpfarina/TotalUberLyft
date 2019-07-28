import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Total {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }


    public static List<Message> listMessagesMatchingQuery(Gmail service, String userId,
      String query) throws IOException {
    ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();
    List<Message> messages = new ArrayList<Message>();
    while (response.getMessages() != null) {
      messages.addAll(response.getMessages());
      if (response.getNextPageToken() != null) {
        String pageToken = response.getNextPageToken();
        response = service.users().messages().list(userId).setQ(query)
            .setPageToken(pageToken).execute();
      } else {
        break;
      }
    }
    return messages;
  }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        String user = "me";
        String queryLyft="Your ride with from:no-reply@lyftmail.com after:2016/01/01 before:2019/07/27"; 
        String query="uber receipts after:2016/01/01 before:2019/07/27";
        String priceRegExp = "\\s+[$](\\d+[.]\\d\\d)";
        Pattern pattern = Pattern.compile(priceRegExp);
        List<Message> l= listMessagesMatchingQuery(service, "me", query);
        List<Message> llyft= listMessagesMatchingQuery(service, "me", queryLyft);
        float tot =0.0f;
        for(Message m: l){
            String str = service.users().messages().get(user, m.getId()).execute().getSnippet();
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()){
                tot+=  Float.parseFloat(matcher.group(1));
            }
        }
        System.out.println("Uber: "+tot);
        float totlyft=0.0f;
        for(Message m: llyft){
            String str = service.users().messages().get(user, m.getId()).execute().getSnippet();
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()){
                totlyft+=  Float.parseFloat(matcher.group(1));
            }
        }
        System.out.println("Lyft: "+totlyft);
        tot+=totlyft;
        System.out.println("Total: "+tot);
    }
}
