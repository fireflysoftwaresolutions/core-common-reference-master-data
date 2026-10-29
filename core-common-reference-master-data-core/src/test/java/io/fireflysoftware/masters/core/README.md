# Reference-data record services: test guide

[Repository guide](../../../../../../../../README.md) · [Module build](../../../../../../../pom.xml)

## Purpose and boundary

This guide maps the authored tests beneath this directory to their source files and gives reproducible commands for reviewing their actual assertions. The test inventory below is a source inventory, not a measured coverage percentage or a record of a successful run.

Mocked country, bank, currency, consent-catalog, and other reference records do not establish regulatory consent, a live bank connection, holiday-calendar completeness, or production data quality. Catalog content and effective dates require their own governed acceptance.

## Prerequisites and fixture ownership

Run commands from the repository root with JDK 25 selected in both `java --version` and `mvn --version`. Resolve the matching parent/framework and reactor dependencies through your authorized Maven configuration. The listed fixture source controls the synthetic IDs, DTOs, mocked dependencies, and expected outcomes; it is not a seed script for a running bank. Read setup and teardown before extending a fixture or executing another module's integration tests.

## Run and inspect

Run one existing class together with its reactor dependencies:

```sh
mvn -B -pl core-common-reference-master-data-core -am test -DskipOpenApiGen \
  -Dtest=IdentityDocumentLocalizationRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false
```

To run that module's configured test lifecycle after inspecting all of its fixtures:

```sh
mvn -B -pl core-common-reference-master-data-core -am test -DskipOpenApiGen
```

`skipOpenApiGen` prevents the parent's fixed-port API-generation startup; it does not skip tests or isolate external resources. `failIfNoSpecifiedTests=false` permits dependency modules without the selected class. Confirm that the selected class appears in `core-common-reference-master-data-core/target/surefire-reports/` and inspect tests, failures, errors, and skips in the XML. A missing report or skipped case is not a passing qualification. These commands were source-checked for this guide; no new product-test pass is claimed here.

## Source inventory

Each entry lists the existing test method names. Names identify intent; read the linked body to see whether a collaborator, static utility, or the method itself is stubbed.

### [IdentityDocumentLocalizationRepositoryTest](repositories/identitydocument/v1/IdentityDocumentLocalizationRepositoryTest.java)

- `findByDocumentId_ShouldReturnLocalizations`
- `countByDocumentId_ShouldReturnCount`
- `findByDocumentIdAndLocaleId_ShouldReturnLocalization_WhenFound`
- `findByDocumentIdAndLocaleId_ShouldReturnEmpty_WhenNotFound`
- `deleteByDocumentId_ShouldDeleteLocalizations`

### [ActivityCodeServiceImplTest](services/activity/v1/ActivityCodeServiceImplTest.java)

- `listActivityCodes_ShouldReturnPaginatedResponse`
- `getActivityCodesByCountry_ShouldReturnActivityCodes`
- `getChildActivityCodes_ShouldReturnChildActivityCodes`
- `createActivityCode_ShouldReturnCreatedActivityCode`
- `getActivityCode_ShouldReturnActivityCodeWhenFound`
- `getActivityCode_ShouldReturnEmptyWhenNotFound`
- `updateActivityCode_ShouldReturnUpdatedActivityCodeWhenFound`
- `updateActivityCode_ShouldReturnEmptyWhenNotFound`
- `deleteActivityCode_ShouldDeleteWhenFound`

### [BankInstitutionCodeServiceImplTest](services/bank/v1/BankInstitutionCodeServiceImplTest.java)

- `listBankInstitutionCodes_ShouldReturnPaginatedResponse`
- `createBankInstitutionCode_ShouldReturnCreatedEntity`
- `getBankInstitutionCode_ShouldReturnEntityWhenFound`
- `getBankInstitutionCode_ShouldReturnEmptyWhenNotFound`
- `updateBankInstitutionCode_ShouldReturnUpdatedEntityWhenFound`
- `updateBankInstitutionCode_ShouldReturnEmptyWhenNotFound`
- `deleteBankInstitutionCode_ShouldDeleteWhenFound`
- `deleteBankInstitutionCode_ShouldReturnEmptyWhenNotFound`

### [ConsentCatalogServiceImplTest](services/consent/v1/ConsentCatalogServiceImplTest.java)

