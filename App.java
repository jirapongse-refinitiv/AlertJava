package com.mycompany.app;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.URI;
/* Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        App app = new App();
        String token = app.GetToken("<username>","<password>", "<client id>");
        System.out.println("Token: "+token);
        
        JsonNode newsResponse = app.SubscribeNewsStories(token);
        
        System.out.println("Endpoint: "+newsResponse.getObject().getJSONObject("transportInfo").getString("endpoint"));
        String endPoint = newsResponse.getObject().getJSONObject("transportInfo").getString("endpoint");
        String cryptographyKey = newsResponse.getObject().getJSONObject("transportInfo").getString("cryptographyKey");
        String subscriptionID = newsResponse.getObject().getString("subscriptionID");
        		
        JsonNode cloudCredResponse = app.GetCloudCredential(token , endPoint);
        
        String accessKeyId = cloudCredResponse.getObject().getJSONObject("credentials").getString("accessKeyId");
        String secretKey = cloudCredResponse.getObject().getJSONObject("credentials").getString("secretKey");
        String sessionToken = cloudCredResponse.getObject().getJSONObject("credentials").getString("sessionToken");
        String cloudEndPoint = cloudCredResponse.getObject().getString("endpoint");
        System.out.println("Credentials:");
        
        System.out.println("\taccessKeyId: "+accessKeyId);
        System.out.println("\tsecretKey: "+secretKey);
        System.out.println("\tsessionToken: "+sessionToken);
        System.out.println("\tendpoint: "+cloudEndPoint);
        
        app.RetrieveMessage(accessKeyId, secretKey, sessionToken, cloudEndPoint, cryptographyKey);
        
        System.out.println("Delete Subscription: "+subscriptionID);
        
        app.DeleteSubcription(token, subscriptionID);
        		
    }

    public void DeleteSubcription(String token, String subscriptionID) {
    	Unirest.setTimeouts(0, 0);
    	try {
			HttpResponse<String> response = Unirest.delete("https://api.refinitiv.com/alerts/v1/news-stories-subscriptions?subscriptionID="+subscriptionID)
				.header("Authorization", "Bearer "+token)
				.asString();
			System.out.println(response.getStatus());
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    public void RetrieveMessage(String accesssKeyId, String secretKey, String sessionToken, String endpoint, String cryptographyKey) {
    	
    	AwsCredentials credentials = AwsSessionCredentials.create(accesssKeyId, secretKey, sessionToken);
    	

    	SqsClient sqsClient = SqsClient.builder()
    	  .region(Region.US_EAST_1)    	
    	  .credentialsProvider(()->credentials)  
//    	  .httpClientBuilder(UrlConnectionHttpClient.builder()
//                  .socketTimeout(Duration.ofMinutes(5))
//                  .proxyConfiguration(proxy -> proxy.endpoint(URI.create("http://localhost:8080"))))    	 
    	  .build();
    	
    	System.out.println(endpoint);
    	// Receive messages from the queue
    	for(int i=1;i<=10;i++) {
    	
	        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
	            .queueUrl(endpoint)
	            .maxNumberOfMessages(10)
	            .waitTimeSeconds(20)
	            .build();
	        
	        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
	        for (Message m : messages) {
	            //System.out.println("\n" +m.body());
	            try {
	            	
	            	 String s = new String(decrypt(cryptographyKey, m.body()), StandardCharsets.UTF_8);
	            	 System.out.println(s);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
    	}
	
    	
        
//    	AmazonSQS sqs = AmazonSQSClientBuilder.standard()
//    			  .withCredentials(new AWSStaticCredentialsProvider(credentials))
//    			  .withRegion(Regions.US_EAST_1)
//    			  .build();

    }
    
    public  byte[] decrypt(String key, String source) throws Exception {
        int GCM_AAD_LENGTH = 16;
        int GCM_TAG_LENGTH = 16;
        int GCM_NONCE_LENGTH = 12;

        byte[] decodedKey = Base64.getDecoder().decode(key);
        byte[] decodedSource = Base64.getDecoder().decode(source);

        byte[] aad = new byte[GCM_AAD_LENGTH];
        System.arraycopy(decodedSource, 0, aad, 0, GCM_AAD_LENGTH);

        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        System.arraycopy(aad, GCM_AAD_LENGTH - GCM_NONCE_LENGTH, nonce, 0, GCM_NONCE_LENGTH);

        byte[] tag = new byte[GCM_TAG_LENGTH];
        System.arraycopy(decodedSource, decodedSource.length - GCM_TAG_LENGTH, tag, 0, GCM_TAG_LENGTH);

        byte[] encMessage = new byte[decodedSource.length - GCM_AAD_LENGTH];
        System.arraycopy(decodedSource, GCM_AAD_LENGTH, encMessage, 0, encMessage.length);

        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParams = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParams);
        cipher.updateAAD(aad);

        byte[] decMessage = cipher.doFinal(encMessage);

        //byte[] authenticated = cipher.doFinal(tag);
        //if (!MessageDigest.isEqual(authenticated, tag)) {
         //   throw new Exception("Authentication tag mismatch!");
        //}

        return decMessage;
    }

    public JsonNode GetCloudCredential(String token, String endpoint) {
    	
    	Unirest.setTimeouts(0, 0);
    	try {
			HttpResponse<JsonNode> response = Unirest.get("https://api.refinitiv.com/auth/cloud-credentials/v1/?endpoint="+endpoint)
			    .header("Authorization", "Bearer "+token)
			    .asJson();
			return response.getBody();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

    }
    
    public JsonNode SubscribeNewsStories(String token) {
    	Unirest.setTimeouts(0, 0);
    	try {
			HttpResponse<JsonNode> response = Unirest.post("https://api.refinitiv.com/alerts/v1/news-stories-subscriptions")
			    .header("content-type", "application/json")
			    .header("Authorization", "Bearer "+token)
			    .body("{\"transport\":{\"transportType\":\"AWS-SQS\"}}")
			    .asJson();
			return response.getBody();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    	
    	

    }
    public JsonNode SubscribeNewsHeadlines(String token) {
    	Unirest.setTimeouts(0, 0);
    	try {
			HttpResponse<JsonNode> response = Unirest.post("https://api.refinitiv.com/alerts/v1/news-headlines-subscriptions")
			    .header("content-type", "application/json")
			    .header("Authorization", "Bearer "+token)
			    .body("{\"transport\":{\"transportType\":\"AWS-SQS\"}}")
			    .asJson();
			return response.getBody();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    	
    	

    }
    public String GetToken(String username, String password, String clientId) {
    	try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<JsonNode> response = Unirest.post("https://api.refinitiv.com/auth/oauth2/v1/token")
              .header("Content-Type", "application/x-www-form-urlencoded")
              .field("username", username)
              .field("password", password)
              .field("grant_type", "password")
              .field("scope", "trapi")
              .field("takeExclusiveSignOnControl", "true")
              .field("client_id", clientId)
              .asJson();
              //.asString();
            return response.getBody().getObject().getString("access_token");
            }catch (UnirestException ex) {
            	return null;
            }

    }
}
