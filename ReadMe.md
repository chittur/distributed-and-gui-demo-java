# Distributed GUI Demo

This repository contains a Java implementation of a distributed GUI demo application showcasing MVVM architecture, networking with UDP, and messaging capabilities for both chat and images. This project is built based on the original .NET application available at: https://github.com/chittur/distributed-and-gui-demo

## Overview

The application demonstrates a distributed system where multiple instances can communicate over a network using UDP. It supports sending and receiving text messages and images, with a GUI interface to interact with the system. The implementation follows the Model-View-ViewModel (MVVM) design pattern to separate concerns between the UI and business logic, alongside other patterns like factory and observer for creating communicators and handling messages.

## Project Structure

- **src/main/java/com/example/distributedguidemojava/**: Contains the Java project with JavaFX for the GUI.
  - Key packages include `gui` (UI), `networking` (UDP communication), `chatmessaging` (text messaging), `imagemessaging` (image handling), and `viewmodel` (business logic).
- **src/test/java/**: Contains unit tests for the application.
- **pom.xml**: Maven build configuration file.
- **Images/**: Contains diagrams and visual resources relevant to the project (e.g., class diagrams, module diagrams).
- **CONTRIBUTING.md**: Guidelines for contributing to this repository.
- **License.txt**: Licensing information for the project.

## Getting Started

### Java Implementation
1. **Prerequisites**: Ensure you have JDK 17 or later installed (download from https://www.oracle.com/java/technologies/javase-jdk17-downloads.html or https://adoptium.net/) and Maven installed (download from https://maven.apache.org/download.cgi). Set the `JAVA_HOME` environment variable and ensure `mvn` is in your PATH.
2. **Build**: Navigate to the project root in a terminal or command prompt and run `mvn clean install` to compile and download dependencies.
3. **Run**: Launch the application with `mvn javafx:run` to start the JavaFX GUI.
4. **Testing**: Run unit tests with `mvn test` to execute the test suite located in `src/test/java/`.
5. **IDE Support**: Import the project into IntelliJ IDEA or Eclipse by opening the `pom.xml` file for easier development and testing.

## Diagrams

- **[ClassDiagram.jpeg](Images/ClassDiagram.jpeg)**: Illustrates the class structure for the application.
- **[ModuleDiagram.jpeg](Images/ModuleDiagram.jpeg)**: Shows the modular architecture of the system.

## License

This project is licensed under the terms specified in `License.txt`.

## Contributing

Please read `CONTRIBUTING.md` for details on how to contribute to this project.
