package com.example.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
@SpringBootApplication


class MyListener implements MessageListener {
	 
    @Override
    public void onMessage(Message message) {
    	
        try {
        	
         // The credentials added are as ## , and are active , simply execute the jar file.
        	BasicAWSCredentials awsCreds = new BasicAWSCredentials("#####################" , "########################" );
            Regions clientRegion = Regions.US_EAST_1; // Deepak Need to change
            String bucketName = "rekognitions3demo"; // Deepak Need to change
            
            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);            
            ListObjectsV2Result result;
            
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withRegion(clientRegion)
                    .build();
            
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
         		   .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
         		   .withRegion(clientRegion)
         		   .build();
            
            result = s3Client.listObjectsV2(req);    
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) 
            {    
               String m = (String) ((TextMessage) message).getText().toString();
     		   if(objectSummary.getKey().contains(m))
     		   {
     			  // System.out.println("Received: " + ((TextMessage) message).getText());
     			   String photo = objectSummary.getKey();
     			   //text rekognition of the image from the queue
     			   DetectTextRequest request = new DetectTextRequest()
     			              .withImage(new Image()
     			              .withS3Object(new S3Object()
     			              .withName(photo)
     			              .withBucket(bucketName)));            			    
     			   try {
     			          DetectTextResult result1 = rekognitionClient.detectText(request);
     			          List<TextDetection> textDetections = result1.getTextDetections();
     			          if (!textDetections.isEmpty()) {
     			        	  	System.out.print("Text Detected lines and words for:  " + photo + " ==> ");
     			        	  for (TextDetection text: textDetections) {
     			        		
     			        		 System.out.print("  Text Detected: " + text.getDetectedText() + " , Confidence: " + text.getConfidence().toString());
     			        		 System.out.println();
     			        		  }			              
     			         }
     			     } catch(AmazonRekognitionException e) {
     			    	  System.out.print("oops in the catch");
     			          e.printStackTrace();
     			      }
     		   }
            }
            
        } catch (JMSException e) {
           System.out.println("Please run the instance 1 first..");
        }
    }
}


public class CloudComputing2Application {
	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudComputing2Application.class, args);
		/***
 	   Please replace Accesskey and SecretAcessKey with your aws credentials.
 	 */
 	   
 	
		BasicAWSCredentials awsCreds = new BasicAWSCredentials("#################" , "##########################3" );
    	
        Regions clientRegion = Regions.DEFAULT_REGION; // Deepak Need to change
        String bucketName = "bucket-name"; // Need to change Deepak
        try {
        	
            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                 .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                 .withRegion(clientRegion)
                 .build();
            
          //creating sqs queue even if it is not created it will wait for instance 2 to start first.
            try {
         // Create a new connection factory with all defaults (credentials and region) set automatically
            SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), AmazonSQSClientBuilder.defaultClient());
            
            // Create the connection.
            SQSConnection connection = connectionFactory.createConnection();
            // Get the wrapped client
            AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
            
            if (!client.queueExists("MyQueue.fifo")) {
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put("FifoQueue", "true");
                attributes.put("ContentBasedDeduplication", "true");
                client.createQueue(new CreateQueueRequest().withQueueName("MyQueue.fifo").withAttributes(attributes));
            }
             
                 
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // Create a queue identity and specify the queue name to the session
            Queue queue = session.createQueue("MyQueue.fifo");
           
         // Create a consumer for the 'MyQueue'.
            MessageConsumer consumer = session.createConsumer(queue);
             
            // Instantiate and set the message listener for the consumer.
            consumer.setMessageListener(new MyListener());
             
            // Start receiving incoming messages.
            connection.start();
            
            Thread.sleep(10000);
            
            }catch(Exception e) {
            	System.out.println("Please run the instance 1, the program will wait for the queue to have elements.");
            	SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), AmazonSQSClientBuilder.defaultClient());
                
                // Create the connection.
                SQSConnection connection = connectionFactory.createConnection();
                // Get the wrapped client
                AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
                
                if (!client.queueExists("MyQueue.fifo")) {
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put("FifoQueue", "true");
                    attributes.put("ContentBasedDeduplication", "true");
                    client.createQueue(new CreateQueueRequest().withQueueName("MyQueue.fifo").withAttributes(attributes));
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    // Create a queue identity and specify the queue name to the session
                    Queue queue = session.createQueue("MyQueue.fifo");
                   
                 // Create a consumer for the 'MyQueue'.
                    MessageConsumer consumer = session.createConsumer(queue);
                     
                    // Instantiate and set the message listener for the consumer.
                    consumer.setMessageListener(new MyListener());
                     
                    // Start receiving incoming messages.
                    connection.start();
                    
                    Thread.sleep(10000);
                }
            }
          
            // Wait for 1 second. The listener onMessage() method is invoked when a message is received.
            
         }catch (AmazonServiceException e) {
        	 System.out.println("Please run the instance 1 first.This program will wait..");           
        }
    	}
}

