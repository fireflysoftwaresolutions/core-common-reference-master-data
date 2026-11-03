# Reference Master Data: source reference

[Repository guide](../../README.md) · [Machine-readable inventory](source-inventory.json)

This reference is derived from checked-in source. It separates declared HTTP mappings, typed contracts, property placeholders, and validation locations. Java reflection, inherited framework routes, generated targets, and runtime infrastructure are outside this static inventory.

## HTTP operations

| Mapping | Method | Controller | Kind |
| --- | --- | --- | --- |
| `GET /api/v1/activity-codes` | `public Mono<ResponseEntity<PaginationResponse<ActivityCodeDTO>>> listActivityCodes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `GET /api/v1/activity-codes/country/{countryId}` | `public Mono<ResponseEntity<Flux<ActivityCodeDTO>>> getActivityCodesByCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `GET /api/v1/activity-codes/parent/{parentCodeId}` | `public Mono<ResponseEntity<Flux<ActivityCodeDTO>>> getChildActivityCodes( @Parameter(in = ParameterIn.PATH, description = "ID of the parent activity code", required = true) @PathVariable UUID parentCodeId )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `GET /api/v1/activity-codes/{activityCodeId}` | `public Mono<ResponseEntity<ActivityCodeDTO>> getActivityCode( @Parameter(in = ParameterIn.PATH, description = "ID of the activity code", required = true) @PathVariable UUID activityCodeId )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `POST /api/v1/activity-codes` | `public Mono<ResponseEntity<ActivityCodeDTO>> createActivityCode( @RequestBody ActivityCodeDTO activityCodeDto )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `PUT /api/v1/activity-codes/{activityCodeId}` | `public Mono<ResponseEntity<ActivityCodeDTO>> updateActivityCode( @Parameter(in = ParameterIn.PATH, description = "ID of the activity code", required = true) @PathVariable UUID activityCodeId, @RequestBody ActivityCodeDTO activityCodeDto )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `DELETE /api/v1/activity-codes/{activityCodeId}` | `public Mono<ResponseEntity<Void>> deleteActivityCode( @Parameter(in = ParameterIn.PATH, description = "ID of the activity code", required = true) @PathVariable UUID activityCodeId )` | [ActivityCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/activity/v1/ActivityCodeController.java) | Local declaration |
| `GET /api/v1/asset-types` | `public Mono<ResponseEntity<PaginationResponse<AssetTypeDTO>>> listAssetTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [AssetTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/assettype/v1/AssetTypeController.java) | Local declaration |
| `GET /api/v1/asset-types/{assetId}` | `public Mono<ResponseEntity<AssetTypeDTO>> getAssetType( @Parameter(in = ParameterIn.PATH, description = "ID of the asset type", required = true) @PathVariable UUID assetId )` | [AssetTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/assettype/v1/AssetTypeController.java) | Local declaration |
| `POST /api/v1/asset-types` | `public Mono<ResponseEntity<AssetTypeDTO>> createAssetType( @RequestBody AssetTypeDTO assetTypeDto )` | [AssetTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/assettype/v1/AssetTypeController.java) | Local declaration |
| `PUT /api/v1/asset-types/{assetId}` | `public Mono<ResponseEntity<AssetTypeDTO>> updateAssetType( @Parameter(in = ParameterIn.PATH, description = "ID of the asset type", required = true) @PathVariable UUID assetId, @RequestBody AssetTypeDTO assetTypeDto )` | [AssetTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/assettype/v1/AssetTypeController.java) | Local declaration |
| `DELETE /api/v1/asset-types/{assetId}` | `public Mono<ResponseEntity<Void>> deleteAssetType( @Parameter(in = ParameterIn.PATH, description = "ID of the asset type", required = true) @PathVariable UUID assetId )` | [AssetTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/assettype/v1/AssetTypeController.java) | Local declaration |
| `GET /api/v1/bank-institution-codes` | `public Mono<ResponseEntity<PaginationResponse<BankInstitutionCodeDTO>>> listBankInstitutionCodes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [BankInstitutionCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/bank/v1/BankInstitutionCodeController.java) | Local declaration |
| `GET /api/v1/bank-institution-codes/{id}` | `public Mono<ResponseEntity<BankInstitutionCodeDTO>> getBankInstitutionCode( @Parameter(in = ParameterIn.PATH, description = "ID of the bank institution code", required = true) @PathVariable UUID id )` | [BankInstitutionCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/bank/v1/BankInstitutionCodeController.java) | Local declaration |
| `POST /api/v1/bank-institution-codes` | `public Mono<ResponseEntity<BankInstitutionCodeDTO>> createBankInstitutionCode( @RequestBody BankInstitutionCodeDTO dto )` | [BankInstitutionCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/bank/v1/BankInstitutionCodeController.java) | Local declaration |
| `PUT /api/v1/bank-institution-codes/{id}` | `public Mono<ResponseEntity<BankInstitutionCodeDTO>> updateBankInstitutionCode( @Parameter(in = ParameterIn.PATH, description = "ID of the bank institution code", required = true) @PathVariable UUID id, @RequestBody BankInstitutionCodeDTO dto )` | [BankInstitutionCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/bank/v1/BankInstitutionCodeController.java) | Local declaration |
| `DELETE /api/v1/bank-institution-codes/{id}` | `public Mono<ResponseEntity<Void>> deleteBankInstitutionCode( @Parameter(in = ParameterIn.PATH, description = "ID of the bank institution code", required = true) @PathVariable UUID id )` | [BankInstitutionCodeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/bank/v1/BankInstitutionCodeController.java) | Local declaration |
| `GET /api/v1/consent-catalog` | `public Mono<ResponseEntity<PaginationResponse<ConsentCatalogDTO>>> listConsentCatalog( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `GET /api/v1/consent-catalog/type/{consentType}` | `public Mono<ResponseEntity<PaginationResponse<ConsentCatalogDTO>>> listConsentCatalogByType( @Parameter(in = ParameterIn.PATH, description = "Type of consent", required = true) @PathVariable String consentType, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `GET /api/v1/consent-catalog/{id}` | `public Mono<ResponseEntity<ConsentCatalogDTO>> getConsentCatalog( @Parameter(in = ParameterIn.PATH, description = "ID of the consent catalog entry", required = true) @PathVariable UUID id )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `POST /api/v1/consent-catalog` | `public Mono<ResponseEntity<ConsentCatalogDTO>> createConsentCatalog( @RequestBody ConsentCatalogDTO dto )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `PUT /api/v1/consent-catalog/{id}` | `public Mono<ResponseEntity<ConsentCatalogDTO>> updateConsentCatalog( @Parameter(in = ParameterIn.PATH, description = "ID of the consent catalog entry", required = true) @PathVariable UUID id, @RequestBody ConsentCatalogDTO dto )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `DELETE /api/v1/consent-catalog/{id}` | `public Mono<ResponseEntity<Void>> deleteConsentCatalog( @Parameter(in = ParameterIn.PATH, description = "ID of the consent catalog entry", required = true) @PathVariable UUID id )` | [ConsentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/consent/v1/ConsentCatalogController.java) | Local declaration |
| `GET /api/v1/contract-document-types` | `public Mono<ResponseEntity<PaginationResponse<ContractDocumentTypeDTO>>> listContractDocumentTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `GET /api/v1/contract-document-types/{documentTypeId}` | `public Mono<ResponseEntity<ContractDocumentTypeDTO>> getContractDocumentType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract document type", required = true) @PathVariable UUID documentTypeId )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `GET /api/v1/contract-document-types/by-code/{documentCode}` | `public Mono<ResponseEntity<ContractDocumentTypeDTO>> getContractDocumentTypeByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the contract document type", required = true) @PathVariable String documentCode )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `POST /api/v1/contract-document-types` | `public Mono<ResponseEntity<ContractDocumentTypeDTO>> createContractDocumentType( @RequestBody ContractDocumentTypeDTO contractDocumentTypeDto )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `PUT /api/v1/contract-document-types/{documentTypeId}` | `public Mono<ResponseEntity<ContractDocumentTypeDTO>> updateContractDocumentType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract document type", required = true) @PathVariable UUID documentTypeId, @RequestBody ContractDocumentTypeDTO contractDocumentTypeDto )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `DELETE /api/v1/contract-document-types/{documentTypeId}` | `public Mono<ResponseEntity<Void>> deleteContractDocumentType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract document type", required = true) @PathVariable UUID documentTypeId )` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) | Local declaration |
| `GET /api/v1/contract-roles/{roleId}` | `public Mono<ResponseEntity<ContractRoleDTO>> getContractRole( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role", required = true) @PathVariable UUID roleId )` | [ContractRoleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleController.java) | Local declaration |
| `POST /api/v1/contract-roles/filter` | `public Mono<ResponseEntity<PaginationResponse<ContractRoleDTO>>> filterContractRoles( @RequestBody FilterRequest<ContractRoleDTO> filterRequest )` | [ContractRoleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleController.java) | Local declaration |
| `POST /api/v1/contract-roles` | `public Mono<ResponseEntity<ContractRoleDTO>> createContractRole( @RequestBody ContractRoleDTO contractRoleDto )` | [ContractRoleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleController.java) | Local declaration |
| `PUT /api/v1/contract-roles/{roleId}` | `public Mono<ResponseEntity<ContractRoleDTO>> updateContractRole( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role", required = true) @PathVariable UUID roleId, @RequestBody ContractRoleDTO contractRoleDto )` | [ContractRoleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleController.java) | Local declaration |
| `DELETE /api/v1/contract-roles/{roleId}` | `public Mono<ResponseEntity<Void>> deleteContractRole( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role", required = true) @PathVariable UUID roleId )` | [ContractRoleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleController.java) | Local declaration |
| `GET /api/v1/contract-role-scopes/{scopeId}` | `public Mono<ResponseEntity<ContractRoleScopeDTO>> getContractRoleScope( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role scope", required = true) @PathVariable UUID scopeId )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `GET /api/v1/contract-role-scopes/role/{roleId}` | `public Flux<ContractRoleScopeDTO> getScopesByRoleId( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role", required = true) @PathVariable UUID roleId )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `GET /api/v1/contract-role-scopes/role/{roleId}/active` | `public Flux<ContractRoleScopeDTO> getActiveScopesByRoleId( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role", required = true) @PathVariable UUID roleId )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `POST /api/v1/contract-role-scopes/filter` | `public Mono<ResponseEntity<PaginationResponse<ContractRoleScopeDTO>>> filterContractRoleScopes( @RequestBody FilterRequest<ContractRoleScopeDTO> filterRequest )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `POST /api/v1/contract-role-scopes` | `public Mono<ResponseEntity<ContractRoleScopeDTO>> createContractRoleScope( @RequestBody ContractRoleScopeDTO contractRoleScopeDto )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `PUT /api/v1/contract-role-scopes/{scopeId}` | `public Mono<ResponseEntity<ContractRoleScopeDTO>> updateContractRoleScope( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role scope", required = true) @PathVariable UUID scopeId, @RequestBody ContractRoleScopeDTO contractRoleScopeDto )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `DELETE /api/v1/contract-role-scopes/{scopeId}` | `public Mono<ResponseEntity<Void>> deleteContractRoleScope( @Parameter(in = ParameterIn.PATH, description = "ID of the contract role scope", required = true) @PathVariable UUID scopeId )` | [ContractRoleScopeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractrole/v1/ContractRoleScopeController.java) | Local declaration |
| `GET /api/v1/contract-types` | `public Mono<ResponseEntity<PaginationResponse<ContractTypeDTO>>> listContractTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [ContractTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contracttype/v1/ContractTypeController.java) | Local declaration |
| `GET /api/v1/contract-types/{contractId}` | `public Mono<ResponseEntity<ContractTypeDTO>> getContractType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract type", required = true) @PathVariable UUID contractId )` | [ContractTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contracttype/v1/ContractTypeController.java) | Local declaration |
| `POST /api/v1/contract-types` | `public Mono<ResponseEntity<ContractTypeDTO>> createContractType( @RequestBody ContractTypeDTO contractTypeDto )` | [ContractTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contracttype/v1/ContractTypeController.java) | Local declaration |
| `PUT /api/v1/contract-types/{contractId}` | `public Mono<ResponseEntity<ContractTypeDTO>> updateContractType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract type", required = true) @PathVariable UUID contractId, @RequestBody ContractTypeDTO contractTypeDto )` | [ContractTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contracttype/v1/ContractTypeController.java) | Local declaration |
| `DELETE /api/v1/contract-types/{contractId}` | `public Mono<ResponseEntity<Void>> deleteContractType( @Parameter(in = ParameterIn.PATH, description = "ID of the contract type", required = true) @PathVariable UUID contractId )` | [ContractTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contracttype/v1/ContractTypeController.java) | Local declaration |
| `GET /api/v1/countries/{countryId}` | `public Mono<ResponseEntity<CountryDTO>> getCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId )` | [CountryController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/country/v1/CountryController.java) | Local declaration |
| `POST /api/v1/countries/filter` | `public Mono<ResponseEntity<PaginationResponse<CountryDTO>>> filterCountries( @RequestBody FilterRequest<CountryDTO> filterRequest )` | [CountryController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/country/v1/CountryController.java) | Local declaration |
| `POST /api/v1/countries` | `public Mono<ResponseEntity<CountryDTO>> createCountry( @RequestBody CountryDTO countryDto )` | [CountryController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/country/v1/CountryController.java) | Local declaration |
| `PUT /api/v1/countries/{countryId}` | `public Mono<ResponseEntity<CountryDTO>> updateCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId, @RequestBody CountryDTO countryDto )` | [CountryController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/country/v1/CountryController.java) | Local declaration |
| `DELETE /api/v1/countries/{countryId}` | `public Mono<ResponseEntity<Void>> deleteCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId )` | [CountryController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/country/v1/CountryController.java) | Local declaration |
| `GET /api/v1/currency-execution-policies/currencies/{currencyId}/source-observation` | `public Mono<CurrencyPolicySourceObservation> observeCurrencyPolicySource( @PathVariable String currencyId)` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) | Local declaration |
| `GET /api/v1/currency-execution-policies/{policyId}` | `public Mono<CurrencyPolicyView> getCurrencyExecutionPolicy( @PathVariable String policyId, @RequestParam String currencyId)` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) | Local declaration |
| `GET /api/v1/currency-execution-policies/by-action/{actionId}` | `public Mono<CurrencyPolicyView> getCurrencyExecutionPolicyByAction( @PathVariable String actionId, @RequestParam String currencyId)` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) | Local declaration |
| `POST /api/v1/currency-execution-policies` | `public Mono<ResponseEntity<CurrencyPolicyView>> publishCurrencyExecutionPolicy( @io.swagger.v3.oas.annotations.parameters.RequestBody( required = true, content = @Content( schema = @Schema( implementation = CurrencyPolicyPublishRequest .class))) @RequestBody String raw)` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) | Local declaration |
| `POST /api/v1/currency-execution-policies/{policyId}/retire` | `public Mono<CurrencyPolicyView> retireCurrencyExecutionPolicy( @PathVariable String policyId, @io.swagger.v3.oas.annotations.parameters.RequestBody( required = true, content = @Content( schema = @Schema( implementation = CurrencyPolicyRetireRequest .class))) @RequestBody String raw)` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) | Local declaration |
| `GET /api/v1/currencies/{currencyId}` | `public Mono<ResponseEntity<CurrencyDTO>> getCurrency( @Parameter(in = ParameterIn.PATH, description = "ID of the currency", required = true) @PathVariable UUID currencyId )` | [CurrencyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/v1/CurrencyController.java) | Local declaration |
| `POST /api/v1/currencies/filter` | `public Mono<ResponseEntity<PaginationResponse<CurrencyDTO>>> filterCurrencies( @RequestBody FilterRequest<CurrencyDTO> filterRequest )` | [CurrencyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/v1/CurrencyController.java) | Local declaration |
| `POST /api/v1/currencies` | `public Mono<ResponseEntity<CurrencyDTO>> createCurrency( @RequestBody CurrencyDTO currencyDto )` | [CurrencyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/v1/CurrencyController.java) | Local declaration |
| `PUT /api/v1/currencies/{currencyId}` | `public Mono<ResponseEntity<CurrencyDTO>> updateCurrency( @Parameter(in = ParameterIn.PATH, description = "ID of the currency", required = true) @PathVariable UUID currencyId, @RequestBody CurrencyDTO currencyDto )` | [CurrencyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/v1/CurrencyController.java) | Local declaration |
| `DELETE /api/v1/currencies/{currencyId}` | `public Mono<ResponseEntity<Void>> deleteCurrency( @Parameter(in = ParameterIn.PATH, description = "ID of the currency", required = true) @PathVariable UUID currencyId )` | [CurrencyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/v1/CurrencyController.java) | Local declaration |
| `GET /api/v1/divisions` | `public Mono<ResponseEntity<PaginationResponse<AdministrativeDivisionDTO>>> listDivisions( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [AdministrativeDivisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/division/v1/AdministrativeDivisionController.java) | Local declaration |
| `GET /api/v1/divisions/{divisionId}` | `public Mono<ResponseEntity<AdministrativeDivisionDTO>> getDivision( @Parameter(in = ParameterIn.PATH, description = "ID of the administrative division", required = true) @PathVariable UUID divisionId )` | [AdministrativeDivisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/division/v1/AdministrativeDivisionController.java) | Local declaration |
| `POST /api/v1/divisions` | `public Mono<ResponseEntity<AdministrativeDivisionDTO>> createDivision( @RequestBody AdministrativeDivisionDTO divisionDto )` | [AdministrativeDivisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/division/v1/AdministrativeDivisionController.java) | Local declaration |
| `PUT /api/v1/divisions/{divisionId}` | `public Mono<ResponseEntity<AdministrativeDivisionDTO>> updateDivision( @Parameter(in = ParameterIn.PATH, description = "ID of the administrative division", required = true) @PathVariable UUID divisionId, @RequestBody AdministrativeDivisionDTO divisionDto )` | [AdministrativeDivisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/division/v1/AdministrativeDivisionController.java) | Local declaration |
| `DELETE /api/v1/divisions/{divisionId}` | `public Mono<ResponseEntity<Void>> deleteDivision( @Parameter(in = ParameterIn.PATH, description = "ID of the administrative division", required = true) @PathVariable UUID divisionId )` | [AdministrativeDivisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/division/v1/AdministrativeDivisionController.java) | Local declaration |
| `GET /api/v1/document-layout-publications/templates/{templateId}/source-observation` | `public Mono<DocumentLayoutSourceObservation> observeDocumentLayoutSource( @PathVariable UUID templateId, @RequestParam String localeCode)` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) | Local declaration |
| `GET /api/v1/document-layout-publications/{layoutRevisionId}` | `public Mono<DocumentLayoutPublicationDTO> getDocumentLayoutRevision( @PathVariable UUID layoutRevisionId)` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) | Local declaration |
| `GET /api/v1/document-layout-publications/resolve` | `public Mono<DocumentLayoutPublicationDTO> resolveDocumentLayoutRevision( @RequestParam String templateCode, @RequestParam String version, @RequestParam String localeCode, @RequestParam String outputProfileCode, @RequestParam String outputProfileVersion, @RequestParam String outputProfileManifestDigest)` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) | Local declaration |
| `POST /api/v1/document-layout-publications/templates/{templateId}` | `public Mono<ResponseEntity<DocumentLayoutPublicationDTO>> publishDocumentLayoutRevision( @PathVariable UUID templateId, @io.swagger.v3.oas.annotations.parameters.RequestBody( required = true, content = @Content( schema = @Schema( implementation = DocumentLayoutPublishRequest .class))) @RequestBody String raw)` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) | Local declaration |
| `POST /api/v1/document-layout-publications/{layoutRevisionId}/retire` | `public Mono<DocumentLayoutPublicationDTO> retireDocumentLayoutRevision( @PathVariable UUID layoutRevisionId, @io.swagger.v3.oas.annotations.parameters.RequestBody( required = true, content = @Content( schema = @Schema( implementation = DocumentLayoutRetireRequest .class))) @RequestBody String raw)` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) | Local declaration |
| `GET /api/v1/document-templates` | `public Mono<ResponseEntity<PaginationResponse<DocumentTemplateCatalogDTO>>> listDocumentTemplates( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `GET /api/v1/document-templates/category/{category}` | `public Mono<ResponseEntity<PaginationResponse<DocumentTemplateCatalogDTO>>> listDocumentTemplatesByCategory( @Parameter(in = ParameterIn.PATH, description = "Category of templates", required = true) @PathVariable String category, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `GET /api/v1/document-templates/type/{typeId}` | `public Mono<ResponseEntity<PaginationResponse<DocumentTemplateCatalogDTO>>> listDocumentTemplatesByTypeId( @Parameter(in = ParameterIn.PATH, description = "Template type ID", required = true) @PathVariable UUID typeId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `GET /api/v1/document-templates/{templateId}` | `public Mono<ResponseEntity<DocumentTemplateCatalogDTO>> getDocumentTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `GET /api/v1/document-templates/code/{templateCode}` | `public Mono<ResponseEntity<DocumentTemplateCatalogDTO>> getDocumentTemplateByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the document template", required = true) @PathVariable String templateCode )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `POST /api/v1/document-templates` | `public Mono<ResponseEntity<DocumentTemplateCatalogDTO>> createDocumentTemplate( @RequestBody DocumentTemplateCatalogDTO documentTemplateDTO )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `PUT /api/v1/document-templates/{templateId}` | `public Mono<ResponseEntity<DocumentTemplateCatalogDTO>> updateDocumentTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId, @RequestBody DocumentTemplateCatalogDTO documentTemplateDTO )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `DELETE /api/v1/document-templates/{templateId}` | `public Mono<ResponseEntity<Void>> deleteDocumentTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId )` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) | Local declaration |
| `GET /api/v1/document-template-localizations/template/{templateId}` | `public Mono<ResponseEntity<Flux<DocumentTemplateLocalizationDTO>>> getLocalizationsByTemplateId( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `GET /api/v1/document-template-localizations/locale/{localeId}` | `public Mono<ResponseEntity<Flux<DocumentTemplateLocalizationDTO>>> getLocalizationsByLocaleId( @Parameter(in = ParameterIn.PATH, description = "ID of the language locale", required = true) @PathVariable UUID localeId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `GET /api/v1/document-template-localizations/{localizationId}` | `public Mono<ResponseEntity<DocumentTemplateLocalizationDTO>> getDocumentTemplateLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the document template localization", required = true) @PathVariable UUID localizationId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `GET /api/v1/document-template-localizations/template/{templateId}/locale/{localeId}` | `public Mono<ResponseEntity<DocumentTemplateLocalizationDTO>> getDocumentTemplateLocalizationByTemplateAndLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId, @Parameter(in = ParameterIn.PATH, description = "ID of the language locale", required = true) @PathVariable UUID localeId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `POST /api/v1/document-template-localizations` | `public Mono<ResponseEntity<DocumentTemplateLocalizationDTO>> createDocumentTemplateLocalization( @RequestBody DocumentTemplateLocalizationDTO localizationDTO )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `PUT /api/v1/document-template-localizations/{localizationId}` | `public Mono<ResponseEntity<DocumentTemplateLocalizationDTO>> updateDocumentTemplateLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the document template localization", required = true) @PathVariable UUID localizationId, @RequestBody DocumentTemplateLocalizationDTO localizationDTO )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `DELETE /api/v1/document-template-localizations/{localizationId}` | `public Mono<ResponseEntity<Void>> deleteDocumentTemplateLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the document template localization", required = true) @PathVariable UUID localizationId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `DELETE /api/v1/document-template-localizations/template/{templateId}` | `public Mono<ResponseEntity<Void>> deleteLocalizationsByTemplateId( @Parameter(in = ParameterIn.PATH, description = "ID of the document template", required = true) @PathVariable UUID templateId )` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) | Local declaration |
| `GET /api/v1/document-template-revisions/{revisionId}` | `public Mono<DocumentTemplateRevisionDTO> getDocumentTemplateRevision(@PathVariable UUID revisionId)` | [DocumentTemplateRevisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateRevisionController.java) | Local declaration |
| `GET /api/v1/document-template-revisions/resolve` | `public Mono<DocumentTemplateRevisionDTO> resolveDocumentTemplateRevision(@RequestParam String templateCode, @RequestParam String version, @RequestParam String localeCode)` | [DocumentTemplateRevisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateRevisionController.java) | Local declaration |
| `POST /api/v1/document-template-revisions/templates/{templateId}/publish` | `public Mono<DocumentTemplateRevisionDTO> publishDocumentTemplateRevision(@PathVariable UUID templateId, @Valid @RequestBody DocumentTemplatePublicationRequest request)` | [DocumentTemplateRevisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateRevisionController.java) | Local declaration |
| `POST /api/v1/document-template-revisions/{revisionId}/retire` | `public Mono<DocumentTemplateRevisionDTO> retireDocumentTemplateRevision(@PathVariable UUID revisionId)` | [DocumentTemplateRevisionController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateRevisionController.java) | Local declaration |
| `GET /api/v1/document-template-types` | `public Mono<ResponseEntity<PaginationResponse<DocumentTemplateTypeCatalogDTO>>> listDocumentTemplateTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `GET /api/v1/document-template-types/{typeId}` | `public Mono<ResponseEntity<DocumentTemplateTypeCatalogDTO>> getDocumentTemplateType( @Parameter(in = ParameterIn.PATH, description = "ID of the document template type", required = true) @PathVariable UUID typeId )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `GET /api/v1/document-template-types/code/{typeCode}` | `public Mono<ResponseEntity<DocumentTemplateTypeCatalogDTO>> getDocumentTemplateTypeByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the document template type", required = true) @PathVariable String typeCode )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `POST /api/v1/document-template-types` | `public Mono<ResponseEntity<DocumentTemplateTypeCatalogDTO>> createDocumentTemplateType( @RequestBody DocumentTemplateTypeCatalogDTO documentTemplateTypeDTO )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `PUT /api/v1/document-template-types/{typeId}` | `public Mono<ResponseEntity<DocumentTemplateTypeCatalogDTO>> updateDocumentTemplateType( @Parameter(in = ParameterIn.PATH, description = "ID of the document template type", required = true) @PathVariable UUID typeId, @RequestBody DocumentTemplateTypeCatalogDTO documentTemplateTypeDTO )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `DELETE /api/v1/document-template-types/{typeId}` | `public Mono<ResponseEntity<Void>> deleteDocumentTemplateType( @Parameter(in = ParameterIn.PATH, description = "ID of the document template type", required = true) @PathVariable UUID typeId )` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) | Local declaration |
| `GET /api/v1/identity-documents` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentCatalogDTO>>> listIdentityDocuments( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `GET /api/v1/identity-documents/category/{categoryId}` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentCatalogDTO>>> listIdentityDocumentsByCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document category", required = true) @PathVariable UUID categoryId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `GET /api/v1/identity-documents/country/{countryId}` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentCatalogDTO>>> listIdentityDocumentsByCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `GET /api/v1/identity-documents/{documentId}` | `public Mono<ResponseEntity<IdentityDocumentCatalogDTO>> getIdentityDocument( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `GET /api/v1/identity-documents/code/{documentCode}` | `public Mono<ResponseEntity<IdentityDocumentCatalogDTO>> getIdentityDocumentByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the identity document", required = true) @PathVariable String documentCode )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `POST /api/v1/identity-documents` | `public Mono<ResponseEntity<IdentityDocumentCatalogDTO>> createIdentityDocument( @RequestBody IdentityDocumentCatalogDTO dto )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `PUT /api/v1/identity-documents/{documentId}` | `public Mono<ResponseEntity<IdentityDocumentCatalogDTO>> updateIdentityDocument( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId, @RequestBody IdentityDocumentCatalogDTO dto )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `DELETE /api/v1/identity-documents/{documentId}` | `public Mono<ResponseEntity<Void>> deleteIdentityDocument( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId )` | [IdentityDocumentCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCatalogController.java) | Local declaration |
| `GET /api/v1/identity-document-categories` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentCategoryCatalogDTO>>> listIdentityDocumentCategories( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/identity-document-categories/{categoryId}` | `public Mono<ResponseEntity<IdentityDocumentCategoryCatalogDTO>> getIdentityDocumentCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document category", required = true) @PathVariable UUID categoryId )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/identity-document-categories/code/{categoryCode}` | `public Mono<ResponseEntity<IdentityDocumentCategoryCatalogDTO>> getIdentityDocumentCategoryByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the identity document category", required = true) @PathVariable String categoryCode )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `POST /api/v1/identity-document-categories` | `public Mono<ResponseEntity<IdentityDocumentCategoryCatalogDTO>> createIdentityDocumentCategory( @RequestBody IdentityDocumentCategoryCatalogDTO dto )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `PUT /api/v1/identity-document-categories/{categoryId}` | `public Mono<ResponseEntity<IdentityDocumentCategoryCatalogDTO>> updateIdentityDocumentCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document category", required = true) @PathVariable UUID categoryId, @RequestBody IdentityDocumentCategoryCatalogDTO dto )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `DELETE /api/v1/identity-document-categories/{categoryId}` | `public Mono<ResponseEntity<Void>> deleteIdentityDocumentCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document category", required = true) @PathVariable UUID categoryId )` | [IdentityDocumentCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/identity-document-localizations` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentLocalizationDTO>>> listIdentityDocumentLocalizations( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `GET /api/v1/identity-document-localizations/document/{documentId}` | `public Mono<ResponseEntity<PaginationResponse<IdentityDocumentLocalizationDTO>>> getLocalizationsByDocumentId( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `GET /api/v1/identity-document-localizations/document/{documentId}/locale/{localeId}` | `public Mono<ResponseEntity<IdentityDocumentLocalizationDTO>> getIdentityDocumentLocalizationByDocumentAndLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId, @Parameter(in = ParameterIn.PATH, description = "ID of the language locale", required = true) @PathVariable UUID localeId )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `POST /api/v1/identity-document-localizations` | `public Mono<ResponseEntity<IdentityDocumentLocalizationDTO>> createIdentityDocumentLocalization( @RequestBody IdentityDocumentLocalizationDTO localizationDTO )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `PUT /api/v1/identity-document-localizations/{localizationId}` | `public Mono<ResponseEntity<IdentityDocumentLocalizationDTO>> updateIdentityDocumentLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document localization", required = true) @PathVariable UUID localizationId, @RequestBody IdentityDocumentLocalizationDTO localizationDTO )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `DELETE /api/v1/identity-document-localizations/{localizationId}` | `public Mono<ResponseEntity<Void>> deleteIdentityDocumentLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document localization", required = true) @PathVariable UUID localizationId )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `DELETE /api/v1/identity-document-localizations/document/{documentId}` | `public Mono<ResponseEntity<Void>> deleteLocalizationsByDocumentId( @Parameter(in = ParameterIn.PATH, description = "ID of the identity document", required = true) @PathVariable UUID documentId )` | [IdentityDocumentLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/identitydocument/v1/IdentityDocumentLocalizationController.java) | Local declaration |
| `GET /api/v1/legal-forms/country/{countryId}` | `public Mono<ResponseEntity<Flux<LegalFormDTO>>> getLegalFormsByCountry( @Parameter(in = ParameterIn.PATH, description = "ID of the country", required = true) @PathVariable UUID countryId )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `GET /api/v1/legal-forms/{legalFormId}` | `public Mono<ResponseEntity<LegalFormDTO>> getLegalForm( @Parameter(in = ParameterIn.PATH, description = "ID of the legal form", required = true) @PathVariable UUID legalFormId )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `POST /api/v1/legal-forms/filter` | `public Mono<ResponseEntity<PaginationResponse<LegalFormDTO>>> listLegalForms( @RequestBody FilterRequest<LegalFormDTO> filterRequest )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `POST /api/v1/legal-forms` | `public Mono<ResponseEntity<LegalFormDTO>> createLegalForm( @RequestBody LegalFormDTO legalFormDto )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `PUT /api/v1/legal-forms/{legalFormId}` | `public Mono<ResponseEntity<LegalFormDTO>> updateLegalForm( @Parameter(in = ParameterIn.PATH, description = "ID of the legal form", required = true) @PathVariable UUID legalFormId, @RequestBody LegalFormDTO legalFormDto )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `DELETE /api/v1/legal-forms/{legalFormId}` | `public Mono<ResponseEntity<Void>> deleteLegalForm( @Parameter(in = ParameterIn.PATH, description = "ID of the legal form", required = true) @PathVariable UUID legalFormId )` | [LegalFormController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/legal/v1/LegalFormController.java) | Local declaration |
| `GET /api/v1/language-locales` | `public Mono<ResponseEntity<PaginationResponse<LanguageLocaleDTO>>> listLanguageLocales( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [LanguageLocaleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/locale/v1/LanguageLocaleController.java) | Local declaration |
| `GET /api/v1/language-locales/{id}` | `public Mono<ResponseEntity<LanguageLocaleDTO>> getLanguageLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the languageLocale", required = true) @PathVariable UUID id )` | [LanguageLocaleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/locale/v1/LanguageLocaleController.java) | Local declaration |
| `POST /api/v1/language-locales` | `public Mono<ResponseEntity<LanguageLocaleDTO>> createLanguageLocale( @RequestBody LanguageLocaleDTO dto )` | [LanguageLocaleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/locale/v1/LanguageLocaleController.java) | Local declaration |
| `PUT /api/v1/language-locales/{id}` | `public Mono<ResponseEntity<LanguageLocaleDTO>> updateLanguageLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the languageLocale", required = true) @PathVariable UUID id, @RequestBody LanguageLocaleDTO dto )` | [LanguageLocaleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/locale/v1/LanguageLocaleController.java) | Local declaration |
| `DELETE /api/v1/language-locales/{id}` | `public Mono<ResponseEntity<Void>> deleteLanguageLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the languageLocale", required = true) @PathVariable UUID id )` | [LanguageLocaleController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/locale/v1/LanguageLocaleController.java) | Local declaration |
| `GET /api/v1/lookup/domains` | `public Mono<ResponseEntity<PaginationResponse<LookupDomainDTO>>> listDomains( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [LookupDomainController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupDomainController.java) | Local declaration |
| `GET /api/v1/lookup/domains/{domainId}` | `public Mono<ResponseEntity<LookupDomainDTO>> getDomain( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup domain", required = true) @PathVariable UUID domainId )` | [LookupDomainController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupDomainController.java) | Local declaration |
| `POST /api/v1/lookup/domains` | `public Mono<ResponseEntity<LookupDomainDTO>> createDomain( @RequestBody LookupDomainDTO domainDto )` | [LookupDomainController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupDomainController.java) | Local declaration |
| `PUT /api/v1/lookup/domains/{domainId}` | `public Mono<ResponseEntity<LookupDomainDTO>> updateDomain( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup domain", required = true) @PathVariable UUID domainId, @RequestBody LookupDomainDTO domainDto )` | [LookupDomainController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupDomainController.java) | Local declaration |
| `DELETE /api/v1/lookup/domains/{domainId}` | `public Mono<ResponseEntity<Void>> deleteDomain( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup domain", required = true) @PathVariable UUID domainId )` | [LookupDomainController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupDomainController.java) | Local declaration |
| `GET /api/v1/lookup/items` | `public Mono<ResponseEntity<PaginationResponse<LookupItemDTO>>> listItems( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `GET /api/v1/lookup/items/domain/{domainId}` | `public Mono<ResponseEntity<Flux<LookupItemDTO>>> getItemsByDomain( @Parameter(in = ParameterIn.PATH, description = "ID of the domain", required = true) @PathVariable UUID domainId )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `GET /api/v1/lookup/items/{itemId}` | `public Mono<ResponseEntity<LookupItemDTO>> getItem( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup item", required = true) @PathVariable UUID itemId )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `POST /api/v1/lookup/items` | `public Mono<ResponseEntity<LookupItemDTO>> createItem( @RequestBody LookupItemDTO itemDto )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `PUT /api/v1/lookup/items/{itemId}` | `public Mono<ResponseEntity<LookupItemDTO>> updateItem( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup item", required = true) @PathVariable UUID itemId, @RequestBody LookupItemDTO itemDto )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `DELETE /api/v1/lookup/items/{itemId}` | `public Mono<ResponseEntity<Void>> deleteItem( @Parameter(in = ParameterIn.PATH, description = "ID of the lookup item", required = true) @PathVariable UUID itemId )` | [LookupItemController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/lookup/v1/LookupItemController.java) | Local declaration |
| `GET /api/v1/message-types` | `public Mono<ResponseEntity<PaginationResponse<MessageTypeCatalogDTO>>> listMessageTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `GET /api/v1/message-types/{typeId}` | `public Mono<ResponseEntity<MessageTypeCatalogDTO>> getMessageType( @Parameter(in = ParameterIn.PATH, description = "ID of the message type", required = true) @PathVariable UUID typeId )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `GET /api/v1/message-types/code/{typeCode}` | `public Mono<ResponseEntity<MessageTypeCatalogDTO>> getMessageTypeByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the message type", required = true) @PathVariable String typeCode )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `POST /api/v1/message-types` | `public Mono<ResponseEntity<MessageTypeCatalogDTO>> createMessageType( @RequestBody MessageTypeCatalogDTO messageTypeDTO )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `PUT /api/v1/message-types/{typeId}` | `public Mono<ResponseEntity<MessageTypeCatalogDTO>> updateMessageType( @Parameter(in = ParameterIn.PATH, description = "ID of the message type", required = true) @PathVariable UUID typeId, @RequestBody MessageTypeCatalogDTO messageTypeDTO )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `DELETE /api/v1/message-types/{typeId}` | `public Mono<ResponseEntity<Void>> deleteMessageType( @Parameter(in = ParameterIn.PATH, description = "ID of the message type", required = true) @PathVariable UUID typeId )` | [MessageTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/MessageTypeCatalogController.java) | Local declaration |
| `GET /api/v1/notification-messages` | `public Mono<ResponseEntity<PaginationResponse<NotificationMessageCatalogDTO>>> listNotificationMessages( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `GET /api/v1/notification-messages/event-type/{eventType}` | `public Mono<ResponseEntity<PaginationResponse<NotificationMessageCatalogDTO>>> listNotificationMessagesByEventType( @Parameter(in = ParameterIn.PATH, description = "Event type", required = true) @PathVariable String eventType, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `GET /api/v1/notification-messages/message-type/{typeId}` | `public Mono<ResponseEntity<PaginationResponse<NotificationMessageCatalogDTO>>> listNotificationMessagesByTypeId( @Parameter(in = ParameterIn.PATH, description = "Message type ID", required = true) @PathVariable UUID typeId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `GET /api/v1/notification-messages/{messageId}` | `public Mono<ResponseEntity<NotificationMessageCatalogDTO>> getNotificationMessage( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `GET /api/v1/notification-messages/code/{messageCode}` | `public Mono<ResponseEntity<NotificationMessageCatalogDTO>> getNotificationMessageByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the notification message", required = true) @PathVariable String messageCode )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `POST /api/v1/notification-messages` | `public Mono<ResponseEntity<NotificationMessageCatalogDTO>> createNotificationMessage( @RequestBody NotificationMessageCatalogDTO notificationMessageDTO )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `PUT /api/v1/notification-messages/{messageId}` | `public Mono<ResponseEntity<NotificationMessageCatalogDTO>> updateNotificationMessage( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId, @RequestBody NotificationMessageCatalogDTO notificationMessageDTO )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `DELETE /api/v1/notification-messages/{messageId}` | `public Mono<ResponseEntity<Void>> deleteNotificationMessage( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageCatalogController.java) | Local declaration |
| `GET /api/v1/notification-localizations/message/{messageId}` | `public Mono<ResponseEntity<Flux<NotificationMessageLocalizationDTO>>> getLocalizationsByMessageId( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `GET /api/v1/notification-localizations/locale/{localeId}` | `public Mono<ResponseEntity<Flux<NotificationMessageLocalizationDTO>>> getLocalizationsByLocaleId( @Parameter(in = ParameterIn.PATH, description = "ID of the language locale", required = true) @PathVariable UUID localeId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `GET /api/v1/notification-localizations/{localizationId}` | `public Mono<ResponseEntity<NotificationMessageLocalizationDTO>> getNotificationMessageLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message localization", required = true) @PathVariable UUID localizationId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `GET /api/v1/notification-localizations/message/{messageId}/locale/{localeId}` | `public Mono<ResponseEntity<NotificationMessageLocalizationDTO>> getNotificationMessageLocalizationByMessageAndLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId, @Parameter(in = ParameterIn.PATH, description = "ID of the language locale", required = true) @PathVariable UUID localeId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `POST /api/v1/notification-localizations` | `public Mono<ResponseEntity<NotificationMessageLocalizationDTO>> createNotificationMessageLocalization( @RequestBody NotificationMessageLocalizationDTO localizationDTO )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `PUT /api/v1/notification-localizations/{localizationId}` | `public Mono<ResponseEntity<NotificationMessageLocalizationDTO>> updateNotificationMessageLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message localization", required = true) @PathVariable UUID localizationId, @RequestBody NotificationMessageLocalizationDTO localizationDTO )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `DELETE /api/v1/notification-localizations/{localizationId}` | `public Mono<ResponseEntity<Void>> deleteNotificationMessageLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message localization", required = true) @PathVariable UUID localizationId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `DELETE /api/v1/notification-localizations/message/{messageId}` | `public Mono<ResponseEntity<Void>> deleteLocalizationsByMessageId( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageLocalizationController.java) | Local declaration |
| `GET /api/v1/notification-templates` | `public Mono<ResponseEntity<PaginationResponse<NotificationMessageTemplateDTO>>> listNotificationMessageTemplates( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `GET /api/v1/notification-templates/type/{templateType}` | `public Mono<ResponseEntity<PaginationResponse<NotificationMessageTemplateDTO>>> listNotificationMessageTemplatesByType( @Parameter(in = ParameterIn.PATH, description = "Template type", required = true) @PathVariable String templateType, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `GET /api/v1/notification-templates/message/{messageId}` | `public Mono<ResponseEntity<Flux<NotificationMessageTemplateDTO>>> getTemplatesByMessageId( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `GET /api/v1/notification-templates/{templateId}` | `public Mono<ResponseEntity<NotificationMessageTemplateDTO>> getNotificationMessageTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message template", required = true) @PathVariable UUID templateId )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `GET /api/v1/notification-templates/message/{messageId}/name/{templateName}` | `public Mono<ResponseEntity<NotificationMessageTemplateDTO>> getNotificationMessageTemplateByNameAndMessageId( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId, @Parameter(in = ParameterIn.PATH, description = "Name of the template", required = true) @PathVariable String templateName )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `POST /api/v1/notification-templates` | `public Mono<ResponseEntity<NotificationMessageTemplateDTO>> createNotificationMessageTemplate( @RequestBody NotificationMessageTemplateDTO templateDTO )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `PUT /api/v1/notification-templates/{templateId}` | `public Mono<ResponseEntity<NotificationMessageTemplateDTO>> updateNotificationMessageTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message template", required = true) @PathVariable UUID templateId, @RequestBody NotificationMessageTemplateDTO templateDTO )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `DELETE /api/v1/notification-templates/{templateId}` | `public Mono<ResponseEntity<Void>> deleteNotificationMessageTemplate( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message template", required = true) @PathVariable UUID templateId )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `DELETE /api/v1/notification-templates/message/{messageId}` | `public Mono<ResponseEntity<Void>> deleteTemplatesByMessageId( @Parameter(in = ParameterIn.PATH, description = "ID of the notification message", required = true) @PathVariable UUID messageId )` | [NotificationMessageTemplateController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/notification/v1/NotificationMessageTemplateController.java) | Local declaration |
| `GET /api/v1/relationship-types` | `public Mono<ResponseEntity<PaginationResponse<RelationshipTypeMasterDTO>>> listRelationshipTypes( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [RelationshipTypeMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/relationships/v1/RelationshipTypeMasterController.java) | Local declaration |
| `GET /api/v1/relationship-types/{relationshipTypeId}` | `public Mono<ResponseEntity<RelationshipTypeMasterDTO>> getRelationshipType( @Parameter(in = ParameterIn.PATH, description = "ID of the relationship type", required = true) @PathVariable UUID relationshipTypeId )` | [RelationshipTypeMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/relationships/v1/RelationshipTypeMasterController.java) | Local declaration |
| `POST /api/v1/relationship-types` | `public Mono<ResponseEntity<RelationshipTypeMasterDTO>> createRelationshipType( @RequestBody RelationshipTypeMasterDTO relationshipTypeDto )` | [RelationshipTypeMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/relationships/v1/RelationshipTypeMasterController.java) | Local declaration |
| `PUT /api/v1/relationship-types/{relationshipTypeId}` | `public Mono<ResponseEntity<RelationshipTypeMasterDTO>> updateRelationshipType( @Parameter(in = ParameterIn.PATH, description = "ID of the relationship type", required = true) @PathVariable UUID relationshipTypeId, @RequestBody RelationshipTypeMasterDTO relationshipTypeDto )` | [RelationshipTypeMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/relationships/v1/RelationshipTypeMasterController.java) | Local declaration |
| `DELETE /api/v1/relationship-types/{relationshipTypeId}` | `public Mono<ResponseEntity<Void>> deleteRelationshipType( @Parameter(in = ParameterIn.PATH, description = "ID of the relationship type", required = true) @PathVariable UUID relationshipTypeId )` | [RelationshipTypeMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/relationships/v1/RelationshipTypeMasterController.java) | Local declaration |
| `GET /api/v1/rule-operation-types/{operationTypeId}` | `public Mono<ResponseEntity<RuleOperationTypeDTO>> getRuleOperationType( @Parameter(in = ParameterIn.PATH, description = "ID of the rule operation type", required = true) @PathVariable UUID operationTypeId )` | [RuleOperationTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/ruleoperationtype/v1/RuleOperationTypeController.java) | Local declaration |
| `POST /api/v1/rule-operation-types/filter` | `public Mono<ResponseEntity<PaginationResponse<RuleOperationTypeDTO>>> filterRuleOperationTypes( @RequestBody FilterRequest<RuleOperationTypeDTO> filterRequest )` | [RuleOperationTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/ruleoperationtype/v1/RuleOperationTypeController.java) | Local declaration |
| `POST /api/v1/rule-operation-types` | `public Mono<ResponseEntity<RuleOperationTypeDTO>> createRuleOperationType( @RequestBody RuleOperationTypeDTO dto )` | [RuleOperationTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/ruleoperationtype/v1/RuleOperationTypeController.java) | Local declaration |
| `PUT /api/v1/rule-operation-types/{operationTypeId}` | `public Mono<ResponseEntity<RuleOperationTypeDTO>> updateRuleOperationType( @Parameter(in = ParameterIn.PATH, description = "ID of the rule operation type", required = true) @PathVariable UUID operationTypeId, @RequestBody RuleOperationTypeDTO dto )` | [RuleOperationTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/ruleoperationtype/v1/RuleOperationTypeController.java) | Local declaration |
| `DELETE /api/v1/rule-operation-types/{operationTypeId}` | `public Mono<ResponseEntity<Void>> deleteRuleOperationType( @Parameter(in = ParameterIn.PATH, description = "ID of the rule operation type", required = true) @PathVariable UUID operationTypeId )` | [RuleOperationTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/ruleoperationtype/v1/RuleOperationTypeController.java) | Local declaration |
| `GET /api/v1/titles` | `public Mono<ResponseEntity<PaginationResponse<TitleMasterDTO>>> listTitles( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [TitleMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/title/v1/TitleMasterController.java) | Local declaration |
| `GET /api/v1/titles/{titleId}` | `public Mono<ResponseEntity<TitleMasterDTO>> getTitle( @Parameter(in = ParameterIn.PATH, description = "ID of the title", required = true) @PathVariable UUID titleId )` | [TitleMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/title/v1/TitleMasterController.java) | Local declaration |
| `POST /api/v1/titles` | `public Mono<ResponseEntity<TitleMasterDTO>> createTitle( @RequestBody TitleMasterDTO titleDto )` | [TitleMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/title/v1/TitleMasterController.java) | Local declaration |
| `PUT /api/v1/titles/{titleId}` | `public Mono<ResponseEntity<TitleMasterDTO>> updateTitle( @Parameter(in = ParameterIn.PATH, description = "ID of the title", required = true) @PathVariable UUID titleId, @RequestBody TitleMasterDTO titleDto )` | [TitleMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/title/v1/TitleMasterController.java) | Local declaration |
| `DELETE /api/v1/titles/{titleId}` | `public Mono<ResponseEntity<Void>> deleteTitle( @Parameter(in = ParameterIn.PATH, description = "ID of the title", required = true) @PathVariable UUID titleId )` | [TitleMasterController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/title/v1/TitleMasterController.java) | Local declaration |
| `GET /api/v1/transaction-categories` | `public Mono<ResponseEntity<PaginationResponse<TransactionCategoryCatalogDTO>>> listTransactionCategories( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/transaction-categories/root` | `public Mono<ResponseEntity<PaginationResponse<TransactionCategoryCatalogDTO>>> listRootTransactionCategories( @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/transaction-categories/parent/{parentCategoryId}` | `public Mono<ResponseEntity<PaginationResponse<TransactionCategoryCatalogDTO>>> listChildTransactionCategories( @Parameter(in = ParameterIn.PATH, description = "ID of the parent category", required = true) @PathVariable UUID parentCategoryId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/transaction-categories/{categoryId}` | `public Mono<ResponseEntity<TransactionCategoryCatalogDTO>> getTransactionCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category", required = true) @PathVariable UUID categoryId )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/transaction-categories/code/{categoryCode}` | `public Mono<ResponseEntity<TransactionCategoryCatalogDTO>> getTransactionCategoryByCode( @Parameter(in = ParameterIn.PATH, description = "Code of the transaction category", required = true) @PathVariable String categoryCode )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `POST /api/v1/transaction-categories` | `public Mono<ResponseEntity<TransactionCategoryCatalogDTO>> createTransactionCategory( @RequestBody TransactionCategoryCatalogDTO dto )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `PUT /api/v1/transaction-categories/{categoryId}` | `public Mono<ResponseEntity<TransactionCategoryCatalogDTO>> updateTransactionCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category to update", required = true) @PathVariable UUID categoryId, @RequestBody TransactionCategoryCatalogDTO dto )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `DELETE /api/v1/transaction-categories/{categoryId}` | `public Mono<ResponseEntity<Void>> deleteTransactionCategory( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category to delete", required = true) @PathVariable UUID categoryId )` | [TransactionCategoryCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryCatalogController.java) | Local declaration |
| `GET /api/v1/transaction-category-localizations/category/{categoryId}` | `public Mono<ResponseEntity<Flux<TransactionCategoryLocalizationDTO>>> getLocalizationsByCategoryId( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category", required = true) @PathVariable UUID categoryId )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `GET /api/v1/transaction-category-localizations/category/{categoryId}/paginated` | `public Mono<ResponseEntity<PaginationResponse<TransactionCategoryLocalizationDTO>>> listLocalizationsByCategoryId( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category", required = true) @PathVariable UUID categoryId, @ParameterObject @ModelAttribute PaginationRequest paginationRequest )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `GET /api/v1/transaction-category-localizations/{localizationId}` | `public Mono<ResponseEntity<TransactionCategoryLocalizationDTO>> getTransactionCategoryLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category localization", required = true) @PathVariable UUID localizationId )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `GET /api/v1/transaction-category-localizations/category/{categoryId}/locale/{localeId}` | `public Mono<ResponseEntity<TransactionCategoryLocalizationDTO>> getTransactionCategoryLocalizationByCategoryAndLocale( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category", required = true) @PathVariable UUID categoryId, @Parameter(in = ParameterIn.PATH, description = "ID of the locale", required = true) @PathVariable UUID localeId )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `POST /api/v1/transaction-category-localizations` | `public Mono<ResponseEntity<TransactionCategoryLocalizationDTO>> createTransactionCategoryLocalization( @RequestBody TransactionCategoryLocalizationDTO dto )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `PUT /api/v1/transaction-category-localizations/{localizationId}` | `public Mono<ResponseEntity<TransactionCategoryLocalizationDTO>> updateTransactionCategoryLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category localization to update", required = true) @PathVariable UUID localizationId, @RequestBody TransactionCategoryLocalizationDTO dto )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |
| `DELETE /api/v1/transaction-category-localizations/{localizationId}` | `public Mono<ResponseEntity<Void>> deleteTransactionCategoryLocalization( @Parameter(in = ParameterIn.PATH, description = "ID of the transaction category localization to delete", required = true) @PathVariable UUID localizationId )` | [TransactionCategoryLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/transaction/v1/TransactionCategoryLocalizationController.java) | Local declaration |

## Data contracts

### ActivityCodeDTO

[ActivityCodeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/activity/v1/ActivityCodeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `activityCodeId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | `@NotNull(message = "Country ID is required")` |
| `code` | `String` | `@NotBlank(message = "Activity code is required")`; `@Size(max = 20, message = "Activity code must not exceed 20 characters")` |
| `classificationSys` | `String` | `@NotBlank(message = "Classification system is required")`; `@Size(max = 50, message = "Classification system must not exceed 50 characters")` |
| `description` | `String` | `@NotBlank(message = "Description is required")`; `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `parentCodeId` | `UUID` | See declaration / service rules |
| `highRisk` | `Boolean` | See declaration / service rules |
| `riskFactors` | `String` | `@Size(max = 1000, message = "Risk factors must not exceed 1000 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### AssetTypeDTO

[AssetTypeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/assettype/v1/AssetTypeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `assetId` | `UUID` | See declaration / service rules |
| `assetCode` | `String` | `@NotBlank(message = "Asset code is required")`; `@Size(max = 20, message = "Asset code must not exceed 20 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `name` | `String` | `@NotBlank(message = "Asset name is required")`; `@Size(max = 100, message = "Asset name must not exceed 100 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@Size`. Inspect the linked source for the field-level constraints.

### BankInstitutionCodeDTO

[BankInstitutionCodeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/bank/v1/BankInstitutionCodeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `institutionId` | `UUID` | See declaration / service rules |
| `bankName` | `String` | `@NotBlank(message = "Bank name is required")`; `@Size(max = 200, message = "Bank name must not exceed 200 characters")` |
| `swiftCode` | `String` | `@NotBlank(message = "SWIFT code is required")` |
| `routingNumber` | `String` | See declaration / service rules |
| `ibanPrefix` | `String` | `@Size(max = 10, message = "IBAN prefix must not exceed 10 characters")` |
| `countryId` | `UUID` | `@NotNull(message = "Country ID is required")` |
| `institutionTypeLkpId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `svgIcon` | `String` | `@Size(max = 10000, message = "SVG icon data must not exceed 10000 characters")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Pattern`, `@Size`. Inspect the linked source for the field-level constraints.

### ConsentCatalogDTO

[ConsentCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/consent/v1/ConsentCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `consentId` | `UUID` | See declaration / service rules |
| `consentType` | `String` | `@NotBlank(message = "Consent type is required")`; `@Size(max = 50, message = "Consent type must not exceed 50 characters")` |
| `consentDescription` | `String` | `@NotBlank(message = "Consent description is required")`; `@Size(max = 500, message = "Consent description must not exceed 500 characters")` |
| `expiryPeriodDays` | `Integer` | See declaration / service rules |
| `consentVersion` | `String` | `@Size(max = 20, message = "Consent version must not exceed 20 characters")` |
| `consentSource` | `String` | `@Size(max = 100, message = "Consent source must not exceed 100 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `isRequired` | `Boolean` | See declaration / service rules |
| `sortOrder` | `Integer` | See declaration / service rules |
| `applicableProduct` | `String` | `@Size(max = 50, message = "Applicable product must not exceed 50 characters")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### ContractDocumentTypeDTO

[ContractDocumentTypeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/contractdocumenttype/v1/ContractDocumentTypeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `documentTypeId` | `UUID` | See declaration / service rules |
| `documentCode` | `String` | `@NotBlank(message = "Document code is required")`; `@Size(max = 50, message = "Document code must not exceed 50 characters")` |
| `name` | `String` | `@NotBlank(message = "Name is required")`; `@Size(max = 100, message = "Name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@Size`. Inspect the linked source for the field-level constraints.

### ContractRoleDTO

[ContractRoleDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/contractrole/v1/ContractRoleDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `roleId` | `UUID` | See declaration / service rules |
| `roleCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ContractRoleScopeDTO

[ContractRoleScopeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/contractrole/v1/ContractRoleScopeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `scopeId` | `UUID` | See declaration / service rules |
| `roleId` | `UUID` | `@NotNull(message = "Role ID is required")` |
| `scopeCode` | `String` | `@NotBlank(message = "Scope code is required")`; `@Size(max = 100, message = "Scope code must not exceed 100 characters")` |
| `scopeName` | `String` | `@NotBlank(message = "Scope name is required")`; `@Size(max = 200, message = "Scope name must not exceed 200 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `actionType` | `String` | `@NotBlank(message = "Action type is required")`; `@Size(max = 50, message = "Action type must not exceed 50 characters")` |
| `resourceType` | `String` | `@Size(max = 100, message = "Resource type must not exceed 100 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### ContractTypeDTO

[ContractTypeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/contracttype/v1/ContractTypeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `contractId` | `UUID` | See declaration / service rules |
| `contractCode` | `String` | `@NotBlank(message = "Contract code is required")`; `@Size(max = 20, message = "Contract code must not exceed 20 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `name` | `String` | `@NotBlank(message = "Contract name is required")`; `@Size(max = 100, message = "Contract name must not exceed 100 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@Size`. Inspect the linked source for the field-level constraints.

### CountryDTO

[CountryDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/country/v1/CountryDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `countryId` | `UUID` | See declaration / service rules |
| `isoCode` | `String` | See declaration / service rules |
| `countryName` | `String` | `@NotBlank(message = "Country name is required")`; `@Size(max = 100, message = "Country name must not exceed 100 characters")` |
| `region` | `RegionEnum` | `@NotNull(message = "Region is required")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `svgFlag` | `String` | `@Size(max = 10000, message = "SVG flag data must not exceed 10000 characters")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Pattern`, `@Size`. Inspect the linked source for the field-level constraints.

### CurrencyPolicyEvent

[CurrencyPolicyEvent](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicyEvent.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `payloadVersion` | `String` | See declaration / service rules |
| `actionKind` | `String` | See declaration / service rules |
| `actionId` | `String` | See declaration / service rules |
| `policyReference` | `CurrencyPolicyReference` | See declaration / service rules |
| `intentEncoding` | `String` | See declaration / service rules |
| `intentDigest` | `String` | See declaration / service rules |
| `observedAtEpochMillis` | `Long` | See declaration / service rules |

### CurrencyPolicyPublishRequest

[CurrencyPolicyPublishRequest](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicyPublishRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actionId` | `String` | See declaration / service rules |
| `currencyId` | `String` | See declaration / service rules |
| `isoCode` | `String` | See declaration / service rules |
| `expectedCurrencyDigest` | `String` | See declaration / service rules |
| `expectedHeadVersion` | `Long` | See declaration / service rules |
| `postedScale` | `Integer` | See declaration / service rules |
| `effectiveFromEpochMillis` | `Long` | See declaration / service rules |
| `effectiveToEpochMillis` | `Long` | See declaration / service rules |
| `provenanceReference` | `String` | See declaration / service rules |

### CurrencyPolicyReference

[CurrencyPolicyReference](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicyReference.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `policyId` | `String` | See declaration / service rules |
| `currencyId` | `String` | See declaration / service rules |
| `isoCode` | `String` | See declaration / service rules |
| `version` | `Long` | See declaration / service rules |
| `digest` | `String` | See declaration / service rules |
| `encodingVersion` | `String` | See declaration / service rules |

### CurrencyPolicyRetireRequest

[CurrencyPolicyRetireRequest](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicyRetireRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actionId` | `String` | See declaration / service rules |
| `currencyId` | `String` | See declaration / service rules |
| `expectedStateVersion` | `Long` | See declaration / service rules |
| `reason` | `String` | See declaration / service rules |

### CurrencyPolicySourceObservation

[CurrencyPolicySourceObservation](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicySourceObservation.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `metadata` | `CurrencyDTO` | See declaration / service rules |
| `encodingVersion` | `String` | See declaration / service rules |
| `canonicalEncoding` | `String` | See declaration / service rules |
| `digest` | `String` | See declaration / service rules |
| `observedAtEpochMillis` | `Long` | See declaration / service rules |
| `boundCurrencyId` | `String` | See declaration / service rules |
| `headVersion` | `Long` | See declaration / service rules |

### CurrencyPolicyView

[CurrencyPolicyView](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/policy/v1/CurrencyPolicyView.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `reference` | `CurrencyPolicyReference` | See declaration / service rules |
| `canonicalEncoding` | `String` | See declaration / service rules |
| `sourceEncoding` | `String` | See declaration / service rules |
| `sourceDigest` | `String` | See declaration / service rules |
| `postedScale` | `Integer` | See declaration / service rules |
| `effectiveFromEpochMillis` | `Long` | See declaration / service rules |
| `effectiveToEpochMillis` | `Long` | See declaration / service rules |
| `provenanceReference` | `String` | See declaration / service rules |
| `publicationActionId` | `String` | See declaration / service rules |
| `publishedAtEpochMillis` | `Long` | See declaration / service rules |
| `lifecycle` | `String` | See declaration / service rules |
| `stateVersion` | `Long` | See declaration / service rules |
| `observedAtEpochMillis` | `Long` | See declaration / service rules |
| `effectiveNow` | `Boolean` | See declaration / service rules |

### CurrencyDTO

[CurrencyDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/currency/v1/CurrencyDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `currencyId` | `UUID` | See declaration / service rules |
| `isoCode` | `String` | `@NotBlank(message = "ISO code is required")` |
| `currencyName` | `String` | `@NotBlank(message = "Currency name is required")`; `@Size(max = 100, message = "Currency name must not exceed 100 characters")` |
| `symbol` | `String` | `@Size(max = 10, message = "Currency symbol must not exceed 10 characters")` |
| `decimalPrecision` | `Integer` | `@Min(value = 0, message = "Decimal precision must be non-negative")`; `@Max(value = 10, message = "Decimal precision must not exceed 10")` |
| `isMajor` | `Boolean` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@Max`, `@Min`, `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### AdministrativeDivisionDTO

[AdministrativeDivisionDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/division/v1/AdministrativeDivisionDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `divisionId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | `@NotNull(message = "Country ID is required")` |
| `code` | `String` | `@NotBlank(message = "Division code is required")`; `@Size(max = 20, message = "Division code must not exceed 20 characters")` |
| `name` | `String` | `@NotBlank(message = "Division name is required")`; `@Size(max = 100, message = "Division name must not exceed 100 characters")` |
| `level` | `String` | `@NotBlank(message = "Division level is required")`; `@Size(max = 50, message = "Division level must not exceed 50 characters")` |
| `parentDivisionId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `postalCodePattern` | `String` | `@Size(max = 50, message = "Postal code pattern must not exceed 50 characters")` |
| `timeZone` | `String` | `@Size(max = 50, message = "Time zone must not exceed 50 characters")` |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### DocumentLayoutDefinition

[DocumentLayoutDefinition](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutDefinition.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `sourceReference` | `DocumentLayoutMasterReference` | See declaration / service rules |
| `profileMetadata` | `DocumentLayoutOutputProfileMetadata` | See declaration / service rules |
| `semanticDefinition` | `DocumentLayoutSemanticDefinition` | See declaration / service rules |

### DocumentLayoutExpectedDraftReference

[DocumentLayoutExpectedDraftReference](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutExpectedDraftReference.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `templateId` | `String` | See declaration / service rules |
| `localizationId` | `String` | See declaration / service rules |
| `localeId` | `String` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `canonicalizationVersion` | `String` | See declaration / service rules |
| `sourceDigest` | `String` | See declaration / service rules |
| `contentDigest` | `String` | See declaration / service rules |
| `schemaDigest` | `String` | See declaration / service rules |
| `styleDigest` | `String` | See declaration / service rules |

### DocumentLayoutMasterReference

[DocumentLayoutMasterReference](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutMasterReference.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `revisionId` | `String` | See declaration / service rules |
| `templateId` | `String` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `canonicalizationVersion` | `String` | See declaration / service rules |
| `sourceDigest` | `String` | See declaration / service rules |
| `contentDigest` | `String` | See declaration / service rules |
| `schemaDigest` | `String` | See declaration / service rules |
| `styleDigest` | `String` | See declaration / service rules |

### DocumentLayoutNoticeReference

[DocumentLayoutNoticeReference](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutNoticeReference.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `role` | `Role` | See declaration / service rules |
| `placement` | `Placement` | See declaration / service rules |
| `masterReference` | `DocumentLayoutMasterReference` | See declaration / service rules |

### DocumentLayoutOutputProfileMetadata

[DocumentLayoutOutputProfileMetadata](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutOutputProfileMetadata.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `reference` | `DocumentLayoutOutputProfileReference` | See declaration / service rules |
| `sectionMode` | `String` | See declaration / service rules |
| `pageSize` | `String` | See declaration / service rules |
| `orientation` | `String` | See declaration / service rules |
| `allowedClassTokens` | `java.util.List<String>` | See declaration / service rules |
| `maxSections` | `Integer` | See declaration / service rules |
| `maxContentBytes` | `Integer` | See declaration / service rules |

### DocumentLayoutOutputProfileReference

[DocumentLayoutOutputProfileReference](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutOutputProfileReference.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `code` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `manifestDigest` | `String` | See declaration / service rules |

### DocumentLayoutPublicationDTO

[DocumentLayoutPublicationDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutPublicationDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `layoutRevisionId` | `java.util.UUID` | See declaration / service rules |
| `sourceRevisionId` | `java.util.UUID` | See declaration / service rules |
| `templateId` | `java.util.UUID` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `canonicalizationVersion` | `String` | See declaration / service rules |
| `canonicalJson` | `String` | See declaration / service rules |
| `definitionDigest` | `String` | See declaration / service rules |
| `definition` | `DocumentLayoutDefinition` | See declaration / service rules |
| `lifecycle` | `String` | See declaration / service rules |
| `stateVersion` | `Integer` | See declaration / service rules |
| `publicationActionId` | `java.util.UUID` | See declaration / service rules |
| `publishedAtEpochMillis` | `Long` | See declaration / service rules |
| `publishedBy` | `String` | See declaration / service rules |
| `retirementActionId` | `java.util.UUID` | See declaration / service rules |
| `retiredAtEpochMillis` | `Long` | See declaration / service rules |
| `retiredBy` | `String` | See declaration / service rules |
| `retirementReason` | `String` | See declaration / service rules |

### DocumentLayoutPublishRequest

[DocumentLayoutPublishRequest](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutPublishRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actionId` | `String` | See declaration / service rules |
| `expectedDraftReference` | `DocumentLayoutExpectedDraftReference` | See declaration / service rules |
| `outputProfile` | `DocumentLayoutOutputProfileReference` | See declaration / service rules |
| `semanticDefinition` | `DocumentLayoutSemanticDefinition` | See declaration / service rules |

### DocumentLayoutRetireRequest

[DocumentLayoutRetireRequest](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutRetireRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `actionId` | `String` | See declaration / service rules |
| `expectedStateVersion` | `Integer` | See declaration / service rules |
| `reason` | `String` | See declaration / service rules |

### DocumentLayoutSemanticDefinition

[DocumentLayoutSemanticDefinition](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutSemanticDefinition.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `sectionMode` | `String` | See declaration / service rules |
| `pageSize` | `String` | See declaration / service rules |
| `orientation` | `String` | See declaration / service rules |
| `legalNotices` | `java.util.List<DocumentLayoutNoticeReference>` | See declaration / service rules |

### DocumentLayoutSourceObservation

[DocumentLayoutSourceObservation](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/layout/v1/DocumentLayoutSourceObservation.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `draftReference` | `DocumentLayoutExpectedDraftReference` | See declaration / service rules |
| `sourceSnapshotCanonicalJson` | `String` | See declaration / service rules |

### DocumentTemplateCatalogDTO

[DocumentTemplateCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/v1/DocumentTemplateCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `templateId` | `UUID` | See declaration / service rules |
| `templateCode` | `String` | `@NotBlank(message = "Template code is required")`; `@Size(max = 50, message = "Template code must not exceed 50 characters")` |
| `typeId` | `UUID` | `@NotNull(message = "Type ID is required")` |
| `templateType` | `DocumentTemplateTypeCatalogDTO` | See declaration / service rules |
| `category` | `String` | `@NotBlank(message = "Category is required")`; `@Size(max = 50, message = "Category must not exceed 50 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `templateName` | `String` | `@NotBlank(message = "Template name is required")`; `@Size(max = 100, message = "Template name must not exceed 100 characters")` |
| `templateContent` | `String` | `@NotBlank(message = "Template content is required")`; `@Size(max = 10000, message = "Template content must not exceed 10000 characters")` |
| `templateVariables` | `Map<String, Object>` | See declaration / service rules |
| `inputSchema` | `Map<String, Object>` | See declaration / service rules |
| `inputSchemaJson` | `String` | `@Size(max = 65536)` |
| `styleContent` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `version` | `String` | `@Size(max = 20, message = "Version must not exceed 20 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`, `@Valid`. Inspect the linked source for the field-level constraints.

### DocumentTemplateLocalizationDTO

[DocumentTemplateLocalizationDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/v1/DocumentTemplateLocalizationDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `templateId` | `UUID` | `@NotNull(message = "Template ID is required")` |
| `localeId` | `UUID` | `@NotNull(message = "Locale ID is required")` |
| `templateName` | `String` | `@NotBlank(message = "Template name is required")`; `@Size(max = 100, message = "Template name must not exceed 100 characters")` |
| `templateContent` | `String` | `@NotBlank(message = "Template content is required")`; `@Size(max = 10000, message = "Template content must not exceed 10000 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### DocumentTemplatePublicationRequest

[DocumentTemplatePublicationRequest](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/v1/DocumentTemplatePublicationRequest.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@Size`. Inspect the linked source for the field-level constraints.

### DocumentTemplateRevisionDTO

[DocumentTemplateRevisionDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/v1/DocumentTemplateRevisionDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `revisionId` | `UUID` | See declaration / service rules |
| `templateId` | `UUID` | See declaration / service rules |
| `localizationId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `templateName` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `templateContent` | `String` | See declaration / service rules |
| `inputSchema` | `Object>` | See declaration / service rules |
| `inputSchemaCanonicalJson` | `String` | See declaration / service rules |
| `styleContent` | `String` | See declaration / service rules |
| `templateVariables` | `Object>` | See declaration / service rules |
| `sourceSnapshot` | `Object>` | See declaration / service rules |
| `sourceSnapshotCanonicalJson` | `String` | See declaration / service rules |
| `canonicalizationVersion` | `String` | See declaration / service rules |
| `contentDigest` | `String` | See declaration / service rules |
| `schemaDigest` | `String` | See declaration / service rules |
| `styleDigest` | `String` | See declaration / service rules |
| `sourceDigest` | `String` | See declaration / service rules |
| `publishedAt` | `Instant` | See declaration / service rules |
| `publishedBy` | `String` | See declaration / service rules |
| `lifecycle` | `String` | See declaration / service rules |
| `retiredAt` | `Instant` | See declaration / service rules |
| `retiredBy` | `String` | See declaration / service rules |

### DocumentTemplateTypeCatalogDTO

[DocumentTemplateTypeCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/document/v1/DocumentTemplateTypeCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `typeId` | `UUID` | See declaration / service rules |
| `typeCode` | `String` | `@NotBlank(message = "Type code is required")`; `@Size(max = 20, message = "Type code must not exceed 20 characters")` |
| `typeName` | `String` | `@NotBlank(message = "Type name is required")`; `@Size(max = 100, message = "Type name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### IdentityDocumentCatalogDTO

[IdentityDocumentCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/identitydocument/v1/IdentityDocumentCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `documentId` | `UUID` | See declaration / service rules |
| `documentCode` | `String` | `@NotBlank(message = "Document code is required")`; `@Size(max = 20, message = "Document code must not exceed 20 characters")` |
| `documentName` | `String` | `@NotBlank(message = "Document name is required")`; `@Size(max = 100, message = "Document name must not exceed 100 characters")` |
| `categoryId` | `UUID` | `@NotNull(message = "Category ID is required")` |
| `category` | `IdentityDocumentCategoryCatalogDTO` | See declaration / service rules |
| `countryId` | `UUID` | `@NotNull(message = "Country ID is required")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `validationRegex` | `String` | `@Size(max = 200, message = "Validation regex must not exceed 200 characters")` |
| `formatDescription` | `String` | `@Size(max = 200, message = "Format description must not exceed 200 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`, `@Valid`. Inspect the linked source for the field-level constraints.

### IdentityDocumentCategoryCatalogDTO

[IdentityDocumentCategoryCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/identitydocument/v1/IdentityDocumentCategoryCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `categoryId` | `UUID` | See declaration / service rules |
| `categoryCode` | `String` | `@NotBlank(message = "Category code is required")`; `@Size(max = 20, message = "Category code must not exceed 20 characters")` |
| `categoryName` | `String` | `@NotBlank(message = "Category name is required")`; `@Size(max = 100, message = "Category name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### IdentityDocumentLocalizationDTO

[IdentityDocumentLocalizationDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/identitydocument/v1/IdentityDocumentLocalizationDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `documentId` | `UUID` | `@NotNull(message = "Document ID is required")` |
| `localeId` | `UUID` | `@NotNull(message = "Locale ID is required")` |
| `documentName` | `String` | `@NotBlank(message = "Document name is required")`; `@Size(max = 100, message = "Document name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `formatDescription` | `String` | `@Size(max = 200, message = "Format description must not exceed 200 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### LegalFormDTO

[LegalFormDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/legal/v1/LegalFormDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `legalFormId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | `@NotNull(message = "Country ID is required")` |
| `code` | `String` | `@NotBlank(message = "Legal form code is required")`; `@Size(max = 20, message = "Legal form code must not exceed 20 characters")` |
| `name` | `String` | `@NotBlank(message = "Legal form name is required")`; `@Size(max = 100, message = "Legal form name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `isCorporate` | `Boolean` | See declaration / service rules |
| `entityType` | `String` | `@Size(max = 50, message = "Entity type must not exceed 50 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### LanguageLocaleDTO

[LanguageLocaleDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/locale/v1/LanguageLocaleDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localeId` | `UUID` | See declaration / service rules |
| `languageCode` | `String` | See declaration / service rules |
| `countryCode` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `languageName` | `String` | See declaration / service rules |
| `nativeName` | `String` | See declaration / service rules |
| `regionName` | `String` | See declaration / service rules |
| `rtl` | `Boolean` | See declaration / service rules |
| `sortOrder` | `Integer` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### LookupDomainDTO

[LookupDomainDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/lookup/v1/LookupDomainDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `domainId` | `UUID` | See declaration / service rules |
| `domainCode` | `String` | `@NotBlank(message = "Domain code is required")`; `@Size(max = 50, message = "Domain code must not exceed 50 characters")` |
| `domainName` | `String` | `@NotBlank(message = "Domain name is required")`; `@Size(max = 100, message = "Domain name must not exceed 100 characters")` |
| `domainDesc` | `String` | `@Size(max = 500, message = "Domain description must not exceed 500 characters")` |
| `parentDomainId` | `UUID` | See declaration / service rules |
| `multiselectAllowed` | `Boolean` | See declaration / service rules |
| `hierarchyAllowed` | `Boolean` | See declaration / service rules |
| `tenantOverridable` | `Boolean` | See declaration / service rules |
| `extraJson` | `String` | `@Size(max = 2000, message = "Extra JSON must not exceed 2000 characters")` |
| `tenantId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### LookupItemDTO

[LookupItemDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/lookup/v1/LookupItemDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `itemId` | `UUID` | See declaration / service rules |
| `domainId` | `UUID` | `@NotNull(message = "Domain ID is required")` |
| `itemCode` | `String` | `@NotBlank(message = "Item code is required")`; `@Size(max = 50, message = "Item code must not exceed 50 characters")` |
| `itemLabelDefault` | `String` | `@NotBlank(message = "Item label is required")`; `@Size(max = 200, message = "Item label must not exceed 200 characters")` |
| `itemDesc` | `String` | `@Size(max = 500, message = "Item description must not exceed 500 characters")` |
| `parentItemId` | `UUID` | See declaration / service rules |
| `sortOrder` | `Integer` | `@Min(value = 0, message = "Sort order must be non-negative")` |
| `effectiveFrom` | `LocalDate` | See declaration / service rules |
| `effectiveTo` | `LocalDate` | See declaration / service rules |
| `isCurrent` | `Boolean` | See declaration / service rules |
| `extraJson` | `String` | `@Size(max = 2000, message = "Extra JSON must not exceed 2000 characters")` |
| `tenantId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |

Validation annotations present in this declaration: `@Min`, `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### MessageTypeCatalogDTO

[MessageTypeCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/notification/v1/MessageTypeCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `typeId` | `UUID` | See declaration / service rules |
| `typeCode` | `String` | `@NotBlank(message = "Type code is required")`; `@Size(max = 20, message = "Type code must not exceed 20 characters")` |
| `typeName` | `String` | `@NotBlank(message = "Type name is required")`; `@Size(max = 100, message = "Type name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### NotificationMessageCatalogDTO

[NotificationMessageCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/notification/v1/NotificationMessageCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `messageId` | `UUID` | See declaration / service rules |
| `messageCode` | `String` | `@NotBlank(message = "Message code is required")`; `@Size(max = 50, message = "Message code must not exceed 50 characters")` |
| `typeId` | `UUID` | `@NotNull(message = "Type ID is required")` |
| `messageType` | `MessageTypeCatalogDTO` | See declaration / service rules |
| `eventType` | `String` | `@NotBlank(message = "Event type is required")`; `@Size(max = 50, message = "Event type must not exceed 50 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `defaultSubject` | `String` | `@Size(max = 200, message = "Default subject must not exceed 200 characters")` |
| `defaultMessage` | `String` | `@Size(max = 2000, message = "Default message must not exceed 2000 characters")` |
| `parameters` | `Map<String, Object>` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`, `@Valid`. Inspect the linked source for the field-level constraints.

### NotificationMessageLocalizationDTO

[NotificationMessageLocalizationDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/notification/v1/NotificationMessageLocalizationDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `messageId` | `UUID` | `@NotNull(message = "Message ID is required")` |
| `localeId` | `UUID` | `@NotNull(message = "Locale ID is required")` |
| `subject` | `String` | `@NotBlank(message = "Subject is required")`; `@Size(max = 200, message = "Subject must not exceed 200 characters")` |
| `message` | `String` | `@NotBlank(message = "Message is required")`; `@Size(max = 2000, message = "Message must not exceed 2000 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### NotificationMessageTemplateDTO

[NotificationMessageTemplateDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/notification/v1/NotificationMessageTemplateDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `templateId` | `UUID` | See declaration / service rules |
| `messageId` | `UUID` | `@NotNull(message = "Message ID is required")` |
| `templateName` | `String` | `@NotBlank(message = "Template name is required")`; `@Size(max = 100, message = "Template name must not exceed 100 characters")` |
| `templateContent` | `String` | `@NotBlank(message = "Template content is required")`; `@Size(max = 5000, message = "Template content must not exceed 5000 characters")` |
| `templateType` | `String` | `@NotBlank(message = "Template type is required")`; `@Size(max = 50, message = "Template type must not exceed 50 characters")` |
| `version` | `String` | `@Size(max = 20, message = "Version must not exceed 20 characters")` |
| `templateVariables` | `Map<String, Object>` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### RelationshipTypeMasterDTO

[RelationshipTypeMasterDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/relationships/v1/RelationshipTypeMasterDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `relationshipTypeId` | `UUID` | See declaration / service rules |
| `code` | `String` | `@NotBlank(message = "Relationship type code is required")`; `@Size(max = 100, message = "Relationship type code must not exceed 100 characters")` |
| `description` | `String` | `@NotBlank(message = "Description is required")`; `@Size(max = 200, message = "Description must not exceed 200 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### RuleOperationTypeDTO

[RuleOperationTypeDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/ruleoperationtype/v1/RuleOperationTypeDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `operationTypeId` | `UUID` | See declaration / service rules |
| `operationTypeCode` | `String` | `@NotBlank(message = "Operation type code is required")`; `@Size(max = 50, message = "Operation type code must not exceed 50 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `name` | `String` | `@NotBlank(message = "Name is required")`; `@Size(max = 100, message = "Name must not exceed 100 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@Size`. Inspect the linked source for the field-level constraints.

### TitleMasterDTO

[TitleMasterDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/title/v1/TitleMasterDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `titleId` | `UUID` | See declaration / service rules |
| `prefix` | `String` | `@NotBlank(message = "Prefix is required")`; `@Size(max = 10, message = "Prefix must not exceed 10 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `isActive` | `Boolean` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### TransactionCategoryCatalogDTO

[TransactionCategoryCatalogDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/transaction/v1/TransactionCategoryCatalogDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `categoryId` | `UUID` | See declaration / service rules |
| `categoryCode` | `String` | `@NotBlank(message = "Category code is required")`; `@Size(max = 20, message = "Category code must not exceed 20 characters")` |
| `categoryName` | `String` | `@NotBlank(message = "Category name is required")`; `@Size(max = 100, message = "Category name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `parentCategoryId` | `UUID` | See declaration / service rules |
| `parentCategory` | `TransactionCategoryCatalogDTO` | See declaration / service rules |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |
| `svgIcon` | `String` | `@Size(max = 10000, message = "SVG icon data must not exceed 10000 characters")` |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### TransactionCategoryLocalizationDTO

[TransactionCategoryLocalizationDTO](../../core-common-reference-master-data-interfaces/src/main/java/io/fireflysoftware/masters/interfaces/dtos/transaction/v1/TransactionCategoryLocalizationDTO.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `categoryId` | `UUID` | `@NotNull(message = "Category ID is required")` |
| `localeId` | `UUID` | `@NotNull(message = "Locale ID is required")` |
| `categoryName` | `String` | `@NotBlank(message = "Category name is required")`; `@Size(max = 100, message = "Category name must not exceed 100 characters")` |
| `description` | `String` | `@Size(max = 500, message = "Description must not exceed 500 characters")` |
| `status` | `StatusEnum` | `@NotNull(message = "Status is required")` |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

Validation annotations present in this declaration: `@NotBlank`, `@NotNull`, `@Size`. Inspect the linked source for the field-level constraints.

### ActivityCode

[ActivityCode](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/activity/v1/ActivityCode.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `activityCodeId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | See declaration / service rules |
| `code` | `String` | See declaration / service rules |
| `classificationSys` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `parentCodeId` | `UUID` | See declaration / service rules |
| `highRisk` | `Boolean` | See declaration / service rules |
| `riskFactors` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### AssetType

[AssetType](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/assettype/v1/AssetType.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `assetId` | `UUID` | See declaration / service rules |
| `assetCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### BankInstitutionCode

[BankInstitutionCode](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/bank/v1/BankInstitutionCode.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `institutionId` | `UUID` | See declaration / service rules |
| `bankName` | `String` | See declaration / service rules |
| `swiftCode` | `String` | See declaration / service rules |
| `routingNumber` | `String` | See declaration / service rules |
| `ibanPrefix` | `String` | See declaration / service rules |
| `countryId` | `UUID` | See declaration / service rules |
| `institutionTypeLkpId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `svgIcon` | `String` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ConsentCatalog

[ConsentCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/consent/v1/ConsentCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `consentId` | `UUID` | See declaration / service rules |
| `consentType` | `String` | See declaration / service rules |
| `consentDescription` | `String` | See declaration / service rules |
| `expiryPeriodDays` | `Integer` | See declaration / service rules |
| `consentVersion` | `String` | See declaration / service rules |
| `consentSource` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `isRequired` | `Boolean` | See declaration / service rules |
| `sortOrder` | `Integer` | See declaration / service rules |
| `applicableProduct` | `String` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ContractDocumentType

[ContractDocumentType](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/contractdocumenttype/v1/ContractDocumentType.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `documentTypeId` | `UUID` | See declaration / service rules |
| `documentCode` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ContractRole

[ContractRole](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/contractrole/v1/ContractRole.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `roleId` | `UUID` | See declaration / service rules |
| `roleCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ContractRoleScope

[ContractRoleScope](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/contractrole/v1/ContractRoleScope.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `scopeId` | `UUID` | See declaration / service rules |
| `roleId` | `UUID` | See declaration / service rules |
| `scopeCode` | `String` | See declaration / service rules |
| `scopeName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `actionType` | `String` | See declaration / service rules |
| `resourceType` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### ContractType

[ContractType](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/contracttype/v1/ContractType.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `contractId` | `UUID` | See declaration / service rules |
| `contractCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### Country

[Country](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/country/v1/Country.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `countryId` | `UUID` | See declaration / service rules |
| `isoCode` | `String` | See declaration / service rules |
| `countryName` | `String` | See declaration / service rules |
| `region` | `RegionEnum` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `svgFlag` | `String` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### Currency

[Currency](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/currency/v1/Currency.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `currencyId` | `UUID` | See declaration / service rules |
| `isoCode` | `String` | See declaration / service rules |
| `currencyName` | `String` | See declaration / service rules |
| `symbol` | `String` | See declaration / service rules |
| `decimalPrecision` | `Integer` | See declaration / service rules |
| `isMajor` | `Boolean` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### AdministrativeDivision

[AdministrativeDivision](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/division/v1/AdministrativeDivision.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `divisionId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | See declaration / service rules |
| `code` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `level` | `String` | See declaration / service rules |
| `parentDivisionId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `postalCodePattern` | `String` | See declaration / service rules |
| `timeZone` | `String` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### DocumentLayoutPublication

[DocumentLayoutPublication](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/document/layout/v1/DocumentLayoutPublication.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `layoutRevisionId` | `UUID` | See declaration / service rules |
| `sourceRevisionId` | `UUID` | See declaration / service rules |
| `templateId` | `UUID` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `canonicalJson` | `String` | See declaration / service rules |
| `definitionDigest` | `String` | See declaration / service rules |
| `publicationIntentCanonical` | `String` | See declaration / service rules |
| `publicationIntentDigest` | `String` | See declaration / service rules |
| `publicationActionId` | `UUID` | See declaration / service rules |
| `publishedAtEpochMillis` | `long` | See declaration / service rules |
| `publishedBy` | `String` | See declaration / service rules |
| `lifecycle` | `String` | See declaration / service rules |
| `stateVersion` | `int` | See declaration / service rules |
| `retirementActionId` | `UUID` | See declaration / service rules |
| `retiredAtEpochMillis` | `Long` | See declaration / service rules |
| `retiredBy` | `String` | See declaration / service rules |
| `retirementReason` | `String` | See declaration / service rules |

### DocumentTemplateCatalog

[DocumentTemplateCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/document/v1/DocumentTemplateCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `templateId` | `UUID` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `typeId` | `UUID` | See declaration / service rules |
| `category` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `templateName` | `String` | See declaration / service rules |
| `templateContent` | `String` | See declaration / service rules |
| `templateVariables` | `Json` | See declaration / service rules |
| `inputSchema` | `Json` | See declaration / service rules |
| `styleContent` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### DocumentTemplateLocalization

[DocumentTemplateLocalization](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/document/v1/DocumentTemplateLocalization.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `templateId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `templateName` | `String` | See declaration / service rules |
| `templateContent` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### DocumentTemplateRevision

[DocumentTemplateRevision](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/document/v1/DocumentTemplateRevision.java) · DTO / record / model

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `revisionId` | `UUID` | See declaration / service rules |
| `templateId` | `UUID` | See declaration / service rules |
| `localizationId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `templateCode` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `templateName` | `String` | See declaration / service rules |
| `mediaType` | `String` | See declaration / service rules |
| `templateContent` | `String` | See declaration / service rules |
| `inputSchema` | `String` | See declaration / service rules |
| `styleContent` | `String` | See declaration / service rules |
| `templateVariables` | `String` | See declaration / service rules |
| `sourceSnapshot` | `String` | See declaration / service rules |
| `contentDigest` | `String` | See declaration / service rules |
| `schemaDigest` | `String` | See declaration / service rules |
| `styleDigest` | `String` | See declaration / service rules |
| `sourceDigest` | `String` | See declaration / service rules |
| `publishedAt` | `Instant` | See declaration / service rules |
| `publishedBy` | `String` | See declaration / service rules |
| `lifecycle` | `String` | See declaration / service rules |
| `retiredAt` | `Instant` | See declaration / service rules |
| `retiredBy` | `String` | See declaration / service rules |

### DocumentTemplateTypeCatalog

[DocumentTemplateTypeCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/document/v1/DocumentTemplateTypeCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `typeId` | `UUID` | See declaration / service rules |
| `typeCode` | `String` | See declaration / service rules |
| `typeName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### IdentityDocumentCatalog

[IdentityDocumentCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/identitydocument/v1/IdentityDocumentCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `documentId` | `UUID` | See declaration / service rules |
| `documentCode` | `String` | See declaration / service rules |
| `documentName` | `String` | See declaration / service rules |
| `categoryId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `validationRegex` | `String` | See declaration / service rules |
| `formatDescription` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### IdentityDocumentCategoryCatalog

[IdentityDocumentCategoryCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/identitydocument/v1/IdentityDocumentCategoryCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `categoryId` | `UUID` | See declaration / service rules |
| `categoryCode` | `String` | See declaration / service rules |
| `categoryName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### IdentityDocumentLocalization

[IdentityDocumentLocalization](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/identitydocument/v1/IdentityDocumentLocalization.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `documentId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `documentName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `formatDescription` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### LegalForm

[LegalForm](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/legal/v1/LegalForm.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `legalFormId` | `UUID` | See declaration / service rules |
| `countryId` | `UUID` | See declaration / service rules |
| `code` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `isCorporate` | `Boolean` | See declaration / service rules |
| `entityType` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### LanguageLocale

[LanguageLocale](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/locale/v1/LanguageLocale.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localeId` | `UUID` | See declaration / service rules |
| `languageCode` | `String` | See declaration / service rules |
| `countryCode` | `String` | See declaration / service rules |
| `localeCode` | `String` | See declaration / service rules |
| `languageName` | `String` | See declaration / service rules |
| `nativeName` | `String` | See declaration / service rules |
| `regionName` | `String` | See declaration / service rules |
| `rtl` | `Boolean` | See declaration / service rules |
| `sortOrder` | `Integer` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### LookupDomain

[LookupDomain](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/lookup/v1/LookupDomain.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `domainId` | `UUID` | See declaration / service rules |
| `domainCode` | `String` | See declaration / service rules |
| `domainName` | `String` | See declaration / service rules |
| `domainDesc` | `String` | See declaration / service rules |
| `parentDomainId` | `UUID` | See declaration / service rules |
| `multiselectAllowed` | `Boolean` | See declaration / service rules |
| `hierarchyAllowed` | `Boolean` | See declaration / service rules |
| `tenantOverridable` | `Boolean` | See declaration / service rules |
| `extraJson` | `String` | See declaration / service rules |
| `tenantId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### LookupItem

[LookupItem](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/lookup/v1/LookupItem.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `itemId` | `UUID` | See declaration / service rules |
| `domainId` | `UUID` | See declaration / service rules |
| `itemCode` | `String` | See declaration / service rules |
| `itemLabelDefault` | `String` | See declaration / service rules |
| `itemDesc` | `String` | See declaration / service rules |
| `parentItemId` | `UUID` | See declaration / service rules |
| `sortOrder` | `Integer` | See declaration / service rules |
| `effectiveFrom` | `LocalDate` | See declaration / service rules |
| `effectiveTo` | `LocalDate` | See declaration / service rules |
| `isCurrent` | `Boolean` | See declaration / service rules |
| `extraJson` | `String` | See declaration / service rules |
| `tenantId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### MessageTypeCatalog

[MessageTypeCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/notification/v1/MessageTypeCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `typeId` | `UUID` | See declaration / service rules |
| `typeCode` | `String` | See declaration / service rules |
| `typeName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### NotificationMessageCatalog

[NotificationMessageCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/notification/v1/NotificationMessageCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `messageId` | `UUID` | See declaration / service rules |
| `messageCode` | `String` | See declaration / service rules |
| `typeId` | `UUID` | See declaration / service rules |
| `eventType` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `defaultSubject` | `String` | See declaration / service rules |
| `defaultMessage` | `String` | See declaration / service rules |
| `parameters` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### NotificationMessageLocalization

[NotificationMessageLocalization](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/notification/v1/NotificationMessageLocalization.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `messageId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `subject` | `String` | See declaration / service rules |
| `message` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### NotificationMessageTemplate

[NotificationMessageTemplate](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/notification/v1/NotificationMessageTemplate.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `templateId` | `UUID` | See declaration / service rules |
| `messageId` | `UUID` | See declaration / service rules |
| `templateName` | `String` | See declaration / service rules |
| `templateContent` | `String` | See declaration / service rules |
| `templateType` | `String` | See declaration / service rules |
| `version` | `String` | See declaration / service rules |
| `templateVariables` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### RelationshipTypeMaster

[RelationshipTypeMaster](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/relationships/v1/RelationshipTypeMaster.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `relationshipTypeId` | `UUID` | See declaration / service rules |
| `code` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### RuleOperationType

[RuleOperationType](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/ruleoperationtype/v1/RuleOperationType.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `operationTypeId` | `UUID` | See declaration / service rules |
| `operationTypeCode` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `name` | `String` | See declaration / service rules |
| `isActive` | `Boolean` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### TitleMaster

[TitleMaster](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/title/v1/TitleMaster.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `titleId` | `UUID` | See declaration / service rules |
| `prefix` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

### TransactionCategoryCatalog

[TransactionCategoryCatalog](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/transaction/v1/TransactionCategoryCatalog.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `categoryId` | `UUID` | See declaration / service rules |
| `categoryCode` | `String` | See declaration / service rules |
| `categoryName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `parentCategoryId` | `UUID` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |
| `svgIcon` | `String` | See declaration / service rules |

### TransactionCategoryLocalization

[TransactionCategoryLocalization](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/entities/transaction/v1/TransactionCategoryLocalization.java) · Persisted entity

| Field | Java type | Validation annotations |
| --- | --- | --- |
| `localizationId` | `UUID` | See declaration / service rules |
| `categoryId` | `UUID` | See declaration / service rules |
| `localeId` | `UUID` | See declaration / service rules |
| `categoryName` | `String` | See declaration / service rules |
| `description` | `String` | See declaration / service rules |
| `status` | `StatusEnum` | See declaration / service rules |
| `dateCreated` | `LocalDateTime` | See declaration / service rules |
| `dateUpdated` | `LocalDateTime` | See declaration / service rules |

## Ports and service contracts

### ActivityCodeService

[ActivityCodeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/activity/v1/ActivityCodeService.java)

```java
Mono<PaginationResponse<ActivityCodeDTO>> listActivityCodes(PaginationRequest paginationRequest);
Flux<ActivityCodeDTO> getActivityCodesByCountry(UUID countryId);
Flux<ActivityCodeDTO> getChildActivityCodes(UUID parentCodeId);
Mono<ActivityCodeDTO> createActivityCode(ActivityCodeDTO activityCodeDto);
Mono<ActivityCodeDTO> getActivityCode(UUID activityCodeId);
Mono<ActivityCodeDTO> updateActivityCode(UUID activityCodeId, ActivityCodeDTO activityCodeDto);
Mono<Void> deleteActivityCode(UUID activityCodeId);
```

### AssetTypeService

[AssetTypeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/assettype/v1/AssetTypeService.java)

```java
Mono<PaginationResponse<AssetTypeDTO>> listAssetTypes(PaginationRequest paginationRequest);
Mono<AssetTypeDTO> createAssetType(AssetTypeDTO assetTypeDto);
Mono<AssetTypeDTO> getAssetType(UUID assetId);
Mono<AssetTypeDTO> updateAssetType(UUID assetId, AssetTypeDTO assetTypeDto);
Mono<Void> deleteAssetType(UUID assetId);
```

### BankInstitutionCodeService

[BankInstitutionCodeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/bank/v1/BankInstitutionCodeService.java)

```java
Mono<PaginationResponse<BankInstitutionCodeDTO>> listBankInstitutionCodes(PaginationRequest paginationRequest);
Mono<BankInstitutionCodeDTO> createBankInstitutionCode(BankInstitutionCodeDTO dto);
Mono<BankInstitutionCodeDTO> getBankInstitutionCode(UUID id);
Mono<BankInstitutionCodeDTO> updateBankInstitutionCode(UUID id, BankInstitutionCodeDTO dto);
Mono<Void> deleteBankInstitutionCode(UUID id);
```

### ConsentCatalogService

[ConsentCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/consent/v1/ConsentCatalogService.java)

```java
Mono<PaginationResponse<ConsentCatalogDTO>> listConsentCatalog(PaginationRequest paginationRequest);
Mono<PaginationResponse<ConsentCatalogDTO>> listConsentCatalogByType(String consentType, PaginationRequest paginationRequest);
Mono<ConsentCatalogDTO> createConsentCatalog(ConsentCatalogDTO dto);
Mono<ConsentCatalogDTO> getConsentCatalog(UUID id);
Mono<ConsentCatalogDTO> updateConsentCatalog(UUID id, ConsentCatalogDTO dto);
Mono<Void> deleteConsentCatalog(UUID id);
```

### ContractDocumentTypeService

[ContractDocumentTypeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/contractdocumenttype/v1/ContractDocumentTypeService.java)

```java
Mono<PaginationResponse<ContractDocumentTypeDTO>> listContractDocumentTypes(PaginationRequest paginationRequest);
Mono<ContractDocumentTypeDTO> createContractDocumentType(ContractDocumentTypeDTO contractDocumentTypeDto);
Mono<ContractDocumentTypeDTO> getContractDocumentType(UUID documentTypeId);
Mono<ContractDocumentTypeDTO> getContractDocumentTypeByCode(String documentCode);
Mono<ContractDocumentTypeDTO> updateContractDocumentType(UUID documentTypeId, ContractDocumentTypeDTO contractDocumentTypeDto);
Mono<Void> deleteContractDocumentType(UUID documentTypeId);
```

### ContractRoleScopeService

[ContractRoleScopeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/contractrole/v1/ContractRoleScopeService.java)

```java
Mono<PaginationResponse<ContractRoleScopeDTO>> listContractRoleScopes(FilterRequest<ContractRoleScopeDTO> filterRequest);
Mono<ContractRoleScopeDTO> createContractRoleScope(ContractRoleScopeDTO contractRoleScopeDto);
Mono<ContractRoleScopeDTO> getContractRoleScope(UUID scopeId);
Mono<ContractRoleScopeDTO> updateContractRoleScope(UUID scopeId, ContractRoleScopeDTO contractRoleScopeDto);
Mono<Void> deleteContractRoleScope(UUID scopeId);
Flux<ContractRoleScopeDTO> getScopesByRoleId(UUID roleId);
Flux<ContractRoleScopeDTO> getActiveScopesByRoleId(UUID roleId);
```

### ContractRoleService

[ContractRoleService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/contractrole/v1/ContractRoleService.java)

```java
Mono<PaginationResponse<ContractRoleDTO>> listContractRoles(FilterRequest<ContractRoleDTO> filterRequest);
Mono<ContractRoleDTO> createContractRole(ContractRoleDTO contractRoleDto);
Mono<ContractRoleDTO> getContractRole(UUID roleId);
Mono<ContractRoleDTO> updateContractRole(UUID roleId, ContractRoleDTO contractRoleDto);
Mono<Void> deleteContractRole(UUID roleId);
```

### ContractTypeService

[ContractTypeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/contracttype/v1/ContractTypeService.java)

```java
Mono<PaginationResponse<ContractTypeDTO>> listContractTypes(PaginationRequest paginationRequest);
Mono<ContractTypeDTO> createContractType(ContractTypeDTO contractTypeDto);
Mono<ContractTypeDTO> getContractType(UUID contractId);
Mono<ContractTypeDTO> updateContractType(UUID contractId, ContractTypeDTO contractTypeDto);
Mono<Void> deleteContractType(UUID contractId);
```

### CountryService

[CountryService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/country/v1/CountryService.java)

```java
Mono<PaginationResponse<CountryDTO>> listCountries(FilterRequest<CountryDTO> filterRequest);
Mono<CountryDTO> createCountry(CountryDTO countryDto);
Mono<CountryDTO> getCountry(UUID countryId);
Mono<CountryDTO> updateCountry(UUID countryId, CountryDTO countryDto);
Mono<Void> deleteCountry(UUID countryId);
```

### CurrencyService

[CurrencyService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/v1/CurrencyService.java)

```java
Mono<PaginationResponse<CurrencyDTO>> listCurrencies(FilterRequest<CurrencyDTO> filterRequest);
Mono<CurrencyDTO> createCurrency(CurrencyDTO currencyDto);
Mono<CurrencyDTO> getCurrency(UUID currencyId);
Mono<CurrencyDTO> updateCurrency(UUID currencyId, CurrencyDTO currencyDto);
Mono<Void> deleteCurrency(UUID currencyId);
```

### AdministrativeDivisionService

[AdministrativeDivisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/division/v1/AdministrativeDivisionService.java)

```java
Mono<PaginationResponse<AdministrativeDivisionDTO>> listDivisions(PaginationRequest paginationRequest);
Mono<AdministrativeDivisionDTO> createDivision(AdministrativeDivisionDTO divisionDto);
Mono<AdministrativeDivisionDTO> getDivision(UUID divisionId);
Mono<AdministrativeDivisionDTO> updateDivision(UUID divisionId, AdministrativeDivisionDTO divisionDto);
Mono<Void> deleteDivision(UUID divisionId);
```

### DocumentLayoutProfileRegistry

[DocumentLayoutProfileRegistry](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutProfileRegistry.java)

```java
DocumentLayoutOutputProfileMetadata requireRecognized( DocumentLayoutOutputProfileReference reference);
```

### DocumentTemplateCatalogService

[DocumentTemplateCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogService.java)

```java
Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplates(PaginationRequest paginationRequest);
Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplatesByCategory(String category, PaginationRequest paginationRequest);
Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplatesByTypeId(UUID typeId, PaginationRequest paginationRequest);
Mono<DocumentTemplateCatalogDTO> createDocumentTemplate(DocumentTemplateCatalogDTO documentTemplateDTO);
Mono<DocumentTemplateCatalogDTO> getDocumentTemplate(UUID templateId);
Mono<DocumentTemplateCatalogDTO> getDocumentTemplateByCode(String templateCode);
Mono<DocumentTemplateCatalogDTO> updateDocumentTemplate(UUID templateId, DocumentTemplateCatalogDTO documentTemplateDTO);
Mono<Void> deleteDocumentTemplate(UUID templateId);
```

### DocumentTemplateLocalizationService

[DocumentTemplateLocalizationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationService.java)

```java
Flux<DocumentTemplateLocalizationDTO> getLocalizationsByTemplateId(UUID templateId);
Flux<DocumentTemplateLocalizationDTO> getLocalizationsByLocaleId(UUID localeId);
Mono<DocumentTemplateLocalizationDTO> createDocumentTemplateLocalization(DocumentTemplateLocalizationDTO localizationDTO);
Mono<DocumentTemplateLocalizationDTO> getDocumentTemplateLocalization(UUID localizationId);
Mono<DocumentTemplateLocalizationDTO> getDocumentTemplateLocalizationByTemplateAndLocale(UUID templateId, UUID localeId);
Mono<DocumentTemplateLocalizationDTO> updateDocumentTemplateLocalization(UUID localizationId, DocumentTemplateLocalizationDTO localizationDTO);
Mono<Void> deleteDocumentTemplateLocalization(UUID localizationId);
Mono<Void> deleteLocalizationsByTemplateId(UUID templateId);
```

### DocumentTemplateTypeCatalogService

[DocumentTemplateTypeCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogService.java)

```java
Mono<PaginationResponse<DocumentTemplateTypeCatalogDTO>> listDocumentTemplateTypes(PaginationRequest paginationRequest);
Mono<DocumentTemplateTypeCatalogDTO> createDocumentTemplateType(DocumentTemplateTypeCatalogDTO documentTemplateTypeDTO);
Mono<DocumentTemplateTypeCatalogDTO> getDocumentTemplateType(UUID typeId);
Mono<DocumentTemplateTypeCatalogDTO> getDocumentTemplateTypeByCode(String typeCode);
Mono<DocumentTemplateTypeCatalogDTO> updateDocumentTemplateType(UUID typeId, DocumentTemplateTypeCatalogDTO documentTemplateTypeDTO);
Mono<Void> deleteDocumentTemplateType(UUID typeId);
```

### IdentityDocumentCatalogService

[IdentityDocumentCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogService.java)

```java
Mono<PaginationResponse<IdentityDocumentCatalogDTO>> listIdentityDocuments(PaginationRequest paginationRequest);
Mono<PaginationResponse<IdentityDocumentCatalogDTO>> listIdentityDocumentsByCategory(UUID categoryId, PaginationRequest paginationRequest);
Mono<PaginationResponse<IdentityDocumentCatalogDTO>> listIdentityDocumentsByCountry(UUID countryId, PaginationRequest paginationRequest);
Mono<IdentityDocumentCatalogDTO> createIdentityDocument(IdentityDocumentCatalogDTO identityDocumentDTO);
Mono<IdentityDocumentCatalogDTO> getIdentityDocument(UUID documentId);
Mono<IdentityDocumentCatalogDTO> getIdentityDocumentByCode(String documentCode);
Mono<IdentityDocumentCatalogDTO> updateIdentityDocument(UUID documentId, IdentityDocumentCatalogDTO identityDocumentDTO);
Mono<Void> deleteIdentityDocument(UUID documentId);
```

### IdentityDocumentCategoryCatalogService

[IdentityDocumentCategoryCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogService.java)

```java
Mono<PaginationResponse<IdentityDocumentCategoryCatalogDTO>> listIdentityDocumentCategories(PaginationRequest paginationRequest);
Mono<IdentityDocumentCategoryCatalogDTO> createIdentityDocumentCategory(IdentityDocumentCategoryCatalogDTO identityDocumentCategoryDTO);
Mono<IdentityDocumentCategoryCatalogDTO> getIdentityDocumentCategory(UUID categoryId);
Mono<IdentityDocumentCategoryCatalogDTO> getIdentityDocumentCategoryByCode(String categoryCode);
Mono<IdentityDocumentCategoryCatalogDTO> updateIdentityDocumentCategory(UUID categoryId, IdentityDocumentCategoryCatalogDTO identityDocumentCategoryDTO);
Mono<Void> deleteIdentityDocumentCategory(UUID categoryId);
```

### IdentityDocumentLocalizationService

[IdentityDocumentLocalizationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationService.java)

```java
Mono<PaginationResponse<IdentityDocumentLocalizationDTO>> listIdentityDocumentLocalizations(PaginationRequest paginationRequest);
Mono<PaginationResponse<IdentityDocumentLocalizationDTO>> getLocalizationsByDocumentId(UUID documentId, PaginationRequest paginationRequest);
Mono<IdentityDocumentLocalizationDTO> createIdentityDocumentLocalization(IdentityDocumentLocalizationDTO localizationDTO);
Mono<IdentityDocumentLocalizationDTO> getIdentityDocumentLocalizationByDocumentAndLocale(UUID documentId, UUID localeId);
Mono<IdentityDocumentLocalizationDTO> updateIdentityDocumentLocalization(UUID localizationId, IdentityDocumentLocalizationDTO localizationDTO);
Mono<Void> deleteIdentityDocumentLocalization(UUID localizationId);
Mono<Void> deleteLocalizationsByDocumentId(UUID documentId);
```

### LegalFormService

[LegalFormService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/legal/v1/LegalFormService.java)

```java
Mono<PaginationResponse<LegalFormDTO>> listLegalForms(FilterRequest<LegalFormDTO> filterRequest);
Flux<LegalFormDTO> getLegalFormsByCountry(UUID countryId);
Mono<LegalFormDTO> createLegalForm(LegalFormDTO legalFormDto);
Mono<LegalFormDTO> getLegalForm(UUID legalFormId);
Mono<LegalFormDTO> updateLegalForm(UUID legalFormId, LegalFormDTO legalFormDto);
Mono<Void> deleteLegalForm(UUID legalFormId);
```

### LanguageLocaleService

[LanguageLocaleService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/locale/v1/LanguageLocaleService.java)

```java
Mono<PaginationResponse<LanguageLocaleDTO>> listLanguageLocales(PaginationRequest paginationRequest);
Mono<LanguageLocaleDTO> createLanguageLocale(LanguageLocaleDTO dto);
Mono<LanguageLocaleDTO> getLanguageLocale(UUID id);
Mono<LanguageLocaleDTO> updateLanguageLocale(UUID id, LanguageLocaleDTO dto);
Mono<Void> deleteLanguageLocale(UUID id);
```

### LookupDomainService

[LookupDomainService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupDomainService.java)

```java
Mono<PaginationResponse<LookupDomainDTO>> listDomains(PaginationRequest paginationRequest);
Mono<LookupDomainDTO> createDomain(LookupDomainDTO domainDto);
Mono<LookupDomainDTO> getDomain(UUID domainId);
Mono<LookupDomainDTO> updateDomain(UUID domainId, LookupDomainDTO domainDto);
Mono<Void> deleteDomain(UUID domainId);
```

### LookupItemService

[LookupItemService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupItemService.java)

```java
Mono<PaginationResponse<LookupItemDTO>> listItems(PaginationRequest paginationRequest);
Flux<LookupItemDTO> getItemsByDomain(UUID domainId);
Mono<LookupItemDTO> createItem(LookupItemDTO itemDto);
Mono<LookupItemDTO> getItem(UUID itemId);
Mono<LookupItemDTO> updateItem(UUID itemId, LookupItemDTO itemDto);
Mono<Void> deleteItem(UUID itemId);
```

### MessageTypeCatalogService

[MessageTypeCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogService.java)

```java
Mono<PaginationResponse<MessageTypeCatalogDTO>> listMessageTypes(PaginationRequest paginationRequest);
Mono<MessageTypeCatalogDTO> createMessageType(MessageTypeCatalogDTO messageTypeDTO);
Mono<MessageTypeCatalogDTO> getMessageType(UUID typeId);
Mono<MessageTypeCatalogDTO> getMessageTypeByCode(String typeCode);
Mono<MessageTypeCatalogDTO> updateMessageType(UUID typeId, MessageTypeCatalogDTO messageTypeDTO);
Mono<Void> deleteMessageType(UUID typeId);
```

### NotificationMessageCatalogService

[NotificationMessageCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogService.java)

```java
Mono<PaginationResponse<NotificationMessageCatalogDTO>> listNotificationMessages(PaginationRequest paginationRequest);
Mono<PaginationResponse<NotificationMessageCatalogDTO>> listNotificationMessagesByEventType(String eventType, PaginationRequest paginationRequest);
Mono<PaginationResponse<NotificationMessageCatalogDTO>> listNotificationMessagesByTypeId(UUID typeId, PaginationRequest paginationRequest);
Mono<NotificationMessageCatalogDTO> createNotificationMessage(NotificationMessageCatalogDTO notificationMessageDTO);
Mono<NotificationMessageCatalogDTO> getNotificationMessage(UUID messageId);
Mono<NotificationMessageCatalogDTO> getNotificationMessageByCode(String messageCode);
Mono<NotificationMessageCatalogDTO> updateNotificationMessage(UUID messageId, NotificationMessageCatalogDTO notificationMessageDTO);
Mono<Void> deleteNotificationMessage(UUID messageId);
```

### NotificationMessageLocalizationService

[NotificationMessageLocalizationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationService.java)

```java
Flux<NotificationMessageLocalizationDTO> getLocalizationsByMessageId(UUID messageId);
Flux<NotificationMessageLocalizationDTO> getLocalizationsByLocaleId(UUID localeId);
Mono<NotificationMessageLocalizationDTO> createNotificationMessageLocalization(NotificationMessageLocalizationDTO localizationDTO);
Mono<NotificationMessageLocalizationDTO> getNotificationMessageLocalization(UUID localizationId);
Mono<NotificationMessageLocalizationDTO> getNotificationMessageLocalizationByMessageAndLocale(UUID messageId, UUID localeId);
Mono<NotificationMessageLocalizationDTO> updateNotificationMessageLocalization(UUID localizationId, NotificationMessageLocalizationDTO localizationDTO);
Mono<Void> deleteNotificationMessageLocalization(UUID localizationId);
Mono<Void> deleteLocalizationsByMessageId(UUID messageId);
```

### NotificationMessageTemplateService

[NotificationMessageTemplateService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateService.java)

```java
Mono<PaginationResponse<NotificationMessageTemplateDTO>> listNotificationMessageTemplates(PaginationRequest paginationRequest);
Mono<PaginationResponse<NotificationMessageTemplateDTO>> listNotificationMessageTemplatesByType(String templateType, PaginationRequest paginationRequest);
Flux<NotificationMessageTemplateDTO> getTemplatesByMessageId(UUID messageId);
Mono<NotificationMessageTemplateDTO> createNotificationMessageTemplate(NotificationMessageTemplateDTO templateDTO);
Mono<NotificationMessageTemplateDTO> getNotificationMessageTemplate(UUID templateId);
Mono<NotificationMessageTemplateDTO> getNotificationMessageTemplateByNameAndMessageId(UUID messageId, String templateName);
Mono<NotificationMessageTemplateDTO> updateNotificationMessageTemplate(UUID templateId, NotificationMessageTemplateDTO templateDTO);
Mono<Void> deleteNotificationMessageTemplate(UUID templateId);
Mono<Void> deleteTemplatesByMessageId(UUID messageId);
```

### RelationshipTypeMasterService

[RelationshipTypeMasterService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/relationships/v1/RelationshipTypeMasterService.java)

```java
Mono<PaginationResponse<RelationshipTypeMasterDTO>> listRelationshipTypes(PaginationRequest paginationRequest);
Mono<RelationshipTypeMasterDTO> createRelationshipType(RelationshipTypeMasterDTO relationshipTypeDto);
Mono<RelationshipTypeMasterDTO> getRelationshipType(UUID relationshipTypeId);
Mono<RelationshipTypeMasterDTO> updateRelationshipType(UUID relationshipTypeId, RelationshipTypeMasterDTO relationshipTypeDto);
Mono<Void> deleteRelationshipType(UUID relationshipTypeId);
```

### RuleOperationTypeService

[RuleOperationTypeService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/ruleoperationtype/v1/RuleOperationTypeService.java)

```java
Mono<PaginationResponse<RuleOperationTypeDTO>> listRuleOperationTypes(FilterRequest<RuleOperationTypeDTO> filterRequest);
Mono<RuleOperationTypeDTO> createRuleOperationType(RuleOperationTypeDTO dto);
Mono<RuleOperationTypeDTO> getRuleOperationType(UUID operationTypeId);
Mono<RuleOperationTypeDTO> updateRuleOperationType(UUID operationTypeId, RuleOperationTypeDTO dto);
Mono<Void> deleteRuleOperationType(UUID operationTypeId);
```

### TitleMasterService

[TitleMasterService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/title/v1/TitleMasterService.java)

```java
Mono<PaginationResponse<TitleMasterDTO>> listTitles(PaginationRequest paginationRequest);
Mono<TitleMasterDTO> createTitle(TitleMasterDTO titleDto);
Mono<TitleMasterDTO> getTitle(UUID titleId);
Mono<TitleMasterDTO> updateTitle(UUID titleId, TitleMasterDTO titleDto);
Mono<Void> deleteTitle(UUID titleId);
```

### TransactionCategoryCatalogService

[TransactionCategoryCatalogService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryCatalogService.java)

```java
Mono<PaginationResponse<TransactionCategoryCatalogDTO>> listTransactionCategories(PaginationRequest paginationRequest);
Mono<PaginationResponse<TransactionCategoryCatalogDTO>> listRootTransactionCategories(PaginationRequest paginationRequest);
Mono<PaginationResponse<TransactionCategoryCatalogDTO>> listChildTransactionCategories(UUID parentCategoryId, PaginationRequest paginationRequest);
Mono<TransactionCategoryCatalogDTO> createTransactionCategory(TransactionCategoryCatalogDTO transactionCategoryDTO);
Mono<TransactionCategoryCatalogDTO> getTransactionCategory(UUID categoryId);
Mono<TransactionCategoryCatalogDTO> getTransactionCategoryByCode(String categoryCode);
Mono<TransactionCategoryCatalogDTO> updateTransactionCategory(UUID categoryId, TransactionCategoryCatalogDTO transactionCategoryDTO);
Mono<Void> deleteTransactionCategory(UUID categoryId);
```

### TransactionCategoryLocalizationService

[TransactionCategoryLocalizationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationService.java)

```java
Flux<TransactionCategoryLocalizationDTO> getLocalizationsByCategoryId(UUID categoryId);
Mono<PaginationResponse<TransactionCategoryLocalizationDTO>> listLocalizationsByCategoryId(UUID categoryId, PaginationRequest paginationRequest);
Mono<TransactionCategoryLocalizationDTO> createTransactionCategoryLocalization(TransactionCategoryLocalizationDTO localizationDTO);
Mono<TransactionCategoryLocalizationDTO> getTransactionCategoryLocalization(UUID localizationId);
Mono<TransactionCategoryLocalizationDTO> getTransactionCategoryLocalizationByCategoryAndLocale(UUID categoryId, UUID localeId);
Mono<TransactionCategoryLocalizationDTO> updateTransactionCategoryLocalization(UUID localizationId, TransactionCategoryLocalizationDTO localizationDTO);
Mono<Void> deleteTransactionCategoryLocalization(UUID localizationId);
```

## Configuration

Values below come from source placeholders. Plain YAML values and configuration-property classes must also be reviewed at the linked configuration sources. Secret fallback values are deliberately redacted.

| Key | Fallback | Source |
| --- | --- | --- |
| `DB_HOST` | `localhost` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `DB_PORT` | `5432` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `DB_NAME` | `postgres` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `DB_USERNAME` | `postgres` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `DB_PASSWORD` | `[redacted]` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `DB_SSL_MODE` | `disable` | [R2dbcConfiguration](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/config/R2dbcConfiguration.java) |
| `spring.application.name` | No fallback | [MasterDataApplication](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/MasterDataApplication.java) |
| `spring.application.version` | No fallback | [MasterDataApplication](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/MasterDataApplication.java) |
| `spring.application.description` | No fallback | [MasterDataApplication](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/MasterDataApplication.java) |
| `spring.application.team.name` | No fallback | [MasterDataApplication](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/MasterDataApplication.java) |
| `spring.application.team.email` | No fallback | [MasterDataApplication](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/MasterDataApplication.java) |
| `DB_HOST` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `DB_PORT` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `DB_NAME` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `DB_SSL_MODE` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `DB_USERNAME` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `DB_PASSWORD` | No fallback | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `SERVER_ADDRESS` | `localhost` | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |
| `SERVER_PORT` | `8080` | [application](../../core-common-reference-master-data-web/src/main/resources/application.yaml) |

## Security and failure evidence

| Declaration or failure site | Source |
| --- | --- |
| `new RuntimeException("Error converting template variables JSON to Map", e);` | [DocumentTemplateCatalogMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/document/v1/DocumentTemplateCatalogMapper.java) |
| `new RuntimeException("Error converting template variables Map to JSON", e);` | [DocumentTemplateCatalogMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/document/v1/DocumentTemplateCatalogMapper.java) |
| `new RuntimeException("Error converting parameters JSON to Map", e);` | [NotificationMessageCatalogMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/notification/v1/NotificationMessageCatalogMapper.java) |
| `new RuntimeException("Error converting parameters Map to JSON", e);` | [NotificationMessageCatalogMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/notification/v1/NotificationMessageCatalogMapper.java) |
| `new RuntimeException("Error converting template variables JSON to Map", e);` | [NotificationMessageTemplateMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/notification/v1/NotificationMessageTemplateMapper.java) |
| `new RuntimeException("Error converting template variables Map to JSON", e);` | [NotificationMessageTemplateMapper](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/mappers/notification/v1/NotificationMessageTemplateMapper.java) |
| `new ResponseStatusException(` | [CurrencyExecutionPolicyService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyExecutionPolicyService.java) |
| `HttpStatus.CONFLICT` | [CurrencyExecutionPolicyService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyExecutionPolicyService.java) |
| `new ResponseStatusException(` | [CurrencyExecutionPolicyService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyExecutionPolicyService.java) |
| `HttpStatus.NOT_FOUND` | [CurrencyExecutionPolicyService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyExecutionPolicyService.java) |
| `new ResponseStatusException(` | [CurrencyPolicyAuthority](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyAuthority.java) |
| `HttpStatus.FORBIDDEN` | [CurrencyPolicyAuthority](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyAuthority.java) |
| `new IllegalStateException(` | [CurrencyPolicyDatabaseContext](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseContext.java) |
| `new IllegalStateException(` | [CurrencyPolicyDatabaseContext](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseContext.java) |
| `new ResponseStatusException(` | [CurrencyPolicyDatabaseContext](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseContext.java) |
| `HttpStatus.SERVICE_UNAVAILABLE` | [CurrencyPolicyDatabaseContext](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseContext.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);` | [CurrencyPolicyRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyRules.java) |
| `new ResponseStatusException(HttpStatus.CONFLICT, reason);` | [CurrencyPolicyRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyRules.java) |
| `HttpStatus.CONFLICT` | [CurrencyServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/currency/v1/CurrencyServiceImpl.java) |
| `new IllegalArgumentException("Duplicate output profile metadata reference");` | [DocumentLayoutProfileConfiguration](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutProfileConfiguration.java) |
| `new IllegalArgumentException();` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new IllegalArgumentException();` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new IllegalStateException(e);` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, message);` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new ResponseStatusException(HttpStatus.CONFLICT, message);` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new ResponseStatusException(` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `HttpStatus.SERVICE_UNAVAILABLE` | [DocumentLayoutRules](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRules.java) |
| `new IllegalStateException("Stored source cannot be read", e);` | [DocumentLayoutSnapshots](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutSnapshots.java) |
| `new IllegalStateException("Stored source cannot be read", e);` | [DocumentLayoutSnapshots](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutSnapshots.java) |
| `new IllegalStateException("Stored layout definition cannot be read", e);` | [DocumentLayoutSnapshots](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutSnapshots.java) |
| `new ResponseStatusException(` | [DocumentLayoutPublicationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutPublicationService.java) |
| `HttpStatus.NOT_FOUND` | [DocumentLayoutPublicationService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutPublicationService.java) |
| `new ResponseStatusException(HttpStatus.CONFLICT, "Document template integrity constraint rejected the operation");` | [DocumentTemplateAccessGuard](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateAccessGuard.java) |
| `new ResponseStatusException(HttpStatus.FORBIDDEN, "Trusted document template permission is required");` | [DocumentTemplateAccessGuard](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateAccessGuard.java) |
| `new IllegalArgumentException("Document template JSON is invalid", error); }` | [DocumentTemplateCanonicalJson](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCanonicalJson.java) |
| `new IllegalArgumentException("Document template canonical numbers must fit 128 characters");` | [DocumentTemplateCanonicalJson](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCanonicalJson.java) |
| `new RuntimeException("Error creating document template: " + e.getMessage(), e)));` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Document template not found with ID: " + templateId)));` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Document template not found with code: " + templateCode)));` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Document template not found with ID: " + templateId)))` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Error updating document template: " + e.getMessage(), e)));` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Document template not found with ID: " + templateId)))` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting document template: " + e.getMessage(), e)));` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply one schema representation: inputSchemaJson or inputSchema");` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST,` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft input schema exceeds supported canonical JSON limits");` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canonical draft schema exceeds the byte limit");` | [DocumentTemplateCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCatalogServiceImpl.java) |
| `new RuntimeException("No localizations found for template ID: " + templateId)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("No localizations found for locale ID: " + localeId)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Error creating document template localization: " + e.getMessage(), e)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Document template localization not found with ID: " + localizationId)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Document template localization not found with template ID: " + templateId + " and locale ID: " + localeId)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Document template localization not found with ID: " + localizationId)))` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Error updating document template localization: " + e.getMessage(), e)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Document template localization not found with ID: " + localizationId)))` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting document template localization: " + e.getMessage(), e)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting localizations for template ID: " + templateId, e)));` | [DocumentTemplateLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateLocalizationServiceImpl.java) |
| `new IllegalStateException("New layout publication requires a local transaction")))` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new IllegalStateException("Published revision JSON cannot be read", error); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new IllegalStateException("Published canonical JSON cannot be read", error); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new IllegalStateException("SHA-256 is unavailable", error); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new ResponseStatusException(HttpStatus.NOT_FOUND, message); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new ResponseStatusException(HttpStatus.CONFLICT, message); }` | [DocumentTemplateRevisionService](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionService.java) |
| `new RuntimeException("Error creating document template type: " + e.getMessage(), e)));` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Document template type not found with ID: " + typeId)));` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Document template type not found with code: " + typeCode)));` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Document template type not found with ID: " + typeId)))` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error updating document template type: " + e.getMessage(), e)));` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Document template type not found with ID: " + typeId)))` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting document template type: " + e.getMessage(), e)));` | [DocumentTemplateTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error creating identity document: " + e.getMessage(), e)));` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Identity document not found with ID: " + documentId)));` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Identity document not found with code: " + documentCode)));` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Identity document not found with ID: " + documentId)))` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Error updating identity document: " + e.getMessage(), e)));` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Identity document not found with ID: " + documentId)))` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting identity document: " + e.getMessage(), e)));` | [IdentityDocumentCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImpl.java) |
| `new RuntimeException("Error creating identity document category: " + e.getMessage(), e)));` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Identity document category not found with ID: " + categoryId)));` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Identity document category not found with code: " + categoryCode)));` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Identity document category not found with ID: " + categoryId)))` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Error updating identity document category: " + e.getMessage(), e)));` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Identity document category not found with ID: " + categoryId)))` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting identity document category: " + e.getMessage(), e)));` | [IdentityDocumentCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImpl.java) |
| `new RuntimeException("Error creating identity document localization: " + e.getMessage(), e)));` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Identity document localization not found with document ID: " + documentId + " and locale ID: " + localeId)));` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Identity document localization not found with ID: " + localizationId)))` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Error updating identity document localization: " + e.getMessage(), e)));` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Identity document localization not found with ID: " + localizationId)))` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting identity document localization: " + e.getMessage(), e)));` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting localizations for document ID: " + documentId, e)));` | [IdentityDocumentLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImpl.java) |
| `new RuntimeException("Error creating message type: " + e.getMessage(), e)));` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Message type not found with ID: " + typeId)));` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Message type not found with code: " + typeCode)));` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Message type not found with ID: " + typeId)))` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error updating message type: " + e.getMessage(), e)));` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Message type not found with ID: " + typeId)))` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting message type: " + e.getMessage(), e)));` | [MessageTypeCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImpl.java) |
| `new RuntimeException("Error creating notification message: " + e.getMessage(), e)));` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Notification message not found with ID: " + messageId)));` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Notification message not found with code: " + messageCode)));` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Notification message not found with ID: " + messageId)))` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Error updating notification message: " + e.getMessage(), e)));` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Notification message not found with ID: " + messageId)))` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("Error deleting notification message: " + e.getMessage(), e)));` | [NotificationMessageCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImpl.java) |
| `new RuntimeException("No localizations found for message ID: " + messageId)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("No localizations found for locale ID: " + localeId)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Error creating notification message localization: " + e.getMessage(), e)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Notification message localization not found with ID: " + localizationId)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Notification message localization not found with message ID: " + messageId + " and locale ID: " + localeId)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Notification message localization not found with ID: " + localizationId)))` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Error updating notification message localization: " + e.getMessage(), e)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Notification message localization not found with ID: " + localizationId)))` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting notification message localization: " + e.getMessage(), e)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("Error deleting localizations for message ID: " + messageId, e)));` | [NotificationMessageLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageLocalizationServiceImpl.java) |
| `new RuntimeException("No templates found for message ID: " + messageId)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Error creating notification message template: " + e.getMessage(), e)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Notification message template not found with ID: " + templateId)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Notification message template not found with message ID: " + messageId + " and template name: " + templateName)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Notification message template not found with ID: " + templateId)))` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Error updating notification message template: " + e.getMessage(), e)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Notification message template not found with ID: " + templateId)))` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Error deleting notification message template: " + e.getMessage(), e)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Error deleting templates for message ID: " + messageId, e)));` | [NotificationMessageTemplateServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageTemplateServiceImpl.java) |
| `new RuntimeException("Transaction category not found with ID: " + categoryId)))` | [TransactionCategoryCatalogServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryCatalogServiceImpl.java) |
| `new RuntimeException("No localizations found for category ID: " + categoryId)));` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new RuntimeException("Error creating transaction category localization: " + e.getMessage(), e)));` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new RuntimeException("Transaction category localization not found with ID: " + localizationId)));` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new RuntimeException("Transaction category localization not found with category ID: " + categoryId + " and locale ID: " + localeId)));` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new RuntimeException("Transaction category localization not found with ID: " + localizationId)));` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new RuntimeException("Transaction category localization not found with ID: " + localizationId)))` | [TransactionCategoryLocalizationServiceImpl](../../core-common-reference-master-data-core/src/main/java/io/fireflysoftware/masters/core/services/transaction/v1/TransactionCategoryLocalizationServiceImpl.java) |
| `new IllegalStateException(` | [CurrencyPolicyRepository](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/repositories/currency/policy/v1/CurrencyPolicyRepository.java) |
| `new OptimisticLockingFailureException(` | [DocumentLayoutRepository](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/repositories/document/layout/v1/DocumentLayoutRepository.java) |
| `new OptimisticLockingFailureException(` | [DocumentLayoutRepository](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/repositories/document/layout/v1/DocumentLayoutRepository.java) |
| `new IllegalStateException(` | [DocumentLayoutRepository](../../core-common-reference-master-data-models/src/main/java/io/fireflysoftware/masters/models/repositories/document/layout/v1/DocumentLayoutRepository.java) |
| `HttpStatus.CREATED` | [ContractDocumentTypeController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/contractdocumenttype/v1/ContractDocumentTypeController.java) |
| `HttpStatus.OK` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) |
| `HttpStatus.CREATED` | [CurrencyExecutionPolicyController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyController.java) |
| `new IllegalArgumentException();` | [CurrencyPolicyRequestDecoder](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyPolicyRequestDecoder.java) |
| `new IllegalArgumentException();` | [CurrencyPolicyRequestDecoder](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyPolicyRequestDecoder.java) |
| `HttpStatus.OK` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) |
| `HttpStatus.CREATED` | [DocumentLayoutPublicationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationController.java) |
| `new IllegalArgumentException();` | [DocumentLayoutRequestDecoder](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutRequestDecoder.java) |
| `new IllegalArgumentException();` | [DocumentLayoutRequestDecoder](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutRequestDecoder.java) |
| `HttpStatus.CREATED` | [DocumentTemplateCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateCatalogController.java) |
| `HttpStatus.CREATED` | [DocumentTemplateLocalizationController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateLocalizationController.java) |
| `HttpStatus.CREATED` | [DocumentTemplateTypeCatalogController](../../core-common-reference-master-data-web/src/main/java/io/fireflysoftware/masters/web/controllers/document/v1/DocumentTemplateTypeCatalogController.java) |

## Build and dependencies

### core-common-reference-master-data-core

[core-common-reference-master-data-core/pom.xml](../../core-common-reference-master-data-core/pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data-core:26.10.6`. Parent: `io.fireflysoftware:core-common-reference-master-data:26.10.6`.

- `io.fireflysoftware:core-common-reference-master-data-interfaces:`
- `io.fireflysoftware:core-common-reference-master-data-models:`
- `io.projectreactor:reactor-test:`
- `org.fireflyframework:fireflyframework-security-spi:`
- `org.fireflyframework:fireflyframework-starter-core:`
- `org.fireflyframework:fireflyframework-utils:`
- `org.fireflyframework:fireflyframework-validators:`
- `org.flywaydb:flyway-database-postgresql:`
- `org.mapstruct:mapstruct:`
- `org.projectlombok:lombok:`
- `org.springframework.boot:spring-boot-starter-test:`
- `org.springframework.boot:spring-boot-starter-webflux:`
- `org.testcontainers:testcontainers-junit-jupiter:`
- `org.testcontainers:testcontainers-postgresql:`

### core-common-reference-master-data-interfaces

[core-common-reference-master-data-interfaces/pom.xml](../../core-common-reference-master-data-interfaces/pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data-interfaces:26.10.6`. Parent: `io.fireflysoftware:core-common-reference-master-data:26.10.6`.

- `com.fasterxml.jackson.core:jackson-databind:`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:`
- `io.swagger.core.v3:swagger-annotations:`
- `org.fireflyframework:fireflyframework-utils:`
- `org.fireflyframework:fireflyframework-validators:`
- `org.projectlombok:lombok:`
- `org.springframework.boot:spring-boot-starter-validation:`
- `org.springframework.boot:spring-boot-starter-webflux:`

### core-common-reference-master-data-models

[core-common-reference-master-data-models/pom.xml](../../core-common-reference-master-data-models/pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data-models:26.10.6`. Parent: `io.fireflysoftware:core-common-reference-master-data:26.10.6`.

- `io.fireflysoftware:core-common-reference-master-data-interfaces:`
- `org.fireflyframework:fireflyframework-r2dbc:`
- `org.flywaydb:flyway-core:`
- `org.postgresql:postgresql:`
- `org.postgresql:r2dbc-postgresql:`
- `org.projectlombok:lombok:`
- `org.springframework.boot:spring-boot-starter-data-r2dbc:`

### core-common-reference-master-data-sdk

[core-common-reference-master-data-sdk/pom.xml](../../core-common-reference-master-data-sdk/pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data-sdk:26.10.6`. Parent: `io.fireflysoftware:core-common-reference-master-data:26.10.6`.

- `com.fasterxml.jackson.core:jackson-databind:`
- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:`
- `com.squareup.okhttp3:logging-interceptor:`
- `com.squareup.okhttp3:okhttp:`
- `io.fireflysoftware:core-common-reference-master-data-web:`
- `io.projectreactor:reactor-test:`
- `io.swagger.core.v3:swagger-annotations:`
- `jakarta.annotation:jakarta.annotation-api:`
- `javax.annotation:javax.annotation-api:1.3.2`
- `org.fireflyframework:fireflyframework-security-resource-server:`
- `org.flywaydb:flyway-database-postgresql:`
- `org.openapitools:jackson-databind-nullable:0.2.8`
- `org.projectlombok:lombok:`
- `org.springframework.boot:spring-boot-starter-test:`
- `org.springframework:spring-webflux:`
- `org.testcontainers:testcontainers-junit-jupiter:`
- `org.testcontainers:testcontainers-postgresql:`

### core-common-reference-master-data-web

[core-common-reference-master-data-web/pom.xml](../../core-common-reference-master-data-web/pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data-web:26.10.6`. Parent: `io.fireflysoftware:core-common-reference-master-data:26.10.6`.

- `io.fireflysoftware:core-common-reference-master-data-core:`
- `io.fireflysoftware:core-common-reference-master-data-interfaces:`
- `io.micrometer:micrometer-registry-prometheus:`
- `io.projectreactor:reactor-test:`
- `org.fireflyframework:fireflyframework-web:`
- `org.projectlombok:lombok:`
- `org.springdoc:springdoc-openapi-starter-webflux-ui:`
- `org.springframework.boot:spring-boot-starter-actuator:`
- `org.springframework.boot:spring-boot-starter-test:`
- `org.springframework.boot:spring-boot-starter-validation:`
- `org.springframework.boot:spring-boot-starter-webflux:`

### core-common-reference-master-data

[pom.xml](../../pom.xml)

Coordinates: `io.fireflysoftware:core-common-reference-master-data:26.10.6`. Parent: `io.fireflysoftware:firefly-parent:26.10.6`.

- `io.fireflysoftware:core-common-reference-master-data-core:${project.version}`
- `io.fireflysoftware:core-common-reference-master-data-interfaces:${project.version}`
- `io.fireflysoftware:core-common-reference-master-data-models:${project.version}`
- `io.fireflysoftware:core-common-reference-master-data-sdk:${project.version}`
- `io.fireflysoftware:core-common-reference-master-data-web:${project.version}`

## Test sources

- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/README.md](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/README.md)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/repositories/identitydocument/v1/IdentityDocumentLocalizationRepositoryTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/repositories/identitydocument/v1/IdentityDocumentLocalizationRepositoryTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/activity/v1/ActivityCodeServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/activity/v1/ActivityCodeServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/bank/v1/BankInstitutionCodeServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/bank/v1/BankInstitutionCodeServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/consent/v1/ConsentCatalogServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/consent/v1/ConsentCatalogServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/country/v1/CountryServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/country/v1/CountryServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyAuthorityTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyAuthorityTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyDatabaseTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyLegacyConflictTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyLegacyConflictTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyRulesTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/policy/v1/CurrencyPolicyRulesTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/v1/CurrencyServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/currency/v1/CurrencyServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/division/v1/AdministrativeDivisionServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/division/v1/AdministrativeDivisionServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutDatabaseTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutDatabaseTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRulesTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/layout/v1/DocumentLayoutRulesTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutProfileContextTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutProfileContextTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutPublicationServiceTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentLayoutPublicationServiceTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCanonicalJsonTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateCanonicalJsonTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateDraftValidationTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateDraftValidationTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionDatabaseTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/DocumentTemplateRevisionDatabaseTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/SandboxDocumentCatalogTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/document/v1/SandboxDocumentCatalogTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCatalogServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentCategoryCatalogServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/identitydocument/v1/IdentityDocumentLocalizationServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/legal/v1/LegalFormServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/legal/v1/LegalFormServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/locale/v1/LanguageLocaleServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/locale/v1/LanguageLocaleServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupDomainServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupDomainServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupItemServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/lookup/v1/LookupItemServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/notification/v1/MessageTypeCatalogServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/notification/v1/NotificationMessageCatalogServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/relationships/v1/RelationshipTypeMasterServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/relationships/v1/RelationshipTypeMasterServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/title/v1/TitleMasterServiceImplTest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/services/title/v1/TitleMasterServiceImplTest.java)
- [core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/utils/TestPaginationRequest.java](../../core-common-reference-master-data-core/src/test/java/io/fireflysoftware/masters/core/utils/TestPaginationRequest.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/CurrencyPolicySdkRuntimeIT.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/CurrencyPolicySdkRuntimeIT.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentLayoutPublicationRuntimeIT.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentLayoutPublicationRuntimeIT.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentTemplateRevisionRuntimeIT.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentTemplateRevisionRuntimeIT.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentTemplateRuntimeBootstrap.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/masters/sdk/DocumentTemplateRuntimeBootstrap.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/currency/CurrencyPolicyFrameworkFallbackConfiguration.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/currency/CurrencyPolicyFrameworkFallbackConfiguration.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/currency/CurrencyPolicyRuntimeBootstrap.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/currency/CurrencyPolicyRuntimeBootstrap.java)
- [core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/layout/DocumentLayoutRuntimeBootstrap.java](../../core-common-reference-master-data-sdk/src/test/java/io/fireflysoftware/qualification/layout/DocumentLayoutRuntimeBootstrap.java)
- [core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyControllerTest.java](../../core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyExecutionPolicyControllerTest.java)
- [core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyPolicyRequestDecoderTest.java](../../core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/currency/policy/v1/CurrencyPolicyRequestDecoderTest.java)
- [core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationControllerTest.java](../../core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutPublicationControllerTest.java)
- [core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutRequestDecoderTest.java](../../core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/controllers/document/layout/v1/DocumentLayoutRequestDecoderTest.java)
- [core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/openapi/OpenApiGenApplication.java](../../core-common-reference-master-data-web/src/test/java/io/fireflysoftware/masters/web/openapi/OpenApiGenApplication.java)

## CI definitions

- [.github/workflows/maven-central.yml](../../.github/workflows/maven-central.yml)
- [.github/workflows/release.yml](../../.github/workflows/release.yml)
