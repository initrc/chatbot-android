# Speech To Text Implementation Plan

Goal: let users talk to the AI from the chat composer using Groq's
`whisper-large-v3-turbo` transcription model.

References:

- Groq Speech to Text: https://console.groq.com/docs/speech-to-text
- Groq API Reference: https://console.groq.com/docs/api-reference
- Android MediaRecorder overview: https://developer.android.com/media/platform/mediarecorder
- Android runtime permissions: https://developer.android.com/training/permissions/requesting

## 1. Add Permission And Assets

- Add `android.permission.RECORD_AUDIO` to `app/src/main/AndroidManifest.xml`.
- Add Android vector drawable resources for:
  - `mic_24`
  - `stop_circle_24`
- Reuse the existing `send_24` drawable for the speech-mode send action.
- Keep these as `res/drawable/*.xml` resources so Compose can load them with
  `painterResource`.

## 2. Add Audio Recording Infrastructure

- Create a focused recorder class, for example `AudioRecorder`.
- Use `MediaRecorder` to capture microphone input into the app cache directory.
- Record as `.m4a` using `MediaRecorder.OutputFormat.MPEG_4` and
  `MediaRecorder.AudioEncoder.AAC`, which Groq supports.
- Track amplitude with `MediaRecorder.maxAmplitude` on a coroutine timer for
  the waveform UI.
- Release recorder resources on stop, cancellation, errors, and ViewModel
  cleanup.

## 3. Add Groq Transcription API Layer

- Add a dedicated speech-to-text service/data source separate from chat
  streaming, for example `SpeechToTextService` and
  `SpeechToTextRemoteDataSource`.
- Reuse `SettingsLocalDataSource` for the API key and base URL.
- Send a multipart `POST` to `${baseUrl}/audio/transcriptions` with:
  - `file`: recorded `.m4a`
  - `model`: `whisper-large-v3-turbo`
  - `response_format`: `json`
  - `temperature`: `0`
- Parse the JSON response field `text`.
- Never log API keys or audio contents.

## 4. Add Speech State Ownership

- Prefer a separate `@HiltViewModel`, such as `SpeechToTextViewModel`, because
  recording/transcription is independent from chat persistence and streaming.
- Expose state such as:
  - `Idle`
  - `Recording(amplitudes, durationMillis)`
  - `Transcribing`
  - `Error(message)`
- Emit one-shot results like `SpeechResult(text, autoSend)` through a
  `SharedFlow`.

## 5. Hoist Composer Draft Text

- `SendView` currently owns its own draft text with `rememberSaveable`.
- Move draft text ownership up into `ChatScreenContent`, then pass `text` and
  `onTextChange` down into `SendView`.
- This lets transcription write into the `TextField` and lets the voice send
  path reuse the existing `onSendClick`.

## 6. Update SendView In-Place Speech UI

- Add the microphone icon immediately before the existing send button.
- Tapping mic checks or requests `RECORD_AUDIO`, then starts recording.
- When speech mode is active, keep rendering inside the existing `SendView`
  `Row` so the current rounded composer shape provides the pill container.
- Hide the `TextField` while speech mode is active.
- In the same `Row`, show:
  - waveform indicator
  - stop button using `stop_circle_24`
  - send button reusing the existing send button styling and `send_24` icon
- Stop button behavior:
  - stop recording
  - dismiss speech mode and return `SendView` to the text composer UI
  - transcribe audio
  - place the transcript into the `TextField`
- Voice send button behavior:
  - stop recording
  - dismiss speech mode and return `SendView` to the text composer UI
  - transcribe audio
  - place the transcript into the `TextField`
  - automatically call `ChatViewModel.onSendClick(transcript, currentModel)` if
    the transcript is nonblank and chat is idle

## 7. Handle Permission And Error UX

- Request microphone permission only after the user taps the mic.
- If permission is denied, keep text chat fully usable and surface a snackbar or
  compact error.
- Disable mic while chat is busy, recording is active, or transcription is in
  flight.
- Clean up active recording if the screen leaves composition.

## 8. Test And Verify

- Add JVM tests for transcription response parsing and request construction
  where the data source is injectable.
- Add focused UI coverage for `SendView` recording-state transitions if
  practical.
- Run `./gradlew :app:testDebugUnitTest`.
- Manually test microphone recording on a physical Android device because
  emulator audio capture is not reliable for this feature.
