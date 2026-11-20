---
"posthog-kmp": minor
---

- Add `SessionRecordingConfig.iosCaptureMethod` to choose how iOS captures session replay screenshots
- **Breaking:** capture iOS replay screenshots with the experimental `GPU` method by default, which doesn't capture web view pages or Metal content — set `iosCaptureMethod = IosCaptureMethod.STANDARD` to keep the previous capture
