# AnimeVost Kotlin SDK

Unofficial Kotlin/JVM SDK for AnimeVost.org.

This library talks directly to AnimeVost endpoints and parses the site responses locally. It does not require a custom backend, proxy service, database, queue, cache, cloud function, or any other paid infrastructure.

> Status: alpha. The SDK is ready for product integration, but the public API may still change before a stable `1.0.0` release.

## Why This Exists

AnimeVost is a website first, not a public JSON API. Mobile clients usually end up mixing scraping, networking, parsing, authentication, and UI behavior in one place. That makes the app harder to test and easy to break when the website changes.

This SDK keeps that work in one focused package:

- stable Kotlin models
- testable parsers
- direct HTTP client behavior
- no private server dependency
- no duplicated scraping logic inside app screens

The core idea is simple: if AnimeVost is reachable, the client app can work.

## Features

### Catalog and Content

- main catalog pages
- catalog filters by site path
- search
- schedule
- navigation metadata
- random title
- anime details
- related series
- episode list
- video source extraction

### Account

- login and logout
- current session detection
- registration
- email activation link handling
- profile read
- profile update

### Favorites

- read favorites
- add favorite
- remove favorite

### Comments

- read embedded first page comments
- read ajax comment pages
- add comments
- delete own comments when allowed by the site
- report comments
- fetch reply quote template
- parse comment actions exposed by AnimeVost
- parse nested DLE quote blocks as structured data

AnimeVost comments are not a real nested DOM tree. The site renders comments as a flat list and stores visual reply state in classes like `commentContent_4` and `commentContent_9`, while quote chains live inside DLE `titlequote` and `quote` blocks. The SDK preserves that shape instead of inventing a fake tree.

### Rating

- submit a 1 to 5 rating vote
- parse updated rating and vote count
- report server rejections through typed SDK exceptions

## Requirements

- Kotlin JVM
- JDK 17
- Gradle 8.9 or the included Gradle Wrapper

Runtime dependencies:

- OkHttp
- Kotlin Coroutines
- Jsoup
- Gson

## Installation

Published artifacts are not configured yet. For now, use the SDK as a source dependency.

From an Android or JVM application that sits next to this repository:

```kotlin
// settings.gradle.kts
include(":animevost-sdk")
project(":animevost-sdk").projectDir = file("../animevost-sdk-kotlin/animevost-sdk")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":animevost-sdk"))
}
```

If the SDK lives in another path, adjust `projectDir` accordingly.

## Quick Start

All network APIs are `suspend` functions.

```kotlin
import com.animevost.sdk.AnimeVostClient

val client = AnimeVostClient()

val page = client.getAnimeList()
val firstTitle = page.items.first()

val details = client.getAnimeDetails(firstTitle.url)
val videos = details.episodes.firstOrNull()
    ?.let { episode -> client.getVideoSources(episode.videoId) }
    .orEmpty()
```

## Common Usage

### Search

```kotlin
val results = client.searchAnime("bleach")

for (item in results.items) {
    println("${item.id}: ${item.title}")
}
```

### Schedule

```kotlin
val schedule = client.getSchedule()

schedule.forEach { day ->
    println(day.weekday.displayName)
    day.entries.forEach { entry ->
        println("${entry.timeLabel.orEmpty()} ${entry.title}")
    }
}
```

### Details and Video Sources

```kotlin
val details = client.getAnimeDetails(
    "tip/tv/3970-mujikaku-seijo-wa-kyou-mo-muishiki-ni-chikara-wo-tare-nagasu.html",
)

val firstEpisode = details.episodes.first()
val sources = client.getVideoSources(firstEpisode.videoId)
```

### Login

```kotlin
val session = client.login(
    username = System.getenv("ANIMEVOST_USERNAME"),
    password = System.getenv("ANIMEVOST_PASSWORD"),
)

println("Logged in as ${session.username}")
```

### Favorites

```kotlin
client.login(username, password)

val favorites = client.getFavorites()
client.addFavorite(newsId = 3970)
client.removeFavorite(newsId = 3970)
```

### Registration

```kotlin
import com.animevost.sdk.model.RegistrationRequest
import com.animevost.sdk.model.RegistrationStatus

val result = client.register(
    RegistrationRequest(
        username = "new_user",
        password = "strong_password",
        email = "user@example.com",
    ),
)

when (result.status) {
    RegistrationStatus.ACTIVE -> println("Account is active")
    RegistrationStatus.PENDING_EMAIL_ACTIVATION -> println("Check email activation link")
}
```

If AnimeVost sends an activation email, pass the activation URL back to the SDK:

```kotlin
val activation = client.activateRegistration(activationUrl)
println(activation.activated)
```

### Profile

```kotlin
import com.animevost.sdk.model.UserProfileUpdate

client.login(username, password)

val profile = client.getProfile()

val updated = client.updateProfile(
    update = UserProfileUpdate(
        fullName = profile.fullName,
        location = "Earth",
        info = "Watching anime from a Kotlin client",
    ),
)
```

### Comments

