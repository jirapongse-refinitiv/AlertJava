# AlertJava

dependencies

```
<dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sqs</artifactId>
</dependency>
<!-- https://mvnrepository.com/artifact/com.mashape.unirest/unirest-java -->
<dependency>
    <groupId>com.mashape.unirest</groupId>
    <artifactId>unirest-java</artifactId>
    <version>1.4.9</version>
</dependency>
<dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>url-connection-client</artifactId>
	
</dependency>
```

mvn clean compile assembly:single
java -cp target/my-app-1.0-SNAPSHOT-jar-with-dependencies.jar com.mycompany.app.App
