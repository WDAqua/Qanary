# Web Frontend for Pipeline Configuration

This is a basic React App providing a web frontend for changing pipeline configurations 
defined in `application.properties` (`application.local.properties`).

## Available Scripts

In the project directory, you can run:

- `npm start` to run the app in development mode
- `npm run build` to build the app for production

## Connecting to the Pipeline

In order to connect to the pipeline service, the two environment variables 
`REACT_APP_HOST` and `REACT_APP_PORT` have to be used. Their default values are 
`localhost` and `8080`.

## Docker

A [Dockerfile](./Dockerfile) is included. Please keep in mind that any changes to environment
variables will only take effect *after rebuilding* the image.

Pipeline and UI can easily be started together using [this](../docker-compose.yml) docker compose file. 
It is meant as a foundation to expand upon as needed. The variables used are defined in an 
external [.env file](../.env).

## Making Changes to the Configuration

As the `application.properties` file provided with the `qanary_pipeline-template` should not
be altered, changes will be saved to `applicaton.local.properties` the path of which is defined by 
`spring.config.location` in the properties file. 

Changes made using the UI will be effective directly after saving without the need to restart the pipeline. 
However, Changes to any other configuration file will require a restart of the application.

Due to limitations in the current implementation this file will be stored outside of the location 
commonly used for spring applications.
