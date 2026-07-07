# AnimeVost Kotlin SDK

Unofficial Kotlin/JVM SDK for AnimeVost.org.

The SDK talks to AnimeVost directly and parses site responses locally. No custom backend, proxy, database, queue, or paid infrastructure is required.

> Status: alpha. Ready for app integration, but the API may still change before `1.0.0`.

## Features

- catalog, search, schedule, navigation
- anime details, episodes, video sources
- login, logout, registration, email activation
- profile read and update
- favorites
- comments, replies, reports, own comment deletion
- rating votes
- typed SDK errors

## Requirements

- JDK 17
- Kotlin/JVM
- Gradle 8.9 or the included wrapper

## Installation

Artifacts are not published yet. Use the SDK as a source dependency.

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

## Usage

```kotlin
import com.animevost.sdk.AnimeVostClient

val client = AnimeVostClient()

val page = client.getAnimeList()
val anime = page.items.first()

val details = client.getAnimeDetails(anime.url)
val sources = details.episodes.firstOrNull()
    ?.let { client.getVideoSources(it.videoId) }
    .orEmpty()
```

### Auth

```kotlin
client.login(username, password)

val profile = client.getProfile()
val favorites = client.getFavorites()
```

### Comments

```kotlin
val comments = client.getComments(anime.url)

client.addComment(
    newsId = comments.newsId ?: error("Missing news id"),
    text = "Looks good so far.",
)

val quote = client.getCommentReplyTemplate(commentId = comments.comments.first().id)
```

### Rating

```kotlin
val vote = client.voteAnime(newsId = 3970, rating = 5)
println("${vote.rating} from ${vote.voteCount} votes")
```

## API Overview

```kotlin
getAnimeList(page, filter)
searchAnime(query, page)
getSchedule()
getNavigation()
getRandomAnime()
getAnimeDetails(url)
getVideoSources(videoId)

login(username, password)
logout()
register(request)
activateRegistration(url)

getProfile(username)
updateProfile(username, update)

getFavorites(page)
addFavorite(newsId)
removeFavorite(newsId)

getComments(url, page)
getComments(newsId, page)
addComment(newsId, text)
getCommentReplyTemplate(commentId)
reportComment(commentId, text)
deleteComment(commentId)

voteAnime(newsId, rating)
```

## Errors

All SDK errors inherit from `AnimeVostException`.

```kotlin
AnimeVostNetworkException
AnimeVostHttpException
AnimeVostAuthException
AnimeVostRegistrationException
AnimeVostValidationException
AnimeVostServerException
AnimeVostCaptchaException
AnimeVostRateLimitException
```

## Testing

```bash
./gradlew :animevost-sdk:test
```

Live checks should use environment variables and must not commit credentials or cookies.

```bash
export ANIMEVOST_USERNAME="username"
export ANIMEVOST_PASSWORD="password"
```

## Notes

AnimeVost is a website, not a public API. HTML and DLE ajax behavior can change, so parser updates may be needed over time.

This project is unofficial and is not affiliated with AnimeVost.org.
