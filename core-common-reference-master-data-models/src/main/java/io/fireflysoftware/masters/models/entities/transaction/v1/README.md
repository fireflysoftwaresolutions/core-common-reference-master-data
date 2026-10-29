# Transaction Category Catalog

[Repository guide](../../../../../../../../../../../README.md) · [Current contracts](../../../../../../../../../../../docs/openplatform/reference.md)

This package contains the implementation of a hierarchical transaction category catalog for categorizing banking transactions.

## Overview

The Transaction Category Catalog provides a flexible way to categorize banking transactions with a hierarchical structure. It supports:

- Parent-child relationships between categories
- Multiple levels of categorization (e.g., root categories, subcategories, and further nested categories)
- SVG icons for visual representation of categories
- Localization support for multiple languages
- Full CRUD operations via REST API
- Pagination for efficient data retrieval


## Prerequisites and record ownership

These examples describe stored catalog records, not customer identity verification or a posted financial transaction. Use a disposable, migrated master-data service and a caller authorized to administer the catalog. Create/read the required parent catalog and country/locale records first. All IDs are UUIDs; the synthetic values below illustrate relationships and must be replaced by IDs actually returned in your fixture. Leave generated IDs/timestamps out of create input.

The current client coordinate is `io.fireflysoftware:core-common-reference-master-data-sdk:26.10.6`. Configure the generated client's base URL and the consuming application's verified credentials/tenant context. Source-checked examples here are not records of executed HTTP calls.

## Components

### Transaction Category Catalog

The Transaction Category Catalog defines the categories that can be used for transaction categorization. Each category has:

- A unique code
- A name
- A description
- An optional reference to a parent category (for hierarchical structure)
- An optional SVG icon for visual representation
- A status (`ACTIVE` or `INACTIVE`)

### Transaction Category Localization

The Transaction Category Localization component provides translations of transaction category information for different languages/locales. Each localization has:

- A reference to a transaction category
- A reference to a language/locale
- A localized category name
- A localized description

## Usage Examples

### Creating a Root Transaction Category

```http
POST /api/v1/transaction-categories
{
  "categoryCode": "INCOME",
  "categoryName": "Income",
  "description": "Money received from various sources",
  "svgIcon": "<svg>...</svg>",
  "status": "ACTIVE"
}
```

### Creating a Child Transaction Category

```http
POST /api/v1/transaction-categories
{
  "categoryCode": "SALARY",
  "categoryName": "Salary",
  "description": "Regular income from employment",
  "parentCategoryId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE"
}
```

### Retrieving Root Categories

```
GET /api/v1/transaction-categories/root
```

### Retrieving Child Categories

```
GET /api/v1/transaction-categories/parent/{parentCategoryId}
```

### Retrieving a Specific Category

```
GET /api/v1/transaction-categories/{categoryId}
```

or

```
GET /api/v1/transaction-categories/code/{categoryCode}
```

### Creating a Transaction Category Localization

```http
POST /api/v1/transaction-category-localizations
{
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "localeId": "550e8400-e29b-41d4-a716-446655440003",
  "categoryName": "Ingresos",
  "description": "Dinero recibido de varias fuentes",
  "status": "ACTIVE"
}
```

### Retrieving Localizations for a Category

```
GET /api/v1/transaction-category-localizations/category/{categoryId}
```

### Retrieving a Specific Localization

```
GET /api/v1/transaction-category-localizations/category/{categoryId}/locale/{localeId}
```

## Hierarchical Structure Example

The transaction category catalog supports a hierarchical structure, allowing for multiple levels of categorization:

- Income (Root Category)
  - Salary (Child Category)
  - Interest (Child Category)
  - Dividend (Child Category)
  - Rental (Child Category)
- Expense (Root Category)
  - Housing (Child Category)
    - Rent (Grandchild Category)
    - Mortgage Payment (Grandchild Category)
    - Property Tax (Grandchild Category)
  - Food (Child Category)
    - Groceries (Grandchild Category)
    - Restaurants (Grandchild Category)
  - Transportation (Child Category)
  - Utilities (Child Category)

This hierarchical structure allows for more detailed categorization of transactions, which can be useful for financial analysis and reporting.

## Verify and govern the hierarchy

Create a root first, read its returned UUID, then use that UUID as `parentCategoryId` in a child. The hierarchy example above is a taxonomy design, not seeded production content. Validate uniqueness, allowed ancestry, and the absence of self-links/cycles in the owning governance workflow. The current create/update service maps and saves the supplied parent reference; it does not prove a general cycle-detection policy.

Category localizations require existing category and locale UUIDs. Read the returned localization ID and verify that its category/locale association is unchanged. Retire referenced categories through a governed procedure; a destructive delete is not equivalent to reclassifying historical transactions. Sanitization and safe rendering of stored SVG metadata belong to the consumer; do not inline untrusted SVG merely because it came from this catalog.

Inspect the actual deployed error contract for missing or conflicting records. The service's empty reactive result, storage constraint error, and controller mapping are separate behaviors; an API declaration alone does not establish a precise recovery response. After a timeout, query by the governed unique code and reconcile before attempting another create.

This guide describes the current record contracts and did not execute the HTTP examples. The repository's unit fixtures and generated SDK/spec require their own runtime qualification; no automatic transaction categorization, account posting, or financial reporting acceptance is established here.

## Source contracts

- [Request/response DTO](../../../../../../../../../../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/transaction/v1/TransactionCategoryCatalogDTO.java)
- [Actual controller mappings](../../../../../../../../../../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java)
- [SDK generation and coordinate](../../../../../../../../../../../core-common-reference-master-data-sdk/pom.xml)
