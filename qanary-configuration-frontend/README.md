# Web Frontend for Qanary Pipeline Configuration

This is a basic React App providing a Web frontend for changing the Qanary pipeline configurations 
defined in `application.properties` (`application.local.properties`).

## Build and Start the Qanary Pipeline Configuration Web Frontend

In the project directory, you can run:

- `npm start` to run the app in development mode
- `npm run build` to build the Web app for production

## Connecting to the Qanary Pipeline

In order to connect to the Qanary pipeline service, the two environment variables 
`REACT_APP_HOST` and `REACT_APP_PORT` have to be used. Their default values are 
`localhost` and `8080`.

## Docker

### Dockerfile: Run as standalone system

A [Dockerfile](./Dockerfile) is included. Please keep in mind that any changes to the environment
variables will only take effect *after rebuilding* the Docker image.

#### Configure Docker Container

```
docker run -e REACT_APP_HOST=<host> -e REACT_APP_PORT=<port> qanary/qanary-configuration-frontend:<version>
```

Example:

```
docker run -e REACT_APP_HOST=localhost -e REACT_APP_PORT=8080 qanary/qanary-configuration-frontend:1.0.0
```

### docker-compose: Run as part of the whole system

The Qanary pipeline and Qanary Configuration UI can easily be started together using [this Docker compose file](../docker-compose.yml). 
It is meant as a foundation to expand upon as needed. The variables used are defined in an external [.env file](../.env).

## Making Changes to the Qanary Configuration

As the `application.properties` file provided with the `qanary_pipeline-template` should not
be altered, changes will be saved to `applicaton.local.properties` the path of which is defined by 
`spring.config.location` in the properties file. 

Changes made using this Configuration UI will be effective directly after saving.
A restart of the Qanary pipeline is not required. 
However, changes to any other configuration file will require a restart of the application.

Due to limitations in the current implementation this file will be stored outside of the location 
commonly used by Spring applications.