- `listConsentCatalog_ShouldReturnPaginatedResponse`
- `listConsentCatalogByType_ShouldReturnPaginatedResponse`
- `createConsentCatalog_ShouldReturnCreatedConsentCatalog`
- `getConsentCatalog_ShouldReturnConsentCatalogWhenFound`
- `getConsentCatalog_ShouldReturnEmptyWhenNotFound`
- `updateConsentCatalog_ShouldReturnUpdatedConsentCatalogWhenFound`
- `updateConsentCatalog_ShouldReturnEmptyWhenNotFound`
- `deleteConsentCatalog_ShouldDeleteWhenFound`

### [CountryServiceImplTest](services/country/v1/CountryServiceImplTest.java)

- `createCountry_ShouldReturnCreatedCountry`
- `getCountry_ShouldReturnCountryWhenFound`
- `getCountry_ShouldReturnEmptyWhenNotFound`
- `updateCountry_ShouldReturnUpdatedCountryWhenFound`
- `updateCountry_ShouldReturnEmptyWhenNotFound`
- `deleteCountry_ShouldDeleteWhenFound`
- `deleteCountry_ShouldReturnEmptyWhenNotFound`

### [CurrencyServiceImplTest](services/currency/v1/CurrencyServiceImplTest.java)

- `createCurrency_ShouldReturnCreatedCurrency`
- `getCurrency_ShouldReturnCurrencyWhenFound`
- `getCurrency_ShouldReturnEmptyWhenNotFound`
- `updateCurrency_ShouldReturnUpdatedCurrencyWhenFound`
- `updateCurrency_ShouldReturnEmptyWhenNotFound`
- `deleteCurrency_ShouldDeleteWhenFound`
- `deleteCurrency_ShouldReturnEmptyWhenNotFound`

### [AdministrativeDivisionServiceImplTest](services/division/v1/AdministrativeDivisionServiceImplTest.java)

- `listDivisions_ShouldReturnPaginatedResponse`
- `createDivision_ShouldReturnCreatedDivision`
- `getDivision_ShouldReturnDivisionWhenFound`
- `getDivision_ShouldReturnEmptyWhenNotFound`
- `updateDivision_ShouldReturnUpdatedDivisionWhenFound`
- `updateDivision_ShouldReturnEmptyWhenNotFound`
- `deleteDivision_ShouldDeleteWhenFound`
- `deleteDivision_ShouldReturnEmptyWhenNotFound`

### [IdentityDocumentCatalogServiceImplTest](services/identitydocument/v1/IdentityDocumentCatalogServiceImplTest.java)

- `listIdentityDocuments_ShouldReturnPaginatedResponse`
- `listIdentityDocumentsByCategory_ShouldReturnPaginatedResponse`
- `listIdentityDocumentsByCountry_ShouldReturnPaginatedResponse`
- `createIdentityDocument_ShouldReturnCreatedDocument`
- `getIdentityDocument_ShouldReturnDocument_WhenFound`
- `getIdentityDocument_ShouldReturnError_WhenNotFound`
- `getIdentityDocumentByCode_ShouldReturnDocument_WhenFound`
- `getIdentityDocumentByCode_ShouldReturnError_WhenNotFound`
- `updateIdentityDocument_ShouldReturnUpdatedDocument_WhenFound`
- `updateIdentityDocument_ShouldReturnError_WhenNotFound`
- `deleteIdentityDocument_ShouldDeleteDocument_WhenFound`
- `deleteIdentityDocument_ShouldReturnError_WhenNotFound`

### [IdentityDocumentCategoryCatalogServiceImplTest](services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImplTest.java)

- `listIdentityDocumentCategories_ShouldReturnPaginatedResponse`
- `createIdentityDocumentCategory_ShouldReturnCreatedCategory`
- `getIdentityDocumentCategory_ShouldReturnCategory_WhenFound`
- `getIdentityDocumentCategory_ShouldReturnError_WhenNotFound`
- `getIdentityDocumentCategoryByCode_ShouldReturnCategory_WhenFound`
- `getIdentityDocumentCategoryByCode_ShouldReturnError_WhenNotFound`
- `updateIdentityDocumentCategory_ShouldReturnUpdatedCategory_WhenFound`
- `updateIdentityDocumentCategory_ShouldReturnError_WhenNotFound`
- `deleteIdentityDocumentCategory_ShouldDeleteCategory_WhenFound`
- `deleteIdentityDocumentCategory_ShouldReturnError_WhenNotFound`