```kotlin
val comments = client.getComments(
    "tip/tv/3970-mujikaku-seijo-wa-kyou-mo-muishiki-ni-chikara-wo-tare-nagasu.html",
)

comments.comments.forEach { comment ->
    println("${comment.author.name}: ${comment.bodyText}")
    println("depth=${comment.depth}, actions=${comment.actions}")
}
```

Read an ajax page directly by `newsId`:

```kotlin
val secondPage = client.getComments(newsId = 3970, page = 2)
```

Add a comment:

```kotlin
client.login(username, password)

val submitted = client.addComment(
    newsId = 3970,
    text = "Looks good so far.",
)
```

Reply with the same quote markup AnimeVost uses:

```kotlin
val template = client.getCommentReplyTemplate(commentId = 2048934)

client.addComment(
    newsId = 3970,
    text = "${template.markup}\nMy reply text",
)
```

Report a comment:

```kotlin
client.reportComment(
    commentId = 2048934,
    text = "Rule violation description",
)
```

Delete your own comment:

```kotlin
client.deleteComment(commentId = 2048948)
```

Deletion requires the `dle_login_hash` value exposed by AnimeVost. The SDK remembers it after authenticated page requests such as `getComments`, `getProfile`, or `getFavorites`. You can also pass it explicitly:

```kotlin
client.deleteComment(
    commentId = 2048948,
    allowHash = comments.allowHash,
)
```

### Rating

```kotlin
val vote = client.voteAnime(newsId = 3970, rating = 5)

println("Rating: ${vote.rating}, votes: ${vote.voteCount}")
```

## Error Handling

All SDK-specific failures inherit from `AnimeVostException`.

```kotlin
import com.animevost.sdk.error.AnimeVostAuthException
import com.animevost.sdk.error.AnimeVostCaptchaException
import com.animevost.sdk.error.AnimeVostException
import com.animevost.sdk.error.AnimeVostRateLimitException
import com.animevost.sdk.error.AnimeVostServerException
import com.animevost.sdk.error.AnimeVostValidationException

try {
    client.voteAnime(newsId = 3970, rating = 5)
} catch (error: AnimeVostAuthException) {
    // Login is required or the session is invalid.
} catch (error: AnimeVostCaptchaException) {
    // AnimeVost asked for captcha or security code.
} catch (error: AnimeVostRateLimitException) {
    // The site rejected the request because it was too frequent.
} catch (error: AnimeVostValidationException) {
    // The request is invalid before it reaches the network layer.
} catch (error: AnimeVostServerException) {
    // AnimeVost returned a domain-level rejection.
} catch (error: AnimeVostException) {
    // Network, HTTP, auth, parsing, or server-level SDK failure.
}
```

## Architecture

The SDK is intentionally small:

```text
AnimeVostClient
  config      base URL and user agent
  http        OkHttp-backed transport and cookie storage
  parser      Jsoup and Gson parsers for AnimeVost HTML and ajax responses
  model       immutable Kotlin data models
  error       typed SDK exceptions
```

Important design choices:

- No backend. Requests go from the client to AnimeVost.
- No global singleton. You can create multiple clients with different cookie stores.
- Parsers are isolated and covered by tests.
- Site-specific DLE behavior is kept in the SDK, not in app UI code.
- Comment quotes are structured recursively, while comments themselves remain a flat list with a computed `depth`.

## Testing

Run the full test suite:

```bash
./gradlew :animevost-sdk:test --no-daemon --max-workers=1
```

Run focused tests:

```bash
./gradlew :animevost-sdk:test --tests com.animevost.sdk.AnimeVostCommentsClientTest
./gradlew :animevost-sdk:test --tests com.animevost.sdk.AnimeVostRatingClientTest
./gradlew :animevost-sdk:test --tests com.animevost.sdk.parser.CommentsParserTest
```

The regular unit tests do not need a real AnimeVost account. Live checks should be run manually and should use environment variables for credentials.

Suggested names:

```bash
export ANIMEVOST_USERNAME="your_username"
export ANIMEVOST_PASSWORD="your_password"
```

Do not commit credentials, cookies, live HTML dumps, or test account data.

## Current Scope

Supported:

- content discovery
- details and playback metadata
- authentication
- registration and activation flow
- profile
- favorites
- comments
- comment reply templates
- comment reporting
- own comment deletion
- rating votes

Not supported yet:

- Maven Central publishing
- GitHub Actions CI
- live contract test task
- comment editing for regular users, because AnimeVost does not expose this action in the observed regular-user HTML
- private messages
- admin or moderator-only actions

## Compatibility Notes

AnimeVost is not a formal public API. HTML classes, DLE ajax endpoints, and mirror behavior can change. The SDK keeps this risk isolated in parser and client layers, but downstream apps should still treat network results as nullable where the model allows it.

For production use, keep the SDK updated and prefer graceful UI states for missing optional fields.

## Project Status

Recommended first app integration version: `0.1.0`.

Before a stable `1.0.0`, the project should add:

- published artifacts
- CI
- live contract tests behind explicit environment variables
- API documentation generated from KDoc
- a declared license

## Legal

This project is unofficial and is not affiliated with AnimeVost.org.

Use it responsibly and respect AnimeVost terms, user accounts, rate limits, and content rules. This SDK does not host, mirror, proxy, or redistribute AnimeVost content.
