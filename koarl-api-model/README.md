# koarl-api-model

Kotlin module which includes the shared models between library and backend.

Those model classes should be used as models for API facing operations and
internal processing, but should not be used for persistence in
e.g. Room on Android or the database in the backend.


This package is not to be published as an artifact standalone, but 
should be included within the respective packages where needed.