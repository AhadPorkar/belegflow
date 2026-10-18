# Source and module reference / Quellcodeverzeichnis

The table links every application source file. Java public signatures follow below. Source, tests and guides together document behavior; generated signatures are not proof of correctness.

| Source | Responsibility |
|---|---|
| [domain/src/main/java/com/belegflow/domain/Actor.java](../../domain/src/main/java/com/belegflow/domain/Actor.java) | Server-resolved identity and role gates. |
| [domain/src/main/java/com/belegflow/domain/BusinessException.java](../../domain/src/main/java/com/belegflow/domain/BusinessException.java) | Supporting module type or application configuration. |
| [domain/src/main/java/com/belegflow/domain/DelegationPolicy.java](../../domain/src/main/java/com/belegflow/domain/DelegationPolicy.java) | Delegation validation and half-open active interval. |
| [domain/src/main/java/com/belegflow/domain/DuplicateFingerprint.java](../../domain/src/main/java/com/belegflow/domain/DuplicateFingerprint.java) | Stable normalized business key and byte hash. |
| [domain/src/main/java/com/belegflow/domain/FilePolicy.java](../../domain/src/main/java/com/belegflow/domain/FilePolicy.java) | Filename, file-size and bounded signature acceptance. |
| [domain/src/main/java/com/belegflow/domain/InvoiceStage.java](../../domain/src/main/java/com/belegflow/domain/InvoiceStage.java) | Supporting module type or application configuration. |
| [domain/src/main/java/com/belegflow/domain/InvoiceXmlParser.java](../../domain/src/main/java/com/belegflow/domain/InvoiceXmlParser.java) | Bounded secure UBL/CII extraction, not standards certification. |
| [domain/src/main/java/com/belegflow/domain/MoneyRules.java](../../domain/src/main/java/com/belegflow/domain/MoneyRules.java) | Exact EUR amount validation and threshold comparisons. |
| [domain/src/main/java/com/belegflow/domain/ReminderPolicy.java](../../domain/src/main/java/com/belegflow/domain/ReminderPolicy.java) | Europe/Berlin calendar-date reminder eligibility. |
| [domain/src/main/java/com/belegflow/domain/Role.java](../../domain/src/main/java/com/belegflow/domain/Role.java) | Supporting module type or application configuration. |
| [domain/src/main/java/com/belegflow/domain/SafeCsv.java](../../domain/src/main/java/com/belegflow/domain/SafeCsv.java) | Quoted UTF-8 BOM CSV with formula neutralization. |
| [domain/src/main/java/com/belegflow/domain/WorkflowAction.java](../../domain/src/main/java/com/belegflow/domain/WorkflowAction.java) | Supporting module type or application configuration. |
| [domain/src/main/java/com/belegflow/domain/WorkflowFacts.java](../../domain/src/main/java/com/belegflow/domain/WorkflowFacts.java) | Immutable facts passed to the pure decision policy. |
| [domain/src/main/java/com/belegflow/domain/WorkflowPolicy.java](../../domain/src/main/java/com/belegflow/domain/WorkflowPolicy.java) | State transitions, readiness and independent signer invariants. |
| [domain/src/main/java/com/belegflow/domain/package-info.java](../../domain/src/main/java/com/belegflow/domain/package-info.java) | Supporting module type or application configuration. |
| [frontend/src/App.jsx](../../frontend/src/App.jsx) | Feature-page composition and the UI error boundary. |
| [frontend/src/api.ts](../../frontend/src/api.ts) | Same-origin fetch, CSRF, errors and downloads; no simulated fallback. |
| [frontend/src/format.ts](../../frontend/src/format.ts) | Exact cents, locale formatting and CSV helper. |
| [frontend/src/i18n.ts](../../frontend/src/i18n.ts) | English/German dictionaries and interpolation. |
| [frontend/src/main.jsx](../../frontend/src/main.jsx) | Production React root and typed store subscription. |
| [frontend/src/models.ts](../../frontend/src/models.ts) | Typed API shapes and application view state. |
| [frontend/src/store.ts](../../frontend/src/store.ts) | State transitions, captured dialogs and stale-request protection. |
| [frontend/src/styles.css](../../frontend/src/styles.css) | Supporting module type or application configuration. |
| [server/src/main/java/com/belegflow/server/BelegFlowApplication.java](../../server/src/main/java/com/belegflow/server/BelegFlowApplication.java) | Supporting module type or application configuration. |
| [server/src/main/java/com/belegflow/server/audit/AuditController.java](../../server/src/main/java/com/belegflow/server/audit/AuditController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/audit/AuditService.java](../../server/src/main/java/com/belegflow/server/audit/AuditService.java) | Append an actor-scoped event within the caller transaction. |
| [server/src/main/java/com/belegflow/server/bootstrap/BootstrapData.java](../../server/src/main/java/com/belegflow/server/bootstrap/BootstrapData.java) | Explicit non-demo first organization or opt-in synthetic fixtures. |
| [server/src/main/java/com/belegflow/server/catalog/CatalogController.java](../../server/src/main/java/com/belegflow/server/catalog/CatalogController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/catalog/CatalogService.java](../../server/src/main/java/com/belegflow/server/catalog/CatalogService.java) | Scoped suppliers/projects/cost centers, users, threshold and delegation. |
| [server/src/main/java/com/belegflow/server/documents/DocumentController.java](../../server/src/main/java/com/belegflow/server/documents/DocumentController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/documents/DocumentService.java](../../server/src/main/java/com/belegflow/server/documents/DocumentService.java) | Byte-preserved scoped documents and evidence uploads. |
| [server/src/main/java/com/belegflow/server/documents/InvoiceImportService.java](../../server/src/main/java/com/belegflow/server/documents/InvoiceImportService.java) | Serialized original ingestion, extraction and duplicate handling. |
| [server/src/main/java/com/belegflow/server/exports/ExportController.java](../../server/src/main/java/com/belegflow/server/exports/ExportController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/exports/ExportService.java](../../server/src/main/java/com/belegflow/server/exports/ExportService.java) | Approved-only ZIP generation, manifest, persistence and idempotency. |
| [server/src/main/java/com/belegflow/server/identity/IdentityService.java](../../server/src/main/java/com/belegflow/server/identity/IdentityService.java) | Active principal/auth-version and organization validation. |
| [server/src/main/java/com/belegflow/server/identity/LoginRateLimitFilter.java](../../server/src/main/java/com/belegflow/server/identity/LoginRateLimitFilter.java) | Bounded in-memory direct-address login throttling. |
| [server/src/main/java/com/belegflow/server/identity/SecurityConfiguration.java](../../server/src/main/java/com/belegflow/server/identity/SecurityConfiguration.java) | Session login, PBKDF2, CSRF and HTTP authorization/headers. |
| [server/src/main/java/com/belegflow/server/identity/SecurityPrincipal.java](../../server/src/main/java/com/belegflow/server/identity/SecurityPrincipal.java) | Supporting module type or application configuration. |
| [server/src/main/java/com/belegflow/server/identity/SessionController.java](../../server/src/main/java/com/belegflow/server/identity/SessionController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/invoices/InvoiceController.java](../../server/src/main/java/com/belegflow/server/invoices/InvoiceController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/invoices/InvoiceRepository.java](../../server/src/main/java/com/belegflow/server/invoices/InvoiceRepository.java) | Tenant-scoped SQL, row locks, version checks and workflow facts. |
| [server/src/main/java/com/belegflow/server/invoices/InvoiceService.java](../../server/src/main/java/com/belegflow/server/invoices/InvoiceService.java) | Atomic invoice commands, allocation, evidence acceptance and questions. |
| [server/src/main/java/com/belegflow/server/notifications/NotificationController.java](../../server/src/main/java/com/belegflow/server/notifications/NotificationController.java) | HTTP adapter to the corresponding service. |
| [server/src/main/java/com/belegflow/server/notifications/ReminderService.java](../../server/src/main/java/com/belegflow/server/notifications/ReminderService.java) | Scheduled in-app notices with daily database deduplication. |
| [server/src/main/java/com/belegflow/server/shared/ApiErrors.java](../../server/src/main/java/com/belegflow/server/shared/ApiErrors.java) | Business/error-to-HTTP mapping without confidential exception leakage. |
| [server/src/main/java/com/belegflow/server/shared/Sql.java](../../server/src/main/java/com/belegflow/server/shared/Sql.java) | Bounded inputs, result normalization and typed access. |