### [IdentityDocumentLocalizationServiceImplTest](services/identitydocument/v1/IdentityDocumentLocalizationServiceImplTest.java)

- `listIdentityDocumentLocalizations_ShouldReturnPaginatedResponse`
- `getLocalizationsByDocumentId_ShouldReturnPaginatedResponse`
- `createIdentityDocumentLocalization_ShouldReturnCreatedLocalization`
- `getIdentityDocumentLocalizationByDocumentAndLocale_ShouldReturnLocalization_WhenFound`
- `getIdentityDocumentLocalizationByDocumentAndLocale_ShouldReturnError_WhenNotFound`
- `updateIdentityDocumentLocalization_ShouldReturnUpdatedLocalization_WhenFound`
- `updateIdentityDocumentLocalization_ShouldReturnError_WhenNotFound`
- `deleteIdentityDocumentLocalization_ShouldDeleteLocalization_WhenFound`
- `deleteIdentityDocumentLocalization_ShouldReturnError_WhenNotFound`
- `deleteLocalizationsByDocumentId_ShouldDeleteLocalizations`

### [LegalFormServiceImplTest](services/legal/v1/LegalFormServiceImplTest.java)

- `getLegalFormsByCountry_ShouldReturnLegalForms`
- `createLegalForm_ShouldReturnCreatedLegalForm`
- `getLegalForm_ShouldReturnLegalFormWhenFound`
- `getLegalForm_ShouldReturnEmptyWhenNotFound`
- `updateLegalForm_ShouldReturnUpdatedLegalFormWhenFound`
- `updateLegalForm_ShouldReturnEmptyWhenNotFound`
- `deleteLegalForm_ShouldDeleteWhenFound`
- `deleteLegalForm_ShouldReturnEmptyWhenNotFound`

### [LanguageLocaleServiceImplTest](services/locale/v1/LanguageLocaleServiceImplTest.java)

- `listLanguageLocales_ShouldReturnPaginatedResponse`
- `createLanguageLocale_ShouldReturnCreatedLanguageLocale`
- `getLanguageLocale_ShouldReturnLanguageLocaleWhenFound`
- `getLanguageLocale_ShouldReturnEmptyWhenNotFound`
- `updateLanguageLocale_ShouldReturnUpdatedLanguageLocaleWhenFound`
- `updateLanguageLocale_ShouldReturnEmptyWhenNotFound`
- `deleteLanguageLocale_ShouldDeleteWhenFound`
- `deleteLanguageLocale_ShouldReturnEmptyWhenNotFound`

### [LookupDomainServiceImplTest](services/lookup/v1/LookupDomainServiceImplTest.java)

- `listDomains_ShouldReturnPaginatedResponse`
- `createDomain_ShouldReturnCreatedEntity`
- `getDomain_ShouldReturnEntityWhenFound`
- `getDomain_ShouldReturnEmptyWhenNotFound`
- `updateDomain_ShouldReturnUpdatedEntityWhenFound`
- `updateDomain_ShouldReturnEmptyWhenNotFound`
- `deleteDomain_ShouldDeleteWhenFound`
- `deleteDomain_ShouldReturnEmptyWhenNotFound`

### [LookupItemServiceImplTest](services/lookup/v1/LookupItemServiceImplTest.java)

- `listItems_ShouldReturnPaginatedResponse`
- `getItemsByDomain_ShouldReturnItems`
- `createItem_ShouldReturnCreatedEntity`
- `getItem_ShouldReturnEntityWhenFound`
- `getItem_ShouldReturnEmptyWhenNotFound`
- `updateItem_ShouldReturnUpdatedEntityWhenFound`
- `updateItem_ShouldReturnEmptyWhenNotFound`
- `deleteItem_ShouldDeleteWhenFound`
- `deleteItem_ShouldReturnEmptyWhenNotFound`

### [MessageTypeCatalogServiceImplTest](services/notification/v1/MessageTypeCatalogServiceImplTest.java)

