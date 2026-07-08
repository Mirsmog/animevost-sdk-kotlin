# Response Fields

Nullable fields are marked with `?`.

## API Returns

| Method | Returns |
| --- | --- |
| `getAnimeList(page, filter)` | `AnimePage` |
| `searchAnime(query, page)` | `AnimePage` |
| `getFavorites(page)` | `AnimePage` |
| `getSchedule()` | `List<ScheduleDay>` |
| `getNavigation()` | `NavigationData` |
| `getRandomAnime()` | `AnimePreview?` |
| `getAnimeDetails(url)` | `AnimeDetails` |
| `getVideoSources(videoId)` | `List<VideoSource>` |
| `login(username, password)` | `AuthSession` |
| `currentSession()` | `AuthSession?` |
| `register(request)` | `RegistrationResult` |
| `activateRegistration(url)` | `RegistrationActivationResult` |
| `getProfile(username)` | `UserProfile` |
| `updateProfile(username, update)` | `UserProfile` |
| `addFavorite(newsId)` | `FavoriteActionResult` |
| `removeFavorite(newsId)` | `FavoriteActionResult` |
| `getComments(url, page)` | `CommentPage` |
| `getComments(newsId, page)` | `CommentPage` |
| `addComment(newsId, text)` | `CommentSubmissionResult` |
| `getCommentReplyTemplate(commentId)` | `CommentReplyTemplate` |
| `reportComment(commentId, text)` | `CommentActionResult` |
| `deleteComment(commentId)` | `CommentActionResult` |
| `voteAnime(newsId, rating)` | `RatingVoteResult` |

## Anime List

### `AnimePage`

| Field | Type | Description |
| --- | --- | --- |
| `items` | `List<AnimePreview>` | Page items. |
| `currentPage` | `Int` | Current page number. |
| `totalPages` | `Int` | Parsed total page count. |

### `AnimePreview`

| Field | Type | Description |
| --- | --- | --- |
| `id` | `Int` | AnimeVost news id. |
| `title` | `String` | Russian title. |
| `originalTitle` | `String?` | Original title when present. |
| `episodeInfo` | `String?` | Episode label from title. |
| `url` | `String` | Absolute page URL. |
| `posterUrl` | `String?` | Poster URL. |
| `publishedDate` | `String?` | Site date label. |
| `viewCount` | `Int?` | Views count. |
| `commentCount` | `Int?` | Comments count. |
| `rating` | `Double?` | Rating from 0 to 5. |
| `voteCount` | `Int?` | Votes count. |
| `categories` | `List<AnimeCategory>` | Linked categories. |

### `AnimeCategory`

| Field | Type | Description |
| --- | --- | --- |
| `title` | `String` | Category title. |
| `url` | `String` | Absolute category URL. |

## Anime Details

### `AnimeDetails`

| Field | Type | Description |
| --- | --- | --- |
| `id` | `Int` | AnimeVost news id. |
| `url` | `String` | Absolute page URL. |
| `title` | `String` | Russian title. |
| `originalTitle` | `String?` | Original title. |
| `episodeInfo` | `String?` | Episode label from title. |
| `alternativeTitle` | `String?` | Extra title from details page. |
| `posterUrl` | `String?` | Poster URL. |
| `publishedDate` | `String?` | Site date label. |
| `viewCount` | `Int?` | Views count. |
| `commentCount` | `Int?` | Comments count. |
| `year` | `String?` | Release year label. |
| `genres` | `List<String>` | Parsed genres. |
| `type` | `String?` | Anime type label. |
| `episodeCount` | `String?` | Episode count label. |
| `director` | `String?` | Director label. |
| `rating` | `Double?` | Rating from 0 to 5. |
| `voteCount` | `Int?` | Votes count. |
| `description` | `String?` | Description text. |
| `categories` | `List<AnimeCategory>` | Linked categories. |
| `relatedSeries` | `List<RelatedSeries>` | Related titles. |
| `episodes` | `List<AnimeEpisode>` | Episodes with video ids. |

