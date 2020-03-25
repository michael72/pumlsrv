# pumlsrv
Small and efficient PlantUML HTTP Server

This is a small (fast) replacement of [https://github.com/plantuml/plantuml-server](Plant-UML Server) which can run without installing any other http servers.

## Abstract
This http server runs on localhost on default port 8080. It can simply be started with
```cmd
java -jar pumlsrv*.jar
```

Editors that can connect to the official PlantUML - such as jebbs excellent PlantUML plugin running in Visual Studio Code - can simply connect via http protocol.

Once the http server is running the following link - when opened on a the same PC - should show the default Alice -> Bob diagram as SVG:
http://localhost:8080/plantuml/svg/SyfFKj2rKt3CoKnELR1Io4ZDoSa70000