- `listMessageTypes_ShouldReturnPaginatedResponse`
- `createMessageType_ShouldReturnCreatedMessageType`
- `getMessageType_ShouldReturnMessageType_WhenFound`
- `getMessageTypeByCode_ShouldReturnMessageType_WhenFound`
- `updateMessageType_ShouldReturnUpdatedMessageType_WhenFound`
- `deleteMessageType_ShouldDeleteMessageType_WhenFound`

### [NotificationMessageCatalogServiceImplTest](services/notification/v1/NotificationMessageCatalogServiceImplTest.java)

- `listNotificationMessages_ShouldReturnPaginatedResponse`
- `createNotificationMessage_ShouldReturnCreatedMessage`
- `getNotificationMessage_ShouldReturnMessage_WhenFound`
- `getNotificationMessage_ShouldReturnError_WhenNotFound`
- `getNotificationMessageByCode_ShouldReturnMessage_WhenFound`
- `updateNotificationMessage_ShouldReturnUpdatedMessage_WhenFound`
- `deleteNotificationMessage_ShouldDeleteMessage_WhenFound`

### [RelationshipTypeMasterServiceImplTest](services/relationships/v1/RelationshipTypeMasterServiceImplTest.java)

- `listRelationshipTypes_ShouldReturnPaginatedResponse`
- `createRelationshipType_ShouldReturnCreatedEntity`
- `getRelationshipType_ShouldReturnEntityWhenFound`
- `getRelationshipType_ShouldReturnEmptyWhenNotFound`
- `updateRelationshipType_ShouldReturnUpdatedEntityWhenFound`
- `updateRelationshipType_ShouldReturnEmptyWhenNotFound`
- `deleteRelationshipType_ShouldDeleteWhenFound`
- `deleteRelationshipType_ShouldReturnEmptyWhenNotFound`

### [TitleMasterServiceImplTest](services/title/v1/TitleMasterServiceImplTest.java)

- `listTitles_ShouldReturnPaginatedResponse`
- `createTitle_ShouldReturnCreatedEntity`
- `getTitle_ShouldReturnEntityWhenFound`
- `getTitle_ShouldReturnEmptyWhenNotFound`
- `updateTitle_ShouldReturnUpdatedEntityWhenFound`
- `updateTitle_ShouldReturnEmptyWhenNotFound`
- `deleteTitle_ShouldDeleteWhenFound`
- `deleteTitle_ShouldReturnEmptyWhenNotFound`

## Actual source example

This excerpt is `findByDocumentId_ShouldReturnLocalizations` from [IdentityDocumentLocalizationRepositoryTest](repositories/identitydocument/v1/IdentityDocumentLocalizationRepositoryTest.java). Use the surrounding class's imports and fixture setup; this excerpt is not a standalone Java program.

```java
@Test
void findByDocumentId_ShouldReturnLocalizations() {
    // Arrange
    when(repository.findByDocumentId(any(UUID.class), any(Pageable.class))).thenReturn(Flux.just(entity));

    // Act
    Flux<IdentityDocumentLocalization> result = repository.findByDocumentId(testDocumentId, pageable);

    // Assert
    StepVerifier.create(result)
            .expectNext(entity)
            .verifyComplete();
}
```

## Interpret and recover

- A Reactor assertion must subscribe and verify termination; constructing a `Mono` alone does not execute it. Match emitted values, empty completion, and errors to the production contract rather than converting every absence to success.
- A repository or mapper mock verifies a local call/result, not committed database state. A stubbed service method or static filter utility does not exercise its implementation. Use real HTTP/SDK/database cases for those boundaries.
- For a failing mock assertion, reread the production branch and fixture data before changing expected values. Do not make a test pass by manufacturing the behavior inside a save callback.
- For dependency resolution or Java errors, verify the actual Maven runtime and matching reactor artifacts. For no matching tests, check the class name and module rather than disabling all tests.
- Coverage requires a configured measurement run and inspected report. Neither a class list nor a successful Maven exit proves every operation, error branch, authorization rule, provider interaction, or customer journey is covered.

## Maintain this guide

Keep the inventory and runnable class selector aligned with the linked sources. Add regression cases for changed behavior, then record the command, fixture boundary, source revision, and reports in the relevant implementation review. Preserve separate evidence for unit behavior, transport contracts, database recovery, provider sandbox behavior, and live-provider acceptance.