## Public Java surface

Constructors wire dependencies; request records define JSON/multipart-adjacent DTO contracts. Methods use the transactions and authorization described in the developer guide.

### Actor

```java
public record Actor(UUID tenantId, UUID userId, String name, Role role)
public void finance()
public void admin()
```

### BusinessException

```java
public BusinessException(String code)
public String code()
public static void require(boolean condition, String code)
```

### DelegationPolicy

```java
public static void validate(UUID owner, UUID delegate, Instant from, Instant until)
```

### DuplicateFingerprint

```java
public static String sha256(byte[] content)
public static String normalize(String value)
public static String businessKey(UUID supplierId, String invoiceNumber)
```

### FilePolicy

```java
public static String validate(String filename, byte[] bytes, boolean original)
```

### InvoiceStage

```java
public boolean editable()
```

### InvoiceXmlParser

```java
public Parsed parse(byte[] bytes)
public void warning(SAXParseException e)
public void error(SAXParseException e)
public void fatalError(SAXParseException e)
public String getNamespaceURI(String prefix)
public String getPrefix(String uri)
public Iterator<String> getPrefixes(String uri)
```

### MoneyRules

```java
public static BigDecimal amount(String value)
public static boolean validTotals(BigDecimal net, BigDecimal tax, BigDecimal gross, String currency)
public static boolean needsSecondApproval(BigDecimal gross, BigDecimal threshold)
```