### `RelatedSeries`

| Field | Type | Description |
| --- | --- | --- |
| `title` | `String` | Related title. |
| `url` | `String` | Absolute related page URL. |
| `description` | `String?` | Relation label. |

### `AnimeEpisode`

| Field | Type | Description |
| --- | --- | --- |
| `name` | `String` | Episode name. |
| `videoId` | `String` | AnimeVost video id. |
| `number` | `Int?` | Parsed episode number. |
| `thumbnailUrl` | `String?` | Generated thumbnail URL. |

### `VideoSource`

| Field | Type | Description |
| --- | --- | --- |
| `quality` | `String` | Quality label, for example `SD (480p)`. |
| `url` | `String` | Playback URL. |
| `downloadUrl` | `String?` | Direct download URL when available. |
| `host` | `String?` | Source host when known. |

## Catalog

### `NavigationData`

| Field | Type | Description |
| --- | --- | --- |
| `genres` | `List<CatalogLink>` | Genre links. |
| `types` | `List<CatalogLink>` | Type links. |
| `years` | `List<CatalogLink>` | Year links. |
| `sections` | `List<CatalogLink>` | Main section links. |

### `CatalogLink`

| Field | Type | Description |
| --- | --- | --- |
| `title` | `String` | Link title. |
| `url` | `String` | Absolute URL. |
| `path` | `String` | Relative path for `CatalogFilter`. |

### `CatalogFilter`

| Field | Type | Description |
| --- | --- | --- |
| `path` | `String?` | Catalog path, for example `tip/tv/`. |
| `sortBy` | `CatalogSort` | DLE sort field. |
| `sortAscending` | `Boolean` | `true` for ascending order. |

### `CatalogSort`

| Value | DLE field |
| --- | --- |
| `DATE` | `date` |
| `RATING` | `rating` |
| `VIEWS` | `news_read` |
| `COMMENTS` | `comm_num` |
| `TITLE` | `title` |

## Schedule

### `ScheduleDay`

| Field | Type | Description |
| --- | --- | --- |
| `weekday` | `Weekday` | Weekday enum. |
| `entries` | `List<ScheduleEntry>` | Scheduled titles. |

### `ScheduleEntry`

| Field | Type | Description |
| --- | --- | --- |
| `title` | `String` | Anime title. |
| `url` | `String` | Absolute page URL. |
| `timeLabel` | `String?` | Site time label. |

## Account

### `AuthSession`

| Field | Type | Description |
| --- | --- | --- |
| `userId` | `Int` | DLE user id. |
| `username` | `String?` | Username when known. |

### `RegistrationRequest`

| Field | Type | Description |
| --- | --- | --- |
| `username` | `String` | New username. |
| `password` | `String` | New password. |
| `email` | `String` | Email address. |

### `RegistrationResult`

| Field | Type | Description |
| --- | --- | --- |
| `username` | `String` | Registered username. |
| `status` | `RegistrationStatus` | `ACTIVE` or `PENDING_EMAIL_ACTIVATION`. |
| `session` | `AuthSession?` | Session if activation was not required. |

### `RegistrationActivationResult`

| Field | Type | Description |
| --- | --- | --- |
| `activated` | `Boolean` | Whether the activation page looked successful. |

### `UserProfile`

| Field | Type | Description |
| --- | --- | --- |
| `userId` | `Int?` | DLE user id. |
| `username` | `String` | Username. |
| `avatarUrl` | `String?` | Avatar URL. |
| `allowHash` | `String?` | DLE hash for profile edits. |
| `fullName` | `String?` | Profile full name. |
| `location` | `String?` | Profile location. |
| `email` | `String?` | Email shown on editable profile. |
| `info` | `String?` | Profile info text. |
| `canEdit` | `Boolean` | True when edit fields are available. |

