# Adding a Maven Dependency to a Java Project

This README provides instructions on how to add the new package `TOOLS RAMSES` to the dependencies of a Java project using Maven.

## Steps:

1. **Copy the package `tools.ramses`:**

   Ensure that the package specified in <groupId> (e.g., tools.ramses) is present in the project directory.
   Run the following command to install the package locally otherwise the new dependency will not be founded in Maven:

    ```
    mvn clean install
    ```

3. **Open the `pom.xml` file:**

   Open the `pom.xml` file located in the root of your project. This is the Maven configuration file that manages the project's dependencies.

4. **Add the dependency:**

   Find the `<dependencies>` section in the `pom.xml` file. Inside this section, add the dependency for the new package in the following format:

   ```xml
   <dependencies>
       <!-- Other dependencies -->
       <dependency>
           <groupId>tools.ramses</groupId>
           <artifactId>module</artifactId>
           <version>1.0</version>
       </dependency>
   </dependencies>