### ReminderPolicy

```java
public static LocalDate businessDate(Instant instant)
public static boolean due(InvoiceStage stage, LocalDate dueOn, Instant now)
```

### Role

```java
public boolean managesFinance()
public boolean mayApprove()
```

### SafeCsv

```java
public static String cell(Object value)
public static byte[] encode(List<List<Object>> rows)
```

### WorkflowPolicy

```java
public record Decision(InvoiceStage stage, String event, int approvalSlot)
```

### BelegFlowApplication

```java
public static void main(String[] args)
```

### AuditController

```java
public AuditController(IdentityService identity,JdbcTemplate db)
public Map<String,Object> events(@RequestParam(defaultValue="0") long before)
```

### AuditService

```java
public AuditService(JdbcTemplate db)
public void append(Actor actor,UUID invoice,String event,String detail)
```

### BootstrapData

```java
public void run(ApplicationArguments args)
public static byte[] ubl(String number,String supplier,LocalDate issue,LocalDate due,String net,String tax,String gross,String note)
```

### CatalogController

```java
public CatalogController(CatalogService service)
public Map<String,Object> all()
public Map<String,UUID> supplier(@RequestBody CatalogService.SupplierBody b)
public Map<String,UUID> supplier(@PathVariable UUID id,@RequestBody CatalogService.SupplierBody b)
public Map<String,UUID> project(@RequestBody CatalogService.ProjectBody b)
public Map<String,UUID> costCenter(@RequestBody CatalogService.CostCenterBody b)
public void rules(@RequestBody CatalogService.RuleBody b)
public Map<String,UUID> delegation(@RequestBody CatalogService.DelegationBody b)
public void revoke(@PathVariable UUID id)
public Map<String,UUID> user(@RequestBody CatalogService.UserBody b)
public Map<String,UUID> user(@PathVariable UUID id,@RequestBody CatalogService.UserBody b)
```

### CatalogService

```java
public CatalogService(JdbcTemplate db,IdentityService identity,AuditService audit,PasswordEncoder passwords)
public Map<String,Object> all()
public record SupplierBody(String name,String email,String vat_id,boolean active)
public UUID supplier(UUID id,SupplierBody b)
public record ProjectBody(String code,String name,UUID manager_id,boolean evidence_required)
public UUID project(ProjectBody b)
public record CostCenterBody(String code,String name)
public UUID costCenter(CostCenterBody b)
public record RuleBody(String threshold)
public void rules(RuleBody b)
public record DelegationBody(UUID owner_id,UUID delegate_id,Instant valid_from,Instant valid_until)
public UUID delegate(DelegationBody b)
public void revoke(UUID id)
public record UserBody(String email,String display_name,Role role,String password,boolean active)
public UUID user(UUID id,UserBody b)
```

