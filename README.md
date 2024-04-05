# DiSSCo Name Usage Searcher

## DiSSCo
The DiSSCo implementation of the Name Usage Searcher.
It connects to a Kafka queue where it will consume DigitalSpecimen events.
It will then retrieve the scientific name and classification from the event and query the Name Usage Searcher for the matching records.
If a match is found it will override the taxonomic information in the DigitalSpecimen event with the information from the Name Usage Searcher.
The original scientificName will be stored in the verbatimIdentification, seperated by a pipe `|` if there were multiple taxonIdentifications.
The link to the Catalogue of Life will be added as a entityRelationship to the DigitalSpecimen event.
Last we will update the specimenName and the topicDiscipline according to the taxonomic identification.
The updated event will be sent to a new Kafka topic.

The best way is to first generate an index and store it on S3 then run the S3 Resolver profile to deploy the APIs/Kafka.

## DiSSCo specific properties
```
# Kafka consumer properties
kafka.consumer.host=# The host of the kafka consumer
kafka.consumer.topic=# The topic name which it needs to listen to
kafka.consumer.group=# The group name of the kafka consumer, defaults to "group"
kafka.consumer.batch-size=# The batch size of the kafka consumer, defaults to 500

# Kafka producer properties
kafka.publisher.host=# The host of the kafka producer
kafka.publisher.topic=# The topic name which it needs to publish to, defaults to "digital-specimen"
```

# Original README

This is a small application that allows you to match names against taxonomies.
It is based on and reuses code developed by the [Global Biodiversity Information Facility (GBIF)](https://www.gbif.org/).
In addition to the GBIF code, this application uses parts of the code developed by [Catalogue of Life](https://www.catalogueoflife.org/).
In essence, it combines the search capabilities of the GBIF name parser with the taxonomic data of the [ChecklistBank](https://www.checklistbank.org/).
It enables the user to pick any dataset from ChecklistBank and run the GBIF matching API on top of it.
This choice was made to reuse as much as possible to avoid conflicting algorithms and data structures.
Additional functional has been added, such as the auto-complete endpoint and the batch endpoint.

## Purpose
The intended purpose is to enable infrastructures to run a name matching index within their own infrastructure, without the need to rely on external services or put their load on them.
This will increase the stability, reliability and speed of the name matching which is especially relevant when matching automatically resolving large amounts of names.
This application is designed to be run as a service, and to be queried by other services.
It takes as input a [ColDP](https://github.com/CatalogueOfLife/coldp) dataset from checklistBank, creates a local index and exposes it through an API.
While it is possible to expose the API to the public domain this is not the intended purpose of this application.
For querying the checklist bank data we would recommend using the [Checklistbank API](https://api.checklistbank.org/).

## Description
When running with all options available the application will have the following flow:
- Retrieves a ColDP dataset from ChecklistBank based on the datasetKey and stores it locally
- Iterates over the NameUsage.tsv file in the ColDP dataset and loads all records in a map (colId -> NameUsage)
- Iterates over the NameUsage.tsv file a second time but now loads the records into a lucene index. 
The first iteration is needed to be able to quickly build the full taxonomic tree of the record.
In the second iteration it will add all related name usages to the record based on the parentId.
It will loop over all records until it can no longer find a parentId, indicating that it reached the root of the tree.
It will then store all information into a lucene index, searches can be done on canonical name and colId.

This concludes the indexing part of the application.
This part can be run separately or be disabled when there is an existing index that can be used, see env variables.

The application will then start a public web server that exposes the index through an API.
The API will allow you to search for a name and return the matching records.
If the request contains an colId it will return the full taxonomic tree of the record.
This endpoint has both an option for single requests (`match`), as well as batch requests (`batch`).
In addition to the name resolution service there is also an auto-complete endpoint (`auto-complete`).
This does a prefix search on the index and searches for records which start with the provided name.

## Profiles
This application uses Spring profiles as feature toggles.
Certain functionality can be enabled or disabled by setting the correct profile.
### Standalone
In this mode the application is compeletly standalone and does not require any external services.
It will download the COL dataset and index it locally.
After indexing it will expose the index through an API.
### S3 Indexer
The S3 Indexer will run only the downloading and indexing part of the application.
After it has created a lucene index it will upload the index to an S3 bucket (`col-indices`).
The Lucene files are prefixed with the COL dataset identifier.
### S3 Resolver
This S3 Resolver profile will download an existing index from an S3 bucket (`col-indices`) and expose it through an API.

## OpenAPI documentation
The API is documented using OpenAPI.
The documentation can be found at the root of the application when running the application at: `localhost:8080/v3/api-docs`
Swagger documentation is included as well at: `localhost:8080/swagger-ui/index.html`

## Environmental variables
The following backend specific properties can be configured:

```
# Indexing properties
The following properties are required for the indexing of the dataset.
indexing.col-dataset=# The identifier of the col dataset. The identifier can be retrieved by looking at the the address bar when viewing a dataset in the checklistbank. For example https://www.checklistbank.org/dataset/2014/about is the dataset with identifier 2014.
indexing.col-username=# The username to use for authenticating with the ChecklistBank. You can create an account for ChecklistBank at https://www.gbif.org/user/profile
indexing.col-password=# The passowrd to use for authenticating with the ChecklistBank. You can create an account for ChecklistBank at https://www.gbif.org/user/profile

The following properties are optionally and have a default value.
indexing.index-location=# The location where the index is stored. Default is src/main/resources/index
indexing.temp-coldp-location=# The location where the ColDP dataset is stored. Default is src/main/resources/sample.zip

# Col properties
These properties are used when downloading the COL Data Package from the ChecklistBank.
All properties have a sensible default which can be overwritten.
col.synonyms=# Whether to include synonyms in the download. Default is true
col.extended=# Whether to include extended data in the download. Default is true
col.extinct=# Whether to include extinct species in the download. Default is true
col.export-status-retry-count=# The amount of times to retry the export status. Default is 10
col.export-status-retry-interval=# The interval between retries in milliseconds. Default is 500 ms (0.5 sec)

# AWS properties
These are properties required for making the connection to the S3 bucket on AWS.
aws.accessKeyId=# The access key id for the AWS account
aws.secretAccessKey=# The secret access key for the AWS account
```

## Install and run
The preferred way to run the application is through container images.
DiSSCo will provide container images for the application through our public image repository.
The application can be run with the following command:
```docker run -p 8080:8080 public.ecr.aws/dissco/name-usage-searcher:latest```

## Docker-Compose
In addition to running the application through a container image, it is also possible to run the application through docker-compose.
An example docker-compose file has been added to the repository.
The command `docker-compose up` will start the application and expose it on port 8080.

### IDE
The application can be run through a Java Integrated Development Environment (IDE) such as IntelliJ.
The environmental variables can be set in the application.properties file.
This way of running is especially helpful when developing or testing the application.

### Other
Other options of running are possible, as the .jar file is generated and can be found in the target folder after running maven.

## Extending the application
This project is meant as a base for further development.
It provides some general functionality which could be sufficient for some use cases.
However, when specific implementations surrounding data models, additional APIs or security is needed you might need to fork this project.
Forking can be done easily through, for example the GitHub interface.
Functionality for which you believe everyone might benefit can be contributed back to the original project through a pull request.

## License
This project is licensed under Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
This license is in line with the code license for both the GBIF code and the Catalogue of Life code.

## Attribution
Each class which was in part or in whole based on the GBIF code or the Catalogue of Life code contains a reference to the original code at the top of the class.
This is to ensure that the original authors are attributed for their work.