### `UserProfileUpdate`

| Field | Type | Description |
| --- | --- | --- |
| `fullName` | `String?` | New full name. |
| `location` | `String?` | New location. |
| `email` | `String?` | New email. |
| `info` | `String?` | New profile info. |

## Favorites and Rating

### `FavoriteActionResult`

| Field | Type | Description |
| --- | --- | --- |
| `newsId` | `Int` | AnimeVost news id. |
| `isFavorite` | `Boolean` | New favorite state. |

### `RatingVoteResult`

| Field | Type | Description |
| --- | --- | --- |
| `newsId` | `Int` | AnimeVost news id. |
| `submittedRating` | `Int` | Submitted value from 1 to 5. |
| `rating` | `Double?` | Updated rating from 0 to 5. |
| `voteCount` | `Int?` | Updated votes count. |
| `ratingHtml` | `String?` | Raw updated rating HTML. |
| `success` | `Boolean` | Server success flag. |

## Comments

### `CommentPage`

| Field | Type | Description |
| --- | --- | --- |
| `newsId` | `Int?` | AnimeVost news id. |
| `allowHash` | `String?` | DLE hash used for delete actions. |
| `comments` | `List<AnimeComment>` | Comments list. |
| `currentPage` | `Int` | Current comments page. |
| `totalPages` | `Int?` | Total comments pages when known. |

### `AnimeComment`

| Field | Type | Description |
| --- | --- | --- |
| `id` | `Int` | Comment id. |
| `author` | `CommentAuthor` | Author data. |
| `avatarUrl` | `String?` | Author avatar URL. |
| `userGroup` | `String?` | Site user group label. |
| `createdAtLabel` | `String?` | Site date label. |
| `authorCommentCount` | `Int?` | Author comment count shown by site. |
| `ordinal` | `Int?` | Comment number in thread. |
| `bodyHtml` | `String` | Comment body without top-level quote blocks. |
| `bodyText` | `String` | Plain body text without top-level quote blocks. |
| `quotes` | `List<CommentQuote>` | Parsed DLE quote tree. |
| `indentLevel` | `Int?` | Raw `commentContent_*` value. |
| `depth` | `Int` | Normalized visual depth. |
| `isOnline` | `Boolean?` | Author online status. |
| `actions` | `Set<CommentAction>` | Actions visible for this comment. |

### `CommentAuthor`

| Field | Type | Description |
| --- | --- | --- |
| `name` | `String` | Display name. |
| `profileUrl` | `String?` | Absolute profile URL. |

### `CommentQuote`

| Field | Type | Description |
| --- | --- | --- |
| `authorName` | `String?` | Quoted author. |
| `bodyHtml` | `String` | Quote HTML without nested quote blocks. |
| `bodyText` | `String` | Quote text without nested quote blocks. |
| `quotes` | `List<CommentQuote>` | Nested quotes. |

### `CommentAction`

| Value | Meaning |
| --- | --- |
| `REPLY` | Reply template can be requested. |
| `REPORT` | Comment can be reported. |
| `DELETE` | Comment can be deleted by current user. |
| `EDIT` | Edit action is visible in HTML. |

### `CommentSubmissionResult`

| Field | Type | Description |
| --- | --- | --- |
| `newsId` | `Int` | AnimeVost news id. |
| `comments` | `List<AnimeComment>` | Comments returned after submission. |
| `rawMessage` | `String?` | Raw server message if no comments were returned. |

### `CommentActionResult`

| Field | Type | Description |
| --- | --- | --- |
| `commentId` | `Int` | Comment id. |
| `success` | `Boolean` | Whether the action succeeded. |
| `message` | `String?` | Server message when available. |

### `CommentReplyTemplate`

| Field | Type | Description |
| --- | --- | --- |
| `commentId` | `Int` | Source comment id. |
| `markup` | `String` | DLE quote markup. |