### DocumentController

```java
public DocumentController(DocumentService documents,InvoiceImportService importer)
public InvoiceImportService.Result ingest(@RequestPart("file") MultipartFile file,@RequestParam Map<String,String> metadata)
public Map<String,UUID> evidence(@PathVariable UUID id,@RequestParam int version,@RequestPart("file") MultipartFile file)
public ResponseEntity<byte[]> download(@PathVariable UUID id)
public static ResponseEntity<byte[]> attachment(String name,String type,String hash,byte[] bytes)
```

### DocumentService

```java
public DocumentService(JdbcTemplate db,IdentityService identity,InvoiceRepository repo,AuditService audit,Clock clock)
public record Download(String filename,String contentType,String sha256,byte[] bytes)
public Download download(UUID id)
public UUID insert(Actor a,UUID invoice,String kind,String filename,String type,byte[] bytes)
public UUID evidence(UUID invoice,int version,String filename,byte[] bytes)
```

### InvoiceImportService

```java
public InvoiceImportService(JdbcTemplate db,IdentityService identity,DocumentService documents,AuditService audit)
public record Result(UUID id,boolean reused,boolean duplicate_warning)
public Result ingest(String filename,byte[] bytes,Map<String,String> metadata)
public Result ingestFor(Actor a,String filename,byte[] bytes,Map<String,String> m)
```

### ExportController

```java
public ExportController(ExportService service)
public List<Map<String,Object>> list()
public Map<String,UUID> create(@RequestBody ExportService.Request request)
public ResponseEntity<byte[]> download(@PathVariable UUID id)
```

### ExportService

```java
public ExportService(JdbcTemplate db,IdentityService identity,InvoiceRepository repo,AuditService audit,Clock clock)
public record Entry(UUID id,int version)
public record Request(UUID idempotency_key,List<Entry> invoices)
public List<Map<String,Object>> list()
public UUID create(Request b)
public DocumentService.Download download(UUID id)
```

### IdentityService

```java
public IdentityService(JdbcTemplate db)
public Actor actor()
public void approverExists(UUID tenant,UUID id)
```

### LoginRateLimitFilter

```java
public LoginRateLimitFilter(Clock clock)
```

### SecurityPrincipal

```java
public SecurityPrincipal(String email,String hash,String role,boolean active,int authVersion)
public int authVersion()
```

### SessionController

```java
public Map<String,String> csrf(CsrfToken token)
public Map<String,Object> config()
public Map<String,Object> session()
public record LocaleBody(String locale)
public void locale(@RequestBody LocaleBody body)
```

### InvoiceController

```java
public InvoiceController(InvoiceService service)
public Map<String,Object> list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String stage,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size)
public Map<String,Object> summary()
public Map<String,Object> get(@PathVariable UUID id)
public void command(@PathVariable UUID id,@RequestBody InvoiceService.Command b)
public void allocation(@PathVariable UUID id,@RequestBody InvoiceService.Allocation b)
public void duplicate(@PathVariable UUID id,@RequestBody InvoiceService.Note b)
public void evidence(@PathVariable UUID id,@RequestBody InvoiceService.Note b)
public Map<String,UUID> comment(@PathVariable UUID id,@RequestBody InvoiceService.Comment b)
public void resolve(@PathVariable UUID id,@PathVariable UUID question,@RequestBody InvoiceService.Comment b)
```

### InvoiceRepository

```java
public InvoiceRepository(JdbcTemplate db)
public Map<String,Object> get(UUID tenant,UUID id,boolean lock)
public boolean delegate(Actor a,UUID owner,Instant now)
public int questions(UUID tenant,UUID id)
public int evidenceCount(UUID tenant,UUID id)
public WorkflowFacts facts(Map<String,Object> row)
public void version(Map<String,Object> row,int expected)
```

