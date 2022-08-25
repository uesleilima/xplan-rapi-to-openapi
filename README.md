# XPLAN Resourceful API to OpenApi

XPLAN Resourceful API document to OpenApi 3 specification converter.

## Execution

This is an example on how to generate an OpenAPI specification for the `/resourceful/entity/client/:entity_id/asset` endpoint:

```shell
export XPLAN_USERNAME=myusername
export XPLAN_PASSWORD=mypassword
./mvnw spring-boot:run
export --uri https://<xplan-host>/resourceful/entity/client/:entity_id/asset --output ./asset_collection_api.json
```
