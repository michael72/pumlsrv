# pumlsrv
Small and efficient PlantUML HTTP Server

This is a small (fast) replacement of [Plant-UML Server](https://github.com/plantuml/plantuml-server) which can run without installing any other http servers.

## Abstract
This http server runs on localhost on with the given port - default port is 8080. It can simply be started with
```cmd
java -jar pumlsrv*.jar <Port>
```

Also the environment variable `PUMLSRV_PORT`is checked and used when no parameter is given.

Editors that can connect to the official PlantUML - such as jebbs excellent PlantUML plugin running in Visual Studio Code - can simply connect via http protocol.

Once the http server is running the following link - when opened on a the same PC - should show the default Alice -> Bob diagram as SVG:
http://localhost:8080/plantuml/svg/SyfFKj2rKt3CoKnELR1Io4ZDoSa70000

The same as the official Plant-UML server pumlsrv also supports output types png, svg, eps, epstext and txt.

Have fun!