### InvoiceService

```java
public InvoiceService(JdbcTemplate db,IdentityService identity,AuditService audit,InvoiceRepository repo,Clock clock)
public Map<String,Object> list(String search,String stage,int page,int size)
public Map<String,Object> summary()
public Map<String,Object> detail(UUID id)
public record Command(int version,WorkflowAction action,String note)
public void command(UUID id,Command command)
public record Allocation(int version,UUID project_id,UUID cost_center_id,UUID approver_id,LocalDate due_date)
public void allocate(UUID id,Allocation body)
public record Note(int version,String note)
public void clearDuplicate(UUID id,Note body)
public void acceptEvidence(UUID id,Note body)
public record Comment(String body)
public UUID comment(UUID id,Comment b)
public void resolveQuestion(UUID id,UUID question,Comment body)
```

### NotificationController

```java
public NotificationController(IdentityService identity,JdbcTemplate db,ReminderService reminders)
public List<Map<String,Object>> list()
public void read(@PathVariable UUID id)
public Map<String,Integer> run()
```

### ReminderService

```java
public ReminderService(JdbcTemplate db,Clock clock)
public void tick()
public int run(UUID tenant)
```

### Sql

```java
public static UUID uuid(Map<String,Object> row,String key)
public static String str(Map<String,Object> row,String key)
public static boolean bool(Map<String,Object> row,String key)
public static int integer(Map<String,Object> row,String key)
public static Instant instant(Map<String,Object> row,String key)
public static Map<String,Object> api(Map<String,Object> row)
public static List<Map<String,Object>> api(List<Map<String,Object>> rows)
public static Map<String,Object> one(List<Map<String,Object>> rows)
public static String text(String value,int max,boolean required)
```

## Feature view files

| Source | Exported view components |
|---|---|
| [`frontend/src/features/accounting/Exports.jsx`](../../frontend/src/features/accounting/Exports.jsx) | `Exports` |
| [`frontend/src/features/audit/AuditPage.jsx`](../../frontend/src/features/audit/AuditPage.jsx) | `AuditPage` |
| [`frontend/src/features/catalog/CatalogPages.jsx`](../../frontend/src/features/catalog/CatalogPages.jsx) | `Suppliers`, `Projects` |
| [`frontend/src/features/identity/Login.jsx`](../../frontend/src/features/identity/Login.jsx) | `Login` |
| [`frontend/src/features/invoices/Workspace.jsx`](../../frontend/src/features/invoices/Workspace.jsx) | `Workspace` |
| [`frontend/src/features/settings/Settings.jsx`](../../frontend/src/features/settings/Settings.jsx) | `Settings` |

## Feature view and test sources (0.2.0)

All paths link to the actual repository files. See the EN/DE module contracts for semantics.

