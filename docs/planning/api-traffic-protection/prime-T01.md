# Prime: T01 explicit order on RequestIdFilter

Read `prime-shared.md` first.

## Current behavior

- `exception/RequestIdFilter.java` is a bare `@Component implements Filter` with no
  order, so it sorts as `LOWEST_PRECEDENCE`, the last of the app filters. It puts
  `UUID.randomUUID()` under MDC key `"requestId"` (a literal), and its `finally`
  block calls `MDC.clear()`.
- `BaseExceptionMessage` reads that key back through its constant
  `REQUEST_HEADER_ID = "requestId"` for the error body. The two spellings must
  stay equal.
- The id is not sent back as a response header. Changing that is out of scope.

## Extension points and traps

- Add `public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;` and
  annotate the class `@Order(ORDER)`. Why-comment: it must come after Boot's
  `CharacterEncodingFilter` (`HIGHEST_PRECEDENCE`), and every app filter must see
  `requestId`.
- Keep `MDC.clear()` as it is. Replacing it with `MDC.remove` would be a behavior
  change, and the AC says there is none.
- Optionally switch the literal to `BaseExceptionMessage.REQUEST_HEADER_ID`. That
  removes the duplication without changing behavior.
- Leave the class in `exception/`. Moving it is churn.

## Tests that already cover the area

None directly. All 5 `@WebMvcTest` controller tests run it implicitly, since the
slice includes `Filter` beans. Error-body tests such as
`MenuItemsControllerTest.createMenuItem_blankName_...` assert `$.code` but not
`$.requestId`.

## Suggested first edit (tests first)

Create `src/test/java/com/mcwendyqueen/exception/RequestIdFilterTest.java` with
`@WebMvcTest(controllers = MenuItemsController.class)`, `@MockBean MenuItemService`,
and an `@Import` of a nested `@TestConfiguration`. That configuration registers a
probe `FilterRegistrationBean` with `setOrder(RequestIdFilter.ORDER + 1)`, and the
probe records `MDC.get("requestId")`. Assert that the recorded value is non-null
after `GET /api/v1/menu`. It fails to compile until `ORDER` exists. Today it would
also fail at runtime, because the probe sorts first. Then add the constant and
`@Order`.

## Verify

`.\mvnw.cmd -q test -Dtest=RequestIdFilterTest`, then the full suite. For the
manual check, send `GET /api/v1/menu/999` to the running app and confirm the 404
body has a `requestId`.
