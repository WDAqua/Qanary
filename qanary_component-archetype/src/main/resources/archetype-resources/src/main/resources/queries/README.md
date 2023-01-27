# SPARQL query templates for the queries used in the application

The queries are used in the application to exchange data with the Qanary triplestore.

The queries should be written in a generic way, where the variables might be replaced by the actual values, depending on the bindings you have defined in your source code.

Typically, the queries are used in the following way:

```java
// map for the bindings of the query
QuerySolutionMap bindings = new QuerySolutionMap();
// add a replacement for the variable "name_of_variable" in the query by a string
bindings.add("name_of_variable", "new_value");
// add a replacement for the variable "name_of_variable2" in the query by a URI
bindings.add("name_of_variable2", ResourceFactory.createResource("new_value"));

// load the query from the file system and replace the variables by the actual values
String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap("filename", bindings);  
```
