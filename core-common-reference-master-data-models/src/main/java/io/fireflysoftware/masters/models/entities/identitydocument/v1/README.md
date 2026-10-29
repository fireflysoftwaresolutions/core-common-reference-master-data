# Identity Document Catalog System

[Repository guide](../../../../../../../../../../../README.md) · [Current contracts](../../../../../../../../../../../docs/openplatform/reference.md)

This document provides an overview of the Identity Document Catalog System, which manages various types of identity documents used for identification.

## Overview

The Identity Document Catalog System provides a flexible way to manage and use identity documents for various purposes:

1. **Identity Document Categories**: Categorizes identity documents (Personal, Business, Government, etc.)
2. **Identity Documents**: Defines the identity documents (Passport, National ID, Driver's License, etc.)
3. **Identity Document Localizations**: Provides translations of identity document information

The system supports:
- Document type categorization
- Country-specific document types
- Validation rules for document formats
- Localization for multiple languages
- Full CRUD operations via REST API


## Prerequisites and record ownership

These examples describe stored catalog records, not customer identity verification or a posted financial transaction. Use a disposable, migrated master-data service and a caller authorized to administer the catalog. Create/read the required parent catalog and country/locale records first. All IDs are UUIDs; the synthetic values below illustrate relationships and must be replaced by IDs actually returned in your fixture. Leave generated IDs/timestamps out of create input.

The current client coordinate is `io.fireflysoftware:core-common-reference-master-data-sdk:26.10.6`. Configure the generated client's base URL and the consuming application's verified credentials/tenant context. Source-checked examples here are not records of executed HTTP calls.

## Components

### Identity Document Category Catalog

The Identity Document Category Catalog defines the categories of identity documents. Each category has:

- A unique code
- A name
- A description

### Identity Document Catalog

The Identity Document Catalog defines the identity documents that can be used for identification. Each document has:

- A unique code
- A name
- A reference to a category
- A country association (for country-specific document types)
- A description
- Optional validation regex for format validation
- Optional format description

### Identity Document Localization

The Identity Document Localization component provides translations of identity document information for different languages/locales. Each localization has:

- A reference to an identity document
- A reference to a language/locale
- A localized document name
- A localized description
- A localized format description

## Usage Examples

### Creating an Identity Document Category

```http
POST /api/v1/identity-document-categories
{
  "categoryCode": "GOVERNMENT",
  "categoryName": "Government",
  "description": "Government-issued identification documents",
  "status": "ACTIVE"
}
```

Example response:
```json
{
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "categoryCode": "GOVERNMENT",
  "categoryName": "Government",
  "description": "Government-issued identification documents",
  "status": "ACTIVE",
  "dateCreated": "2023-06-15T11:30:00",
  "dateUpdated": "2023-06-15T11:30:00"
}
```

### Creating an Identity Document

```http
POST /api/v1/identity-documents
{
  "documentCode": "PASSPORT",
  "documentName": "Passport",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "countryId": "550e8400-e29b-41d4-a716-446655440001",
  "description": "International passport for travel and identification",
  "validationRegex": "^[A-Z0-9]{9}$",
  "formatDescription": "9 characters, alphanumeric",
  "status": "ACTIVE"
}
```

Example response:
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "documentCode": "PASSPORT",
  "documentName": "Passport",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "category": {
    "categoryId": "550e8400-e29b-41d4-a716-446655440000",
    "categoryCode": "GOVERNMENT",
    "categoryName": "Government",
    "description": "Government-issued identification documents",
    "status": "ACTIVE",
    "dateCreated": "2023-06-15T11:30:00",
    "dateUpdated": "2023-06-15T11:30:00"
  },
  "countryId": "550e8400-e29b-41d4-a716-446655440001",
  "description": "International passport for travel and identification",
  "validationRegex": "^[A-Z0-9]{9}$",
  "formatDescription": "9 characters, alphanumeric",
  "status": "ACTIVE",
  "dateCreated": "2023-06-15T12:45:00",
  "dateUpdated": "2023-06-15T12:45:00"
}
```

### Creating an Identity Document Localization

```http
POST /api/v1/identity-document-localizations
{
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "documentName": "Pasaporte",
  "description": "Pasaporte internacional para viajes e identificación",
  "formatDescription": "9 caracteres, alfanuméricos",
  "status": "ACTIVE"
}
```

Example response:
```json
{
  "localizationId": "550e8400-e29b-41d4-a716-446655440004",
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "documentName": "Pasaporte",
  "description": "Pasaporte internacional para viajes e identificación",
  "formatDescription": "9 caracteres, alfanuméricos",
  "status": "ACTIVE",
  "dateCreated": "2023-06-15T13:00:00",
  "dateUpdated": "2023-06-15T13:00:00"
}
```

### Listing Identity Documents by Category

```
GET /api/v1/identity-documents/category/550e8400-e29b-41d4-a716-446655440000?pageNumber=0&pageSize=10
```

Example response:
```json
{
  "content": [
    {
      "documentId": "550e8400-e29b-41d4-a716-446655440002",
      "documentCode": "PASSPORT",
      "documentName": "Passport",
      "categoryId": "550e8400-e29b-41d4-a716-446655440000",
      "category": {
        "categoryId": "550e8400-e29b-41d4-a716-446655440000",
        "categoryCode": "GOVERNMENT",
        "categoryName": "Government",
        "description": "Government-issued identification documents",
        "status": "ACTIVE",
        "dateCreated": "2023-06-15T11:30:00",
        "dateUpdated": "2023-06-15T11:30:00"
      },
      "countryId": "550e8400-e29b-41d4-a716-446655440001",
      "description": "International passport for travel and identification",
      "validationRegex": "^[A-Z0-9]{9}$",
      "formatDescription": "9 characters, alphanumeric",
      "status": "ACTIVE",
      "dateCreated": "2023-06-15T11:35:00",
      "dateUpdated": "2023-06-15T11:35:00"
    },
    {
      "documentId": "550e8400-e29b-41d4-a716-446655440005",
      "documentCode": "NATIONAL_ID",
      "documentName": "National ID Card",
      "categoryId": "550e8400-e29b-41d4-a716-446655440000",
      "category": {
        "categoryId": "550e8400-e29b-41d4-a716-446655440000",
        "categoryCode": "GOVERNMENT",
        "categoryName": "Government",
        "description": "Government-issued identification documents",
        "status": "ACTIVE",
        "dateCreated": "2023-06-15T11:30:00",
        "dateUpdated": "2023-06-15T11:30:00"
      },
      "countryId": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Generic national identification card",
      "status": "ACTIVE",
      "dateCreated": "2023-06-15T11:40:00",
      "dateUpdated": "2023-06-15T11:40:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0
}
```

### Getting Localizations for a Document

```
GET /api/v1/identity-document-localizations/document/550e8400-e29b-41d4-a716-446655440002?pageNumber=0&pageSize=10
```

Example response:
```json
{
  "content": [
    {
      "localizationId": "550e8400-e29b-41d4-a716-446655440004",
      "documentId": "550e8400-e29b-41d4-a716-446655440002",
      "localeId": "550e8400-e29b-41d4-a716-446655440003",
      "documentName": "Pasaporte",
      "description": "Pasaporte internacional para viajes e identificación",
      "formatDescription": "9 caracteres, alfanuméricos",
      "status": "ACTIVE",
      "dateCreated": "2023-06-15T13:00:00",
      "dateUpdated": "2023-06-15T13:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

### Getting a Specific Localization

```
GET /api/v1/identity-document-localizations/document/550e8400-e29b-41d4-a716-446655440002/locale/550e8400-e29b-41d4-a716-446655440003
```

Example response:
```json
{
  "localizationId": "550e8400-e29b-41d4-a716-446655440004",
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "documentName": "Pasaporte",
  "description": "Pasaporte internacional para viajes e identificación",
  "formatDescription": "9 caracteres, alfanuméricos",
  "status": "ACTIVE",
  "dateCreated": "2023-06-15T13:00:00",
  "dateUpdated": "2023-06-15T13:00:00"
}
```

### Updating a Localization

```http
PUT /api/v1/identity-document-localizations/550e8400-e29b-41d4-a716-446655440004
{
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "documentName": "Pasaporte",
  "description": "Pasaporte internacional para viajes e identificación personal",
  "formatDescription": "9 caracteres, alfanuméricos",
  "status": "ACTIVE"
}
```

Example response:
```json
{
  "localizationId": "550e8400-e29b-41d4-a716-446655440004",
  "documentId": "550e8400-e29b-41d4-a716-446655440002",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "documentName": "Pasaporte",
  "description": "Pasaporte internacional para viajes e identificación personal",
  "formatDescription": "9 caracteres, alfanuméricos",
  "status": "ACTIVE",
  "dateCreated": "2023-06-15T13:00:00",
  "dateUpdated": "2023-06-15T13:05:00"
}
```

### Deleting an Identity Document

```
DELETE /api/v1/identity-documents/550e8400-e29b-41d4-a716-446655440002
```

Response: HTTP 204 No Content

### Deleting a Localization

```
DELETE /api/v1/identity-document-localizations/550e8400-e29b-41d4-a716-446655440004
```

Response: HTTP 204 No Content

### Deleting All Localizations for a Document

```
DELETE /api/v1/identity-document-localizations/document/550e8400-e29b-41d4-a716-446655440002
```

Response: HTTP 204 No Content

## Integrate through the generated SDK

The current OpenAPI tag generates `IdentityDocumentsApi`, and the model is `IdentityDocumentCatalogDTO`. The following consumer fragment uses current SDK signatures. Configure `MASTERS_BASE_URL` for the private target and supply verified deployment credentials on the underlying WebClient before use. The optional retry header is null for these reads.

```java
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import io.fireflysoftware.common.reference.master.data.sdk.api.IdentityDocumentsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.IdentityDocumentLocalizationsApi;

ApiClient client = new ApiClient();
client.setBasePath(System.getenv("MASTERS_BASE_URL"));
IdentityDocumentsApi documents = new IdentityDocumentsApi(client);
IdentityDocumentLocalizationsApi localizations = new IdentityDocumentLocalizationsApi(client);

var document = documents.getIdentityDocumentByCode("PASSPORT", null);
var localizedName = document.flatMap(record ->
    localizations.getIdentityDocumentLocalizationByDocumentAndLocale(
        record.getDocumentId(), ownedLocaleId, null)
        .map(localized -> localized.getDocumentName()));
```

`ownedLocaleId` is the UUID of an existing language/locale record in the fixture. Compose the returned publishers in the application's reactive flow; do not treat constructing them as an executed read. Check ACTIVE status and the effective country/document policy before using catalog data in an onboarding decision. The example shows lookup/localization only, not document authenticity or format acceptance.

## Validation and error semantics

`validationRegex` is catalog metadata. A consuming service must decide how to validate input length, approved regex complexity, country/type policy, missing rules, and invalid catalog patterns. A regular-expression match does not prove ownership, authenticity, expiry, AML acceptance, or identity. Do not convert a network outage or invalid catalog configuration into an ordinary invalid-document result, and do not accept a missing regex as proof of a valid identity document.

The current localization controller maps broad service errors to HTTP 404. Consequently a 404 is not reliable proof that only a translation is missing; it may hide a storage/service failure. Avoid an unconditional localization fallback until that error contract is corrected and qualified. Inspect the response and preserve diagnostic correlation without exposing identity-document contents.

## Verify and recover

After creation, read the returned UUID and compare code, category, country, status, and translated fields. Pagination uses `pageNumber` and `pageSize`; the current response is `content`, `totalElements`, `totalPages`, and `currentPage`, rather than the old Spring Page envelope. A delete can cascade localizations; use a disposable fixture and a governed retirement process for production catalog content.

From the repository root, inspect the existing service fixture and run its actual class selector:

```sh
mvn -B -pl core-common-reference-master-data-core -am test -DskipOpenApiGen \
  -Dtest=IdentityDocumentCatalogServiceImplTest,IdentityDocumentLocalizationServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

These service fixtures mock collaborators; they do not establish a complete SDK/HTTP/database round trip or regulated onboarding acceptance. This guide source-checked the contracts and inspected the existing SDK binary signatures, but did not run the snippet or a new product test suite. Keep separate evidence for the deployed API, persistence, and customer verification journey.

## Source contracts

- [Request/response DTO](../../../../../../../../../../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/identitydocument/v1/IdentityDocumentCatalogDTO.java)
- [Actual controller mappings](../../../../../../../../../../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java)
- [SDK generation and coordinate](../../../../../../../../../../../core-common-reference-master-data-sdk/pom.xml)
