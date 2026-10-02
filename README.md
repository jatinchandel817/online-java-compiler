# Online Java Compiler

A basic local Java compiler website using Java 25, Spring Boot, Thymeleaf, HTML and CSS. It uses no JavaScript.

## Requirements
- JDK 25; both `java -version` and `javac -version` should work.
- IntelliJ IDEA.
- Internet access on first run so Maven can download dependencies.

## Run
1. Extract the ZIP.
2. In IntelliJ IDEA, select **File → Open** and choose the extracted `online-java-compiler` folder containing `pom.xml`.
3. Wait for Maven to import dependencies.
4. Under **File → Project Structure → Project**, select JDK 25 as the Project SDK.
5. Run `OnlineJavaCompilerApplication.java`.
6. Visit http://localhost:8080.

If Maven dependencies do not load, open the Maven tool window and click **Reload All Maven Projects**.

## Use
- Enter Java code in the source editor.
- Keep the class name `Main` and do not add a package declaration.
- Add optional input for `Scanner`.
- Click **Compile and Run**.

## Security warning
This is a local learning prototype, not a production-safe public compiler. It executes submitted code as the operating-system user running Spring Boot. Do not deploy it publicly or expose port 8080 to the internet. A public compiler needs a separately isolated sandbox/container, network disabled, strict resource and process limits, and additional security controls.
