# XPLAN Resourceful API to OpenApi

XPLAN Resourceful API document to OpenApi 3 specification converter.

## Description

A Spring Boot Shell application used to generate OAS 3 specs based on parsed HTML XPLAN documentation pages.

## Execution

This is an example on how to generate an OpenAPI specification for the `/resourceful/entity/client/:entity_id/asset` endpoint:

### Local execution

```shell
export XPLAN_USERNAME=myusername
export XPLAN_PASSWORD=mypassword
export XPLAN_APP_ID=myappid
./mvnw spring-boot:run
shell:> generate --uri https://<xplan-host>/resourceful/entity/client/:entity_id/asset --output ./asset_collection_api.json
```

### Docker execution

```shell
docker run -it -v `pwd`:/tmp -e XPLAN_USERNAME=<myusername> -e XPLAN_PASSWORD=<mypassword> -e XPLAN_APP_ID=<myappid> uesleilima/xplan-rapi-to-openapi
shell:> generate --uri https://<xplan-host>/resourceful/portfolio/group/account/code/:code/position_summary
```
Then a file named `openapi.json` will be created in your current (`$(pwd)`) folder.

## Custom Build

Java 17 is required.

```shell
./mvnw spring-boot:build-image
```
