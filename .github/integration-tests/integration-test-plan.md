# Integration Test Plan

## Scope
- Validate scheduled post publishing through the real application flow.
- Cover the two current publishing targets: Facebook Pages and TikTok.
- Keep the test focused on the backend path that moves data from the database through the Quartz job into the publisher layer.

## Layer 1 Strategy
- Use TestContainers with PostgreSQL so repository and transaction behavior match production more closely.
- Use the application's own `AutoPostQuartzJob` and repositories instead of calling publisher classes directly.
- Use a local HTTP server to simulate platform publish responses while still exercising the application's request-building logic.
- Cover one successful publish path and one failure path for behavior validation.

## Components Covered
- `AutoPostQuartzJob`
- `ScheduledPostRepository`
- `SocialAccountRepository`
- `UserRepository`
- `TokenCryptoService`
- `MediaPreparationService`
- `PlatformApiService`
- `MetaSocialPostPublisher`
- `TikTokSocialPostPublisher`

## Validation Criteria
- Facebook posts should transition from `PENDING` to `POSTED` and store a published post id.
- TikTok posts should transition from `PENDING` to `POSTED` and store the returned publish id.
- Unsupported media should fail the job and persist an error message.
- Platform errors should fail the job and persist a failure state.

## Execution
- Run the Layer 1 test class with the `Layer1` tag.
- Verify the default Maven test discovery still includes the integration test class.