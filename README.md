# Akka Service Discovery

This project aims to be an Akka extension for exposing service discovery. Here are some goals:

- Decoupled domain model for service discovery
- Easy setup and usage
- Lookups and registration from locally cached information
- Enable the collection of local statistics to make smarter lookup decisions.
- Enable smarter edge routing
- Enable better cloud reporting

# Back Ends

The Akka extension defines the service discovery operations, but they are implemented by a discovery back end.

To pick a backend, include one of: `akka-service-discovery-cluster` or `akka-service-discovery-eureka` in your
project… but not both.

## Akka Data Replication

Use Akka clustering to replicate service instance state across the cluster, allowing for local lookups of services.
 
## Eureka

Sure, why not.

# Core Model

The core model contains the case classes which define the service discovery domain. There are no dependencies beyond
the scala-library and [Enumeratum](https://github.com/lloydmeta/enumeratum)

![Core Model Diagram](doc/model.png)