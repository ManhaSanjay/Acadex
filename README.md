## Acadex - A Student Management System
** How to Use:**
- Download the Acadex-main.zip
- Extract the zip files
- Go to the folder containing the extracted files. eg cd Acadex-main
- Compile the files. ```javac -cp ".:lib/*" *.java```
- Find your IP address.
- Open Mainframe.java and go to the line ```private static final String FORUM_SERVER = "http://192.168.1.14:8080";``` and replace ```192.168.1.14 ``` with your own IP Address.
- Start the Server. ```java -cp ".:lib/*" Server```
- Open Acadex. Open a second terminal window ```java -cp ".:lib/*" Main```
- Use the demo login info or create your own account

** Note: Make sure port 8080 is not already in use. Kill the process already running in 8080 if it is busy. **
