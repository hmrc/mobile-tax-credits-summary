mobile-tax-credits-summary
=============================================

[ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-tax-credits-summary/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-tax-credits-summary/_latestVersion)

Allows users to view their tax credits information and perform a renewal.

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```. 

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/income/:nino/tax-credits/tax-credits-summary``` | GET | Fetch the Tax Credits Summary object for a given NINO. [More...](docs/tax-credits-summary.md)|

# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint,e.g.
```
    GET /sandbox/income/:nino/tax-credits/tax-credits-summary
```

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:

| *Value* | *Description* |
|--------|----|
| "TAX-CREDITS-USER" | Happy path, non-excluded Tax Credits User with full summary |
| "WORKING-TAX-CREDIT-ONLY" | Happy path, non-excluded Tax Credits User with working tax credit data but no child tax credit data |
| "CHILD-TAX-CREDIT-ONLY" | Happy path, non-excluded Tax Credits User with child tax credit data but no working tax credit data |
| "PRE_FTNAE" | Happy path, where they have FTNAE children but it is BEFORE the 31st August deadline |
| "POST_FTNAE" | Happy path, where they have FTNAE children but it is AFTER the 31st August deadline |
| "NON-TAX-CREDITS-USER" | Happy path, non-excluded, Non Tax Credits User with no summary |
| "EXCLUDED-TAX-CREDITS-USER" | Happy path, excluded Tax Credits User with no summary |
| "CLAIMANTS_FAILURE" | Unhappy path, non-excluded Tax Credits User with partial summary missing claimants section | 
| "ERROR-401" | Unhappy path, trigger a 401 Unauthorized response |
| "ERROR-403" | Unhappy path, trigger a 403 Forbidden response |
| "ERROR-500" | Unhappy path, trigger a 500 Internal Server Error response |
| Not set or any other value | Happy path, non-excluded Tax Credits Users |

To start the service locally either use service-manager or clone this repo and use sbt to start:
```
 cd $WORKSPACE/mobile-tax-credits-summary
 sbt start 
```

Once its running then to test the default behaviour (non-excluded Tax Credits Users payload):
```
curl -i -H"Accept: application/vnd.hmrc.1.0+json" localhost:8246/sandbox/income/CS700100A/tax-credits/tax-credits-summary
```

For more information, visit Confluence and see the following page:
```/display/NGC/How+to+shutter+NGC+apis```

# Definition
API definition for the service will be available under `/api/definition` endpoint.
See definition in `/conf/api-definition.json` for the format.