| Source | Kind |
|---|---|
| [frontend/src/features/accounting/Exports.jsx](../../frontend/src/features/accounting/Exports.jsx) | Feature view |
| [frontend/src/features/audit/AuditPage.jsx](../../frontend/src/features/audit/AuditPage.jsx) | Feature view |
| [frontend/src/features/catalog/CatalogPages.jsx](../../frontend/src/features/catalog/CatalogPages.jsx) | Feature view |
| [frontend/src/features/identity/Login.jsx](../../frontend/src/features/identity/Login.jsx) | Feature view |
| [frontend/src/features/invoices/Workspace.jsx](../../frontend/src/features/invoices/Workspace.jsx) | Feature view |
| [frontend/src/features/settings/Settings.jsx](../../frontend/src/features/settings/Settings.jsx) | Feature view |
| [frontend/src/components/Dialogs.jsx](../../frontend/src/components/Dialogs.jsx) | Shared view |
| [frontend/src/components/Shell.jsx](../../frontend/src/components/Shell.jsx) | Shared view |
| [frontend/src/components/ui.jsx](../../frontend/src/components/ui.jsx) | Shared view |
| [server/src/test/java/com/belegflow/server/AdapterTest.java](../../server/src/test/java/com/belegflow/server/AdapterTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/ConcurrencyIntegrationTest.java](../../server/src/test/java/com/belegflow/server/ConcurrencyIntegrationTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/IntegrationTest.java](../../server/src/test/java/com/belegflow/server/IntegrationTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/ModuleIntegrationTest.java](../../server/src/test/java/com/belegflow/server/ModuleIntegrationTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/audit/AuditModuleTest.java](../../server/src/test/java/com/belegflow/server/audit/AuditModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/bootstrap/BootstrapModuleTest.java](../../server/src/test/java/com/belegflow/server/bootstrap/BootstrapModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/catalog/CatalogModuleTest.java](../../server/src/test/java/com/belegflow/server/catalog/CatalogModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/config/JacksonConfigurationTest.java](../../server/src/test/java/com/belegflow/server/config/JacksonConfigurationTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/documents/DocumentModuleTest.java](../../server/src/test/java/com/belegflow/server/documents/DocumentModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/documents/ImportModuleTest.java](../../server/src/test/java/com/belegflow/server/documents/ImportModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/exports/ExportModuleTest.java](../../server/src/test/java/com/belegflow/server/exports/ExportModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/identity/IdentityModuleTest.java](../../server/src/test/java/com/belegflow/server/identity/IdentityModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/identity/LoginRateLimitFilterTest.java](../../server/src/test/java/com/belegflow/server/identity/LoginRateLimitFilterTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/invoices/InvoiceModuleTest.java](../../server/src/test/java/com/belegflow/server/invoices/InvoiceModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/invoices/InvoiceRepositoryTest.java](../../server/src/test/java/com/belegflow/server/invoices/InvoiceRepositoryTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/notifications/NotificationModuleTest.java](../../server/src/test/java/com/belegflow/server/notifications/NotificationModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/shared/SharedModuleTest.java](../../server/src/test/java/com/belegflow/server/shared/SharedModuleTest.java) | Backend test/fixture |
| [server/src/test/java/com/belegflow/server/support/ServiceFixture.java](../../server/src/test/java/com/belegflow/server/support/ServiceFixture.java) | Backend test/fixture |
| [frontend/tests/components/accounting-audit.test.jsx](../../frontend/tests/components/accounting-audit.test.jsx) | Frontend test/fixture |
| [frontend/tests/components/catalog-settings.test.jsx](../../frontend/tests/components/catalog-settings.test.jsx) | Frontend test/fixture |
| [frontend/tests/components/dialogs.test.jsx](../../frontend/tests/components/dialogs.test.jsx) | Frontend test/fixture |
| [frontend/tests/components/helpers.jsx](../../frontend/tests/components/helpers.jsx) | Frontend test/fixture |
| [frontend/tests/components/identity.test.jsx](../../frontend/tests/components/identity.test.jsx) | Frontend test/fixture |
| [frontend/tests/components/setup.js](../../frontend/tests/components/setup.js) | Frontend test/fixture |
| [frontend/tests/components/shell-app.test.jsx](../../frontend/tests/components/shell-app.test.jsx) | Frontend test/fixture |
| [frontend/tests/components/workspace.test.jsx](../../frontend/tests/components/workspace.test.jsx) | Frontend test/fixture |
| [frontend/tests/e2e/modules.spec.js](../../frontend/tests/e2e/modules.spec.js) | Frontend test/fixture |
| [frontend/tests/e2e/workflows.spec.js](../../frontend/tests/e2e/workflows.spec.js) | Frontend test/fixture |
| [frontend/tests/support/fixtures.mjs](../../frontend/tests/support/fixtures.mjs) | Frontend test/fixture |
| [frontend/tests/support/test-api.mjs](../../frontend/tests/support/test-api.mjs) | Frontend test/fixture |
| [frontend/tests/unit/core.test.mjs](../../frontend/tests/unit/core.test.mjs) | Frontend test/fixture |
| [frontend/tests/unit/http.test.mjs](../../frontend/tests/unit/http.test.mjs) | Frontend test/fixture |
| [frontend/tests/unit/modules.test.mjs](../../frontend/tests/unit/modules.test.mjs) | Frontend test/fixture |
